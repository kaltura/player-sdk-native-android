package com.kaltura.playersdk.utils;


import android.util.Log;

public class LogUtils {

    private static final String LOG_PREFIX = "KPLAYER_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;

    private static final boolean DEBUG = false;

    private LogUtils() {
    }

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }

    /**
     * WARNING: Don't use this when obfuscating class names with Proguard!
     */
    public static String makeLogTag(Class<?> cls) {
        return makeLogTag(cls.getSimpleName());
    }

    public static void LOGD(final String tag, String message) {
        if (DEBUG && Log.isLoggable(tag, Log.DEBUG)) {
            LOGD(tag, getVersionPrefix() + message);
        }
    }

    public static void LOGD(final String tag, String message, Throwable cause) {
        if (DEBUG && Log.isLoggable(tag, Log.DEBUG)) {
            LOGD(tag, getVersionPrefix() + message, cause);
        }
    }

    public static void LOGV(final String tag, String message) {
        if (DEBUG && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, getVersionPrefix() + message);
        }
    }

    public static void LOGV(final String tag, String message, Throwable cause) {
        if (DEBUG && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(tag, getVersionPrefix() + message, cause);
        }
    }

    public static void LOGI(final String tag, String message) {
        LOGI(tag, getVersionPrefix() + message);
    }

    public static void LOGI(final String tag, String message, Throwable cause) {
        LOGI(tag, message, cause);
    }

    public static void LOGW(final String tag, String message) {
        LOGW(tag, getVersionPrefix() + message);
    }

    public static void LOGW(final String tag, String message, Throwable cause) {
        LOGW(tag, getVersionPrefix() + message, cause);
    }

    public static void LOGE(final String tag, String message) {
        LOGE(tag, getVersionPrefix() + message);
    }

    public static void LOGE(final String tag, String message, Throwable cause) {
        LOGE(tag, getVersionPrefix() + message, cause);
    }

    public static String getVersionPrefix() {
        return "[v" + 2 + "] ";
    }

}
