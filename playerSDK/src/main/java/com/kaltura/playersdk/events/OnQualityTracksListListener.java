package com.kaltura.playersdk.events;

import com.kaltura.playersdk.QualityTrack;

import java.util.List;

public abstract class OnQualityTracksListListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.QUALITY_TRACKS_LIST_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        AudioTrackSwitchingInputObject input = (AudioTrackSwitchingInputObject)inputObject;
        OnQualityTracksList(input.list,input.defaultTrackIndex);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }

    abstract public void OnQualityTracksList( List<QualityTrack> list, int defaultTrackIndex );

    public static class AudioTrackSwitchingInputObject extends InputObject{
        public List<QualityTrack> list;
        public int defaultTrackIndex;
    }
}
