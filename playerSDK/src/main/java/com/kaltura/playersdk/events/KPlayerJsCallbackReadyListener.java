package com.kaltura.playersdk.events;

public abstract class KPlayerJsCallbackReadyListener  extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.JS_CALLBACK_READY;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        jsCallbackReady();
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return true;
//        return inputObject instanceof JsCallbackReadyInputObject;
    }

    abstract public void jsCallbackReady();

    public static class JsCallbackReadyInputObject extends InputObject{

    }
}
