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
import java.util.HashMap;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
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
    private boolean isEnded = false;
    private HashMap<String, Integer> mTextTracks;
    private List<Integer> mVideoTracks;


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
                                isEnded = true;
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
            mTextTracks = new HashMap<>();
            mVideoTracks = new ArrayList<>();
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);

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
        } catch (IOException e) {
            LOGE(TAG, e.getMessage());
        }

    }

    private void startTimer() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    long currentTime = mRemoteMediaPlayer.getApproximateStreamPosition();
                    if (currentTime != 0 && currentTime < mRemoteMediaPlayer.getStreamDuration()) {
                        for (KCastMediaRemoteControlListener listener : mListeners) {
                            LOGD(TAG, "CC SEND TIME UPDATE " + currentTime);
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

    private void stopTimer() {
        LOGD(TAG, "remove handler callbacks");
        mHandler.removeMessages(0);
    }

    public void play() {
        if (!hasMediaSession()) {
            return;
        }

        LOGD(TAG, "Start PLAY");
        if (isEnded) {
            load(0);
            isEnded = false;
            stopTimer();
            startTimer();
            updateState(State.Playing);
            return;
        }

        mRemoteMediaPlayer.play(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                Status status = mediaChannelResult.getStatus();
                LOGD(TAG, "play status " + status.isSuccess());
                if (status.isSuccess()) {
                    startTimer();
                    updateState(State.Playing);
                }
            }
        });
    }

    public void pause() {
        if (!hasMediaSession()) {
            return;
        }
        LOGD(TAG, "Start PAUSE");
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
        if (!hasMediaSession()) {
            return;
        }
        LOGD(TAG, "CC seek to " + currentPosition);
        LOGD(TAG, "CC SEND SEEKING");
        updateState(State.Seeking);
        mRemoteMediaPlayer.seek(mApiClient, currentPosition, RemoteMediaPlayer.RESUME_STATE_UNCHANGED).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
            @Override
            public void onResult(RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
                if (!mediaChannelResult.getStatus().isSuccess()) {
                    LOGD(TAG, "FAILED to Seeked");
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LOGD(TAG, "CC SEND SEEKED");
                            updateState(State.Seeked);
                        }
                    }, 2500);
                }
            }
        });
    }

    @Override
    public boolean  isPlaying() {
        if (mApiClient != null) {
            if (mApiClient.isConnected()) {
                if (mRemoteMediaPlayer != null) {
                    if (mRemoteMediaPlayer.getMediaStatus().equals(MediaStatus.PLAYER_STATE_PLAYING)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void addListener(KCastMediaRemoteControlListener listener) {
        if (mListeners != null) {
            if (mListeners.size() == 0 || mListeners.size() > 0 && !mListeners.contains(listener)) {
                mListeners.add(listener);
            }
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
        if (!hasMediaSession()) {
            return;
        }
        LOGD(TAG, "CC setStreamVolume " + streamVolume);
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
    public double getCurrentVolume() {
        if (hasMediaSession()) {
            return mRemoteMediaPlayer.getMediaStatus().getStreamVolume();
        }
        return 0;
    }

    @Override
    public boolean isMute() {
        if (hasMediaSession()) {
            return mRemoteMediaPlayer.getMediaStatus().isMute();
        }
        return false;
    }

    @Override
    public void removeListener(KCastMediaRemoteControlListener listener) {
        if (mListeners != null && mListeners.size() > 0 && mListeners.contains(listener)) {
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
    public long getDuration() {
        return mRemoteMediaPlayer.getStreamDuration();
    }

    @Override
    public boolean hasMediaSession() {
        return mApiClient != null && mApiClient.isConnected();
    }

    @Override
    public void switchTextTrack(int index) {
        if (mListeners != null) {
            for (KCastMediaRemoteControlListener listener : mListeners) {
                listener.onTextTrackSwitch(index);
            }
        }
    }

    @Override
    public void setTextTracks(HashMap<String, Integer> textTrackHash) {
        mTextTracks = textTrackHash;
    }

    @Override
    public void setVideoTracks(List<Integer> videoTracksList) {
        mVideoTracks = videoTracksList;
    }

    @Override
    public HashMap<String, Integer> getTextTracks() {
        return mTextTracks;
    }

    @Override
    public List<Integer> getVideoTracks() {
        return mVideoTracks;
    }

    private void updateState(State state) {
        if (state != State.VolumeChanged) {
            mState = state;
        }
        if (mListeners != null) {
            for (KCastMediaRemoteControlListener listener : mListeners) {
                listener.onCastMediaStateChanged(state);
            }
        }
    }

    @Override
    public void onResult(@NonNull RemoteMediaPlayer.MediaChannelResult mediaChannelResult) {
        if (mediaChannelResult.getStatus().isSuccess()) {
            updateState(State.Loaded);
        }
    }
}

