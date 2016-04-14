package com.kaltura.playersdk.players;


/**
 * Created by nissopa on 6/30/15.
 */
public interface KPlayerCallback {
    int CAN_PLAY = 1;
    int SHOULD_PAUSE = 2;
    int SHOULD_PLAY = 3;
    int ENDED = 4;
    int REMOVE_ADS = 5;

    void playerStateChanged(int state);
}

