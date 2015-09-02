package com.kaltura.playersdk.events;

/**
 * Created by nissopa on 9/2/15.
 */

public enum KPlayerState {
    LOADED("Loaded", 0),
    READY("Ready", 1),
    PLAING("Playing", 2),
    PAUSED("Paused", 3),
    SEEKING("Seeking", 4);

    private String stringValue;
    private int intValue;
    KPlayerState(String toString, int value) {
        stringValue = toString;
        intValue = value;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
