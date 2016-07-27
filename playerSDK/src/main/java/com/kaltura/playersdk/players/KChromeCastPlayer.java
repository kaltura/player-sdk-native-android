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
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;

import java.io.IOException;
import java.util.ArrayList;

import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by nissimpardo on 07/07/16.
 */
public class KChromeCastPlayer implements KCastMediaRemoteControl, ResultCallback<RemoteMediaPlayer.MediaChannelResult> {
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private GoogleApiClient mApiClient;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private static int PLAYHEAD_UPDATE_INTERVAL = 200;
    private ArrayList<KCastMediaRemoteControlListener> mListeners = new ArrayList<>();
    private State mState;
    private String[] mMediaInfoParams;

    String TAG = "KChromeCastPlayer";

    public KChromeCastPlayer(GoogleApiClient apiClient) {
        mApiClient = apiClient;
        mRemoteMediaPlayer = new RemoteMediaPlayer();

        mRemoteMediaPlayer.setOnStatusUpdatedListener(new RemoteMediaPlayer.OnStatusUpdatedListener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
                if (mediaStatus != null) {
                    switch (mediaStatus.getPlayerState()) {
                        case MediaStatus.PLAYER_STATE_IDLE:
                            if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                                mHandler.removeMessages(0);
                                updateState(State.Ended);
                            }
                            break;
                        case MediaStatus.PLAYER_STATE_BUFFERING:
                            break;
                        case MediaStatus.PLAYER_STATE_PLAYING:
                            break;
                    }
                }
            }
        });

        mRemoteMediaPlayer.setOnPreloadStatusUpdatedListener(new RemoteMediaPlayer.OnPreloadStatusUpdatedListener() {
            @Override
            public void onPreloadStatusUpdated() {

            }
        });

        mRemoteMediaPlayer.setOnMetadataUpdatedListener(new RemoteMediaPlayer.OnMetadataUpdatedListener() {
            @Override
            public void onMetadataUpdated() {

            }
        });
    }

    public void setMediaInfoParams(final String[] mediaInfoParams) {
        mMediaInfoParams = mediaInfoParams;
    }

    public void load(final long fromPosition) {
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

                } else {
                    // Prepare the content according to Kaltura's reciever
                    MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
                    mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My video");
                    MediaInfo mediaInfo = new MediaInfo.Builder(
                            mMediaInfoParams[0])
                            .setContentType(mMediaInfoParams[1])
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setMetadata(mediaMetadata)
                            .build();

                    if (fromPosition > 0) {
                        mRemoteMediaPlayer.load(mApiClient, mediaInfo, true, fromPosition).setResultCallback(KChromeCastPlayer.this);
                    } else {
                        mRemoteMediaPlayer.load(mApiClient, mediaInfo).setResultCallback(KChromeCastPlayer.this);
                    }
                }
            }
        });
    }

    private void startTimer() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    long currentTime = mRemoteMediaPlayer.getApproximateStreamPosition();
                    if (currentTime != 0 && currentTime < mRemoteMediaPlayer.getStreamDuration()) {
                        for (KCastMediaRemoteControlListener listener : mListeners) {
                            listener.onCastMediaProgressUpdate(currentTime);
                        }
                    }
                } catch (IllegalStateException e) {
                    String errMsg = "Failed to request status";
                }
                mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
            }
        });
    }


    public void play() {
        mRemoteMediaPlayer.play(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                Status status = mediaChannelResult.getStatus();
                if (status.isSuccess()) {
                    startTimer();
                    updateState(State.Playing);
                }
            }
        });
    }

    public void pause() {
        mRemoteMediaPlayer.pause(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                Status status = mediaChannelResult.getStatus();
                if (status.isSuccess()) {
                    mHandler.removeMessages(0);
                    updateState(State.Pause);
                }
            }
        });
    }

    public void seek(long currentPosition) {
        updateState(State.Seeking);
        mRemoteMediaPlayer.seek(mApiClient, currentPosition).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                updateState(State.Seeked);
            }
        });
    }

    @Override
    public void addListener(KCastMediaRemoteControlListener listener) {
        if (mListeners.size() == 0 || mListeners.size() > 0 && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    @Override
    public void removeListeners() {
        if (mListeners != null && mListeners.size() > 0) {
            mListeners.clear();
            mListeners = null;
        }
        mHandler.removeMessages(0); // remove the timer that is responsible for time update
        mHandler = null;
    }

    @Override
    public void setStreamVolume(double streamVolume) {
        mRemoteMediaPlayer.setStreamVolume(mApiClient, streamVolume).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                Status status = mediaChannelResult.getStatus();
                if (status.isSuccess()) {
                    updateState(State.VolumeChanged);
                }
            }
        });
    }

    @Override
    public void removeListener(KCastMediaRemoteControlListener listener) {
        if (mListeners.size() > 0 && mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    @Override
    public State getCastMediaRemoteControlState() {
        return mState;
    }

    @Override
    public long getCurrentPosition() {
        return mRemoteMediaPlayer.getApproximateStreamPosition();
    }

    @Override
    public boolean hasMediaSession() {
        return mApiClient != null && mApiClient.isConnected();
    }

    private void updateState(State state) {
        mState = state;
        for (KCastMediaRemoteControlListener listener: mListeners) {
            listener.onCastMediaStateChanged(state);
        }
    }

    @Override
    public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
        if (mediaChannelResult.getStatus().isSuccess()) {
            updateState(State.Loaded);
        }
    }
}

