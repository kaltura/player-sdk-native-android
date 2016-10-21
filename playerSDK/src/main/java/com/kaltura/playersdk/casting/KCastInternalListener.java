package com.kaltura.playersdk.casting;

import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.players.KChromeCastPlayer;

/**
 * Created by Gleb on 9/13/16.
 */
public interface KCastInternalListener extends KCastMediaRemoteControl.KCastMediaRemoteControlListener {
    void onStartCasting(KChromeCastPlayer remoteMediaPlayer);
    void onCastStateChanged(String state);
    void onStopCasting();
}