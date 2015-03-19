package com.kaltura.playersdk.events;

public abstract class KPlayerJsCallbackReadyListener  extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.JS_CALLBACK_READY_LISTENER_TYPE;
    }

    @Override
    final protected boolean executeInternalCallback(InputObject inputObject){
        jsCallbackReady();

        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return true;
//        return inputObject instanceof JsCallbackReadyInputObject;
    }

    abstract public void jsCallbackReady();

    public static class JsCallbackReadyInputObject extends InputObject{

    }
}
