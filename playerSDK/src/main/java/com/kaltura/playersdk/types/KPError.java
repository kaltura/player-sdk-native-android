package com.kaltura.playersdk.types;

/**
 * Created by nissimpardo on 10/03/16.
 */
public enum KPError {
    UNKNOWN("unknown"),
    SslError("loaded"),
    READY("canplay"),
    PLAYING("play"),
    PAUSED("pause"),
    SEEKED("seeked"),
    SEEKING("seeking"),
    ENDED("ended");


    private String stringValue;
    KPError(String toString) {
        stringValue = toString;
    }

    public static KPError error(String description) {
        switch (description) {
            case "loaded":
                return SslError;
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
