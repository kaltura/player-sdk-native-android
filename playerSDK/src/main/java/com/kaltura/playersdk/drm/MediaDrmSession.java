package com.kaltura.playersdk.drm;

import android.annotation.TargetApi;
import android.media.DeniedByServerException;
import android.media.MediaDrm;
import android.media.MediaDrmException;
import android.media.NotProvisionedException;
import android.os.Build;
import android.support.annotation.NonNull;

import java.util.Map;

/**
 * Created by noamt on 05/05/2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class MediaDrmSession {
    final MediaDrm mMediaDrm;
    byte[] mSessionId;

    private MediaDrmSession(@NonNull MediaDrm mediaDrm) {
        mMediaDrm = mediaDrm;
    }
    
    static MediaDrmSession open(@NonNull MediaDrm mediaDrm) throws MediaDrmException {
        MediaDrmSession session = new MediaDrmSession(mediaDrm);
        session.mSessionId = mediaDrm.openSession();
        return session;
    }
    
    byte[] getId() {
        return mSessionId;
    }
    
    void close() {
        mMediaDrm.closeSession(mSessionId);
    }
    
    void restoreKeys(byte[] keySetId) {
        mMediaDrm.restoreKeys(mSessionId, keySetId);
    }


    public Map<String, String> queryKeyStatus() {
        return mMediaDrm.queryKeyStatus(mSessionId);
    }
    
    MediaDrm.KeyRequest getOfflineKeyRequest(byte[] initData, String mimeType) {
        try {
            return mMediaDrm.getKeyRequest(mSessionId, initData, mimeType, MediaDrm.KEY_TYPE_OFFLINE, null);
        } catch (NotProvisionedException e) {
            throw new WidevineNotSupportedException(e);
        }
    }
    
    byte[] provideKeyResponse(byte[] keyResponse) throws DeniedByServerException {
        try {
            return mMediaDrm.provideKeyResponse(mSessionId, keyResponse);
        } catch (NotProvisionedException e) {
            throw new WidevineNotSupportedException(e);
        }
    }
}
