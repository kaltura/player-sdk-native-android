package com.kaltura.playersdk;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class KPPlayerConfig implements Serializable{

	/// Key names of the video request
	private static final String sKsKey = "ks";
	private static final String sWidKey = "wid";
	private static final String sUiConfIdKey = "uiconf_id";
	private static final String sEntryIdKey = "entry_id";

	private String mServerURL;
	private String mEntryId;
	private String mUiConfId;
	private String mPartnerId;
	private float mCacheSize = 100f;	// 100mb is a sane default.
	private String mKS;
	private Map<String, String> mExtraConfig = new HashMap<>();

	public String getPartnerId() {
		return mPartnerId;
	}

	public String getServerURL() {
		return mServerURL;
	}

	public KPPlayerConfig(String serverURL, String uiConfId, String partnerId) {
		mServerURL = serverURL;
		mUiConfId = uiConfId;
		mPartnerId = partnerId;
	}
	
	private KPPlayerConfig() {}

	/**
	 * Creates a KPPlayerConfig object that wraps the given EmbedFrame URL.
	 * The returned object is immutable, except for cache size. The other fields are not even set,
	 * so their getters will return null.
	 * @param url EmbedFrame URL.
	 * @return new KPPlayerConfig.
     */
	public static KPPlayerConfig fromEmbedFrameURL(String url) {
		final String embedFrameURL = url;
		KPPlayerConfig config =  new KPPlayerConfig() {
			
			@Override
			public String getVideoURL() {
				// just return the input embedFrameURL, don't build it.
				return embedFrameURL;
			}

			// Block the setters that would change the url.
			@Override
			public KPPlayerConfig setEntryId(String entryId) {
				throw new UnsupportedOperationException("Can't set entryId");
			}

			@Override
			public KPPlayerConfig setKS(String KS) {
				throw new UnsupportedOperationException("Can't set ks");
			}
			
			@Override
			public KPPlayerConfig addConfig(String key, String value) {
				throw new UnsupportedOperationException("Can't add config");
			}
		};
		Uri uri = Uri.parse(embedFrameURL);
		config.mServerURL = uri.getScheme() + "://" + uri.getAuthority();
		
		return config;
	}
	
	public static KPPlayerConfig fromJSONObject(JSONObject configJSON) throws JSONException {

		JSONObject base = configJSON.getJSONObject("base");
		JSONObject extra = configJSON.getJSONObject("extra");

		KPPlayerConfig config = new KPPlayerConfig(
				base.getString("server"), 
				base.getString("uiConfId"), 
				base.getString("partnerId"));
		
		if (base.has("entryId")) {
			config.setEntryId(base.getString("entryId"));
		}
		if (base.has("ks")) {
			config.setKS(base.getString("ks"));
		}

		for (Iterator<String> it = extra.keys(); it.hasNext(); ) {
			String key = it.next();
			Object value = extra.opt(key);
			if (value != null) {
				config.addConfig(key, value.toString());
			}
		}
		return config;
	}

	public KPPlayerConfig addConfig(String key, String value) {
		if (key != null && key.length() > 0 && value != null && value.length() > 0) {
			
			mExtraConfig.put(key, value);
		}
		return this;
	}
	
	public String getQueryString() {

		Uri.Builder builder = new Uri.Builder();
		
		// First add basic fields
		if (mPartnerId != null) {
			builder.appendQueryParameter(sWidKey, "_" + mPartnerId);
		}
		if (mUiConfId != null) {
			builder.appendQueryParameter(sUiConfIdKey, mUiConfId);
		}
		if (mEntryId != null) {
			builder.appendQueryParameter(sEntryIdKey, mEntryId);
		}
		if (mKS != null) {
			builder.appendQueryParameter(sKsKey, mKS);
		}
		
		// Then the extras
		for (Map.Entry<String, String> entry : mExtraConfig.entrySet()) {
			builder.appendQueryParameter("flashvars[" + entry.getKey() + "]", entry.getValue());
		}

		return builder.build().getEncodedQuery();
	}

	public void setChromecastEnabled(boolean chromecastEnabled) {
		addConfig("chromecast.plugin", String.valueOf(chromecastEnabled));
	}
	
	public void setHideControlsOnPlay(boolean hide) {
		addConfig("controlBarContainer.hover", Boolean.toString(hide));
	}

	public void setCacheSize (float cacheSize) {
		mCacheSize = cacheSize;
	}

	public float getCacheSize() {
		return mCacheSize;
	}

	public String getVideoURL() {
		Uri.Builder builder = Uri.parse(mServerURL).buildUpon();
		builder.appendPath("p").appendPath(mPartnerId).appendPath("sp").appendPath(mPartnerId + "00")
				.appendPath("embedIframeJs").appendPath("uiconf_id").appendPath(mUiConfId);
		
		if (mEntryId != null) {
			builder.appendPath(sEntryIdKey).appendPath(mEntryId);
		}
		
		builder.appendQueryParameter("iframeembed", "true");

		return builder.build().toString() + "&" + getQueryString(); 
	}

	public String getEntryId() {
		return mEntryId;
	}

	public KPPlayerConfig setEntryId(String entryId) {
		mEntryId = entryId;
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
