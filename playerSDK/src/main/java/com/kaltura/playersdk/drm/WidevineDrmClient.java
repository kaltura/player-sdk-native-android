
package com.kaltura.playersdk.drm;

import android.content.ContentValues;
import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfo;
import android.drm.DrmInfoEvent;
import android.drm.DrmInfoRequest;
import android.drm.DrmInfoStatus;
import android.drm.DrmManagerClient;
import android.drm.DrmStore;
import android.os.Build;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;
import static com.kaltura.playersdk.utils.LogUtils.LOGI;

// Based on Widevine for Android demo app

public class WidevineDrmClient {
    
    public static final String TAG = "WidevineDrm";

    private final static String DEVICE_IS_PROVISIONED = "0";
    private final static String DEVICE_IS_NOT_PROVISIONED = "1";
    private final static String DEVICE_IS_PROVISIONED_SD_ONLY = "2";
    
    public static final String WV_DRM_SERVER_KEY = "WVDRMServerKey";
    public static final String WV_ASSET_URI_KEY = "WVAssetURIKey";
    public static final String WV_DEVICE_ID_KEY = "WVDeviceIDKey";
    public static final String WV_PORTAL_KEY = "WVPortalKey";
    public static final String WV_DRM_INFO_REQUEST_STATUS_KEY = "WVDrmInfoRequestStatusKey";
    public static final String WV_DRM_INFO_REQUEST_VERSION_KEY = "WVDrmInfoRequestVersionKey";
    
    private String mWVDrmInfoRequestStatusKey = DEVICE_IS_PROVISIONED;
    public static String WIDEVINE_MIME_TYPE = "video/wvm";
    public static String PORTAL_NAME = "kaltura";

    private String mDeviceId;
    private DrmManagerClient mDrmManager;

    public void setEventListener(EventListener eventListener) {
        mEventListener = eventListener;
    }

    private EventListener mEventListener;
    
    public static class RightsInfo {
        
        public enum Status {
            VALID,
            INVALID,
            EXPIRED,
            NOT_ACQUIRED,
        }
        
        public Status status;
        
        public int startTime;
        public int expiryTime;
        public int availableTime;
        
        public ContentValues rawConstraints;
        
        private RightsInfo(int status, ContentValues values) {
            this.rawConstraints = values;

            switch (status) {
                case DrmStore.RightsStatus.RIGHTS_VALID:
                    this.status = Status.VALID;
                    if (values != null) {
                        try {
                            this.startTime = values.getAsInteger(DrmStore.ConstraintsColumns.LICENSE_START_TIME);
                            this.expiryTime = values.getAsInteger(DrmStore.ConstraintsColumns.LICENSE_EXPIRY_TIME);
                            this.availableTime = values.getAsInteger(DrmStore.ConstraintsColumns.LICENSE_AVAILABLE_TIME);
                        } catch (NullPointerException e) {
                            LOGE(TAG, "Invalid constraints: " + values);
                        }
                    }
                    break;

                case DrmStore.RightsStatus.RIGHTS_INVALID:
                    this.status = Status.INVALID;
                    break;

                case DrmStore.RightsStatus.RIGHTS_EXPIRED:
                    this.status = Status.EXPIRED;
                    break;

                case DrmStore.RightsStatus.RIGHTS_NOT_ACQUIRED:
                    this.status = Status.NOT_ACQUIRED;
                    break;
            }
            
        }
    }

    public interface EventListener {
        void onError(DrmErrorEvent event);
        void onEvent(DrmEvent event);
    }

    public static boolean isSupported(Context context) {
        DrmManagerClient drmManagerClient = new DrmManagerClient(context);
        boolean canHandle = false;
        // adding try catch due some android devices have different canHandle method implementation regarding the arguments validation inside it
        try {
            canHandle = drmManagerClient.canHandle("", WIDEVINE_MIME_TYPE);
        } catch (IllegalArgumentException ex) {
            LOGE(TAG, "drmManagerClient.canHandle failed");
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                LOGI(TAG, "Assuming WV Classic is supported although canHandle has failed");
                canHandle = true;
            }
        } finally {
            drmManagerClient.release();
        }
        return canHandle;
    }

    public WidevineDrmClient(Context context) {

        mDrmManager = new DrmManagerClient(context) {
            @Override
            protected void finalize() throws Throwable {
                // Release on finalize. Doesn't matter when, just prevent Android's CloseGuard errors.
                try {
                    release();
                } finally {
                    super.finalize();
                }
            }
        };

        // Detect if this device can play widevine classic
        if (! mDrmManager.canHandle("", WIDEVINE_MIME_TYPE)) {
            throw new UnsupportedOperationException("Widevine Classic is not supported");
        }
        
        mDeviceId = new DeviceUuidFactory(context).getDeviceUuid().toString();

        mDrmManager.setOnInfoListener(new DrmManagerClient.OnInfoListener() {
            @Override
            public void onInfo(DrmManagerClient client, DrmInfoEvent event) {
                logEvent(event);
                if (mEventListener != null) {
                    mEventListener.onEvent(event);
                }
            }
        });

        mDrmManager.setOnEventListener(new DrmManagerClient.OnEventListener() {
            @Override
            public void onEvent(DrmManagerClient client, DrmEvent event) {
                logEvent(event);
                if (mEventListener != null) {
                    mEventListener.onEvent(event);
                }
            }
        });

        mDrmManager.setOnErrorListener(new DrmManagerClient.OnErrorListener() {
            @Override
            public void onError(DrmManagerClient client, DrmErrorEvent event) {
                logEvent(event);
                if (mEventListener != null) {
                    mEventListener.onError(event);
                }
            }
        });
        
        registerPortal();
    }
    

    private void logEvent(DrmEvent event) {
//		if (! BuildConfig.DEBUG) {
//			// Basic log
//			LOGD(TAG, "DrmEvent(" + event + ")");
//			return;
//		}
        String eventTypeString = null;
        String eventClass;
        // pbpaste | perl -ne 'if (/.+public static final int (\w+).+/) {print qq(case DrmInfoEvent.$1: eventTypeString="$1"; break;\n);}'
        int eventType = event.getType();
        if (event instanceof DrmInfoEvent) {
            eventClass = "info";
            switch (eventType) {
                case DrmInfoEvent.TYPE_ALREADY_REGISTERED_BY_ANOTHER_ACCOUNT: eventTypeString="TYPE_ALREADY_REGISTERED_BY_ANOTHER_ACCOUNT"; break;
                case DrmInfoEvent.TYPE_REMOVE_RIGHTS: eventTypeString="TYPE_REMOVE_RIGHTS"; break;
                case DrmInfoEvent.TYPE_RIGHTS_INSTALLED: eventTypeString="TYPE_RIGHTS_INSTALLED"; break;
                case DrmInfoEvent.TYPE_WAIT_FOR_RIGHTS: eventTypeString="TYPE_WAIT_FOR_RIGHTS"; break;
                case DrmInfoEvent.TYPE_ACCOUNT_ALREADY_REGISTERED: eventTypeString="TYPE_ACCOUNT_ALREADY_REGISTERED"; break;
                case DrmInfoEvent.TYPE_RIGHTS_REMOVED: eventTypeString="TYPE_RIGHTS_REMOVED"; break;
            }
        } else if (event instanceof DrmErrorEvent) {
            eventClass = "error";
            switch (eventType) {
                case DrmErrorEvent.TYPE_RIGHTS_NOT_INSTALLED: eventTypeString="TYPE_RIGHTS_NOT_INSTALLED"; break;
                case DrmErrorEvent.TYPE_RIGHTS_RENEWAL_NOT_ALLOWED: eventTypeString="TYPE_RIGHTS_RENEWAL_NOT_ALLOWED"; break;
                case DrmErrorEvent.TYPE_NOT_SUPPORTED: eventTypeString="TYPE_NOT_SUPPORTED"; break;
                case DrmErrorEvent.TYPE_OUT_OF_MEMORY: eventTypeString="TYPE_OUT_OF_MEMORY"; break;
                case DrmErrorEvent.TYPE_NO_INTERNET_CONNECTION: eventTypeString="TYPE_NO_INTERNET_CONNECTION"; break;
                case DrmErrorEvent.TYPE_PROCESS_DRM_INFO_FAILED: eventTypeString="TYPE_PROCESS_DRM_INFO_FAILED"; break;
                case DrmErrorEvent.TYPE_REMOVE_ALL_RIGHTS_FAILED: eventTypeString="TYPE_REMOVE_ALL_RIGHTS_FAILED"; break;
                case DrmErrorEvent.TYPE_ACQUIRE_DRM_INFO_FAILED: eventTypeString="TYPE_ACQUIRE_DRM_INFO_FAILED"; break;
            }
        } else {
            eventClass = "generic";
            switch (eventType) {
                case DrmEvent.TYPE_ALL_RIGHTS_REMOVED: eventTypeString="TYPE_ALL_RIGHTS_REMOVED"; break;
                case DrmEvent.TYPE_DRM_INFO_PROCESSED: eventTypeString="TYPE_DRM_INFO_PROCESSED"; break;
            }
        }

        StringBuilder logString = new StringBuilder(50);
        logString.append("DrmEvent class=").append(eventClass).append(" type=")
                .append(eventTypeString).append(" message={").append(event.getMessage()).append("}");

        DrmInfoStatus drmStatus = (DrmInfoStatus) event.getAttribute(DrmEvent.DRM_INFO_STATUS_OBJECT);
        if (drmStatus != null) {
            logString.append(" status=").append(drmStatus.statusCode==DrmInfoStatus.STATUS_OK ? "OK" : "ERROR");
        }

        DrmInfo drmInfo = (DrmInfo) event.getAttribute(DrmEvent.DRM_INFO_OBJECT);
        logString.append("info=").append(extractDrmInfo(drmInfo));

        LOGD(TAG, logString.toString());
    }

    private String extractDrmInfo(DrmInfo drmInfo) {
        StringBuilder sb = new StringBuilder();
        if (drmInfo != null) {
            sb.append("{");
            for (Iterator<String> it = drmInfo.keyIterator(); it.hasNext();) {
                String key = it.next();
                Object value = drmInfo.get(key);
                sb.append("{").append(key).append("=").append(value).append("}");
                if (it.hasNext()) {
                    sb.append(" ");
                }
            }
            sb.append("}");
        }
        return sb.toString();
    }

    private DrmInfoRequest createDrmInfoRequest(String assetUri, String licenseServerUri) {
        DrmInfoRequest rightsAcquisitionInfo;
        rightsAcquisitionInfo = new DrmInfoRequest(DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO,
                WIDEVINE_MIME_TYPE);

        if (licenseServerUri != null) {
            rightsAcquisitionInfo.put(WV_DRM_SERVER_KEY, licenseServerUri);
        }
        rightsAcquisitionInfo.put(WV_ASSET_URI_KEY, assetUri);
        rightsAcquisitionInfo.put(WV_DEVICE_ID_KEY, mDeviceId);
        rightsAcquisitionInfo.put(WV_PORTAL_KEY, PORTAL_NAME);

        return rightsAcquisitionInfo;
    }

    private DrmInfoRequest createDrmInfoRequest(String assetUri) {
        return createDrmInfoRequest(assetUri, null);
    }
    
    public void registerPortal() {

        String portal = PORTAL_NAME;
        DrmInfoRequest request = new DrmInfoRequest(DrmInfoRequest.TYPE_REGISTRATION_INFO,
                WIDEVINE_MIME_TYPE);
        request.put(WV_PORTAL_KEY, portal);
        DrmInfo response = mDrmManager.acquireDrmInfo(request);

        LOGI(TAG, "Widevine Plugin Info: " + extractDrmInfo(response));

        String drmInfoRequestStatusKey = (String)response.get(WV_DRM_INFO_REQUEST_STATUS_KEY);
        LOGI(TAG, "Widevine provision status: " + drmInfoRequestStatusKey);
    }

    /**
     * returns whether or not we should acquire rights for this url
     *
     * @param assetUri
     * @return
     */
    public boolean needToAcquireRights(String assetUri){
        mDrmManager.acquireDrmInfo(createDrmInfoRequest(assetUri));
        int rightsStatus = mDrmManager.checkRightsStatus(assetUri);
        if(rightsStatus == DrmStore.RightsStatus.RIGHTS_INVALID){
            mDrmManager.removeRights(assetUri); // clear current invalid rights and re-acquire new rights
        }
        return rightsStatus != DrmStore.RightsStatus.RIGHTS_VALID;
    }

    public int acquireRights(String assetUri, String licenseServerUri) {

        if (assetUri.startsWith("/")) {
            return acquireLocalAssetRights(assetUri, licenseServerUri);
        }
        
        DrmInfoRequest drmInfoRequest = createDrmInfoRequest(assetUri, licenseServerUri);

        DrmInfo drmInfo = mDrmManager.acquireDrmInfo(drmInfoRequest);
        if (drmInfo == null) {
            return DrmManagerClient.ERROR_UNKNOWN;
        }
        int rights = mDrmManager.processDrmInfo(drmInfo);

        logMessage("acquireRights = " + rights + "\n");

        return rights;
    }
    
    public int acquireLocalAssetRights(String assetPath, String licenseServerUri) {
        DrmInfoRequest drmInfoRequest = createDrmInfoRequest(assetPath, licenseServerUri);
        FileInputStream fis = null;

        int rights = 0;
        DrmInfo drmInfo;
        // A local file needs special treatment -- open and get FD
        try {
            fis = new FileInputStream(assetPath);
            FileDescriptor fd = fis.getFD();
            if (fd != null && fd.valid()) {
                drmInfoRequest.put("FileDescriptorKey", fd.toString());
                drmInfo = mDrmManager.acquireDrmInfo(drmInfoRequest);
                if (drmInfo == null) {
                    throw new IOException("DrmManagerClient couldn't prepare request for asset " + assetPath);
                }
                rights = mDrmManager.processDrmInfo(drmInfo);
            }
        } catch (java.io.IOException e) {
            LOGE(TAG, "Error opening local file:", e);
            rights = -1;
        } finally {
            safeClose(fis);
        }
        
        logMessage("acquireRights = " + rights + "\n");
        
        return rights;
    }
    
    private static void safeClose(FileInputStream fis) {
        if (fis != null) {
            try {
                fis.close();
            } catch (IOException e) {
                LOGE(TAG, "Failed to close file", e);
            }
        }
    }

    public RightsInfo getRightsInfo(String assetUri) {

        // Need to use acquireDrmInfo prior to calling checkRightsStatus
        mDrmManager.acquireDrmInfo(createDrmInfoRequest(assetUri));
        int status = mDrmManager.checkRightsStatus(assetUri);
        logMessage("getRightsInfo  = " + status + "\n");
        
        ContentValues values = mDrmManager.getConstraints(assetUri, DrmStore.Action.PLAY);
        
        return new RightsInfo(status, values);
    }
    
    public int removeRights(String assetUri) {

        // Need to use acquireDrmInfo prior to calling removeRights
        mDrmManager.acquireDrmInfo(createDrmInfoRequest(assetUri));
        int removeStatus = mDrmManager.removeRights(assetUri);
        logMessage("removeRights = " + removeStatus + "\n");

        return removeStatus;
    }

    public int removeAllRights() {
        int removeAllStatus = mDrmManager.removeAllRights();
        logMessage("removeAllRights = " + removeAllStatus + "\n");
        return removeAllStatus;
    }
    
    private void logMessage(String message) {
        LOGD(TAG, message);
    }
}
