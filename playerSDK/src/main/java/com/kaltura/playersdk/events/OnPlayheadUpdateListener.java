package com.kaltura.playersdk.events;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public abstract class OnPlayheadUpdateListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.PLAYHEAD_UPDATE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){

    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }



    abstract public void onPlayheadUpdated(int msec);

    public static class AudioTrackSwitchingInputObject extends InputObject{

    }
}
