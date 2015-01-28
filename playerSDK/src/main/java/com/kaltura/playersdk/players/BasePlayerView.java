package com.kaltura.playersdk.players;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.kaltura.playersdk.events.Listener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by itayi on 1/28/15.
 */
public abstract class BasePlayerView extends FrameLayout {

    private Map<Listener.EventType, Listener> mEventTypeListenerMap = new HashMap<>();

    protected final static Listener.EventType[] DEFAULT_LISTENERS = {
            Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE,
            Listener.EventType.ERROR_LISTENER_TYPE,
            Listener.EventType.PLAYHEAD_UPDATE_LISTENER_TYPE,
            Listener.EventType.PROGRESS_LISTENER_TYPE,
            Listener.EventType.KPLAYER_EVENT_LISTENER_TYPE
    };

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

    public abstract void play();

    public abstract void pause();

    public abstract void stop();

    public abstract void seek(int msec);

    public abstract boolean isPlaying();

    public abstract boolean canPause();

    // events
//    public abstract void registerPlayerStateChange(OnPlayerStateChangeListener listener);
//
//    public abstract void registerError(OnErrorListener listener);
//
//    public abstract void registerPlayheadUpdate(OnPlayheadUpdateListener listener);
//
//    public abstract void removePlayheadUpdateListener();
//
//    public abstract void registerProgressUpdate(OnProgressListener listener);


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
        return Arrays.asList(DEFAULT_LISTENERS);
    }

    final protected Listener getListener(Listener.EventType event){
        return mEventTypeListenerMap.get(event);
    }

    public void registerListener(Listener listener){
        if ( mEventTypeListenerMap.containsKey(listener.getEventType()) ){
            mEventTypeListenerMap.put(listener.getEventType(), listener);
        }
    }

    public void removeListener(Listener.EventType eventType){
        if ( mEventTypeListenerMap.containsKey(eventType) ){
            mEventTypeListenerMap.put(eventType, null);
        }
    }

    protected void executeListener(Listener.EventType eventType, Listener.InputObject inputObject){
        Listener listener = mEventTypeListenerMap.get(eventType);
        if (listener != null){
            listener.executeCallback(inputObject);
        }
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
}
