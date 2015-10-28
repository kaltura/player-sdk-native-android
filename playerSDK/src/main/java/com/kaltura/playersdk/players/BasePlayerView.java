package com.kaltura.playersdk.players;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.kaltura.playersdk.events.Listener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnProgressUpdateListener;
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

    protected class ListenersExecutor {



        boolean executeOnStateChanged(PlayerStates state){
            OnPlayerStateChangeListener.PlayerStateChangeInputObject inputObject = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
            inputObject.state = state;
            return BasePlayerView.this.executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, inputObject);
        }


        boolean executeOnProgressUpdate(int progress){
            OnProgressUpdateListener.ProgressInputObject input = new OnProgressUpdateListener.ProgressInputObject();
            input.progress = progress;
            return BasePlayerView.this.executeListener(Listener.EventType.PROGRESS_UPDATE_LISTENER_TYPE, input);
        }




    }

}
