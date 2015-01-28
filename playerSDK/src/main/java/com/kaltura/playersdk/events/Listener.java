package com.kaltura.playersdk.events;

import android.util.Log;

/**
 * Created by itayi on 1/28/15.
 */
public abstract class Listener {

    private static String TAG;
    protected EventType mEventType;

    final void executeCallback(InputObject inputObject)
    {
        if(checkValidInputObjectType(inputObject)){
            executeInternalCallback(inputObject);
        }else{
            Log.e(TAG, "Recieved wrong inputObject type");
        }
    }

    abstract protected void executeInternalCallback(InputObject inputObject);
    abstract protected boolean checkValidInputObjectType(InputObject inputObject);

    public EventType getEventType(){
        return mEventType;
    }

    abstract protected void setEventType();

    public Listener(){
        TAG = this.getClass().getSimpleName();
        setEventType();
    }

    public abstract static class InputObject{

    }

    public static enum EventType{
        JS_CALLBACK_READY,
        AUDIO_TRACKS_LIST,
        AUDIO_TRACK_SWITCH,
        CAST_DEVICE_CHANGE,
        CAST_ROUTE_DETECTED,
        ERROR,
        PLAYER_STATE_CHANGE,
        PLAYHEAD_UPDATE,
        PROGRESS,
        QUALITY_SWITCHING,
        QUALITY_TRACKS_LIST,
        TEXT_TRACK_CHANGE,
        TEXT_TRACK_LIST,
        TEXT_TRACK_TEXT,
        TOGGLE_FULLSCREEN,
        WEB_VIEW_MINIMIZE,
        KPLAYER_EVENT_LISTENER
    }
}
