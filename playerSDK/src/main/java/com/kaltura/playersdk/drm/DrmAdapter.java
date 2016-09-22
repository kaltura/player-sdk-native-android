package com.kaltura.playersdk.drm;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.kaltura.playersdk.LocalAssetsManager;

import java.io.IOException;

/**
 * Created by noamt on 20/04/2016.
 */
public abstract class DrmAdapter {
    @NonNull public static DrmAdapter getDrmAdapter(@NonNull final Context context, @NonNull final String localPath) {
        if (localPath.endsWith(".wvm")) {
            return new WidevineClassicAdapter(context);
        }
        
        if (localPath.endsWith(".mpd")) {
            return new WidevineModularAdapter(context);
        }
        
        return new NullDrmAdapter();
    }

    public abstract boolean registerAsset(@NonNull final String localPath, String licenseUri, @Nullable final LocalAssetsManager.AssetRegistrationListener listener) throws IOException;

    public abstract boolean refreshAsset(@NonNull final String localPath, String licenseUri, @Nullable final LocalAssetsManager.AssetRegistrationListener listener);

    public abstract boolean unregisterAsset(@NonNull final String localPath, final LocalAssetsManager.AssetRemovalListener listener);

    public abstract boolean checkAssetStatus(@NonNull String localPath, @Nullable final LocalAssetsManager.AssetStatusListener listener);

    public abstract DRMScheme getScheme();

    public enum DRMScheme {
        Null, WidevineClassic, WidevineCENC
    }

    static class NullDrmAdapter extends DrmAdapter {
        @Override
        public boolean checkAssetStatus(@NonNull String localPath, @Nullable LocalAssetsManager.AssetStatusListener listener) {
            if (listener != null) {
                listener.onStatus(localPath, -1, -1);
            }
            return true;
        }
    
        @Override
        public DRMScheme getScheme() {
            return DRMScheme.Null;
        }
    
        @Override
        public boolean registerAsset(@NonNull String localPath, @NonNull String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
            if (listener != null) {
                listener.onRegistered(localPath);
            }
            return true;
        }
    
        @Override
        public boolean refreshAsset(@NonNull String localPath, @NonNull String licenseUri, @Nullable LocalAssetsManager.AssetRegistrationListener listener) {
            return registerAsset(localPath, licenseUri, listener);
        }
    
        @Override
        public boolean unregisterAsset(@NonNull String localPath, LocalAssetsManager.AssetRemovalListener listener) {
            if (listener != null) {
                listener.onRemoved(localPath);
            }
            return true;
        }
    }
}


