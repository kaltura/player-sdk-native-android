package com.kaltura.playersdk;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class KPPlayerConfig {

	/// Key names of the video request
	static String sEntryIdKey = "entry_id";
	static String sNativeAdIDKey = "&flashvars[nativeAdId]=";
	static String sEnableHoverKey = "&flashvars[controlBarContainer.hover]=true";
	static String sIFrameEmbedKey = "&iframeembed=true";




	private HashMap<String, String> mParamsMap;
	private String mUrl;

	private String mDomain;
	private String mAdvertiserID;
	private String mEntryId;
	private boolean mEnableHover;
	private String mUiConfId;
	private String mPartnerId;

	public KPPlayerConfig(String domain, String uiConfId, String partnerId) {
		mDomain = domain;
		mUiConfId = uiConfId;
		mPartnerId = partnerId;
	}

	private HashMap<String, String> getParamsMap() {
		if (mParamsMap == null) {
			mParamsMap = new HashMap<String, String>();
		}
		return mParamsMap;
	}

	public void addConfig(String key, String value) {
		if (key != null && key.length() > 0 && value != null && value.length() > 0) {
			String configKey = "flashvars[" + key + "]";
			getParamsMap().put(configKey, value);
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
		mUrl = mDomain + "/p/" + mPartnerId + "/sp/" + mPartnerId + "00/embedIframeJs/uiconf_id/" + mUiConfId + "/partner_id/" + mPartnerId + "?";
		for (String key: getParamsMap().keySet()) {
			mUrl += key + "=" + getParamsMap().get(key) + "&";
		}
		mUrl = mUrl.substring(0, mUrl.length() - 1);
		return mUrl;
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
		getParamsMap().put(sEntryIdKey, entryId);
	}

	public String getUiConfId() {
		return mUiConfId;
	}
}
