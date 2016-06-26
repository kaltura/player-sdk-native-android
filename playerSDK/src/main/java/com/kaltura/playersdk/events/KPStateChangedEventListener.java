package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

public interface KPStateChangedEventListener {
    void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state);
}
