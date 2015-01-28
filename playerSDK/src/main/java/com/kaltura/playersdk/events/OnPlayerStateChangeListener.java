package com.kaltura.playersdk.events;

import com.kaltura.playersdk.types.PlayerStates;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public abstract class OnPlayerStateChangeListener extends Listener{
    @Override
    final protected void setEventType() {
        mEventType = EventType.PLAYER_STATE_CHANGE;
    }

    @Override
    final protected void executeInternalCallback(InputObject inputObject){

    }

    @Override
    final protected boolean checkValidInputObjectType(InputObject inputObject){
        return inputObject instanceof PlayerStateChangeInputObject;
    }



    abstract public boolean onStateChanged(PlayerStates state);

    public static class PlayerStateChangeInputObject extends InputObject{

    }
}
