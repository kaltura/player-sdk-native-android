package com.kaltura.playersdk.players;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.hlsplayersdk.events.OnDurationChangedListener;
import com.kaltura.hlsplayersdk.types.PlayerStates;
import com.kaltura.playersdk.AlternateAudioTracksInterface;
import com.kaltura.playersdk.LiveStreamInterface;
import com.kaltura.playersdk.QualityTrack;
import com.kaltura.playersdk.QualityTracksInterface;
import com.kaltura.playersdk.TextTracksInterface;
import com.kaltura.playersdk.events.Listener;
import com.kaltura.playersdk.types.TrackType;

import java.util.ArrayList;
import java.util.List;

public class HLSPlayer extends BasePlayerView implements
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
        LiveStreamInterface
{

    private static final String TAG = HLSPlayer.class.getSimpleName();
    private HLSPlayerViewController mPlayer;

    public HLSPlayer(Activity activity) {
        super(activity);
        mPlayer = new HLSPlayerViewController(activity);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        this.addView(mPlayer, lp);
        mPlayer.initialize();
    }

    public void close(){
        mPlayer.close();
        mPlayer = null;
    }

    @Override
    public String getVideoUrl() {
        return mPlayer.getVideoUrl();
    }

    @Override
    public void setVideoUrl(String url) {
        if ( mPlayer.getVideoUrl() == null || !url.equals(mPlayer.getVideoUrl())) {
            mPlayer.setVideoUrl(url);
        }

    }

    @Override
    public int getDuration() {
        return mPlayer.getDuration() - 1;
    }

    @Override
    public void play() {
        mPlayer.play();
    }

    @Override
    public void pause() {
        mPlayer.pause();
    }

    @Override
    public void stop() {
        mPlayer.stop();
    }

    @Override
    public void seek(int msec) {
        mPlayer.seek(msec);
    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public boolean canPause() {
        return mPlayer != null;
    }


    @Override
    public void setStartingPoint(int point) {
        mPlayer.setStartingPoint(point);
    }

    @Override
    public void release() {
        mPlayer.release();
    }

    @Override
    public void recoverRelease() {
        mPlayer.recoverRelease();
    }

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

    @Override
    public void hardSwitchAudioTrack(int newAudioIndex) {
        mPlayer.hardSwitchAudioTrack(newAudioIndex);

    }

    @Override
    public void softSwitchAudioTrack(int newAudioIndex) {
        mPlayer.softSwitchAudioTrack(newAudioIndex);
    }

    @Override
    public void switchTextTrack(int newIndex) {
        mPlayer.switchTextTrack(newIndex);

    }

    @Override
    public void switchToLive() {
        mPlayer.goToLive();
    }
    /////////////////////////////////////////////////////////
    //
    //      HlsPlayerSDK Listeners
    //
    ////////////////////////////////////////////////////////

    @Override
    public boolean onStateChanged(PlayerStates state) {
        com.kaltura.playersdk.types.PlayerStates kState;
        switch (state) {
            case START:
                kState = com.kaltura.playersdk.types.PlayerStates.START;
                break;
            case LOAD:
                kState = com.kaltura.playersdk.types.PlayerStates.LOAD;
                break;
            case PLAY:
                kState = com.kaltura.playersdk.types.PlayerStates.PLAY;
                break;
            case PAUSE:
                kState = com.kaltura.playersdk.types.PlayerStates.PAUSE;
                break;
            case END:
                kState = com.kaltura.playersdk.types.PlayerStates.END;
                break;
            case SEEKING:
                kState = com.kaltura.playersdk.types.PlayerStates.SEEKING;
                break;
            case SEEKED:
                kState = com.kaltura.playersdk.types.PlayerStates.SEEKED;
                break;
            default:
                kState = com.kaltura.playersdk.types.PlayerStates.START;
        }

        mListenerExecutor.executeOnStateChanged(kState);
        return false;

    }

    @Override
    public void onPlayheadUpdated(int msec) {
        mListenerExecutor.executeOnPlayheadUpdated(msec);
    }

    @Override
    public void onProgressUpdate(int progress) {
        mListenerExecutor.executeOnProgressUpdate(progress);
    }

    @Override
    public void OnQualityTracksList(List<com.kaltura.hlsplayersdk.QualityTrack> list, int defaultTrackIndex) {
        List<QualityTrack> newList = new ArrayList<QualityTrack>();
        for ( int i=0; i < list.size(); i++ ) {
            com.kaltura.hlsplayersdk.QualityTrack currentTrack = list.get(i);
            QualityTrack newTrack = new QualityTrack();
            newTrack.bitrate = currentTrack.bitrate;
            newTrack.height = currentTrack.height;
            newTrack.width = currentTrack.width;
            newTrack.trackId = currentTrack.trackId;
            newTrack.type = currentTrack.type == com.kaltura.hlsplayersdk.types.TrackType.VIDEO ? TrackType.VIDEO: TrackType.AUDIO;
            newList.add(newTrack);
        }
        mListenerExecutor.executeOnQualityTracksList(newList,defaultTrackIndex);
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        Log.e(TAG, "HLS Error Occurred - error code: " + errorCode + " " + "msg: " + errorMessage);
    }

    @Override
    public void onFatalError(int errorCode, String errorMessage) {
        mListenerExecutor.executeOnError(errorCode,errorMessage);
    }

    @Override
    protected List<Listener.EventType> getCompatibleListenersList() {
        List<Listener.EventType> list = super.getCompatibleListenersList();
        list.add(Listener.EventType.AUDIO_TRACK_SWITCH_LISTENER_TYPE);
        list.add(Listener.EventType.AUDIO_TRACKS_LIST_LISTENER_TYPE);
        list.add(Listener.EventType.QUALITY_SWITCHING_LISTENER_TYPE);
        list.add(Listener.EventType.QUALITY_TRACKS_LIST_LISTENER_TYPE);
        list.add(Listener.EventType.DURATION_CHANGED_LISTENER_TYPE);
        return list;
    }

    @Override
    public void removeListener(Listener.EventType eventType) {
        super.removeListener(eventType);
        registerHLSListener(eventType, false);
    }

    @Override
    public void registerListener(Listener listener){
        super.registerListener(listener);
        if (listener != null) {
            registerHLSListener(listener.getEventType(), true);
        }
    }

//    TODO: discuss this issue with dp so they will change their player listeners scheme
    private void registerHLSListener(Listener.EventType eventType, boolean shouldRegister){

        switch(eventType){
            case JS_CALLBACK_READY_LISTENER_TYPE:
                break;
            case AUDIO_TRACKS_LIST_LISTENER_TYPE:
                mPlayer.registerAudioTracksList(shouldRegister ? this : null);
                break;
            case AUDIO_TRACK_SWITCH_LISTENER_TYPE:
                mPlayer.registerAudioSwitchingChange(shouldRegister ? this : null);
                break;
            case CAST_DEVICE_CHANGE_LISTENER_TYPE:
                break;
            case CAST_ROUTE_DETECTED_LISTENER_TYPE:
                break;
            case ERROR_LISTENER_TYPE:
                    mPlayer.registerError(shouldRegister ? this : null);
                break;
            case PLAYER_STATE_CHANGE_LISTENER_TYPE:
                    mPlayer.registerPlayerStateChange(shouldRegister ? this : null);
                break;
            case PLAYHEAD_UPDATE_LISTENER_TYPE:
                    mPlayer.registerPlayheadUpdate(shouldRegister ? this : null);
                break;
            case PROGRESS_UPDATE_LISTENER_TYPE:
                    mPlayer.registerProgressUpdate(shouldRegister ? this : null);
                break;
            case QUALITY_SWITCHING_LISTENER_TYPE:
                    mPlayer.registerQualitySwitchingChange(shouldRegister ? this : null);
                break;
            case QUALITY_TRACKS_LIST_LISTENER_TYPE:
                    mPlayer.registerQualityTracksList(shouldRegister ? this : null);
                break;
            case TEXT_TRACK_CHANGE_LISTENER_TYPE:
                break;
            case TEXT_TRACK_LIST_LISTENER_TYPE:
                break;
            case TEXT_TRACK_TEXT_LISTENER_TYPE:
                break;
            case TOGGLE_FULLSCREEN_LISTENER_TYPE:
                break;
            case WEB_VIEW_MINIMIZE_LISTENER_TYPE:
                break;
            case KPLAYER_EVENT_LISTENER_TYPE:

                break;
            case DURATION_CHANGED_LISTENER_TYPE:
                mPlayer.registerDurationChanged(shouldRegister ? this : null);
        }
    }


    @Override
    public void onAudioSwitchingStart(int oldTrackIndex, int newTrackIndex) {
        mListenerExecutor.executeOnAudioSwitchingStart(oldTrackIndex, newTrackIndex);
    }

    @Override
    public void onAudioSwitchingEnd(int newTrackIndex) {
        mListenerExecutor.executeonAudioSwitchingEnd(newTrackIndex);
    }

    @Override
    public void OnAudioTracksList(List<String> list, int defaultTrackIndex) {
        mListenerExecutor.executeOnAudioTracksList(list,defaultTrackIndex);
    }

    @Override
    public void onQualitySwitchingStart(int oldTrackIndex, int newTrackIndex) {

    }

    @Override
    public void onQualitySwitchingEnd(int newTrackIndex) {

    }

    @Override
    public void onDurationChanged(int msec) {
        mListenerExecutor.executeOnDurationChanged(msec);
    }


}