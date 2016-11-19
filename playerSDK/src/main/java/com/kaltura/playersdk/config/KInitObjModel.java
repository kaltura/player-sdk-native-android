package com.kaltura.playersdk.config;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Gleb on 10/7/16.
 */

public class KInitObjModel {

    private KLocaleModel mLocale;
    private String mPlatform;
    private String mSiteGuid;
    private int mDomainID;
    private String mUDID;
    private String mApiUser;
    private String mApiPass;

    public KInitObjModel() {
        mLocale = new KLocaleModel();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("ApiPass", mApiPass);
            obj.put("ApiUser", mApiUser);
            obj.put("UDID", mUDID);
            obj.put("DomainID", mDomainID);
            obj.put("SiteGuid", mSiteGuid);
            obj.put("Platform", mPlatform);
            obj.put("Locale", mLocale.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public void setLocale(KLocaleModel locale) {
        mLocale = locale;
    }

    public void setPlatform(String platform) {
        mPlatform = platform;
    }

    public void setSiteGuid(String siteGuid) {
        mSiteGuid = siteGuid;
    }

    public void setDomainID(int domainID) {
        mDomainID = domainID;
    }

    public void setUDID(String UDID) {
        mUDID = UDID;
    }

    public void setApiUser(String apiUser) {
        mApiUser = apiUser;
    }

    public void setApiPass(String apiPass) {
        mApiPass = apiPass;
    }
}
