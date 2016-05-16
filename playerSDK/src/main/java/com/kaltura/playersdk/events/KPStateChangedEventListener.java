package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

/**
 * Created by gilad.nadav on 16/05/2016.
 */
public interface KPStateChangedEventListener extends KPEventListener{
    void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state);

}
