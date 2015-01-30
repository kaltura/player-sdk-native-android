package com.kaltura.playersdk.events;

import com.kaltura.playersdk.types.PlayerStates;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public abstract class OnPlayerStateChangeListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){
        PlayerStateChangeInputObject input = (PlayerStateChangeInputObject)inputObject;
        onStateChanged(input.state);
    }

    @Override
    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof PlayerStateChangeInputObject;
    }



    abstract public void onStateChanged(PlayerStates state);

    public static class PlayerStateChangeInputObject extends InputObject{
        public PlayerStates state;
    }
}
