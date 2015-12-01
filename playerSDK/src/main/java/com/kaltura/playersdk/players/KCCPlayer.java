package com.kaltura.playersdk.players;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;


/**
 * Created by nissimpardo on 23/11/15.
 */
public class KCCPlayer implements KPlayerController.KPlayer, RemoteMediaPlayer.OnMetadataUpdatedListener, RemoteMediaPlayer.OnStatusUpdatedListener, RemoteMediaPlayer.OnPreloadStatusUpdatedListener {
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
    private boolean isPlaying = false;
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;


    private static final String TAG = "KCCPlayer";

    public KCCPlayer(Context context, String applicationId) {
        mContext = context;

        mMovieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mCastManager = VideoCastManager.getInstance();
        mCastManager.getRemoteMediaPlayer().setOnMetadataUpdatedListener(this);
        mCastManager.getRemoteMediaPlayer().setOnStatusUpdatedListener(this);
        mCastManager.getRemoteMediaPlayer().setOnPreloadStatusUpdatedListener(this);
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
        mMediaInfo =  new MediaInfo.Builder(mPlayerSource)
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setContentType("video/mp4")
                .setMetadata(mMovieMetadata)
                .build();

        try {
            mCastManager.loadMedia(mMediaInfo, false, (int) (mCurrentPlaybackTime * 1000));
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPlayerSource() {
        return mPlayerSource;
    }

    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mCurrentPlaybackTime = currentPlaybackTime;

        try {

            mCastManager.seek((int) (currentPlaybackTime * 1000));
            mPlayerListener.eventWithValue(this, KPlayer.SeekedKey, null);

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

                //convert to milliseconds
                pos = (float)mCastManager.getCurrentMediaPosition() / 1000;

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
            duration = (float) (mCastManager.getMediaDuration() / 1000);
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
            if ( !isPlaying ) {
                mCastManager.play();
                mHandler.removeCallbacks(null);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            float currentTime = getCurrentPlaybackTime();
                            if (currentTime != 0 && currentTime < getDuration() && mPlayerListener != null) {
                                mPlayerListener.eventWithValue(KCCPlayer.this, KPlayer.TimeUpdateKey, Float.toString(currentTime));
                                mPlayerListener.eventWithValue(KCCPlayer.this, KPlayer.ProgressKey, Float.toString(currentTime / getDuration()));
                            } else if (currentTime >= getDuration()) {
//                                mPlayerCallback.playerStateChanged(KPlayerController.ENDED);
                            }
                        } catch (IllegalStateException e) {
                            Log.e(TAG, "Looper Exception", e);
                        }
                        mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);

                    }
                });
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {
        try {
            if (isPlaying) {
                mCastManager.pause();
            }
        } catch (CastException e) {
            e.printStackTrace();
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void changeSubtitleLanguage(String languageCode) {

    }

    @Override
    public void removePlayer() {
        mHandler.removeMessages(0);
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

    @Override
    public void onMetadataUpdated() {

    }

    @Override
    public void onStatusUpdated() {
        MediaStatus mediaStatus = mCastManager.getRemoteMediaPlayer().getMediaStatus();
        if (mediaStatus != null) {
            switch (mediaStatus.getPlayerState()) {
                case MediaStatus.PLAYER_STATE_PLAYING:
                    if (!isPlaying) {
                        isPlaying = true;
                        mPlayerListener.eventWithValue(this, KPlayer.PlayKey, null);
                    }
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    Log.d(getClass().getSimpleName(), "PLAYER_STATE_BUFFERING");
                    break;
                case MediaStatus.PLAYER_STATE_PAUSED:
                    if (isPlaying) {
                        isPlaying = false;
                        mPlayerListener.eventWithValue(this, KPlayer.PauseKey, null);
                    }
                    break;
                case MediaStatus.PLAYER_STATE_IDLE:
                    if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                        Log.d(getClass().getSimpleName(), "IDLE_REASON_FINISHED");
                    } else if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_INTERRUPTED) {
                        Log.d(getClass().getSimpleName(), "IDLE_REASON_INTERRUPTED");
                    }
                    break;
                case MediaStatus.PLAYER_STATE_UNKNOWN:
                    Log.d(getClass().getSimpleName(), "PLAYER_STATE_UNKNOWN");
                    break;
            }
        }
    }

    @Override
    public void onPreloadStatusUpdated() {
        try {
            if (mCastManager.getMediaDuration() > 0) {
                mPlayerListener.eventWithValue(this, KPlayer.DurationChangedKey, Float.toString((float)mCastManager.getMediaDuration() / 1000));
                mPlayerListener.eventWithValue(this, KPlayer.LoadedMetaDataKey, "");
                mPlayerListener.eventWithValue(this, KPlayer.CanPlayKey, null);
            }
        } catch (TransientNetworkDisconnectionException e) {
            e.printStackTrace();
        } catch (NoConnectionException e) {
            e.printStackTrace();
        }

    }
}
