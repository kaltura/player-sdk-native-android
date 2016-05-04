package com.kaltura.playersdk.drm;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;

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
            throw new WidevineNotSupported(e);
        }
    }
    
    public static OfflineKeySetStorage getStorage(Context context) {
        return new OfflineKeySetStorage(context);
    }


} 
