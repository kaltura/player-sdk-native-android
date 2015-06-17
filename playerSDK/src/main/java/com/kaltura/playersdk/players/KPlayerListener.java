package com.kaltura.playersdk.players;

/**
 * Created by nissopa on 6/14/15.
 */
public interface KPlayerListener {
    public void eventWithValue(KPlayerController.KPlayer player, String eventName, String eventValue);
    public void eventWithJSON(KPlayerController.KPlayer player, String eventName, String jsonValue);
    public void contentCompleted(KPlayerController.KPlayer currentPlayer);
}


