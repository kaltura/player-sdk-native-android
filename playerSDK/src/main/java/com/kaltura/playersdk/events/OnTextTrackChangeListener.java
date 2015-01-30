package com.kaltura.playersdk.events;

public abstract class OnTextTrackChangeListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.TEXT_TRACK_CHANGE_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        TextTrackChangedInputObject input = (TextTrackChangedInputObject) inputObject;
        onTextTrackChanged(input.newTrackIndex);
    }

    @Override
    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof TextTrackChangedInputObject;
    }

    abstract public void onTextTrackChanged( int newTrackIndex );

    public static class TextTrackChangedInputObject extends InputObject{
        public int newTrackIndex;
    }

}
