package com.kaltura.playersdk.events;

import java.util.List;

public abstract class OnAudioTracksListListener extends Listener{
    @Override
    protected void setEventType() {
        mEventType = EventType.AUDIO_TRACKS_LIST_LISTENER_TYPE;
    }

    @Override
    final protected boolean executeInternalCallback(InputObject inputObject){
        AudioTracksListInputObject input = (AudioTracksListInputObject)inputObject;
        List<String> list = input.list;
        int defaultTrackIndex = input.defaultTrackIndex;
        OnAudioTracksList(list, defaultTrackIndex);
        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTracksListInputObject;
    }


    abstract public void OnAudioTracksList( List<String> list, int defaultTrackIndex );

    public static class AudioTracksListInputObject extends InputObject{
        public List<String> list;
        public int defaultTrackIndex;
    }
}
