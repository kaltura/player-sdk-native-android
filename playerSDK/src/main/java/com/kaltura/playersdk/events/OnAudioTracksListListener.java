package com.kaltura.playersdk.events;

import java.util.List;

public abstract class OnAudioTracksListListener extends Listener{
    @Override
    protected void setEventType() {
        mEventType = EventType.AUDIO_TRACKS_LIST;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        AudioTracksListInputObjectInputObject input = (AudioTracksListInputObjectInputObject)inputObject;
        List<String> list = input.list;
        int defaultTrackIndex = input.defaultTrackIndex;
        OnAudioTracksList(list, defaultTrackIndex);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTracksListInputObjectInputObject;
    }


    abstract public void OnAudioTracksList( List<String> list, int defaultTrackIndex );

    public static class AudioTracksListInputObjectInputObject extends InputObject{
        public List<String> list;
        public int defaultTrackIndex;
    }
}
