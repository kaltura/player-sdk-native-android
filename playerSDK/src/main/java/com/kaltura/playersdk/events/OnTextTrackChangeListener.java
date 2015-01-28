package com.kaltura.playersdk.events;

public abstract class OnTextTrackChangeListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.TEXT_TRACK_CHANGE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        AudioTrackSwitchingInputObject input = (AudioTrackSwitchingInputObject) inputObject;
        onOnTextTrackChanged(input.newTrackIndex);
    }

    @Override
    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }

    abstract public void onOnTextTrackChanged( int newTrackIndex );

    public static class AudioTrackSwitchingInputObject extends InputObject{
        public int newTrackIndex;
    }

}
