package com.kaltura.playersdk.events;

/**
 * Created by itayi on 2/19/15.
 */
public abstract class OnDurationChangedListener extends Listener{
    @Override
    protected void setEventType() {
        mEventType = EventType.DURATION_CHANGED_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        DurationChangedInputObject input = (DurationChangedInputObject)inputObject;
        int mSec = input.msec;
        OnDurationChanged(mSec);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof DurationChangedInputObject;
    }


    abstract public void OnDurationChanged(int mSec);

    public static class DurationChangedInputObject extends InputObject{
        public int msec;
    }
}
