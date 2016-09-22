package com.kaltura.playersdk.drm;

import android.annotation.TargetApi;
import android.media.MediaCrypto;
import android.media.MediaCryptoException;
import android.media.MediaDrm;
import android.media.NotProvisionedException;
import android.media.UnsupportedSchemeException;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.exoplayer.drm.DrmInitData;
import com.google.android.exoplayer.drm.DrmSessionManager;
import com.google.android.exoplayer.drm.UnsupportedDrmException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil.WIDEVINE_UUID;
import static com.kaltura.playersdk.helpers.KStringUtilities.toHexString;

/**
 * Created by noamt on 04/05/2016.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class OfflineDrmSessionManager implements DrmSessionManager {


    private static final String TAG = "OfflineDrmSessionMgr";

    private final MediaDrm mMediaDrm;
    private final OfflineKeySetStorage mStorage;
    private MediaCrypto mMediaCrypto;
    private MediaDrmSession mSession;
    private int mState = STATE_CLOSED;
    private Exception mLastError;
    private AtomicInteger mOpenCount = new AtomicInteger(0);

    OfflineDrmSessionManager(OfflineKeySetStorage storage) throws UnsupportedDrmException {

        mStorage = storage;

        try {
            mMediaDrm = new MediaDrm(WIDEVINE_UUID);
            OfflineDrmManager.printAllProperties(mMediaDrm);

            mMediaDrm.setOnEventListener(new MediaDrm.OnEventListener() {
                @Override
                public void onEvent(@NonNull MediaDrm md, byte[] sessionId, int event, int extra, byte[] data) {
                    Log.d(TAG, "onEvent:" + toHexString(sessionId) + ":" + event + ":" + extra + ":" + toHexString(data));
                }
            });


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setOnKeyStatusChangeListener();
            }


        } catch (UnsupportedSchemeException e) {
            throw new UnsupportedDrmException(UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME, e);
        }
    }
    
    @TargetApi(Build.VERSION_CODES.M)
    private void setOnKeyStatusChangeListener() {
        mMediaDrm.setOnKeyStatusChangeListener(new MediaDrm.OnKeyStatusChangeListener() {
            @Override
            public void onKeyStatusChange(@NonNull MediaDrm md, @NonNull byte[] sessionId, @NonNull List<MediaDrm.KeyStatus> keyInformation, boolean hasNewUsableKey) {
                Log.d(TAG, "onKeyStatusChange:" + toHexString(sessionId) + ":" + hasNewUsableKey);
                logKeyInformation(keyInformation);
            }
        }, null);
    }

    private void onError(Exception error) {
        mLastError = error;
        mState = STATE_ERROR;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void logKeyInformation(List<MediaDrm.KeyStatus> keyInformation) {
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

        mState = STATE_OPENING;

        DrmInitData.SchemeInitData schemeInitData = OfflineDrmManager.getWidevineInitData(drmInitData);
        if (schemeInitData == null) {
            onError(new IllegalStateException("Widevine PSSH not found"));
            return;
        }
        byte[] initData = schemeInitData.data;

        try {
            mSession = OfflineDrmManager.openSessionWithKeys(mMediaDrm, mStorage, initData);
            mMediaCrypto = new MediaCrypto(WIDEVINE_UUID, mSession.getId());
            mState = STATE_OPENED_WITH_KEYS;

        } catch (NotProvisionedException e) {
            throw new WidevineNotSupportedException(e);
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
        if (mSession != null) {
            mSession.close();
            mSession = null;
        }

        mState = STATE_CLOSED;
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
