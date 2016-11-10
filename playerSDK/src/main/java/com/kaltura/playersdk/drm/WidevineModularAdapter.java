package com.kaltura.playersdk.drm;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.DeniedByServerException;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
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

        return ExoplayerUtil.executePost(licenseUri, data, headers);
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
        RegisterException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
    
    private class NoWidevinePSSHException extends RegisterException {
        NoWidevinePSSHException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }

    private SimpleDashParser parseDash(@NonNull String localPath) throws RegisterException {
        SimpleDashParser dashParser;
        try {
            dashParser = new SimpleDashParser().parse(localPath);
            if (dashParser.format == null) {
                throw new RegisterException("Unknown format", null);
            }
            if (dashParser.hasContentProtection && dashParser.widevineInitData == null) {
                throw new NoWidevinePSSHException("No Widevine PSSH in media", null);
            }
        } catch (IOException e) {
            throw new RegisterException("Can't parse local dash", e);
        }

        return dashParser;
    }
    
    private boolean registerAsset(@NonNull String localPath, String licenseUri) throws RegisterException {

        SimpleDashParser dash = parseDash(localPath);
        
        if (!dash.hasContentProtection) {
            // Not protected -- nothing to do.
            return true;
        }
        
        String mimeType = dash.format.mimeType;
        byte[] initData = dash.widevineInitData;
        
        MediaDrmSession session;
        MediaDrm mediaDrm = createMediaDrm();
        try {
            session = MediaDrmSession.open(mediaDrm);
        } catch (MediaDrmException e) {
            throw new RegisterException("Can't open session", e);
        }


        // Get keyRequest
        MediaDrm.KeyRequest keyRequest = session.getOfflineKeyRequest(initData, mimeType);
        Log.d(TAG, "registerAsset: init data (b64): " + Base64.encodeToString(initData, Base64.NO_WRAP));

        byte[] data = keyRequest.getData();
        
        // Send request to server
        byte[] keyResponse;
        try {
            Log.d(TAG, "registerAsset: request data (b64): " + Base64.encodeToString(data, Base64.NO_WRAP));
            keyResponse = httpPost(licenseUri, data);
            Log.d(TAG, "registerAsset: response data (b64): " + Base64.encodeToString(keyResponse, Base64.NO_WRAP));
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

    private boolean unregisterAsset(String localPath) throws RegisterException {

        SimpleDashParser dash = parseDash(localPath);
        
        
        byte[] keySetId;
        try {
            keySetId = mStore.loadKeySetId(dash.widevineInitData);
        } catch (FileNotFoundException e) {
            throw new RegisterException("Can't unregister -- keySetId not found", e);
        }


        MediaDrm mediaDrm = createMediaDrm();
        MediaDrm.KeyRequest releaseRequest;
        try {
            releaseRequest = mediaDrm.getKeyRequest(keySetId, null, null, MediaDrm.KEY_TYPE_RELEASE, null);
        } catch (NotProvisionedException e) {
            throw new WidevineNotSupportedException(e);
        }
        
        Log.d(TAG, "releaseRequest:" + Base64.encodeToString(releaseRequest.getData(), Base64.NO_WRAP));

        mStore.removeKeySetId(dash.widevineInitData);
        
        return true;
    }


    @Override
    public boolean unregisterAsset(@NonNull String localPath, LocalAssetsManager.AssetRemovalListener listener) {
        // TODO

        try {
            unregisterAsset(localPath);
            return true;
        } catch (RegisterException e) {
            Log.e(TAG, "Failed to unregister", e);
            return false;
        } finally {
            if (listener != null) {
                listener.onRemoved(localPath);
            }
        }
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
        // TODO -- verify that we just need to register again
        
        return registerAsset(localPath, licenseUri, listener);
    }

    @Override
    public boolean checkAssetStatus(@NonNull String localPath, @Nullable LocalAssetsManager.AssetStatusListener listener) {

        try {
            Map<String, String> assetStatus = checkAssetStatus(localPath);
            if (assetStatus != null) {
                long licenseDurationRemaining = 0;
                long playbackDurationRemaining = 0;
                try {
                    licenseDurationRemaining = Long.parseLong(assetStatus.get("LicenseDurationRemaining"));
                    playbackDurationRemaining = Long.parseLong(assetStatus.get("PlaybackDurationRemaining"));
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid integers in KeyStatus: " + assetStatus);
                }
                if (listener != null) {
                    listener.onStatus(localPath, licenseDurationRemaining, playbackDurationRemaining);
                }
            }
        } catch (NoWidevinePSSHException e) {
            // Not a Widevine file
            if (listener != null) {
                listener.onStatus(localPath, -1, -1);
            }
            return false;
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
            throw new NoWidevinePSSHException("No Widevine PSSH in media", null);
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
