package com.kaltura.playersdk.drm;

/**
 * Created by noamt on 04/05/2016.
 * 
 * This exception is intended to wrap all exception that occur because Widevine is not
 * supported or provisioned. It's a RuntimeException because:
 * 1. We explicitly check for Widevine support before attempting to use it.
 * 2. Having to handle those exceptions where they occur just complicates the code.
 */
public class WidevineNotSupportedException extends RuntimeException {
    public WidevineNotSupportedException(Throwable throwable) {
        super(throwable);
    }
}
