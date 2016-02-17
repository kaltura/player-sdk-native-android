package com.kaltura.playersdk.events;

import com.kaltura.playersdk.PlayerViewController;

/**
 * Created by nissimpardo on 17/02/16.
 */
public interface OnPlayheadUpdateListener {
    void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime);
}
