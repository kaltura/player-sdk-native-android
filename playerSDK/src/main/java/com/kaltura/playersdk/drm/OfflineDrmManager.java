package com.kaltura.playersdk.drm;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.NotProvisionedException;
import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.google.android.exoplayer.drm.DrmInitData;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.extractor.mp4.PsshAtomUtil;
import com.kaltura.playersdk.ImpossibleException;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil.WIDEVINE_UUID;
import static com.kaltura.playersdk.helpers.KStringUtilities.toHexString;

/**
 * Created by noamt on 20/04/2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OfflineDrmManager {

    private static final String TAG = "OfflineDrmManager";

    
    public static DrmSessionManager getSessionManager(Context context) {
        try {
            return new SessionManager(context);
        } catch (UnsupportedDrmException e) {
            throw new ImpossibleException("Widevine must be supported", e);
        }
    }
    
    public static KeySetStorage getStorage(Context context) {
        return new KeySetStorage(context);
    }

    
    private static class SessionManager implements DrmSessionManager {

        private final MediaDrm mMediaDrm;
        private final KeySetStorage mStorage;
        private final HandlerThread mRequestHandlerThread;
        private MediaCrypto mMediaCrypto;
        private byte[] mSessionId;
        private int mState = STATE_CLOSED;
        private Exception mLastError;
        private AtomicInteger mOpenCount = new AtomicInteger(0);
        private DrmInitData mDrmInitData;

        void printAllProperties() {
            String[] stringProps = {MediaDrm.PROPERTY_VENDOR, MediaDrm.PROPERTY_VERSION, MediaDrm.PROPERTY_DESCRIPTION, MediaDrm.PROPERTY_ALGORITHMS, "securityLevel", "systemId", "privacyMode", "sessionSharing", "usageReportingSupport", "appId", "origin", "hdcpLevel", "maxHdcpLevel", "maxNumberOfSessions", "numberOfOpenSessions"};
            String[] byteArrayProps = {MediaDrm.PROPERTY_DEVICE_UNIQUE_ID, "provisioningUniqueId", "serviceCertificate"};

            Map<String, String> map = new LinkedHashMap<>();
            
            for (String prop : stringProps) {
                try {
                    map.put(prop, mMediaDrm.getPropertyString(prop));
                } catch (Exception e) {
                    Log.d(TAG, "Invalid property " + prop);
                }
            }
            for (String prop : byteArrayProps) {
                try {
                    map.put(prop, Base64.encodeToString(mMediaDrm.getPropertyByteArray(prop), Base64.NO_WRAP));
                } catch (Exception e) {
                    Log.d(TAG, "Invalid property " + prop);
                }
            }
            
            Log.d(TAG, "MediaDrm properties: " + map);
        }

        SessionManager(Context context) throws UnsupportedDrmException {
            
            mStorage = new KeySetStorage(context);
            mRequestHandlerThread = new HandlerThread("DrmRequestHandler");
            mRequestHandlerThread.start();

            try {
                mMediaDrm = new MediaDrm(WIDEVINE_UUID);
                printAllProperties();

                mMediaDrm.setOnEventListener(new MediaDrm.OnEventListener() {
                    @Override
                    public void onEvent(MediaDrm md, byte[] sessionId, int event, int extra, byte[] data) {
                        Log.d(TAG, "onEvent:" + toHexString(sessionId) + ":" + event + ":" + extra + ":" + toHexString(data));
                    }
                });


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    mMediaDrm.setOnKeyStatusChangeListener(new MediaDrm.OnKeyStatusChangeListener() {
                        @Override
                        public void onKeyStatusChange(@NonNull MediaDrm md, @NonNull byte[] sessionId, @NonNull List<MediaDrm.KeyStatus> keyInformation, boolean hasNewUsableKey) {
                            Log.d(TAG, "onKeyStatusChange:" + toHexString(sessionId) + ":" + hasNewUsableKey);
                            logKeyInformation(keyInformation);
                        }
                    }, null);
                }
                

            } catch (UnsupportedSchemeException e) {
                throw new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME, e);
            }
        }
        
        void onError(Exception error) {
            mLastError = error;
            mState = STATE_ERROR;
        }
        
        @TargetApi(Build.VERSION_CODES.M)
        void logKeyInformation(List<MediaDrm.KeyStatus> keyInformation) {
            LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
            for (MediaDrm.KeyStatus keyStatus : keyInformation) {
                map.put(toHexString(keyStatus.getKeyId()), keyStatus.getStatusCode());
            }
            Log.d(TAG, "keyInformation:" + map);
        }

        @Override
        public void open(DrmInitData drmInitData) {
            
            if (mOpenCount.incrementAndGet() != 1) {
                return;
            }

            mDrmInitData = drmInitData;

            mState = STATE_OPENING;
            
            new Handler(mRequestHandlerThread.getLooper()).post(new Runnable() {
                @Override
                public void run() {
                    openInternal();
                }
            });
        }

        private void openInternal() {
            DrmInitData.SchemeInitData schemeInitData = mDrmInitData.get(WIDEVINE_UUID);
            if (schemeInitData == null) {
                onError(new IllegalStateException("Media does not support Widevine"));
                return;
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // Prior to L the Widevine CDM required data to be extracted from the PSSH atom.
                byte[] psshData = PsshAtomUtil.parseSchemeSpecificData(schemeInitData.data, WIDEVINE_UUID);
                if (psshData == null) {
                    // Extraction failed. schemeData isn't a Widevine PSSH atom, so leave it unchanged.
                } else {
                    schemeInitData = new DrmInitData.SchemeInitData(schemeInitData.mimeType, psshData);
                }
            }

            try {
                mSessionId = mMediaDrm.openSession();
                
                mMediaCrypto = new MediaCrypto(WIDEVINE_UUID, mSessionId);
                
                mState = STATE_OPENED;
                
                byte[] keySetId = mStorage.loadKeySetId(schemeInitData.data);
                mMediaDrm.restoreKeys(mSessionId, keySetId);

                HashMap<String, String> keyStatus = mMediaDrm.queryKeyStatus(mSessionId);
                Log.d(TAG, "keyStatus: " + keyStatus);

                mState = STATE_OPENED_WITH_KEYS;

            } catch (NotProvisionedException e) {
                throw new ImpossibleException("Widevine must be provisioned", e);
            } catch (MediaCryptoException e) {
                Log.e(TAG, "Can't create MediaCrypto for offline Widevine playback", e);
                onError(e);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                onError(e);
            }
        }

        @Override
        public void close() {
            
            if (mOpenCount.decrementAndGet() != 0) {
                return;
            }
            
            mMediaCrypto.release();
            mMediaCrypto = null;
            
            mLastError = null;
            if (mSessionId != null) {
                mMediaDrm.closeSession(mSessionId);
                mSessionId = null;
            }

            mState = STATE_CLOSED;
            
            mDrmInitData = null;
        }

        @Override
        public int getState() {
            return mState;
        }

        @Override
        public MediaCrypto getMediaCrypto() {
            return mMediaCrypto;
        }

        @Override
        public boolean requiresSecureDecoderComponent(String mimeType) {
            return mMediaCrypto.requiresSecureDecoderComponent(mimeType);
        }

        @Override
        public Exception getError() {
            return mLastError;
        }
    }

    public static class KeySetStorage {

        private static final String SHARED_PREFS_NAME = "OfflineDrmStore";
        private final SharedPreferences mSettings;

        KeySetStorage(Context context) {
            mSettings = context.getSharedPreferences(SHARED_PREFS_NAME, 0);
        }

        public void storeKeySetId(byte[] initData, byte[] keySetId) {
            String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
            String encodedKeySetId = Base64.encodeToString(keySetId, Base64.NO_WRAP);
            mSettings.edit()
                    .putString(encodedInitData, encodedKeySetId)
                    .apply();
        }

        public byte[] loadKeySetId(byte[] initData) throws FileNotFoundException {
            String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
            String encodedKeySetId = mSettings.getString(encodedInitData, null);

            if (encodedKeySetId == null) {
                throw new FileNotFoundException("Can't load keySetId");
            }

            return Base64.decode(encodedKeySetId, 0);
        }

        public void removeKeySetId(byte[] initData) {
            String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
            mSettings.edit()
                    .remove(encodedInitData)
                    .apply();
        }
    }
} 
