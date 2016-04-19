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
