package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.types.KPError;

public interface KPErrorEventListener {
    void onKPlayerError(PlayerViewController playerViewController, KPError error);
}
