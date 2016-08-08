package com.kaltura.playersdk.utils;

import android.util.Log;


public class LogUtils {

    private static boolean sDebugMode = true;
    private static boolean sWebViewDebugMode = true;
    
    public static boolean isDebugModeOn() {
        return sDebugMode;
    }

    public static boolean isWebViewDebugModeOn() {
        return sWebViewDebugMode;
    }

    public static void disableDebugMode() {
        sDebugMode = false;
    }

    public static void enableDebugMode() {
        sDebugMode = true;
    }

    public static void disableWebViewDebugMode() {
        sWebViewDebugMode = false;
    }

    public static void enableWebViewDebugMode() {
        sWebViewDebugMode = true;
    }

    public static void LOGD(final String tag, String message) {
        if (sDebugMode) {
            Log.d(tag, message);
        }
    }

    public static void LOGD(final String tag, String message, Throwable cause) {
        if (sDebugMode) {
            Log.d(tag, message, cause);
        }
    }

    public static void LOGV(final String tag, String message) {
        if (sDebugMode) {
            Log.v(tag, message);
        }
    }

    public static void LOGV(final String tag, String message, Throwable cause) {
        if (sDebugMode) {
            Log.v(tag, message, cause);
        }
    }

    // Info, error and warning are always enabled.
    public static void LOGI(final String tag, String message) {
        Log.i(tag, message);
    }

    public static void LOGI(final String tag, String message, Throwable cause) {
        Log.i(tag, message, cause);
    }

    public static void LOGW(final String tag, String message) {
        Log.w(tag, message);
    }

    public static void LOGW(final String tag, String message, Throwable cause) {
        Log.w(tag, message, cause);
    }

    public static void LOGE(final String tag, String message) {
        Log.e(tag, message);
    }

    public static void LOGE(final String tag, String message, Throwable cause) {
        Log.e(tag, message, cause);
    }
}
