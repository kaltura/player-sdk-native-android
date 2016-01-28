package com.kaltura.playersdk;

import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
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
	static String sEnableChromecastKey = "&flashvars[chromecast.plugin]=true";


	Map<String, String> mParamsMap;
	String mUrl;

	String mDomain;
	String mAdvertiserID;
	String mEntryId;
	boolean mEnableHover;
	boolean mShouldEnableChromecast = false;
	String mUiConfId;
	String mPartnerId;
	float mCacheSize = 4f;	// 4mb is a sane default.
	String mKS;

	public String getPartnerId() {
		return mPartnerId;
	}

	public String getDomain() {
		return mDomain;
	}

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
	
	public static KPPlayerConfig fromJSONObject(JSONObject configJSON) {
		KPPlayerConfig config = null;
		try {
			JSONObject base = configJSON.getJSONObject("base");
			config = new KPPlayerConfig(base.getString("server"), base.getString("uiConfId"), base.getString("partnerId"));
			
			if (base.has("entryId")) {
				config.setEntryId(base.getString("entryId"));
			}
			if (base.has("advertiserId")) {
				config.setAdvertiserID(base.getString("advertiserId"));
			}
			if (base.has("cacheSize")) {
				config.setCacheSize((float)base.getDouble("cacheSize"));
			}

			JSONObject extra = configJSON.getJSONObject("extra");

			for (Iterator<String> it = extra.keys(); it.hasNext(); ) {
				String key = it.next();
				Object value = extra.opt(key);
				if (value != null) {
					config.addConfig(key, value.toString());
				}
			}
		} catch (JSONException e) {
			Log.e("JSON Error", "", e);
			return null;
		}
		return config;
	}

	public KPPlayerConfig addConfig(String key, String value) {
		if (key != null && key.length() > 0 && value != null && value.length() > 0) {
			String configKey = "flashvars%5B" + key + "%5D";
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

	public void enableChromcast(boolean shouldEnableChromecast) {
		mShouldEnableChromecast = shouldEnableChromecast;
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
		if (mShouldEnableChromecast) {
			mUrl += sEnableChromecastKey;
		}
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

	public KPPlayerConfig setKS(String KS) {
		mKS = KS;
		return this;
	}

	public String getKS() {
		return mKS;
	}
}
