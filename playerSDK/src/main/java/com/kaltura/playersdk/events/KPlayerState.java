package com.kaltura.playersdk.events;

/**
 * Created by nissopa on 9/2/15.
 */

public enum KPlayerState {
    LOADED("loaded"),
    READY("canplay"),
    PLAING("play"),
    PAUSED("pause"),
    SEEKING("Seeking"),
    ENDED("ended");

    private String stringValue;
    KPlayerState(String toString) {
        stringValue = toString;
    }

    public static KPlayerState getStateForEventName(String eventName) {
        switch (eventName) {
            case "loaded":
                return LOADED;
            case "canplay":
                return READY;
            case "play":
                return PLAING;
            case "pause":
                return PAUSED;
            case "Seeking":
                return SEEKING;
            case "ended":
                return ENDED;
        }
        return null;
    }

    @Override
    public String toString() {
        return stringValue;
    }
}
