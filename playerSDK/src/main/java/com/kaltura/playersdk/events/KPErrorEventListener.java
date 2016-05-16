package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.types.KPError;

/**
 * Created by gilad.nadav on 16/05/2016.
 */
public interface KPErrorEventListener extends KPEventListener{
    void onKPlayerError(PlayerViewController playerViewController, KPError error);

}
