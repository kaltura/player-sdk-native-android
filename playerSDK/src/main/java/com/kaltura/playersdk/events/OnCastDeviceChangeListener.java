package com.kaltura.playersdk.events;

import com.google.android.gms.cast.CastDevice;

public abstract class OnCastDeviceChangeListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.CAST_DEVICE_CHANGE_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        AudioTrackSwitchingInputObject input = (AudioTrackSwitchingInputObject)inputObject;
        onCastDeviceChange(input.oldDevice, input.newDevice);
    }

    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof AudioTrackSwitchingInputObject;
    }



    abstract public void onCastDeviceChange(CastDevice oldDevice, CastDevice newDevice);

    public static class AudioTrackSwitchingInputObject extends InputObject{
        public CastDevice oldDevice;
        public CastDevice newDevice;

    }
}
