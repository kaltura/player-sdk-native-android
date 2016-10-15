package com.kaltura.playersdk.helpers;

import android.net.Uri;
import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;

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
    public static String LocalContentId = "localContentId";

    private String string;
    private String argsString;

    public KStringUtilities(String string) {
        this.string = string;
        this.argsString = null;
    }

    public boolean isJSFrame() {
       return this.string.startsWith("js-frame:");
    }

    public boolean isEmbedFrame() {
        return this.string.contains("mwEmbedFrame.php");
    }

    public boolean isPlay() {
        return this.string.equals("play");
    }

    public boolean isSeeked() {
        return this.string.equals("seeked");
    }
    public boolean isTimeUpdate() {
        return this.string.equals("timeupdate");
    }
    public boolean isEnded() {
        return this.string.equals("ended");
    }
    public boolean isError() {
        return this.string.equals("error");
    }


    public boolean canPlay() {
        return this.string.equals("canplay");
    }

    public static boolean isHLSSource(String src) {
        return src.contains(".m3u8");
    }

    public static boolean isToggleFullScreen(String event) {
        return event.equals("toggleFullscreen");
    }


    public String[] fetchArgs() {
        if (this.argsString != null && this.argsString.length() > 3) {
            try {
                String value = URLDecoder.decode(this.argsString, "UTF-8");
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

    public String getArgsString() {
        return this.argsString;
    }

    public String getAction() {
        String[] arr = this.string.split(":");
        if (arr.length > 1) {
            if (arr.length > 3) {
                this.argsString = arr[3].equals("%5B%5D") ? null : arr[3];
            }
            return arr[1];
        }
        return null;
    }

    public enum Attribute {
        src, currentTime, visible, licenseUri, nativeAction, doubleClickRequestAds, language, textTrackSelected, audioTrackSelected, goLive, chromecastAppId, playerError;
    }

    static public String addEventListener(String event) {
        return JSMethod(AddJSListener, "'" + event + "'");
    }

    static public String removeEventListener(String event) {
        return JSMethod(RemoveJSListener, "'" + event + "'");
    }

    static public String asyncEvaluate(String expression, String evaluateID) {
        return JSMethod(AsyncEvaluate, "'" + expression + "'", "'" + evaluateID + "'");
    }

    static public String setKDPAttribute(String pluginName, String propertyName, String value) {
        return JSMethod(SetKDPAttribute, "'" + pluginName + "'", "'" + propertyName + "'", value);
    }

    //adding 2 setKDPAttribute for String and JSON inorder not to create backwards compatibility problems (fixing the missing "'" in both value sides)
    static public String setKDPAttribute(String pluginName, String propertyName, JSONObject value) {
        return JSMethod(SetKDPAttribute, "'" + pluginName + "'", "'" + propertyName + "'", value.toString());
    }

    static public String setStringKDPAttribute(String pluginName, String propertyName, String value) {
        return JSMethod(SetKDPAttribute, "'" + pluginName + "'", "'" + propertyName + "'", "'" + value + "'");
    }

    static public String triggerEvent(String event, String value) {
        return JSMethod(TriggerEvent, "'" + event + "'", "'" + value + "'");
    }

    static public String triggerEventWithJSON(String event, String jsonString) {
        return JSMethod(TriggerEvent, "'" + event + "'", jsonString);
    }

    static public String sendNotification(String notificationName, String params) {
        return JSMethod(SendNotification, "'" + notificationName + "'," + params);
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
        if (params.equals("null")) {
            return JavaScriptPrefix + action + "()";
        }
        return JavaScriptPrefix + action +"(" + params + ")";
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
        try {
            return Attribute.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String extractFragmentParam(Uri url, String name) {
        return new Uri.Builder().encodedQuery(url.getEncodedFragment()).build().getQueryParameter(name);
    }

    static public String md5(String string) {
        return md5(string.getBytes());
    }

    static public String md5(byte[] data) {
        try {
            // Create MD5 Hash
            java.security.MessageDigest digest = java.security.MessageDigest
                    .getInstance("MD5");
            digest.update(data);
            byte messageDigest[] = digest.digest();
            return toHexString(messageDigest);


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    @NonNull
    public static String toHexString(byte[] bytes) {
        // Create Hex String
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String h = Integer.toHexString(0xFF & b);
            hexString.append(h.length() == 1 ? "0" : "").append(h);
        }
        return hexString.toString();
    }
}
