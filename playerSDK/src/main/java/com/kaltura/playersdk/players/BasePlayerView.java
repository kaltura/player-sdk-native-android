package com.kaltura.playersdk.players;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.google.android.gms.cast.CastDevice;
import com.kaltura.playersdk.QualityTrack;
import com.kaltura.playersdk.events.KPlayerEventListener;
import com.kaltura.playersdk.events.KPlayerJsCallbackReadyListener;
import com.kaltura.playersdk.events.Listener;
import com.kaltura.playersdk.events.OnAudioTrackSwitchingListener;
import com.kaltura.playersdk.events.OnAudioTracksListListener;
import com.kaltura.playersdk.events.OnCastDeviceChangeListener;
import com.kaltura.playersdk.events.OnCastRouteDetectedListener;
import com.kaltura.playersdk.events.OnDurationChangedListener;
import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressUpdateListener;
import com.kaltura.playersdk.events.OnQualitySwitchingListener;
import com.kaltura.playersdk.events.OnQualityTracksListListener;
import com.kaltura.playersdk.events.OnTextTrackChangeListener;
import com.kaltura.playersdk.events.OnTextTrackTextListener;
import com.kaltura.playersdk.events.OnTextTracksListListener;
import com.kaltura.playersdk.events.OnToggleFullScreenListener;
import com.kaltura.playersdk.events.OnWebViewMinimizeListener;
import com.kaltura.playersdk.types.PlayerStates;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by itayi on 1/28/15.
 */
public abstract class BasePlayerView extends FrameLayout {

    protected final static Listener.EventType[] DEFAULT_LISTENERS = {
            Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE,
            Listener.EventType.ERROR_LISTENER_TYPE,
            Listener.EventType.PLAYHEAD_UPDATE_LISTENER_TYPE,
            Listener.EventType.PROGRESS_UPDATE_LISTENER_TYPE,
            Listener.EventType.KPLAYER_EVENT_LISTENER_TYPE
    };
    private Map<Listener.EventType, Listener> mEventTypeListenerMap = new HashMap<>();

    protected final ListenersExecutor mListenerExecutor = new ListenersExecutor();

    private int mCurrentPosition = 0;

    public BasePlayerView(Context context) {
        super(context);
        initListeners();
    }

    public BasePlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initListeners();
    }

    public BasePlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initListeners();
    }

    public abstract String getVideoUrl();

    public abstract void setVideoUrl(String url);

    public abstract int getDuration();

    public int getCurrentPosition(){
        return mCurrentPosition;
    }

    public abstract void play();

    public abstract void pause();

    public abstract void stop();

    public abstract void seek(int msec);

    public abstract boolean isPlaying();

    public abstract boolean canPause();

    // events

    final private void initListeners(){
        List<Listener.EventType> eventsArr = getCompatibleListenersList();
        if (eventsArr != null){
            for (Listener.EventType eventType : eventsArr){
                mEventTypeListenerMap.put(eventType,null);
            }
        }
    }

    /*
    Override if you would like to add more listeners
     */
    protected List<Listener.EventType> getCompatibleListenersList(){
        List<Listener.EventType> list = new ArrayList<>();
        for (Listener.EventType eventType : DEFAULT_LISTENERS){
            list.add(eventType);
        }
        return list;

    }

    final protected Listener getListener(Listener.EventType event){
        return mEventTypeListenerMap.get(event);
    }

    public void registerListener(Listener listener){
        if (listener != null) {
            if (mEventTypeListenerMap.containsKey(listener.getEventType())) {
                mEventTypeListenerMap.put(listener.getEventType(), listener);
            }
        }
    }

    public void removeListener(Listener.EventType eventType){
        if ( mEventTypeListenerMap.containsKey(eventType) ){
            mEventTypeListenerMap.put(eventType, null);
        }
    }

    protected boolean executeListener(Listener.EventType eventType, Listener.InputObject inputObject){
        Listener listener = mEventTypeListenerMap.get(eventType);
        if (listener != null){
            return listener.executeCallback(inputObject);
        }

        return false;
    }
    /**
     * Set starting point in milliseconds for the next play
     * @param point
     */
    public abstract void setStartingPoint(int point);

    /**
     * Some players require release when application goes to background
     */
    public abstract void release();

    /**
     * Recover from release
     */
    public abstract void recoverRelease();

    protected class ListenersExecutor {

        boolean executeOnKPlayerEvent(Object body){
            KPlayerEventListener.KPlayerInputObject input = new KPlayerEventListener.KPlayerInputObject();
            input.body = body;
            return BasePlayerView.this.executeListener(Listener.EventType.KPLAYER_EVENT_LISTENER_TYPE, input);
        }

        boolean executeJsCallbackReady(){
            KPlayerJsCallbackReadyListener.JsCallbackReadyInputObject input = new KPlayerJsCallbackReadyListener.JsCallbackReadyInputObject();
            return BasePlayerView.this.executeListener(Listener.EventType.JS_CALLBACK_READY_LISTENER_TYPE, input);
        }

        boolean executeOnAudioTracksList( List<String> list, int defaultTrackIndex ){
            OnAudioTracksListListener.AudioTracksListInputObject input = new OnAudioTracksListListener.AudioTracksListInputObject();
            input.list = list;
            input.defaultTrackIndex = defaultTrackIndex;
            return BasePlayerView.this.executeListener(Listener.EventType.AUDIO_TRACKS_LIST_LISTENER_TYPE, input);
        }

        boolean executeOnAudioSwitchingStart( int oldTrackIndex, int newTrackIndex ){
            OnAudioTrackSwitchingListener.AudioTrackSwitchingInputObject input =  new OnAudioTrackSwitchingListener.AudioTrackSwitchingInputObject();
            input.newTrackIndex = newTrackIndex;
            input.oldTrackIndex = oldTrackIndex;
            input.methodChoice = OnAudioTrackSwitchingListener.AudioTrackSwitchingInputObject.MethodChoice.START;
            return BasePlayerView.this.executeListener(Listener.EventType.AUDIO_TRACK_SWITCH_LISTENER_TYPE, input);
        }

        boolean executeonAudioSwitchingEnd( int newTrackIndex ){
            OnAudioTrackSwitchingListener.AudioTrackSwitchingInputObject input =  new OnAudioTrackSwitchingListener.AudioTrackSwitchingInputObject();
            input.newTrackIndex = newTrackIndex;
            input.methodChoice = OnAudioTrackSwitchingListener.AudioTrackSwitchingInputObject.MethodChoice.END;
            return BasePlayerView.this.executeListener(Listener.EventType.AUDIO_TRACK_SWITCH_LISTENER_TYPE, input);
        }

        boolean executeonCastDeviceChange(CastDevice oldDevice, CastDevice newDevice){
            OnCastDeviceChangeListener.CastDeviceChangeInputObject input = new OnCastDeviceChangeListener.CastDeviceChangeInputObject();
            input.newDevice = newDevice;
            input.oldDevice = oldDevice;
            return BasePlayerView.this.executeListener(Listener.EventType.CAST_DEVICE_CHANGE_LISTENER_TYPE, input);
        }

        boolean executeonCastRouteDetected(){
            OnCastRouteDetectedListener.CastRouteDetectedInputObject input = new OnCastRouteDetectedListener.CastRouteDetectedInputObject();
            return BasePlayerView.this.executeListener(Listener.EventType.CAST_ROUTE_DETECTED_LISTENER_TYPE, input);
        }

        boolean executeOnError(int errorCode, String errorMessage){
            OnErrorListener.ErrorInputObject input = new OnErrorListener.ErrorInputObject();
            input.errorMessage = errorMessage;
            input.errorCode = errorCode;
            return BasePlayerView.this.executeListener(Listener.EventType.ERROR_LISTENER_TYPE, input);
        }

        boolean executeOnStateChanged(PlayerStates state){
            OnPlayerStateChangeListener.PlayerStateChangeInputObject inputObject = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
            inputObject.state = state;
            return BasePlayerView.this.executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, inputObject);
        }

        boolean executeOnPlayheadUpdated(int msec){
            mCurrentPosition = msec;
            OnPlayheadUpdateListener.PlayheadUpdateInputObject input = new OnPlayheadUpdateListener.PlayheadUpdateInputObject();
            input.msec = msec;
            return BasePlayerView.this.executeListener(Listener.EventType.PLAYHEAD_UPDATE_LISTENER_TYPE, input);
        }

        boolean executeOnProgressUpdate(int progress){
            OnProgressUpdateListener.ProgressInputObject input = new OnProgressUpdateListener.ProgressInputObject();
            input.progress = progress;
            return BasePlayerView.this.executeListener(Listener.EventType.PROGRESS_UPDATE_LISTENER_TYPE, input);
        }

        boolean executeOnQualitySwitchingStart( int oldTrackIndex, int newTrackIndex ){
            OnQualitySwitchingListener.QualitySwitchingInputObject input = new OnQualitySwitchingListener.QualitySwitchingInputObject();
            input.methodChoice = OnQualitySwitchingListener.QualitySwitchingInputObject.MethodChoice.START;
            input.oldTrackIndex = oldTrackIndex;
            input.newTrackIndex = newTrackIndex;
            return BasePlayerView.this.executeListener(Listener.EventType.QUALITY_SWITCHING_LISTENER_TYPE,input);
        }

        boolean executeOnQualitySwitchingEnd( int newTrackIndex ){
            OnQualitySwitchingListener.QualitySwitchingInputObject input = new OnQualitySwitchingListener.QualitySwitchingInputObject();
            input.methodChoice = OnQualitySwitchingListener.QualitySwitchingInputObject.MethodChoice.END;
            input.newTrackIndex = newTrackIndex;
            return BasePlayerView.this.executeListener(Listener.EventType.QUALITY_SWITCHING_LISTENER_TYPE,input);
        }

        boolean executeOnQualityTracksList( List<QualityTrack> list, int defaultTrackIndex ){
            OnQualityTracksListListener.QualityTracksListInputObject input = new OnQualityTracksListListener.QualityTracksListInputObject();
            input.list = list;
            input.defaultTrackIndex = defaultTrackIndex;
            return BasePlayerView.this.executeListener(Listener.EventType.QUALITY_TRACKS_LIST_LISTENER_TYPE, input);
        }

        boolean executeOnTextTrackChanged( int newTrackIndex ){
            OnTextTrackChangeListener.TextTrackChangedInputObject input = new OnTextTrackChangeListener.TextTrackChangedInputObject();
            input.newTrackIndex = newTrackIndex;
            return BasePlayerView.this.executeListener(Listener.EventType.TEXT_TRACK_CHANGE_LISTENER_TYPE, input);
        }

        boolean executeOnTextTracksList( List<String> list, int defaultTrackIndex ){
            OnTextTracksListListener.TextTracksListInputObject input = new OnTextTracksListListener.TextTracksListInputObject();
            input.list = list;
            input.defaultTrackIndex = defaultTrackIndex;
            return BasePlayerView.this.executeListener(Listener.EventType.TEXT_TRACK_LIST_LISTENER_TYPE, input);
        }

        boolean executeOnSubtitleText(double startTime, double length, String buffer){
            OnTextTrackTextListener.TextTrackTextInputObject input = new OnTextTrackTextListener.TextTrackTextInputObject();
            input.startTime = startTime;
            input.length = length;
            input.buffer = buffer;
            return BasePlayerView.this.executeListener(Listener.EventType.TEXT_TRACK_TEXT_LISTENER_TYPE, input);
        }

        boolean executeOnToggleFullScreen(){
            OnToggleFullScreenListener.ToggleFullScreenInputObject input = new OnToggleFullScreenListener.ToggleFullScreenInputObject();
            return BasePlayerView.this.executeListener(Listener.EventType.TOGGLE_FULLSCREEN_LISTENER_TYPE, input);
        }

        boolean executeSetMinimize( boolean minimize ){
            OnWebViewMinimizeListener.WebViewMinimizeInputObject input = new OnWebViewMinimizeListener.WebViewMinimizeInputObject();
            input.minimize = minimize;
            return BasePlayerView.this.executeListener(Listener.EventType.WEB_VIEW_MINIMIZE_LISTENER_TYPE, input);
        }

        boolean executeOnDurationChanged(int mSec){
            OnDurationChangedListener.DurationChangedInputObject input = new OnDurationChangedListener.DurationChangedInputObject();
            input.msec = mSec;
            return BasePlayerView.this.executeListener(Listener.EventType.DURATION_CHANGED_LISTENER_TYPE, input);
        }
    }

}
