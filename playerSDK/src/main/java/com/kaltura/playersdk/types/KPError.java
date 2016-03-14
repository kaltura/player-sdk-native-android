package com.kaltura.playersdk.types;


public class KPError {

    private String errorMsg;
    private int errorCode;
    private Exception exception;

    public KPError(String errorMsg) {
        this.errorMsg  = errorMsg;
        exception = null;
        errorCode = -1;
    }

    public KPError(String errorMsg, int errorCode, Exception exception) {
        this.errorMsg  = errorMsg;
        this.errorCode = errorCode;
        this.exception = exception;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }
}

///**
// * Created by nissimpardo on 10/03/16.
// */
//public enum KPError {
//    UNKNOWN("unknown"),
//    SslError("loaded"),
//    READY("canplay"),
//    PLAYING("play"),
//    PAUSED("pause"),
//    SEEKED("seeked"),
//    SEEKING("seeking"),
//    ENDED("ended");
//
//
//    private String stringValue;
//    KPError(String toString) {
//        stringValue = toString;
//    }
//
//    public static KPError error(String description) {
//        switch (description) {
//            case "loaded":
//                return SslError;
//            case "canplay":
//                return READY;
//            case "play":
//                return PLAYING;
//            case "pause":
//                return PAUSED;
//            case "seeked":
//                return SEEKED;
//            case "seeking":
//                return SEEKING;
//            case "ended":
//                return ENDED;
//        }
//        return UNKNOWN;
//    }
//
//    @Override
//    public String toString() {
//        return stringValue;
//    }
//}
