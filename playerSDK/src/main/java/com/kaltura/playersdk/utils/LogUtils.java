package com.kaltura.playersdk.utils;

import android.util.Log;


public class LogUtils {

    private static boolean DEBUG_MODE_ON = true;
    private static boolean WEBVIEW_DEBUG_MODE_ON = true;
    private static String TAG = LogUtils.class.getSimpleName();

    public static boolean isDebugModeOn() {
        return DEBUG_MODE_ON;
    }

    public static boolean isWebViewDebugModeOn() {
        return WEBVIEW_DEBUG_MODE_ON;
    }

    public static void disableDebugMode() {
        DEBUG_MODE_ON = false;
    }

    public static void enableDebugMode() {
        DEBUG_MODE_ON = true;
    }

    public static void disableWebViewDebugMode() {
        WEBVIEW_DEBUG_MODE_ON = false;
    }

    public static void enableWebViewDebugMode() {
        WEBVIEW_DEBUG_MODE_ON = true;
    }

    public static void LOGD(final String tag, String message) {
        if (DEBUG_MODE_ON) {
            Log.d(tag, message);
        }
    }

    public static void LOGD(final String tag, String message, Throwable cause) {
        if (DEBUG_MODE_ON) {
            Log.d(tag, message, cause);
        }
    }

    public static void LOGV(final String tag, String message) {
        if (DEBUG_MODE_ON) {
            Log.v(tag, message);
        }
    }

    public static void LOGV(final String tag, String message, Throwable cause) {
        if (DEBUG_MODE_ON) {
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