package com.kaltura.playersdk.events;

/*
 * This event is fired once when there is a line of text available.
 * 
 * The start time and length is in seconds, and the buffer contains
 * the text to be displayed.
 * 
 */

public abstract class OnTextTrackTextListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.TEXT_TRACK_TEXT_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        TextTrackTextInputObject input = (TextTrackTextInputObject) inputObject;
        onSubtitleText(input.startTime, input.length, input.buffer);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof TextTrackTextInputObject;
    }

    abstract void onSubtitleText(double startTime, double length, String buffer);

    public static class TextTrackTextInputObject extends InputObject{
        public double startTime;
        public double length;
        public String buffer;
    }
}