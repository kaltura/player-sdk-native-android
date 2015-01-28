package com.kaltura.playersdk.events;

/**
 * Created by michalradwantzor on 9/17/13.
 */
public abstract class OnProgressListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.PROGRESS;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        AudioTrackSwitchingInputObject input = (AudioTrackSwitchingInputObject) inputObject;
        onProgressUpdate(input.progress);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }



    abstract public void onProgressUpdate(int progress);

    public static class AudioTrackSwitchingInputObject extends InputObject{
        public int progress;
    }
}
