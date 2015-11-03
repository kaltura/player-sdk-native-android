package com.kaltura.playersdk;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class KPPlayerConfig implements Serializable{

	/// Key names of the video request
	static String sWidKey = "wid";
	static String sUiConfIdKey = "uiconf_id";
	static String sEntryIdKey = "entry_id";
	static String sUridKey = "urid";
	static String sNativeAdIDKey = "&flashvars[nativeAdId]=";
	static String sEnableHoverKey = "&flashvars[controlBarContainer.hover]=true";
	static String sIFrameEmbedKey = "&iframeembed=true";




	private Map<String, String> mParamsMap;
	private String mUrl;

	private String mDomain;
	private String mWid;
	private String mUrid;
	private String mAdvertiserID;
	private String mEntryId;
	private boolean mEnableHover;
	private String mUiConfId;
	private String mPartnerId;
	private float mCacheSize = 0;

	public KPPlayerConfig(String domain, String uiConfId, String partnerId) {
		mDomain = domain;
		mUiConfId = uiConfId;
		mPartnerId = partnerId;
		mParamsMap = new HashMap<String, String>();
	}

	public KPPlayerConfig addConfig(String key, String value) {
		if (key != null && key.length() > 0 && value != null && value.length() > 0) {
			String configKey = "flashvars[" + key + "]";
			mParamsMap.put(configKey, value);
		}
		return this;
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

	public void setCacheSize (float cacheSize) {
		mCacheSize = cacheSize;
	}

	public float getCacheSize() {
		return mCacheSize;
	}

	public String getVideoURL() {
		mUrl = mDomain + "/p/" + mPartnerId + "/sp/" + mPartnerId + "00/embedIframeJs/uiconf_id/" + mUiConfId;
		if (mEntryId != null) {
			mUrl += "/entry_id/" + mEntryId + "?";
		} else {
			mUrl += "?";
		}
		mUrl += "wid=_" + mPartnerId + "&";
		for (String key: mParamsMap.keySet()) {
			mUrl += key + "=" + mParamsMap.get(key) + "&";
		}
		mUrl += "iframeembed=true";
		return mUrl;
	}

	public String getAdvertiserID() {
		return mAdvertiserID;
	}

	public KPPlayerConfig setAdvertiserID(String advertiserID) {
		mAdvertiserID = advertiserID;
		return this;
	}

	public boolean isEnableHover() {
		return mEnableHover;
	}

	public KPPlayerConfig setEnableHover(boolean enableHover) {
		mEnableHover = enableHover;
		return this;
	}

	public String getEntryId() {
		return mEntryId;
	}

	public KPPlayerConfig setEntryId(String entryId) {
		mEntryId = entryId;
		mParamsMap.put(sEntryIdKey, entryId);
		return this;
	}

	public String getUiConfId() {
		return mUiConfId;
	}
}
