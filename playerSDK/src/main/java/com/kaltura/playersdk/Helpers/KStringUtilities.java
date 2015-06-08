package com.kaltura.playersdk.Helpers;

import org.json.JSONArray;
import java.net.URLDecoder;
import java.util.ArrayList;

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

    public ArrayList<String> fetchArgs() {
        if (this.argsString != null && this.argsString.length() > 3) {
            try {
                String value = URLDecoder.decode(this.argsString, "UTF-8");
                if (value != null && value.length() > 2) {
                    JSONArray jsonArr = new JSONArray(value);

                    ArrayList<String> params = new ArrayList<String>();
                    for (int i = 0; i < jsonArr.length(); i++) {
                        params.add(jsonArr.getString(i));
                    }
                    return params;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getAction() {
        String[] arr = this.string.split(":");
        if (arr.length > 1) {
            if (arr.length > 3) {
                this.argsString = arr[3];
            }
            return arr[1];
        }
        return null;
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

    static private String JSMethod(String action, String params) {
        return JavaScriptPrefix + action +"(" + params + ")";
    }

    static private String JSMethod(String action, String param1, String param2) {
        return JavaScriptPrefix + action + "(" + param1 + "," + param2 + ")";
    }

    static private String JSMethod(String action, String param1, String param2, String param3) {
        return JavaScriptPrefix + action + "(" + param1 + "," + param2 + "," + param3 +")";
    }

}
