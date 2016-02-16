
package com.kaltura.playersdk.widevine;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfo;
import android.drm.DrmInfoEvent;
import android.drm.DrmInfoRequest;
import android.drm.DrmInfoStatus;
import android.drm.DrmManagerClient;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

// Based on Widevine for Android demo app

public class WidevineDrmClient {
    
    public static final String TAG = "WidevineDrm";

    private final static long DEVICE_IS_PROVISIONED = 0;
    private final static long DEVICE_IS_NOT_PROVISIONED = 1;
    private final static long DEVICE_IS_PROVISIONED_SD_ONLY = 2;
    
    public static final String WV_DRM_SERVER_KEY = "WVDRMServerKey";
    public static final String WV_ASSET_URI_KEY = "WVAssetURIKey";
    public static final String WV_DEVICE_ID_KEY = "WVDeviceIDKey";
    public static final String WV_PORTAL_KEY = "WVPortalKey";
    public static final String WV_DRM_INFO_REQUEST_STATUS_KEY = "WVDrmInfoRequestStatusKey";
    public static final String WV_DRM_INFO_REQUEST_VERSION_KEY = "WVDrmInfoRequestVersionKey";
    
    private long mWVDrmInfoRequestStatusKey = DEVICE_IS_PROVISIONED;
    public static String WIDEVINE_MIME_TYPE = "video/wvm";
    public static String PORTAL_NAME = "kaltura";

    private String mDeviceId;
    private DrmManagerClient mDrmManager;

    public void setEventListener(EventListener eventListener) {
        mEventListener = eventListener;
    }

    private EventListener mEventListener;

    public interface EventListener {
        void onError(DrmErrorEvent event);
        void onEvent(DrmEvent event);
    }

    public static boolean isSupported(Context context) {
        DrmManagerClient drmManagerClient = new DrmManagerClient(context);
        boolean canHandle = drmManagerClient.canHandle("", WIDEVINE_MIME_TYPE);
        drmManagerClient.release();
        return canHandle;
    }
    
    
    public WidevineDrmClient(Context context) {

        mDrmManager = new DrmManagerClient(context);

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
//			Log.d(TAG, "DrmEvent(" + event + ")");
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
        if (drmInfo != null) {
            logString.append(" info={");
            for (Iterator<String> it = drmInfo.keyIterator(); it.hasNext();) {
                String key = it.next();
                Object value = drmInfo.get(key);
                logString.append("{").append(key).append("=").append(value).append("}");
                if (it.hasNext()) {
                    logString.append(" ");
                }
            }
            logString.append("}");
        }

        Log.d(TAG, logString.toString());
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
    
    public boolean isProvisionedDevice() {

        return ((mWVDrmInfoRequestStatusKey == DEVICE_IS_PROVISIONED) ||
                (mWVDrmInfoRequestStatusKey == DEVICE_IS_PROVISIONED_SD_ONLY));
    }

    public void registerPortal() {

        String portal = PORTAL_NAME;
        DrmInfoRequest request = new DrmInfoRequest(DrmInfoRequest.TYPE_REGISTRATION_INFO,
                WIDEVINE_MIME_TYPE);
        request.put(WV_PORTAL_KEY, portal);
        DrmInfo response = mDrmManager.acquireDrmInfo(request);

        String drmInfoRequestStatusKey = (String)response.get(WV_DRM_INFO_REQUEST_STATUS_KEY);
        if (null != drmInfoRequestStatusKey && !drmInfoRequestStatusKey.equals("")) {
            mWVDrmInfoRequestStatusKey = Long.parseLong(drmInfoRequestStatusKey);
        }

        String pluginVersion = (String) response.get(WV_DRM_INFO_REQUEST_VERSION_KEY);
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
            Log.e(TAG, "Error opening local file:", e);
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
                Log.e(TAG, "Failed to close file", e);
            }
        }
    }

    public int checkRightsStatus(String assetUri) {

        // Need to use acquireDrmInfo prior to calling checkRightsStatus
        mDrmManager.acquireDrmInfo(createDrmInfoRequest(assetUri));
        int status = mDrmManager.checkRightsStatus(assetUri);
        logMessage("checkRightsStatus  = " + status + "\n");

        return status;
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
        Log.d(TAG, message);
    }
}
