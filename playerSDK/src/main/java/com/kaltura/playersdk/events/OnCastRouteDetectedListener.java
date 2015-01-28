package com.kaltura.playersdk.events;

/**
 * Will be called when a cast media route was added / removed
 * @author michalradwantzor
 *
 */
public abstract class OnCastRouteDetectedListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.CAST_ROUTE_DETECTED_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        onCastRouteDetected();
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return true;
//        return inputObject instanceof CastRouteDetectedInputObject;
    }



    abstract public void onCastRouteDetected();

    public static class CastRouteDetectedInputObject extends InputObject{

    }

}
