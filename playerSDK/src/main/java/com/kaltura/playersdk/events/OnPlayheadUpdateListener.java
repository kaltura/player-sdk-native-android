package com.kaltura.playersdk.events;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public abstract class OnPlayheadUpdateListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.PLAYHEAD_UPDATE_LISTENER_TYPE;
    }

    @Override
    final protected boolean executeInternalCallback(InputObject inputObject){
        PlayheadUpdateInputObject input = (PlayheadUpdateInputObject) inputObject;
        onPlayheadUpdated(input.msec);
        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof PlayheadUpdateInputObject;
    }



    abstract public void onPlayheadUpdated(int msec);

    public static class PlayheadUpdateInputObject extends InputObject{
        public int msec;
    }
}
