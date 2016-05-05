package com.kaltura.playersdk.drm;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.DeniedByServerException;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil;
import com.kaltura.playersdk.LocalAssetsManager;

import java.io.FileNotFoundException;
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
    
    private final OfflineKeySetStorage mStore;
    
    public static boolean isSupported() {
        // Make sure Widevine is supported.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID);
    }
    

    WidevineModularAdapter(Context context) {
        mStore = OfflineDrmManager.getStorage(context);
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
            initData = dashParser.widevineInitData;
            if (initData == null) {
                throw new RegisterException("No Widevine PSSH in media", null);
            }
            if (dashParser.format == null) {
                throw new RegisterException("Unknown format", null);
            }
            mimeType = dashParser.format.mimeType;
        } catch (IOException e) {
            throw new RegisterException("Can't parse local dash", e);
        }

        
        MediaDrmSession session;
        MediaDrm mediaDrm = createMediaDrm();
        try {
            session = MediaDrmSession.open(mediaDrm);
        } catch (MediaDrmException e) {
            throw new RegisterException("Can't open session", e);
        }


        // Get keyRequest
        MediaDrm.KeyRequest keyRequest = session.getOfflineKeyRequest(initData, mimeType);

        // Send request to server
        byte[] keyResponse;
        try {
            keyResponse = httpPost(licenseUri, keyRequest.getData());
        } catch (IOException e) {
            throw new RegisterException("Can't send key request for registration", e);
        }

        // Provide keyResponse
        try {
            byte[] offlineKeyId = session.provideKeyResponse(keyResponse);
            mStore.storeKeySetId(initData, offlineKeyId);
        } catch (DeniedByServerException e) {
            throw new RegisterException("Request denied by server", e);
        }

        session.close();

        return true;
    }

    @NonNull
    private MediaDrm createMediaDrm() throws RegisterException {
        MediaDrm mediaDrm;
        try {
            mediaDrm = new MediaDrm(WIDEVINE_UUID);
        } catch (UnsupportedSchemeException e) {
            throw new WidevineNotSupportedException(e);
        }
        return mediaDrm;
    }

    @Override
    public boolean refreshAsset(@NonNull String localPath, String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
        // TODO
        return false;
    }

    @Override
    public boolean unregisterAsset(@NonNull String localPath, LocalAssetsManager.AssetRemovalListener listener) {
        // TODO
        return false;
    }

    @Override
    public boolean checkAssetStatus(@NonNull String localPath, @Nullable LocalAssetsManager.AssetStatusListener listener) {
        // TODO

        try {
            Map<String, String> assetStatus = checkAssetStatus(localPath);
            if (assetStatus != null) {
                int licenseDurationRemaining = 0;
                int playbackDurationRemaining = 0;
                try {
                    licenseDurationRemaining = Integer.parseInt(assetStatus.get("LicenseDurationRemaining"));
                    playbackDurationRemaining = Integer.parseInt(assetStatus.get("PlaybackDurationRemaining"));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid integers in KeyStatus: " + assetStatus);
                }
                if (listener != null) {
                    listener.onStatus(localPath, licenseDurationRemaining, playbackDurationRemaining);
                }
            }            
        } catch (RegisterException e) {
            if (listener != null) {
                listener.onStatus(localPath, 0, 0);
            }
            return false;
        }

        return true;
    }

    private Map<String, String> checkAssetStatus(@NonNull String localPath) throws RegisterException {
        SimpleDashParser dashParser;
        try {
            dashParser = new SimpleDashParser().parse(localPath);
        } catch (IOException e) {
            throw new RegisterException("Can't parse dash", e);
        }
        if (dashParser.widevineInitData == null) {
            throw new RegisterException("No Widevine PSSH in media", null);
        }


        MediaDrm mediaDrm = createMediaDrm();

        MediaDrmSession session;
        try {
            session = OfflineDrmManager.openSessionWithKeys(mediaDrm, mStore, dashParser.widevineInitData);
        } catch (MediaDrmException | MediaCryptoException | FileNotFoundException e) {
            throw new RegisterException("Can't open session with keys", e);
        }

        Map<String, String> keyStatus = session.queryKeyStatus();

        session.close();
        mediaDrm.release();
        
        return keyStatus;
    }

    @Override
    public DRMScheme getScheme() {
        return DRMScheme.WidevineCENC;
    }
}
