package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.types.KPError;

/**
 * Created by nissopa on 9/2/15.
 */
@Deprecated
public interface KPEventListener {
    @Deprecated
    void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state);
    @Deprecated
    void onKPlayerError(PlayerViewController playerViewController, KPError error);
    @Deprecated
    void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime);
    @Deprecated
    void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn);
}
