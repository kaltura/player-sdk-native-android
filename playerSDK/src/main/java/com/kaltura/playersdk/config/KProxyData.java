package com.kaltura.playersdk.config;

import android.annotation.TargetApi;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Gleb on 10/7/16.
 */

public class KProxyData {
    private boolean mWithDynamic;
    private KInitObjModel mInitObj;
    private String mIMediaID;
    private String mMediaID;
    private String mPicSize;
    private String mMediaType;
    private KProxyConfig mProxyConfig;

    private KProxyData() {
        mInitObj = new KInitObjModel();
        mProxyConfig = new KProxyConfig();
    }

    public static Builder newBuilder() {
        return new KProxyData().new Builder();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("withDynamic", mWithDynamic ? "true" : "false");
            obj.put("iMediaID", mIMediaID);
            obj.put("MediaID", mMediaID);
            obj.put("picSize", mPicSize);
            obj.put("mediaType", mMediaType);
            obj.put("initObj", mInitObj.toJson());
            obj.put("config", mProxyConfig.toJson());

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    public class Builder {

        private Builder() {
            // private constructor
        }

        public Builder setMediaId(String mediaId) {
            mMediaID = mediaId;
            return this;
        }

        public Builder setIMediaId(String iMediaId) {
            mIMediaID = iMediaId;
            return this;
        }

        public Builder setPicSize(int w, int h) {
            mPicSize = w + "x" + h;
            return this;
        }

        public Builder setMediaType(String mediaType) {
            mMediaType = mediaType;
            return this;
        }

        public Builder setWithDynamic(boolean withDynamic) {
            mWithDynamic = withDynamic;
            return this;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
        public Builder addProxyConfigFilter(String filter) {
            mProxyConfig.addFilter(filter);
            return this;
        }

        public Builder setUserProtection(String userName, String password, String uDID) {
            mInitObj.setApiUser(userName);
            mInitObj.setApiPass(password);
            mInitObj.setUDID(uDID);
            return this;
        }

        public Builder setPlatform(String platform) {
            mInitObj.setPlatform(platform);
            return this;
        }

        public Builder setDomainId(int domainId) {
            mInitObj.setDomainID(domainId);
            return this;
        }

        public Builder setSiteGuid(String siteGuid) {
            mInitObj.setSiteGuid(siteGuid);
            return this;
        }

        public Builder setLocale(String localeCountry, String localeDevice, String localeUserState, String localeLanguage) {
            KLocaleModel localeModel = new KLocaleModel();
            localeModel.setLocaleCountry(localeCountry);
            localeModel.setLocaleDevice(localeDevice);
            localeModel.setLocaleUserState(localeUserState);
            localeModel.setLocaleLanguage(localeLanguage);
            mInitObj.setLocale(localeModel);
            return this;
        }

        public KProxyData build() {
            return KProxyData.this;
        }

    }

}
