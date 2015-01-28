package com.kaltura.playersdk.events;

/**
 * Created by michalradwantzor on 10/1/13.
 */
public abstract class OnToggleFullScreenListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.TOGGLE_FULLSCREEN;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        onToggleFullScreen();
    }

    @Override
    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return true;
//        return inputObject instanceof ToggleFullScreenInputObject;
    }

    abstract public void onToggleFullScreen();

    public static class ToggleFullScreenInputObject extends InputObject{

    }
}
