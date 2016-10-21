package com.kaltura.playersdk.drm;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.google.android.exoplayer.drm.DrmInitData;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.extractor.mp4.PsshAtomUtil;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil.WIDEVINE_UUID;

/**
 * Created by noamt on 20/04/2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OfflineDrmManager {

    private static final String TAG = "OfflineDrmManager";

    
    public static DrmSessionManager getSessionManager(Context context) {
        try {
            return new OfflineDrmSessionManager(getStorage(context));
        } catch (UnsupportedDrmException e) {
            throw new WidevineNotSupportedException(e);
        }
    }
    
    public static OfflineKeySetStorage getStorage(Context context) {
        return new OfflineKeySetStorage(context);
    }


    static void printAllProperties(MediaDrm mediaDrm) {
        String[] stringProps = {MediaDrm.PROPERTY_VENDOR, MediaDrm.PROPERTY_VERSION, MediaDrm.PROPERTY_DESCRIPTION, MediaDrm.PROPERTY_ALGORITHMS, "securityLevel", "systemId", "privacyMode", "sessionSharing", "usageReportingSupport", "appId", "origin", "hdcpLevel", "maxHdcpLevel", "maxNumberOfSessions", "numberOfOpenSessions"};
        String[] byteArrayProps = {MediaDrm.PROPERTY_DEVICE_UNIQUE_ID, "provisioningUniqueId", "serviceCertificate"};

        Map<String, String> map = new LinkedHashMap<>();

        for (String prop : stringProps) {
            try {
                map.put(prop, mediaDrm.getPropertyString(prop));
            } catch (Exception e) {
                Log.d(TAG, "Invalid property " + prop);
            }
        }
        for (String prop : byteArrayProps) {
            try {
                map.put(prop, Base64.encodeToString(mediaDrm.getPropertyByteArray(prop), Base64.NO_WRAP));
            } catch (Exception e) {
                Log.d(TAG, "Invalid property " + prop);
            }
        }

        Log.d(TAG, "MediaDrm properties: " + map);
    }

    @Nullable
    static DrmInitData.SchemeInitData getWidevineInitData(@Nullable DrmInitData drmInitData) {
        
        if (drmInitData == null) {
            Log.e(TAG, "No PSSH in media");
            return null;
        }
        
        DrmInitData.SchemeInitData schemeInitData = drmInitData.get(WIDEVINE_UUID);
        if (schemeInitData == null) {
            Log.e(TAG, "No Widevine PSSH in media");
            return null;
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
        return schemeInitData;
    }

    static MediaDrmSession openSessionWithKeys(MediaDrm mediaDrm, OfflineKeySetStorage storage, byte[] initData) throws MediaDrmException, MediaCryptoException, FileNotFoundException {

        byte[] keySetId = storage.loadKeySetId(initData);

        MediaDrmSession session = MediaDrmSession.open(mediaDrm);
        session.restoreKeys(keySetId);
        
        Map<String, String> keyStatus = session.queryKeyStatus();
        Log.d(TAG, "keyStatus: " + keyStatus);
        
        return session;
    }
} 
