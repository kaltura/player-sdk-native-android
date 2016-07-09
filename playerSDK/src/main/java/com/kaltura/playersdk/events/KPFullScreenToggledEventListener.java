package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

public interface KPFullScreenToggledEventListener {
    void onKPlayerFullScreenToggled(PlayerViewController playerViewController, boolean isFullscreen);
}
