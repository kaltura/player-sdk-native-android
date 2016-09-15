package com.kaltura.playersdk.players;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.media.RemoteMediaClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.images.WebImage;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by nissimpardo on 07/07/16.
 */
public class KChromeCastPlayer implements KCastMediaRemoteControl{
    public static final String KEY_DESCRIPTION = "description";
    private CastSession mCastSession;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private static int PLAYHEAD_UPDATE_INTERVAL = 200;
    private ArrayList<KCastMediaRemoteControlListener> mListeners = new ArrayList<>();
    private State mState;
    private String[] mMediaInfoParams;
    private boolean isEnded = false;
    private HashMap<String, Integer> mTextTracks;
    private List<Integer> mVideoTracks;
    private int currentSelectedTextTrack = 0;

    private String mEntryName = "";
    private String mEntryDescription = "";
    private String mEntryThumbnailUrl = "";

    String TAG = "KChromeCastPlayer";

    public KChromeCastPlayer(CastSession castSession) {
        mCastSession = castSession;
        mCastSession.getRemoteMediaClient().addListener(new RemoteMediaClient.Listener() {
            @Override
            public void onStatusUpdated() {
                MediaStatus mediaStatus = mCastSession.getRemoteMediaClient().getMediaStatus();
                LOGD(TAG, "onStatusUpdated mediaStatus = " + mediaStatus.getPlayerState());
                if (mediaStatus != null) {
                    switch (mediaStatus.getPlayerState()) {
                        case MediaStatus.PLAYER_STATE_IDLE:
                            if (mediaStatus.getIdleReason() == MediaStatus.IDLE_REASON_FINISHED) {
                                stopTimer();
                                updateState(State.Ended);
                                isEnded = true;
                            }
                            break;
                        case MediaStatus.PLAYER_STATE_PAUSED:
                            //updateState(State.Pause);
                            break;
                        case MediaStatus.PLAYER_STATE_PLAYING:
                            //updateState(State.Playing);
                            break;
                        case MediaStatus.PLAYER_STATE_BUFFERING:
                            break;
                    }
                }
            }

            @Override
            public void onMetadataUpdated() {

            }

            @Override
            public void onQueueStatusUpdated() {

            }

            @Override
            public void onPreloadStatusUpdated() {

            }

            @Override
            public void onSendingRemoteMediaRequest() {

            }
        });
    }

    public void setMediaInfoParams(final String[] mediaInfoParams) {
        mMediaInfoParams = mediaInfoParams;
    }

    public void load(final long fromPosition, String entryTitle, String entryDescription, String entryThumbnailUrl) {
            //Init the tracks
            mTextTracks = new HashMap<>();
            mVideoTracks = new ArrayList<>();

            mEntryName = entryTitle;
            mEntryDescription = entryDescription;
            mEntryThumbnailUrl = entryThumbnailUrl;
        LOGD(TAG, "CC LOAD " +  mEntryName);

        JSONObject descriptionJsonObj = null;
        try {
            descriptionJsonObj = new JSONObject();
            descriptionJsonObj.put(KEY_DESCRIPTION, mEntryDescription);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to add description to the json object", e);
        };

        MediaMetadata mediaMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
        mediaMetadata.putString(MediaMetadata.KEY_TITLE, mEntryName);
        //small thumbnail
        mediaMetadata.addImage(new WebImage(Uri.parse(mEntryThumbnailUrl)));// + "/width/480/hight/270")));
        //big thumbnail
        mediaMetadata.addImage(new WebImage(Uri.parse(mEntryThumbnailUrl)));// + "/width/1200/hight/780")));

        MediaInfo mediaInfo = new MediaInfo.Builder(
                mMediaInfoParams[0])
                .setContentType(mMediaInfoParams[1])
                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                .setMetadata(mediaMetadata)
                .setCustomData(descriptionJsonObj)
                .build();

            RemoteMediaClient remoteMediaClient = mCastSession.getRemoteMediaClient();
            remoteMediaClient.load(mediaInfo, false, fromPosition).setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {

                @Override
                public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                    if (!mediaChannelResult.getStatus().isSuccess()) {
                        LOGE(TAG, "CC LOAD failed");
                    } else {
                        //stopTimer();
                        startTimer();
                        updateState(State.Loaded);
                    }
                }
            });
    }

    private void startTimer() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mCastSession == null || mCastSession.getRemoteMediaClient() == null) {
                        return;
                    }
                    long currentTime = mCastSession.getRemoteMediaClient().getApproximateStreamPosition();
                    if (currentTime != 0 && currentTime < mCastSession.getRemoteMediaClient().getStreamDuration()) {
                        if (mCastSession.getRemoteMediaClient().isPlaying()) {
                            LOGD(TAG, "CC SEND TIME UPDATE " + currentTime);
                            for (KCastMediaRemoteControlListener listener : mListeners) {
                                listener.onCastMediaProgressUpdate(currentTime);
                            }
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
        if (mHandler != null) {
            LOGD(TAG, "remove handler callbacks");
            mHandler.removeMessages(0);
        }
    }

    public void play() {
        if (!hasMediaSession(true)) {
            return;
        }

        LOGD(TAG, "Start PLAY");
        if (isEnded) {
            load(0, mEntryName, mEntryDescription , mEntryThumbnailUrl);
            isEnded = false;
            stopTimer();
            startTimer();
            updateState(State.Playing);
            return;
        }
        mCastSession.getRemoteMediaClient().play().setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
            @Override
            public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                if (!mediaChannelResult.getStatus().isSuccess()) {
                    LOGE(TAG, "CC PLAY failed");
                } else {
                    startTimer();
                    updateState(State.Playing);
                }
            }
        });
    }

    public void pause() {
        if (!hasMediaSession(true)) {
            return;
        }
        LOGD(TAG, "Start PAUSE");
        mCastSession.getRemoteMediaClient().pause().setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
            @Override
            public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                if (!mediaChannelResult.getStatus().isSuccess()) {
                    LOGE(TAG, "CC PAUSE failed");
                } else {
                    updateState(State.Pause);
                }
            }
        });
        stopTimer();

    }

    public void seek(long currentPosition) {
        if (!hasMediaSession(true)) {
            return;
        }
        LOGD(TAG, "CC seek to " + currentPosition);
        LOGD(TAG, "CC SEND SEEKING");
        updateState(State.Seeking);
        mCastSession.getRemoteMediaClient().seek(currentPosition).setResultCallback(new ResultCallback<RemoteMediaClient.MediaChannelResult>() {
            @Override
            public void onResult(@NonNull RemoteMediaClient.MediaChannelResult mediaChannelResult) {
                if (!mediaChannelResult.getStatus().isSuccess()) {
                    LOGE(TAG, "Seek to currentPosition failed");
                }
            }
        });

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            LOGD(TAG, "CC SEND SEEKED");
                            updateState(State.Seeked);
                        }
                    }, 2500);
    }

    @Override
    public boolean isPlaying() {
        if (mCastSession != null && mCastSession.getRemoteMediaClient() != null) {
            return mCastSession.getRemoteMediaClient().isPlaying();
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
        stopTimer(); // remove the timer that is responsible for time update
        mHandler = null;
    }

    @Override
    public void setStreamVolume(double streamVolume) {
        if (!hasMediaSession(true)) {
            return;
        }
        LOGD(TAG, "CC setStreamVolume " + streamVolume);
        mCastSession.getRemoteMediaClient().setStreamVolume(streamVolume);
        updateState(State.VolumeChanged);
    }

    @Override
    public double getCurrentVolume() {
        if (hasMediaSession(true)) {
            return mCastSession.getRemoteMediaClient().getMediaStatus().getStreamVolume();
        }
        return 0;
    }

    @Override
    public boolean isMute() {
        if (hasMediaSession(true)) {
            return mCastSession.getRemoteMediaClient().getMediaStatus().isMute();
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
        if (mCastSession != null && mCastSession.getRemoteMediaClient() != null) {
            return mCastSession.getRemoteMediaClient().getApproximateStreamPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mCastSession != null && mCastSession.getRemoteMediaClient() != null) {
            return mCastSession.getRemoteMediaClient().getStreamDuration();
        }
        return 0;
    }

    @Override
    public boolean hasMediaSession(boolean validateCastConnectingState) {
        if (mCastSession == null) {
            return false;
        }
        boolean isCastSessionValid = mCastSession.isConnected();
        if (validateCastConnectingState) {
            boolean isCastSessionInConnectingMode = mCastSession.isConnecting();
            if (isCastSessionInConnectingMode) {
                return false; // no session to work with
            }
        }
        return isCastSessionValid;
    }

    @Override
    public void switchTextTrack(int index) {
        if (mListeners != null) {
            for (KCastMediaRemoteControlListener listener : mListeners) {
                currentSelectedTextTrack = index;
                listener.onTextTrackSwitch(index);
            }
        }
    }

    @Override
    public int getSelectedTextTrackIndex() {
        return currentSelectedTextTrack;
    }

    @Override
    public void setTextTracks(HashMap<String, Integer> textTrackHash) {
        mTextTracks = textTrackHash;
        updateState(State.TextTracksUpdated);
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
        if (state != State.VolumeChanged && state != State.TextTracksUpdated) {
            mState = state;
        }
        if (mListeners != null) {
            for (KCastMediaRemoteControlListener listener : mListeners) {
                listener.onCastMediaStateChanged(state);
            }
        }
    }
}

