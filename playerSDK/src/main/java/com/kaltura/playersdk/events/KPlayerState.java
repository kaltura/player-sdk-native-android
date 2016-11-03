package com.kaltura.playersdk.events;

/**
 * Created by nissopa on 9/2/15.
 */

public enum KPlayerState {
    UNKNOWN("unknown"),
    LOADED("loadedmetadata"),
    PRE_LOADED("loadedmetadata"),
    READY("canplay"),
    CC_READY("canplay"),
    PLAYING("play"),
    PAUSED("pause"),
    SEEKED("seeked"),
    SEEKING("seeking"),
    ENDED("ended");


    private String stringValue;
    KPlayerState(String toString) {
        stringValue = toString;
    }

    public static KPlayerState getStateForEventName(String eventName) {
        switch (eventName) {
            case "loadedmetadata":
                return LOADED;
            case "canplay":
                return READY;
            case "play":
                return PLAYING;
            case "pause":
                return PAUSED;
            case "seeked":
                return SEEKED;
            case "seeking":
                return SEEKING;
            case "ended":
                return ENDED;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
