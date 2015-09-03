package com.kaltura.playersdk.events;

/**
 * Created by nissopa on 9/2/15.
 */

public enum KPlayerState {
    LOADED("loaded", 0),
    READY("canplay", 1),
    PLAING("play", 2),
    PAUSED("pause", 3),
    SEEKING("Seeking", 4),
    ENDED("ended", 5);

    private String stringValue;
    private int intValue;
    KPlayerState(String toString, int value) {
        stringValue = toString;
        intValue = value;
    }

    public static KPlayerState stateIndex(int index) {
        switch (index) {
            case 0:
                return LOADED;
            case 1:
                return READY;
            case 2:
                return PLAING;
            case 3:
                return PAUSED;
            case 4:
                return SEEKING;
            case 5:
                return ENDED;
        }
        return null;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
