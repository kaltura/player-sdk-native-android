package com.kaltura.playersdk.players;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastController;
import com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastControllerActivity;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by nissimpardo on 23/11/15.
 */
public class KCCPlayer implements KPlayerController.KPlayer {
    private Context mContext;
    private MediaInfo mMediaInfo;
    private MediaMetadata mMovieMetadata;
    private VideoCastManager mCastManager;
    private VideoCastConsumerImpl mCastConsumer;
    private String mPlayerSource;
    private float mCurrentPlaybackTime = 0;
    private KPlayerListener mPlayerListener;
    private KPlayerCallback mPlayerCallback;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;


    private static final String TAG = "KCCPlayer";

    public KCCPlayer(Context context, String applicationId) {
        mContext = context;

        mMovieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mCastManager = VideoCastManager.getInstance();

    }

    @Override
    public void setPlayerListener(KPlayerListener listener) {
        mPlayerListener = listener;
    }

    @Override
    public void setPlayerCallback(KPlayerCallback callback) {
        mPlayerCallback = callback;
    }

    @Override
    public void setPlayerSource(String playerSource) {
        mPlayerSource = playerSource;
    }

    @Override
    public String getPlayerSource() {
        return mPlayerSource;
    }

    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mCurrentPlaybackTime = currentPlaybackTime;

        try {
            if (mCastManager.isRemoteMediaLoaded()) {
                mCastManager.seek((int) (currentPlaybackTime * 1000));
            }
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public float getCurrentPlaybackTime() {
        float pos = 0;
        try {
            if ( mCastManager.isRemoteMediaLoaded() ) {
                //convert to milliseconds
                pos = (float)(mCastManager.getCurrentMediaPosition() / 1000);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pos;
    }

    @Override
    public float getDuration() {
        float duration = 0;
        try {
            if (mCastManager.isRemoteMediaLoaded()) {
                duration = (float) (mCastManager.getMediaDuration() / 1000);
            }
        } catch (TransientNetworkDisconnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return duration;
    }

    @Override
    public void play() {
        try {
            if ( mMediaInfo == null ) {
                mMediaInfo =  new MediaInfo.Builder(mPlayerSource)
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setContentType("video/mp4")
                        .setMetadata(mMovieMetadata)
                        .build();

                mCastManager.loadMedia(mMediaInfo, true, (int)(mCurrentPlaybackTime * 1000));
                mHandler.removeCallbacks(null);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            float currentTime = getCurrentPlaybackTime();
                            if (currentTime != 0 && currentTime < getDuration() && mPlayerListener != null) {
//                                mPlayerListener.eventWithValue(KCCPlayer.this, KPlayer.TimeUpdateKey, Float.toString(currentTime));
                            } else if (currentTime >= getDuration()) {
//                                mPlayerCallback.playerStateChanged(KPlayerController.ENDED);
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "Looper Exception", e);
                        }
                        mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);

                    }
                });

                mPlayerListener.eventWithValue(this, KPlayer.PlayKey, null);

            } else if ( mCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_PAUSED ) {
                mCastManager.play();
                mPlayerListener.eventWithValue(this, KPlayer.PlayKey, null);
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        try {
            if ( mCastManager.isRemoteMediaLoaded() && !mCastManager.isRemoteMediaPaused() ) {
                mCastManager.pause();
                mPlayerListener.eventWithValue(this, KPlayer.PauseKey, null);
            }

        } catch (CastException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (TransientNetworkDisconnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void changeSubtitleLanguage(String languageCode) {

    }

    @Override
    public void removePlayer() {

    }

    @Override
    public void recoverPlayer() {

    }

    @Override
    public boolean isKPlayer() {
        return false;
    }

    @Override
    public void setShouldCancelPlay(boolean shouldCancelPlay) {

    }
}
