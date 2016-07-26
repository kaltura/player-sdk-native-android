package com.kaltura.playersdk.utils;

import android.util.Log;

public class LogUtils {

    private static boolean DEBUG = true;
    private static String TAG = LogUtils.class.getSimpleName();

    public static boolean isDEBUG() {
        return DEBUG;
    }

    public static void disableDebugMode() {
        DEBUG = false;
    }

    public static void enableDebugMode() {
        DEBUG = true;
    }
    public static void LOGD(final String tag, String message) {
        if (DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void LOGD(final String tag, String message, Throwable cause) {
        if (DEBUG) {
            Log.d(tag, message, cause);
        }
    }

    public static void LOGV(final String tag, String message) {
        if (DEBUG) {
            Log.v(tag, message);
        }
    }

    public static void LOGV(final String tag, String message, Throwable cause) {
        if (DEBUG) {
            Log.v(tag, message, cause);
        }
    }

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