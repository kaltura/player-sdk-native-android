package com.kaltura.playersdk;

import android.net.Uri;

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
	

	protected Map<String, String> mParamsMap;
	protected String mUrl;

	protected String mDomain;
	protected String mAdvertiserID;
	protected String mEntryId;
	protected boolean mEnableHover;
	protected String mUiConfId;
	protected String mPartnerId;
	protected float mCacheSize = 4f;	// 4mb is a sane default.

	public KPPlayerConfig(String domain, String uiConfId, String partnerId) {
		mDomain = domain;
		mUiConfId = uiConfId;
		mPartnerId = partnerId;
		mParamsMap = new HashMap<String, String>();
	}
	
	public static KPPlayerConfig valueOf(String url) {
		KPPlayerConfig config =  new KPPlayerConfig(null, null, null) {

			@Override
			public String getVideoURL() {
				// just return the input url, don't build it.
				return mUrl;
			}

			// block all setters that would change mURL
			@Override
			public KPPlayerConfig setAdvertiserID(String advertiserID) {
				throw new UnsupportedOperationException("Can't set advertiserID");
			}

			@Override
			public KPPlayerConfig setEnableHover(boolean enableHover) {
				throw new UnsupportedOperationException("Can't set enableHover");
			}

			@Override
			public KPPlayerConfig setEntryId(String entryId) {
				throw new UnsupportedOperationException("Can't set entryId");
			}

			@Override
			public KPPlayerConfig addConfig(String key, String value) {
				throw new UnsupportedOperationException("Can't add config");
			}

			@Override
			public String appendConfiguration(String videoURL) {
				throw new UnsupportedOperationException("Can't append configuration");
			}

		};
		Uri uri = Uri.parse(url);
		config.mDomain = uri.getScheme() + "://" + uri.getAuthority();
		config.mUrl = url;
		
		return config;
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
