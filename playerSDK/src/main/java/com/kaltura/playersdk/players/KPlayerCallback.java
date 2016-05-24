package com.kaltura.playersdk.players;


/**
 * Created by nissopa on 6/30/15.
 */
public interface KPlayerCallback {
    int CAN_PLAY = 1;
    int ENDED = 4;
    int REMOVE_ADS = 5;
    int SEEKED = 6;

    void playerStateChanged(int state);
}

