package com.kaltura.playersdk.events;

public abstract class KPlayerEventListener extends Listener{

    @Override
    final protected void setEventType() {
        mEventType = EventType.KPLAYER_EVENT_LISTENER_TYPE;
    }

    @Override
    final protected boolean executeInternalCallback(InputObject inputObject){
        Object body = ((KPlayerInputObject)inputObject).body;
        onKPlayerEvent(body);

        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof KPlayerInputObject;
    }

    abstract public void onKPlayerEvent(Object body);

    public String getCallbackName(){
        return String.valueOf(java.lang.System.identityHashCode(this));
    }

    public static class KPlayerInputObject extends InputObject{
        public Object body;
    }
}
