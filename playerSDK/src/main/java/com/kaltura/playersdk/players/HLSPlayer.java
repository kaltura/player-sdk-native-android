package com.kaltura.playersdk.players;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.hlsplayersdk.types.PlayerStates;
import com.kaltura.playersdk.AlternateAudioTracksInterface;
import com.kaltura.playersdk.QualityTracksInterface;
import com.kaltura.playersdk.TextTracksInterface;
import com.kaltura.playersdk.events.Listener;
import com.kaltura.playersdk.events.OnAudioTrackSwitchingListener;
import com.kaltura.playersdk.events.OnAudioTracksListListener;
import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.events.OnQualitySwitchingListener;
import com.kaltura.playersdk.events.OnQualityTracksListListener;
import com.kaltura.playersdk.events.OnTextTrackChangeListener;
import com.kaltura.playersdk.events.OnTextTrackTextListener;
import com.kaltura.playersdk.events.OnTextTracksListListener;

import java.util.List;

public class HLSPlayer extends BasePlayerView implements TextTracksInterface, AlternateAudioTracksInterface, QualityTracksInterface, com.kaltura.hlsplayersdk.events.OnPlayheadUpdateListener, com.kaltura.hlsplayersdk.events.OnErrorListener, com.kaltura.hlsplayersdk.events.OnPlayerStateChangeListener, com.kaltura.hlsplayersdk.events.OnProgressListener, com.kaltura.hlsplayersdk.events.OnQualityTracksListListener{

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
        //TODO add to HLSPlayerViewController

        return mPlayer != null;
    }


    @Override
    public void setStartingPoint(int point) {
        mPlayer.setStartingPoint(point);

    }

    @Override
    public void release() {
        mPlayer.release();
        //TODO add to HLSPlayerViewController

    }

    @Override
    public void recoverRelease() {
        mPlayer.recoverRelease();
        //TODO add to HLSPlayerViewController

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

    /////////////////////////////////////////////////////////
    //
    //      HlsPlayerSDK Listeners
    //
    ////////////////////////////////////////////////////////


    @Override
    public void registerQualityTracksList(final OnQualityTracksListListener listener) {
//        mQualityTracksListener = listener;
//        mPlayer.registerQualityTracksList(listener != null ? this : null);
    }

    @Override
    public void registerQualitySwitchingChange(OnQualitySwitchingListener listener) {
        //mPlayer.registerQualitySwitchingChange(listener);

    }

    @Override
    public void registerAudioTracksList(OnAudioTracksListListener listener) {
        //mPlayer.registerAudioTracksList(listener);
    }

    @Override
    public void registerAudioSwitchingChange(OnAudioTrackSwitchingListener listener) {
        //mPlayer.registerAudioSwitchingChange(listener);

    }

    @Override
    public void registerTextTracksList(OnTextTracksListListener listener) {
        //mPlayer.registerTextTracksList(listener);

    }

    @Override
    public void registerTextTrackChanged(OnTextTrackChangeListener listener) {
        //mPlayer.registerTextTrackChanged(listener);

    }

    @Override
    public void registerTextTrackText(OnTextTrackTextListener listener) {
        //mPlayer.registerTextTrackText(listener);

    }

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
        OnPlayerStateChangeListener.PlayerStateChangeInputObject inputObject = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
        inputObject.state = kState;
        executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, inputObject);

        return false;

    }


    @Override
    public void onPlayheadUpdated(int msec) {
        OnPlayheadUpdateListener.PlayheadUpdateInputObject inputObject = new OnPlayheadUpdateListener.PlayheadUpdateInputObject();
        inputObject.msec = msec;
        executeListener(Listener.EventType.PLAYHEAD_UPDATE_LISTENER_TYPE, inputObject);
    }

    @Override
    public void onProgressUpdate(int progress) {
        OnProgressListener.ProgressInputObject inputObject = new OnProgressListener.ProgressInputObject();
        inputObject.progress = progress;
        executeListener(Listener.EventType.PROGRESS_LISTENER_TYPE, inputObject);
    }

    @Override
    public void OnQualityTracksList(List<com.kaltura.hlsplayersdk.QualityTrack> list, int defaultTrackIndex) {
//        if (mQualityTracksListener != null){
//            List<QualityTrack> newList = new ArrayList<QualityTrack>();
//            for ( int i=0; i < list.size(); i++ ) {
//                com.kaltura.hlsplayersdk.QualityTrack currentTrack = list.get(i);
//                QualityTrack newTrack = new QualityTrack();
//                newTrack.bitrate = currentTrack.bitrate;
//                newTrack.height = currentTrack.height;
//                newTrack.width = currentTrack.width;
//                newTrack.trackId = currentTrack.trackId;
//                newTrack.type = currentTrack.type == com.kaltura.hlsplayersdk.types.TrackType.VIDEO ? TrackType.VIDEO: TrackType.AUDIO;
//                newList.add(newTrack);
//            }
//            mQualityTracksListener.OnQualityTracksList(newList, defaultTrackIndex);
//        }
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        Log.e(TAG, "HLS Error Occurred - error code: " + errorCode + " " + "msg: " + errorMessage);
    }

    @Override
    public void onFatalError(int errorCode, String errorMessage) {
        OnErrorListener.ErrorInputObject inputObject = new OnErrorListener.ErrorInputObject();
        inputObject.errorCode = errorCode;
        inputObject.errorMessage = errorMessage;
        executeListener(Listener.EventType.ERROR_LISTENER_TYPE, inputObject);
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
                break;
            case AUDIO_TRACK_SWITCH_LISTENER_TYPE:
                break;
            case CAST_DEVICE_CHANGE_LISTENER_TYPE:
                break;
            case CAST_ROUTE_DETECTED_LISTENER_TYPE:
                break;
            case ERROR_LISTENER_TYPE:
                if(shouldRegister) {
                    mPlayer.registerError(this);
                }else {
                    mPlayer.registerError(null);
                }
                break;
            case PLAYER_STATE_CHANGE_LISTENER_TYPE:
                if(shouldRegister) {
                    mPlayer.registerPlayerStateChange(this);
                }else{
                    mPlayer.registerPlayerStateChange(null);
                }
                break;
            case PLAYHEAD_UPDATE_LISTENER_TYPE:
                if(shouldRegister) {
                    mPlayer.registerPlayheadUpdate(this);
                }else{
                    mPlayer.registerPlayheadUpdate(null);
                }
                break;
            case PROGRESS_LISTENER_TYPE:
                if(shouldRegister) {
                    mPlayer.registerProgressUpdate(this);
                }else{
                    mPlayer.registerProgressUpdate(null);
                }
                break;
            case QUALITY_SWITCHING_LISTENER_TYPE:
//                    mPlayer.registerQualitySwitchingChange(this);
                break;
            case QUALITY_TRACKS_LIST_LISTENER_TYPE:
                if(shouldRegister) {
                    mPlayer.registerQualityTracksList(this);
                }else {
                    mPlayer.registerQualityTracksList(null);
                }
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
        }
    }


}