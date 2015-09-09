package com.kaltura.playersdk.Helpers;

import org.json.JSONArray;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by nissopa on 6/7/15.
 */
public class KStringUtilities {
    public static String JavaScriptPrefix = "javascript:NativeBridge.videoPlayer.";
    private static String AddJSListener = "addJsListener";
    private static String RemoveJSListener = "removeJsListener";
    private static String AsyncEvaluate = "asyncEvaluate";
    private static String SetKDPAttribute = "setKDPAttribute";
    private static String TriggerEvent = "trigger";
    private static String SendNotification = "sendNotification";



    static public boolean isJSFrame(String str) {
       return str.startsWith("js-frame:");
    }

    static public boolean isEmbedFrame(String str) {
        return str.contains("mwEmbedFrame.php");
    }

    static public boolean isPlay(String str) {
        return str.equals("play");
    }

    static public boolean isSeeked(String str) {
        return str.equals("seeked");
    }
    static public boolean isTimeUpdate(String str) {return str.equals("timeupdate");}

    static public boolean isContentPauseRequested(String str) {
        return str.equals(KIMAManager.ContentPauseRequestedKey);
    }

    static public boolean isContentResumeRequested(String str) {
        return str.equals(KIMAManager.ContentResumeRequestedKey);
    }

    static public boolean isAllAdsCompleted(String str) {
        return str.equals(KIMAManager.AllAdsCompletedKey);
    }

    static public boolean canPlay(String str) {
        return str.equals("canplay");
    }

    public static boolean isHLSSource(String src) {
        return src.contains(".m3u8");
    }

    public static boolean isToggleFullScreen(String event) {
        return event.equals("toggleFullscreen");
    }

    static public String getAction(String str) {
        String[] arr = str.split(":");
        if (arr.length > 1) {
            return arr[1];
        }
        return null;
    }

    static public String getArgs(String str) {
        String[] arr = str.split(":");
        if (arr.length > 3) {
            str = arr[3].equals("%5B%5D") ? null : arr[3];
            return str;
        }
        return null;
    }

    static public enum Attribute {
        src, currentTime, visible, wvServerKey, nativeAction, doubleClickRequestAds, language, textTrackSelected, goLive;
    }

    static public String addEventListener(String event) {
        return JSMethod(AddJSListener, event);
    }

    static public String removeEventListener(String event) {
        return JSMethod(RemoveJSListener, event);
    }

    static public String asyncEvaluate(String expression, String evaluateID) {
        return JSMethod(AsyncEvaluate, expression, evaluateID);
    }

    static public String setKDPAttribute(String pluginName, String propertyName, String value) {
        return JSMethod(SetKDPAttribute, "'" + pluginName + "'", "'" + propertyName + "'", value);
    }

    static public String triggerEvent(String event, String value) {
        return JSMethod(TriggerEvent, "'" + event + "'", "'" + value + "'");
    }

    static public String triggerEventWithJSON(String event, String jsonString) {
        return JSMethod(TriggerEvent, "'" + event + "'", jsonString);
    }

    static public String sendNotification(String notificationName, String params) {
        return JSMethod(SendNotification, "\"" + notificationName + "\"," + params);
    }

    static public String[] fetchArgs(String jsonString) {
        if (jsonString != null && jsonString.length() > 3) {
            try {
                String value = URLDecoder.decode(jsonString, "UTF-8");
                if (value != null && value.length() > 2) {
                    JSONArray jsonArr = new JSONArray(value);

                    String[] params = new String[jsonArr.length()];
                    for (int i = 0; i < jsonArr.length(); i++) {
                        params[i] = jsonArr.getString(i);
                    }
                    return params;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    static private String JSMethod(String action, String params) {
        return JavaScriptPrefix + action +"(\"" + params + "\")";
    }

    static private String JSMethod(String action, String param1, String param2) {
        return JavaScriptPrefix + action + "(" + param1 + "," + param2 + ")";
    }

    static private String JSMethod(String action, String param1, String param2, String param3) {
        return JavaScriptPrefix + action + "(" + param1 + "," + param2 + "," + param3 +")";
    }

    static public Method isMethodImplemented(Object obj, String methodName) {
        Method[] methods = obj.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().contains(methodName)) {
                return m;
            }
        }
        return null;
    }


    static public Attribute attributeEnumFromString(String value) {
        return Attribute.valueOf(value);
    }
}
