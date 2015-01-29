package com.kaltura.playersdk.events;

import java.util.List;

public abstract class OnTextTracksListListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.TEXT_TRACK_LIST_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        TextTracksListInputObject input = (TextTracksListInputObject) inputObject;
        onTextTracksList(input.list, input.defaultTrackIndex);
    }

    @Override
    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof TextTracksListInputObject;
    }



    abstract public void onTextTracksList( List<String> list, int defaultTrackIndex );

    public static class TextTracksListInputObject extends InputObject{
        public List<String> list;
        public int defaultTrackIndex;
    }
}
