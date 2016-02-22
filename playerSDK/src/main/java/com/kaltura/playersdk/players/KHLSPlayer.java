package com.kaltura.playersdk.players;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.hlsplayersdk.QualityTrack;
import com.kaltura.hlsplayersdk.events.OnDurationChangedListener;
import com.kaltura.hlsplayersdk.types.PlayerStates;
import com.kaltura.playersdk.AlternateAudioTracksInterface;
import com.kaltura.playersdk.LiveStreamInterface;
import com.kaltura.playersdk.QualityTracksInterface;
import com.kaltura.playersdk.TextTracksInterface;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by nissopa on 7/6/15.
 */
public class KHLSPlayer extends FrameLayout implements
        KPlayer,
        TextTracksInterface,
        AlternateAudioTracksInterface,
        QualityTracksInterface,
        com.kaltura.hlsplayersdk.events.OnPlayheadUpdateListener,
        com.kaltura.hlsplayersdk.events.OnErrorListener,
        com.kaltura.hlsplayersdk.events.OnPlayerStateChangeListener,
        com.kaltura.hlsplayersdk.events.OnProgressListener,
        com.kaltura.hlsplayersdk.events.OnAudioTracksListListener,
        com.kaltura.hlsplayersdk.events.OnAudioTrackSwitchingListener,
        com.kaltura.hlsplayersdk.events.OnQualityTracksListListener,
        com.kaltura.hlsplayersdk.events.OnQualitySwitchingListener,
        OnDurationChangedListener,
        LiveStreamInterface {
    private static final String TAG = "KHLSPlayer";
    private HLSPlayerViewController mPlayer;
    private KPlayerListener mListener;
    private KPlayerCallback mCallback;

    public static Set<MediaFormat> supportedFormats(Context context) {
        return Collections.singleton(MediaFormat.hls_clear);
    }

    public KHLSPlayer(Context context) {
        super(context);
        mPlayer = new HLSPlayerViewController(context);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        this.addView(mPlayer, lp);
        mPlayer.registerDurationChanged(this);
        mPlayer.registerPlayerStateChange(this);
        mPlayer.registerPlayheadUpdate(this);
        mPlayer.registerAudioSwitchingChange(this);
        mPlayer.registerError(this);
        mPlayer.registerAudioTracksList(this);
        mPlayer.registerQualitySwitchingChange(this);
        mPlayer.registerQualityTracksList(this);
        mPlayer.registerProgressUpdate(this);
        mPlayer.initialize();
    }

    //region KPlayerController.KPlayer methods
    @Override
    public void setPlayerListener(KPlayerListener listener) {
        mListener = listener;
    }

    @Override
    public void setPlayerCallback(KPlayerCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setPlayerSource(String playerSource) {
        if ( mPlayer.getVideoUrl() == null || !playerSource.equals(mPlayer.getVideoUrl())) {
            mPlayer.setVideoUrl(playerSource);
        }
    }

    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mPlayer.seek((int)(currentPlaybackTime * 1000) + mPlayer.getPlaybackWindowStartTime());
    }

    @Override
    public float getCurrentPlaybackTime() {
        return (float)mPlayer.getCurrentPosition() / 1000;
    }

    @Override
    public float getDuration() {
        return (float)mPlayer.getDuration() / 1000;
    }

    @Override
    public void play() {
        if (!mPlayer.isPlaying()) {
            mPlayer.play();
        }
    }

    @Override
    public void pause() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    @Override
    public void changeSubtitleLanguage(String languageCode) {

    }

    @Override
    public void removePlayer() {
        mPlayer.release();
    }

    @Override
    public void recoverPlayer() {
        mPlayer.recoverRelease();
    }

    @Override
    public void setShouldCancelPlay(boolean shouldCancelPlay) {

    }

    @Override
    public void setLicenseUri(String licenseUri) {
        // Irrelevant, no DRM support.
    }
    //endregion

    //region TextTracksInterface
    @Override
    public void switchTextTrack(int newIndex) {
        mPlayer.switchTextTrack(newIndex);
    }
    //endregion

    //region AlternateAudioTracksInterface
    @Override
    public void hardSwitchAudioTrack(int newAudioIndex) {
        mPlayer.hardSwitchAudioTrack(newAudioIndex);
    }

    @Override
    public void softSwitchAudioTrack(int newAudioIndex) {
        mPlayer.softSwitchAudioTrack(newAudioIndex);
    }
    //endregion

    //region QualityTracksInterface
    @Override
    public void setBufferTime(int newTime) {
        mPlayer.setBufferTime(newTime);
    }

    @Override
    public void switchQualityTrack(int newIndex) {
        mPlayer.switchQualityTrack(newIndex);
    }

    @Override
    public void setAutoSwitch(boolean autoSwitch) {
        mPlayer.setAutoSwitch(autoSwitch);
    }

    @Override
    public float getLastDownloadTransferRate() {
        return mPlayer.getLastDownloadTransferRate();
    }

    @Override
    public float getDroppedFramesPerSecond() {
        return mPlayer.getDroppedFramesPerSecond();
    }

    @Override
    public float getBufferPercentage() {
        return mPlayer.getBufferPercentage();
    }

    @Override
    public int getCurrentQualityIndex() {
        return mPlayer.getCurrentQualityIndex();
    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnPlayheadUpdateListener
    @Override
    public void onPlayheadUpdated(int msec) {
        float updateTimeVal = msec - mPlayer.getPlaybackWindowStartTime();
        mListener.eventWithValue(this, KPlayerListener.TimeUpdateKey, Float.toString(updateTimeVal / 1000));
    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnErrorListener
    @Override
    public void onError(int errorCode, String errorMessage) {
        Log.d("HLS Error", errorMessage);
    }

    @Override
    public void onFatalError(int errorCode, String errorMessage) {
        Log.d("HLS FatalError", errorMessage);
    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnPlayerStateChangeListener
    @Override
    public boolean onStateChanged(PlayerStates state) {
        switch (state) {
            case START:
                mListener.eventWithValue(this, KPlayerListener.LoadedMetaDataKey, "");
                mListener.eventWithValue(this, KPlayerListener.CanPlayKey, null);
                mCallback.playerStateChanged(KPlayerCallback.CAN_PLAY);
                break;
            case LOAD:

                break;
            case PLAY:
                mListener.eventWithValue(this, KPlayerListener.PlayKey, null);
                break;
            case PAUSE:
                mListener.eventWithValue(this, KPlayerListener.PauseKey, null);
                break;
            case END:
                mListener.contentCompleted(this);
                mCallback.playerStateChanged(KPlayerCallback.ENDED);
                break;
            case SEEKED:
                mListener.eventWithValue(this, KPlayerListener.SeekedKey, null);
                break;
            case SEEKING:
                break;
        }
        return false;
    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnProgressListener
    @Override
    public void onProgressUpdate(int progress) {
        mListener.eventWithValue(this, KPlayerListener.BufferingChangeKey, progress < 100 ? "true" : "false");
    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnAudioTracksListListener
    @Override
    public void OnAudioTracksList(List<String> list, int defaultTrackIndex) {

    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnAudioTrackSwitchingListener
    @Override
    public void onAudioSwitchingStart(int oldTrackIndex, int newTrackIndex) {

    }

    @Override
    public void onAudioSwitchingEnd(int newTrackIndex) {

    }
    //endregion
    
    
    private static JSONObject qualityTrackToJSONObject(int originalIndex, QualityTrack track) {
        Map<String, Object> objectMap = new HashMap<>();
        objectMap.put("originalIndex", originalIndex);
        objectMap.put("trackId", track.trackId);
        objectMap.put("bitrate", track.bitrate);
        objectMap.put("height", track.height);
        objectMap.put("width", track.width);
        return new JSONObject(objectMap);
    }

    //region com.kaltura.hlsplayersdk.events.OnQualityTracksListListener
    @Override
    public void OnQualityTracksList(List<QualityTrack> list, int defaultTrackIndex) {
        JSONArray jsonArray = new JSONArray();
        
        for (ListIterator<QualityTrack> it = list.listIterator(); it.hasNext(); ) {
            QualityTrack track = it.next();
            if (track.width >= 1024) {
                continue;
            }
            jsonArray.put(qualityTrackToJSONObject(it.nextIndex() - 1, track));
        }
        
        
        JSONObject jsonResponse = new JSONObject();
        try {
            jsonResponse.put("tracks", jsonArray);
            jsonResponse.put("defaultTrackIndex", defaultTrackIndex);
        } catch (JSONException e) {
            Log.wtf(TAG, "JSONException in put can only happen with double values", e);
        }

        mListener.eventWithJSON(this, KPlayerListener.FlavorsListChangedKey, jsonResponse.toString());
    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnQualitySwitchingListener
    @Override
    public void onQualitySwitchingStart(int oldTrackIndex, int newTrackIndex) {
        JSONObject jsonResponse = new JSONObject();
        try {
            jsonResponse = new JSONObject().put("oldIndex", oldTrackIndex).put("newIndex", newTrackIndex);
        } catch (JSONException e) {
            Log.wtf(TAG, "JSONException in put can only happen with double values", e);
        }
        mListener.eventWithJSON(this, KPlayerListener.SourceSwitchingStartedKey, jsonResponse.toString());
    }

    @Override
    public void onQualitySwitchingEnd(int newTrackIndex) {
        JSONObject jsonResponse = new JSONObject();
        try {
            jsonResponse = new JSONObject().put("newIndex", newTrackIndex);
        } catch (JSONException e) {
            Log.wtf(TAG, "JSONException in put can only happen with double values", e);
        }
        mListener.eventWithJSON(this, KPlayerListener.SourceSwitchingEndKey, jsonResponse.toString());
    }
    //endregion

    //region OnDurationChangedListener
    @Override
    public void onDurationChanged(int msec) {
        mListener.eventWithValue(this, KPlayerListener.DurationChangedKey, Float.toString(msec / 1000));
    }
    //endregion

    //region LiveStreamInterface
    @Override
    public void switchToLive() {
        mPlayer.goToLive();
    }
    //endregion

}
