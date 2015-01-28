package com.kaltura.playersdk.events;

import java.util.List;

public abstract class OnTextTracksListListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.TEXT_TRACK_LIST_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        AudioTrackSwitchingInputObject input = (AudioTrackSwitchingInputObject) inputObject;
        OnTextTracksList(input.list, input.defaultTrackIndex);
    }

    @Override
    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }



    abstract public void OnTextTracksList( List<String> list, int defaultTrackIndex );

    public static class AudioTrackSwitchingInputObject extends InputObject{
        public List<String> list;
        public int defaultTrackIndex;
    }
}
