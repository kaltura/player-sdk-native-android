package com.google.android.libraries.mediaframework.exoplayerextensions;

import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.MediaDrmCallback;

/**
 * Created by noamt on 01/05/2016.
 */
public interface ExtendedMediaDrmCallback extends MediaDrmCallback {
    /**
     * Optional DrmSessionManager. If not provided, the default (StreamingDrmSessionManager) is used.
     * @return A DrmSessionManager, ready to be opened. 
     */
    DrmSessionManager getSessionManager();
}
