package com.kaltura.playersdk.players;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import java.io.IOException;

/**
 * Created by nissimpardo on 08/12/15.
 */
public class KCCRemotePlayer implements KPlayerController.KPlayer, RemoteMediaPlayer.OnStatusUpdatedListener, RemoteMediaPlayer.OnMetadataUpdatedListener, RemoteMediaPlayer.OnPreloadStatusUpdatedListener {
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private GoogleApiClient mApiClient;
    private KPlayerListener mPlayerListener;
    private String mPlayerSource;
    private float mCurrentPlaybackTime;
    private boolean isPlaying = false;
    private boolean isReady = false;
    private MediaInfo mMediaInfo;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;

    public KCCRemotePlayer(GoogleApiClient apiClient) {
        mApiClient = apiClient;
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener(this);
        mRemoteMediaPlayer.setOnMetadataUpdatedListener(this);
        mRemoteMediaPlayer.setOnPreloadStatusUpdatedListener(this);
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(),mRemoteMediaPlayer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRemoteMediaPlayer.requestStatus(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                if (!mediaChannelResult.getStatus().isSuccess()) {
                    Log.e(getClass().getSimpleName(), "Failed to request status.");
                } else {

                }
            }
        });
    }

    private void startTimer() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    float currentTime = getCurrentPlaybackTime();
                    if (currentTime != 0 && currentTime < getDuration() && mPlayerListener != null) {
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.TimeUpdateKey, Float.toString(currentTime));
                        float percent = currentTime / getDuration();
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.ProgressKey, Float.toString(percent));
                    }
                } catch (IllegalStateException e) {
                    Log.e(getClass().getSimpleName(), "Looper Exception", e);
                }
                mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
            }
        });
    }

    private void stopTimer() {
        mHandler.removeMessages(0);
    }

    @Override
    public void setPlayerListener(KPlayerListener listener) {
        mPlayerListener = listener;
    }

    @Override
    public void setPlayerCallback(KPlayerCallback callback) {

    }

    @Override
    public void setPlayerSource(String playerSource) {
        mPlayerSource = playerSource;
        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My video");
        mMediaInfo = new MediaInfo.Builder(
                playerSource)
                .setContentType("video/mp4")
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .build();
        loadMedia();
    }

    private void loadMedia() {
        try {
            mRemoteMediaPlayer.load(mApiClient, mMediaInfo, false)
                    .setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult result) {
                            if (result.getStatus().isSuccess()) {
                                Log.d(getClass().getSimpleName(), "Media loaded successfully");
                                isReady = true;
                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.DurationChangedKey, Float.toString(getDuration()));
                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.LoadedMetaDataKey, "");
                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.CanPlayKey, null);
                                if (mCurrentPlaybackTime > 0) {
                                    setCurrentPlaybackTime(mCurrentPlaybackTime);
                                }
                            }
                        }
                    });
        } catch (IllegalStateException e) {
            Log.e(getClass().getSimpleName(), "Problem occurred with media during loading", e);
        } catch (Exception e) {
            Log.e(getClass().getSimpleName(), "Problem opening media during loading", e);
        }
    }

    @Override
    public String getPlayerSource() {
        return mPlayerSource;
    }

    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mCurrentPlaybackTime = currentPlaybackTime;
        if (isReady) {
            stopTimer();
            mRemoteMediaPlayer.seek(mApiClient, (long) (currentPlaybackTime * 1000)).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                @Override
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        if (isPlaying) {
                            startTimer();
                            mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.SeekedKey, null);
                        }
                    } else {
                        Log.w(getClass().getSimpleName(), "Unable to toggle seek: "
                                + status.getStatusCode());
                    }
                }
            });
        }
    }

    @Override
    public float getCurrentPlaybackTime() {
        Log.d(getClass().getSimpleName(), Long.toString(mRemoteMediaPlayer.getApproximateStreamPosition()));
        return mRemoteMediaPlayer.getApproximateStreamPosition() / 1000f;
    }

    @Override
    public float getDuration() {
        return mRemoteMediaPlayer.getStreamDuration() / 1000f;
    }

    @Override
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            mRemoteMediaPlayer.play(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                @Override
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        startTimer();
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.PlayKey, null);
                    } else {
                        isPlaying = false;
                        Log.w(getClass().getSimpleName(), "Unable to toggle play: "
                                + status.getStatusCode());
                    }
                }
            });
        }
    }

    @Override
    public void pause() {
        if (isPlaying) {
            isPlaying = false;
            mRemoteMediaPlayer.pause(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                @Override
                public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        stopTimer();
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.PauseKey, null);
                    } else {
                        isPlaying = true;
                        Log.w(getClass().getSimpleName(), "Unable to toggle pause: "
                                + status.getStatusCode());
                    }
                }
            });
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

    @Override
    public void onStatusUpdated() {
        MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
        if (mediaStatus != null) {
            switch (mediaStatus.getPlayerState()) {
                case MediaStatus.PLAYER_STATE_IDLE:
                    if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                        stopTimer();
                        mCurrentPlaybackTime = 0f;
                        isReady = false;
                        mPlayerListener.eventWithValue(this, KPlayer.EndedKey, null);
                        loadMedia();
                    }
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    break;
            }
        } else {
            mPlayerListener.eventWithValue(this, "hideConnectingMessage", null);
            mPlayerListener.eventWithValue(this, "chromecastDeviceConnected", null);
        }
    }

    @Override
    public void onMetadataUpdated() {

    }

    @Override
    public void onPreloadStatusUpdated() {

    }
}
