package com.kaltura.playersdk.players;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;

import java.io.IOException;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;
import static com.kaltura.playersdk.utils.LogUtils.LOGW;

/**
 * Created by nissimpardo on 08/12/15.
 */
public class KCCRemotePlayer implements KPlayer, RemoteMediaPlayer.OnStatusUpdatedListener, RemoteMediaPlayer.OnMetadataUpdatedListener, RemoteMediaPlayer.OnPreloadStatusUpdatedListener {
    private static final String TAG = "KCCRemotePlayer";
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private GoogleApiClient mApiClient;
    private KPlayerListener mPlayerListener;
    private String mPlayerSource;
    private long mCurrentPlaybackTime = 0;
    private boolean isPlaying = false;
    private boolean isConnecting = true;
    private MediaInfo mMediaInfo;
    private KCCRemotePlayerListener mListener;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;


    public interface KCCRemotePlayerListener {
        void remoteMediaPlayerReady();
        void mediaLoaded();
    }

    public KCCRemotePlayer(GoogleApiClient apiClient, KCCRemotePlayerListener listener) {
        mApiClient = apiClient;
        mListener = listener;
        mRemoteMediaPlayer = new RemoteMediaPlayer();
        mRemoteMediaPlayer.setOnStatusUpdatedListener(this);
        mRemoteMediaPlayer.setOnMetadataUpdatedListener(this);
        mRemoteMediaPlayer.setOnPreloadStatusUpdatedListener(this);
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mRemoteMediaPlayer.requestStatus(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                if (!mediaChannelResult.getStatus().isSuccess()) {
                    String errMsg = "Failed to request status";
                    LOGE(TAG, errMsg);
                    mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, errMsg);
                } else {
                    mListener.remoteMediaPlayerReady();
                }
            }
        });
    }

    private void startTimer() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    long currentTime = getCurrentPlaybackTime();
                    if (currentTime != 0 && currentTime < getDuration() && mPlayerListener != null) {
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.TimeUpdateKey, Float.toString(currentTime / 1000f));
                        LOGD(TAG, Long.toString(mRemoteMediaPlayer.getApproximateStreamPosition()));
//                        float percent = currentTime / getDuration();
//                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.ProgressKey, Float.toString(percent));
                    }
                } catch (IllegalStateException e) {
                    String errMsg = "Failed to request status";
                    LOGE(TAG, errMsg);
                    mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, errMsg + "-" + e.getMessage());
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
                                LOGD(TAG, "Media loaded successfully");
                                if (isConnecting) {
                                    isConnecting = false;
                                    mPlayerListener.eventWithValue(KCCRemotePlayer.this, "hideConnectingMessage", null);
                                }

                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.DurationChangedKey, Float.toString(getDuration() / 1000f));
                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.LoadedMetaDataKey, "");
                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.CanPlayKey, null);
                                mListener.mediaLoaded();
                            }
                        }
                    });
        } catch (IllegalStateException e){
            String errMsg = "Error occurred with media during loading";
            LOGE(TAG, errMsg, e);
            mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, errMsg);

        } catch (Exception e) {
            String errMsg = "Error in opening media during loading";
            LOGE(TAG, errMsg, e);
            mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, errMsg);
        }
    }

    @Override
    public void setCurrentPlaybackTime(long currentPlaybackTime) {
        if (currentPlaybackTime > 0) {
            mCurrentPlaybackTime = currentPlaybackTime;
            stopTimer();
            mRemoteMediaPlayer.seek(mApiClient, (currentPlaybackTime)).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                @Override
                public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        if (isPlaying) {
                            startTimer();
                        }
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.SeekedKey, null);
                    } else {
                        LOGW(TAG, "Unable to toggle seek: " + status.getStatusCode());
                    }
                }
            });
        }
    }

    @Override
    public long getCurrentPlaybackTime() {
        return mRemoteMediaPlayer.getApproximateStreamPosition();
    }

    @Override
    public long getDuration() {
        return mRemoteMediaPlayer.getStreamDuration();
    }

    @Override
    public void play() {
        if (!isPlaying) {
            isPlaying = true;
            mRemoteMediaPlayer.play(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                @Override
                public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        startTimer();
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.PlayKey, null);
                    } else {
                        isPlaying = false;
                        LOGW(TAG, "Unable to toggle play: " + status.getStatusCode());
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
                public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        stopTimer();
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.PauseKey, null);
                    } else {
                        isPlaying = true;
                        LOGW(TAG, "Unable to toggle pause: " + status.getStatusCode());
                    }
                }
            });
        }
    }

    @Override
    public void freezePlayer() {

    }

    @Override
    public void removePlayer() {
        mRemoteMediaPlayer.stop(mApiClient);
        mRemoteMediaPlayer.setOnStatusUpdatedListener(null);
        mRemoteMediaPlayer.setOnMetadataUpdatedListener(null);
        mRemoteMediaPlayer.setOnPreloadStatusUpdatedListener(null);
        mRemoteMediaPlayer = null;
        stopTimer();
    }

    @Override
    public void recoverPlayer(boolean isPlaying) {

    }

    @Override
    public void setShouldCancelPlay(boolean shouldCancelPlay) {

    }

    @Override
    public void setLicenseUri(String licenseUri) {
        // empty
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void switchToLive() {
        LOGW(TAG, "switchToLive - Feature is not implemented yet");
        //TODO
        //loadMedia();
    }

    @Override
    public TrackFormat getTrackFormat(TrackType trackType, int index) {
        return null;
    }

    @Override
    public int getTrackCount(TrackType trackType) {
        return 0;
    }

    @Override
    public int getCurrentTrackIndex(TrackType trackType) {
        return -1;
    }

    @Override
    public void switchTrack(TrackType trackType, int newIndex) {

    }

    @Override
    public void attachSurfaceViewToPlayer() {

    }

    @Override
    public void detachSurfaceViewFromPlayer() {

    }

    @Override
    public void setPrepareWithConfigurationMode() {

    }

    @Override
    public void onStatusUpdated() {
        MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
        if (mediaStatus != null) {
            switch (mediaStatus.getPlayerState()) {
                case MediaStatus.PLAYER_STATE_IDLE:
                    if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                        stopTimer();
                        mCurrentPlaybackTime = 0;
                        mPlayerListener.eventWithValue(this, KPlayerListener.EndedKey, null);
                        loadMedia();
                    }
                    break;
                case MediaStatus.PLAYER_STATE_BUFFERING:
                    break;
                case MediaStatus.PLAYER_STATE_PLAYING:
                    LOGD(TAG, "PLAYER_STATE_PLAYING");
                    break;
            }
        }
    }

    @Override
    public void onMetadataUpdated() {
        LOGD(TAG, "nameSpace "+ mRemoteMediaPlayer.getNamespace());
    }

    @Override
    public void onPreloadStatusUpdated() {

    }
}
