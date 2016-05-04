package com.kaltura.playersdk.drm;

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
import com.kaltura.playersdk.LocalAssetsManager;

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
    
    private final MediaDrm mMediaDrm;
    private final OfflineKeySetStorage mStore;
    
    public static boolean isSupported() {
        // Make sure Widevine is supported.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID);
    }
    

    WidevineModularAdapter(Context context) {
        mStore = OfflineDrmManager.getStorage(context);
        try {
            mMediaDrm = new MediaDrm(WIDEVINE_UUID);
        } catch (UnsupportedSchemeException e) {
            throw new WidevineNotSupported(e);
        }
    }
    
    private byte[] httpPost(@NonNull String licenseUri, byte[] data) throws IOException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/octet-stream");

        byte[] response = ExoplayerUtil.executePost(licenseUri, data, headers);
        Log.d(TAG, "response data (b64): " + Base64.encodeToString(response, 0));
        return response;
    }

    @Override
    public boolean registerAsset(@NonNull String localPath, String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {

        try {
            boolean result = registerAsset(localPath, licenseUri);
            if (listener != null) {
                listener.onRegistered(localPath);
            }
            return result;
        } catch (RegisterException e) {
            if (listener != null) {
                listener.onFailed(localPath, e);
            }
            return false;
        }
    }

    private class RegisterException extends Exception {
        public RegisterException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
    
    private boolean registerAsset(@NonNull String localPath, String licenseUri) throws RegisterException {

        String mimeType;
        byte[] initData;
        try {
            SimpleDashParser dashParser = new SimpleDashParser().parse(localPath);
            initData = dashParser.drmInitData.get(WIDEVINE_UUID).data;
            mimeType = dashParser.format.mimeType;
        } catch (IOException e) {
            throw new RegisterException("Can't parse local dash", e);
        } catch (NullPointerException e) {
            throw new RegisterException("Dash missing mimeType or pssh", e);
        }


        byte[] sessionId;
        try {
            sessionId = mMediaDrm.openSession();
        } catch (NotProvisionedException e) {
            throw new WidevineNotSupported(e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RegisterException("Can't open session", e);
        }


        // Get keyRequest
        MediaDrm.KeyRequest keyRequest;
        try {
            keyRequest = mMediaDrm.getKeyRequest(sessionId, initData, mimeType, MediaDrm.KEY_TYPE_OFFLINE, null);
        } catch (NotProvisionedException e) {
            throw new WidevineNotSupported(e);
        }
        
        // Send request to server
        byte[] keyResponse;
        try {
            keyResponse = httpPost(licenseUri, keyRequest.getData());
        } catch (IOException e) {
            throw new RegisterException("Can't send key request for registration", e);
        }

        // Provide keyResponse
        try {
            byte[] offlineKeyId = mMediaDrm.provideKeyResponse(sessionId, keyResponse);
            mStore.storeKeySetId(initData, offlineKeyId);
        } catch (NotProvisionedException e) {
            throw new WidevineNotSupported(e);
        } catch (DeniedByServerException e) {
            throw new ImpossibleException("Server denial is already handled", e);
        }

        mMediaDrm.closeSession(sessionId);

        return true;
    }

    @Override
    public boolean refreshAsset(@NonNull String localPath, String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
        return false;
    }

    @Override
    public boolean unregisterAsset(@NonNull String localPath, LocalAssetsManager.AssetRemovalListener listener) {
        return false;
    }

    @Override
    public boolean checkAssetStatus(@NonNull String localPath, @Nullable LocalAssetsManager.AssetStatusListener listener) {
        return false;
    }

    @Override
    public DRMScheme getScheme() {
        return DRMScheme.WidevineCENC;
    }

}
