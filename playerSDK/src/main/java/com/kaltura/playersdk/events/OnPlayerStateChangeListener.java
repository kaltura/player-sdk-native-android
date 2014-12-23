package com.kaltura.playersdk.events;

import com.kaltura.playersdk.types.PlayerStates;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public interface OnPlayerStateChangeListener {
    public boolean onStateChanged(PlayerStates state);
}
