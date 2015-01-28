package com.kaltura.playersdk.events;

public abstract class OnAudioTrackSwitchingListener extends Listener{
    @Override
    protected void setEventType() {
        mEventType = EventType.AUDIO_TRACK_SWITCH_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){

    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }


    abstract public void onAudioSwitchingStart( int oldTrackIndex, int newTrackIndex );

    abstract public void onAudioSwitchingEnd( int newTrackIndex );

    public static class AudioTrackSwitchingInputObject extends InputObject{
        int oldTrackIndex;
        int newTrackIndex;
    }
}
