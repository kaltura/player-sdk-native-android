package com.kaltura.hlsplayersdk.events;

import com.kaltura.hlsplayersdk.types.PlayerStates;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public interface OnPlayerStateChangeListener {
    public boolean onStateChanged(PlayerStates state);
}
