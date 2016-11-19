package com.kaltura.playersdk.config;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Gleb on 10/7/16.
 */

public class KLocaleModel {

    private String mLocaleLanguage;
    private String mLocaleCountry;
    private String mLocaleDevice;
    private String mLocaleUserState;

    public KLocaleModel(String localeLanguage, String localeCountry, String localeDevice) {
        this();
        mLocaleLanguage = localeLanguage;
        mLocaleCountry = localeCountry;
        mLocaleDevice = localeDevice;
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("LocaleCountry", mLocaleCountry);
            obj.put("LocaleDevice", mLocaleDevice);
            obj.put("LocaleLanguage", mLocaleLanguage);
            obj.put("LocaleUserState", mLocaleUserState);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public KLocaleModel () {
        mLocaleUserState = "Unknown";
    }

    public void setLocaleLanguage(String localeLanguage) {
        mLocaleLanguage = localeLanguage;
    }

    public void setLocaleCountry(String localeCountry) {
        mLocaleCountry = localeCountry;
    }

    public void setLocaleDevice(String localeDevice) {
        mLocaleDevice = localeDevice;
    }

    public void setLocaleUserState(String localeUserState) {
        mLocaleUserState = localeUserState;
    }
}
