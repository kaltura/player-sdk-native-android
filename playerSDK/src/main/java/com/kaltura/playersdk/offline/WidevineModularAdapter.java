package com.kaltura.playersdk.offline;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.media.NotProvisionedException;
import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil;
import com.kaltura.playersdk.ImpossibleException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.android.exoplayer.drm.StreamingDrmSessionManager.WIDEVINE_UUID;


/**
 * Created by noamt on 19/04/2016.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class WidevineModularAdapter extends DrmAdapter {
    
    private static final String TAG = "WidevineModularAdapter";
    public static final String VIDEO_MIME_TYPE = "video/mp4";
    
    private final MediaDrm mMediaDrm;
    private final OfflineDrmManager.KeySetStorage mStore;
    
    public static boolean isSupported() {
        // Make sure Widevine is supported.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID);
    }
    
    byte[] extractInitData(@NonNull String localPath) {

        String psshString = "AAAAUHBzc2gAAAAA7e+LqXnWSs6jyCfc1R0h7QAAADAIARIQo586eNRX5Z5yXUqgoFpBrxoHa2FsdHVyYSIKMF9wbDVsYmZvMCoFU0RfSEQ=";
        
        return Base64.decode(psshString, Base64.NO_WRAP);
        
        // TODO: parse xml, extract widevine pssh
    }
    
    WidevineModularAdapter(Context context) {
        mStore = OfflineDrmManager.getStorage(context);
        try {
            mMediaDrm = new MediaDrm(WIDEVINE_UUID);
        } catch (UnsupportedSchemeException e) {
            throw new ImpossibleException("Widevine is always supported on Android", e);
        }
    }
    
    byte[] openSession() {
        try {
            return mMediaDrm.openSession();
        } catch (NotProvisionedException e) {
            throw new ImpossibleException("Widevine on Android is pre-provisioned", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Error: can't open session", e);
            return null;
        }
    }
    
    boolean unregisterAsset(@NonNull String localPath) {
        // TODO: remove offline key
        return false;
    }

    private byte[] provideKeyResponse(byte[] sessionId, byte[] response) {
        try {
            return mMediaDrm.provideKeyResponse(sessionId, response);
        } catch (NotProvisionedException e) {
            throw new ImpossibleException("Widevine on Android is pre-provisioned", e);
        } catch (DeniedByServerException e) {
            throw new ImpossibleException("Server denial is already handled", e);
        }
    }

    private byte[] httpPost(@NonNull String licenseUri, byte[] data) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/octet-stream");

        byte[] response = ExoplayerUtil.executePost(licenseUri, data, headers);
        Log.d(TAG, "response data (b64): " + Base64.encodeToString(response, 0));
        return response;
    }

    @NonNull
    private MediaDrm.KeyRequest getDashOfflineKeyRequest(byte[] sessionId, byte[] pssh) {
        try {
            return mMediaDrm.getKeyRequest(sessionId, pssh, VIDEO_MIME_TYPE, MediaDrm.KEY_TYPE_OFFLINE, null);
        } catch (NotProvisionedException e) {
            throw new ImpossibleException("Widevine on Android is pre-provisioned", e);
        }
    }

    @Override
    boolean registerAsset(@NonNull String localPath, String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
        byte[] sessionId = openSession();
        if (sessionId == null) {
            Log.e(TAG, "Error: can't open session for registration");
            return false;
        }

        byte[] initData = extractInitData(localPath);

        MediaDrm.KeyRequest keyRequest = getDashOfflineKeyRequest(sessionId, initData);


        // Send request to server
        byte[] keyResponse = null;
        try {
            keyResponse = httpPost(licenseUri, keyRequest.getData());
        } catch (IOException e) {
            Log.e(TAG, "Error: can't send key request for registration");
            if (listener != null) {
                listener.onFailed(localPath, e);
            }
            return false;
        }

        // Forward keyResponse
        byte[] offlineKeyId = provideKeyResponse(sessionId, keyResponse);

        // Store offlineKeyId for later
        mStore.storeKeySetId(initData, offlineKeyId);

        mMediaDrm.closeSession(sessionId);
        
        if (listener != null) {
            listener.onRegistered(localPath);
        }

        return true;    
    }

    @Override
    boolean refreshAsset(@NonNull String localPath, String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
        return false;
    }

    @Override
    boolean unregisterAsset(@NonNull String localPath, LocalAssetsManager.AssetRemovalListener listener) {
        return false;
    }

    @Override
    boolean checkAssetStatus(@NonNull String localPath, @Nullable LocalAssetsManager.AssetStatusListener listener) {
        return false;
    }

    @Override
    DRMScheme getScheme() {
        return DRMScheme.WidevineCENC;
    }
}
