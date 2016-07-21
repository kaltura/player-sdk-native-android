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
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by nissimpardo on 07/07/16.
 */
public class KChromeCastPlayer implements KCastMediaRemoteControl {
    private RemoteMediaPlayer mRemoteMediaPlayer;
    private GoogleApiClient mApiClient;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private static int PLAYHEAD_UPDATE_INTERVAL = 200;
    private ArrayList<KCastMediaRemoteControlListener> mListeners = new ArrayList<>();
    private State mState;

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
                    Log.e(TAG, errMsg);

                } else {
                    // Prepare the content according to Kaltura's reciever
                    MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
                    mediaMetadata.putString(MediaMetadata.KEY_TITLE, "My video");
                    MediaInfo mediaInfo = new MediaInfo.Builder(
                            mediaInfoParams[0])
                            .setContentType(mediaInfoParams[1])
                            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                            .setMetadata(mediaMetadata)
                            .build();
                    mRemoteMediaPlayer.load(mApiClient, mediaInfo).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
                        @Override
                        public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                            if (mediaChannelResult.getStatus().isSuccess()) {
                                updateState(State.Loaded);
                            }
                        }
                    });
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
    public void setStreamVolume(double streamVolume) {
        mRemoteMediaPlayer.setStreamVolume(mApiClient, streamVolume).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                Status status = mediaChannelResult.getStatus();
                if (status.isSuccess()) {
                    updateState(State.VolumeCganged);
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

    private void updateState(State state) {
        mState = state;
        for (KCastMediaRemoteControlListener listener: mListeners) {
            listener.onCastMediaStateChanged(state);
        }
    }

}

