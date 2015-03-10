package com.kaltura.playersdk.events;

import android.util.Log;

/**
 * Created by itayi on 1/28/15.
 */
public abstract class Listener {

    private static String TAG;
    protected EventType mEventType;

    final public boolean executeCallback(InputObject inputObject)
    {
        if(checkValidInputObjectType(inputObject)){
            return executeInternalCallback(inputObject);
        }else{
            Log.e(TAG, "Recieved wrong inputObject type");
        }
        return false;
    }

    abstract protected boolean executeInternalCallback(InputObject inputObject);
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
        JS_CALLBACK_READY_LISTENER_TYPE,
        AUDIO_TRACKS_LIST_LISTENER_TYPE,
        AUDIO_TRACK_SWITCH_LISTENER_TYPE,
        CAST_DEVICE_CHANGE_LISTENER_TYPE,
        CAST_ROUTE_DETECTED_LISTENER_TYPE,
        ERROR_LISTENER_TYPE,
        PLAYER_STATE_CHANGE_LISTENER_TYPE,
        PLAYHEAD_UPDATE_LISTENER_TYPE,
        PROGRESS_UPDATE_LISTENER_TYPE,
        QUALITY_SWITCHING_LISTENER_TYPE,
        QUALITY_TRACKS_LIST_LISTENER_TYPE,
        TEXT_TRACK_CHANGE_LISTENER_TYPE,
        TEXT_TRACK_LIST_LISTENER_TYPE,
        TEXT_TRACK_TEXT_LISTENER_TYPE,
        TOGGLE_FULLSCREEN_LISTENER_TYPE,
        WEB_VIEW_MINIMIZE_LISTENER_TYPE,
        KPLAYER_EVENT_LISTENER_TYPE,
        DURATION_CHANGED_LISTENER_TYPE,
        SHARE_LISTENER_TYPE
    }
}
