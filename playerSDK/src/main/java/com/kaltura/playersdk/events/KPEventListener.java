package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

/**
 * Created by nissopa on 9/2/15.
 */
public interface KPEventListener {
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state);
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime);
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn);
}
