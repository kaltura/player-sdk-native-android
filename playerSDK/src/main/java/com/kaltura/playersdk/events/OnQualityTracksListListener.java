package com.kaltura.playersdk.events;

import com.kaltura.playersdk.QualityTrack;

import java.util.List;

public abstract class OnQualityTracksListListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.QUALITY_TRACKS_LIST_LISTENER_TYPE;
    }

    @Override
    final protected boolean executeInternalCallback(InputObject inputObject){
        QualityTracksListInputObject input = (QualityTracksListInputObject)inputObject;
        OnQualityTracksList(input.list,input.defaultTrackIndex);
        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof QualityTracksListInputObject;
    }

    abstract public void OnQualityTracksList( List<QualityTrack> list, int defaultTrackIndex );

    public static class QualityTracksListInputObject extends InputObject{
        public List<QualityTrack> list;
        public int defaultTrackIndex;
    }
}
