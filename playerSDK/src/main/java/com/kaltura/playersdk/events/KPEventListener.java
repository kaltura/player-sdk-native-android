package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

/**
 * Created by nissopa on 9/2/15.
 */
public interface KPEventListener {
    void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state);
    void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime);
    void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn);
}
