package com.kaltura.playersdk.events;

public abstract class OnQualitySwitchingListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.QUALITY_SWITCHING_LISTENER_TYPE;
    }

    @Override
    final protected boolean executeInternalCallback(InputObject inputObject){
        QualitySwitchingInputObject input = (QualitySwitchingInputObject) inputObject;
        switch (input.methodChoice){
            case START:
                onQualitySwitchingStart(input.oldTrackIndex, input.newTrackIndex);
                break;
            case END:
                onQualitySwitchingEnd(input.newTrackIndex);
                break;
        }

        return true;
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof QualitySwitchingInputObject;
    }



    abstract public void onQualitySwitchingStart( int oldTrackIndex, int newTrackIndex );

    abstract public void onQualitySwitchingEnd( int newTrackIndex );

    public static class QualitySwitchingInputObject extends InputObject{
        public MethodChoice methodChoice;
        public int oldTrackIndex;
        public int newTrackIndex;

        public enum MethodChoice{
            START,
            END
        }
    }
}
