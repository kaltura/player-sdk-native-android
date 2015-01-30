package com.kaltura.playersdk.events;

public abstract class OnAudioTrackSwitchingListener extends Listener{
    @Override
    protected void setEventType() {
        mEventType = EventType.AUDIO_TRACK_SWITCH_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        AudioTrackSwitchingInputObject input = (AudioTrackSwitchingInputObject) inputObject;
        switch (input.methodChoice){
            case START:
                onAudioSwitchingStart(input.oldTrackIndex,input.newTrackIndex);
                break;
            case END:
                onAudioSwitchingEnd(input.newTrackIndex);
                break;
        }
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }


    abstract public void onAudioSwitchingStart( int oldTrackIndex, int newTrackIndex );

    abstract public void onAudioSwitchingEnd( int newTrackIndex );

    public static class AudioTrackSwitchingInputObject extends InputObject{
        public MethodChoice methodChoice;
        public int oldTrackIndex;
        public int newTrackIndex;

        public enum MethodChoice{
            START,
            END
        }
    }
}
