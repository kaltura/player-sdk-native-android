package com.kaltura.playersdk;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class KPPlayerConfig {

	/// Key names of the video request
	static String sWidKey = "wid";
	static String sUiConfIdKey = "uiconf_id";
	static String sEntryIdKey = "entry_id";
	static String sUridKey = "urid";
	static String sNativeAdIDKey = "&flashvars[nativeAdId]=";
	static String sEnableHoverKey = "&flashvars[controlBarContainer.hover]=true";
	static String sIFrameEmbedKey = "&iframeembed=true";




	private Map<String, String> mParamsHash;
	private String mUrl;

	private String mDomain;
	private String mWid;
	private String mUrid;
	private String mAdvertiserID;
	private String mEntryId;
	private boolean mEnableHover;
	private String mUiConfId;

	public KPPlayerConfig(String domain, String uiConfId) {
		mDomain = domain;
		mUiConfId = uiConfId;
		mParamsHash = new HashMap<String, String>();
		mParamsHash.put(sUiConfIdKey, uiConfId);
	}

	public void addConfig(String key, String value) {
		if (key != null && key.length() > 0 && value != null && value.length() > 0) {
			String configKey = "flashvars[" + key + "]";
			mParamsHash.put(configKey, value);
		}
	}

	public String appendConfiguration(String videoURL) {
		if (mAdvertiserID != null && mAdvertiserID.length() > 0) {
			videoURL += sNativeAdIDKey + mAdvertiserID;
		}
		if (mEnableHover) {
			videoURL += sEnableHoverKey;
		}
		videoURL += sIFrameEmbedKey;
		return videoURL;
	}

	public String getVideoURL() {
		mUrl = mDomain + "?";
		for (String key: mParamsHash.keySet()) {
			mUrl += key + "=" + mParamsHash.get(key) + "&";
		}
		mUrl = mUrl.substring(0, mUrl.length() - 1);
		return mUrl;
	}

	public void setWid(String wid) {
		mWid = wid;
		mParamsHash.put(sWidKey, wid);
	}

	public String getAdvertiserID() {
		return mAdvertiserID;
	}

	public void setAdvertiserID(String advertiserID) {
		mAdvertiserID = advertiserID;
	}

	public boolean isEnableHover() {
		return mEnableHover;
	}

	public void setEnableHover(boolean enableHover) {
		mEnableHover = enableHover;
	}

	public String getEntryId() {
		return mEntryId;
	}

	public void setEntryId(String entryId) {
		mEntryId = entryId;
		mParamsHash.put(sEntryIdKey, entryId);
	}

	public String getUiConfId() {
		return mUiConfId;
	}
}
