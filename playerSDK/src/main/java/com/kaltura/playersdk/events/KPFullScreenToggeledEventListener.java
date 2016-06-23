package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

public interface KPFullScreenToggeledEventListener {
    void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen);
}
