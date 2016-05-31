package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

public interface KPPlayheadUpdateEventListener {
    void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, long currentTimeMilliSeconds);
}
