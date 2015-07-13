package com.kaltura.playersdk.players;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
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

import org.w3c.dom.NameList;

import java.util.List;

/**
 * Created by nissopa on 7/6/15.
 */
public class KHLSPlayer extends FrameLayout implements
        KPlayerController.KPlayer,
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
    private HLSPlayerViewController mPlayer;
    private KPlayerListener mListener;
    private KPlayerCallback mCallback;

    public KHLSPlayer(Activity activity) {
        super(activity);
        mPlayer = new HLSPlayerViewController(activity);
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
    public String getPlayerSource() {
        return mPlayer.getVideoUrl();
    }

    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mPlayer.seek((int)currentPlaybackTime * 1000);
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
    public boolean isKPlayer() {
        return false;
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
        mListener.eventWithValue(this, KPlayer.TimeUpdateKey, Float.toString((float)msec / 1000));
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
                mListener.eventWithValue(this, KPlayer.LoadedMetaDataKey, "");
                mListener.eventWithValue(this, KPlayer.CanPlayKey, null);
//                mPlayer.registerProgressUpdate(this);
//                mCallback.playerStateChanged(KPlayerController.CAN_PLAY);
                break;
            case LOAD:

                break;
            case PLAY:
                mListener.eventWithValue(this, KPlayer.PlayKey, null);
                break;
            case PAUSE:
                mListener.eventWithValue(this, KPlayer.PauseKey, null);
                break;
            case END:
                mListener.contentCompleted(this);
//                mCallback.playerStateChanged(KPlayerController.ENDED);
                break;
            case SEEKED:
                mListener.eventWithValue(this, KPlayer.SeekedKey, null);
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
        mListener.eventWithValue(this, KPlayer.ProgressKey, Float.toString((float)progress / mPlayer.getDuration()));
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

    //region com.kaltura.hlsplayersdk.events.OnQualityTracksListListener
    @Override
    public void OnQualityTracksList(List<QualityTrack> list, int defaultTrackIndex) {

    }
    //endregion

    //region com.kaltura.hlsplayersdk.events.OnQualitySwitchingListener
    @Override
    public void onQualitySwitchingStart(int oldTrackIndex, int newTrackIndex) {

    }

    @Override
    public void onQualitySwitchingEnd(int newTrackIndex) {

    }
    //endregion

    //region OnDurationChangedListener
    @Override
    public void onDurationChanged(int msec) {
        mListener.eventWithValue(this, KPlayer.DurationChangedKey, Float.toString(msec / 1000));
    }
    //endregion

    //region LiveStreamInterface
    @Override
    public void switchToLive() {
        mPlayer.goToLive();
    }
    //endregion

}
