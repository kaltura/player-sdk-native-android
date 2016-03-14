package com.kaltura.playersdk.players;

import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.kplayersdk.R;
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
                    Log.e(TAG, Resources.getSystem().getString(R.string.failed_to_request_status));
                    mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, Resources.getSystem().getString(R.string.failed_to_request_status));
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
                        Log.d(TAG, Long.toString(mRemoteMediaPlayer.getApproximateStreamPosition()));
//                        float percent = currentTime / getDuration();
//                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayer.ProgressKey, Float.toString(percent));
                    }
                } catch (IllegalStateException e) {
                    Log.e(TAG, Resources.getSystem().getString(R.string.looper_exception));
                    mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, Resources.getSystem().getString(R.string.looper_exception));
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
                                Log.d(TAG, "Media loaded successfully");
                                if (isConnecting) {
                                    isConnecting = false;
                                    mPlayerListener.eventWithValue(KCCRemotePlayer.this, "hideConnectingMessage", null);
                                    mPlayerListener.eventWithValue(KCCRemotePlayer.this, "chromecastDeviceConnected", null);
                                }

                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.DurationChangedKey, Float.toString(getDuration() / 1000f));
                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.LoadedMetaDataKey, "");
                                mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.CanPlayKey, null);
                                mListener.mediaLoaded();
                            }
                        }
                    });
        } catch (IllegalStateException e){
            Log.e(TAG, Resources.getSystem().getString(R.string.media_loading_error));
            mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, Resources.getSystem().getString(R.string.media_loading_error));

        } catch (Exception e) {
            Log.e(TAG, Resources.getSystem().getString(R.string.media_loading_opening_error));
            mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.ErrorKey, Resources.getSystem().getString(R.string.media_loading_opening_error));
        }
    }

    @Override
    public void setCurrentPlaybackTime(long currentPlaybackTime) {
        if (currentPlaybackTime > 0) {
            mCurrentPlaybackTime = currentPlaybackTime;
            stopTimer();
            mRemoteMediaPlayer.seek(mApiClient, (long) (currentPlaybackTime)).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                @Override
                public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        if (isPlaying) {
                            startTimer();
                        }
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.SeekedKey, null);
                    } else {
                        Log.w(TAG, "Unable to toggle seek: "
                                + status.getStatusCode());
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
                        Log.w(TAG, "Unable to toggle play: "
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
                public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                    Status status = mediaChannelResult.getStatus();
                    if (status.isSuccess()) {
                        stopTimer();
                        mPlayerListener.eventWithValue(KCCRemotePlayer.this, KPlayerListener.PauseKey, null);
                    } else {
                        isPlaying = true;
                        Log.w(TAG, "Unable to toggle pause: "
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
    public void freezePlayer() {

    }

    @Override
    public void removePlayer() {

    }

    @Override
    public void recoverPlayer() {

    }

    @Override
    public void setShouldCancelPlay(boolean shouldCancelPlay) {

    }

    @Override
    public void setLicenseUri(String licenseUri) {
        // empty
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
                    Log.d(TAG, "PLAYER_STATE_PLAYING");
                    break;
            }
        }
    }

    @Override
    public void onMetadataUpdated() {
        Log.d("nameSpace", mRemoteMediaPlayer.getNamespace());
    }

    @Override
    public void onPreloadStatusUpdated() {

    }
}
