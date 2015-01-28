package com.kaltura.playersdk.events;

/**
 * Created by michalradwantzor on 9/17/13.
 */
public abstract class OnProgressListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.PROGRESS_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        ProgressInputObject input = (ProgressInputObject) inputObject;
        onProgressUpdate(input.progress);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof ProgressInputObject;
    }



    abstract public void onProgressUpdate(int progress);

    public static class ProgressInputObject extends InputObject{
        public int progress;
    }
}
