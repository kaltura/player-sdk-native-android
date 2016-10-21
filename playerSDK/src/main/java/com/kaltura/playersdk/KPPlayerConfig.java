package com.kaltura.playersdk;

import android.net.Uri;

import com.kaltura.playersdk.players.KMediaFormat;
import com.kaltura.playersdk.utils.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;


public class KPPlayerConfig implements Serializable{
	
	public static class CacheConfig {
		List<Pattern> includePatterns = new ArrayList<>();
		
		public void addIncludePattern(String pattern) {
			includePatterns.add(Pattern.compile(pattern));
		}
		public void addIncludePattern(Pattern pattern) {
			includePatterns.add(pattern);
		}
		public void reset() {
			includePatterns.clear();
		}
	}

	public static String TAG = "KPPlayerConfig";

	/// Key names of the video request
	private static final String sKsKey = "ks";
	private static final String sWidKey = "wid";
	private static final String sUiConfIdKey = "uiconf_id";
	private static final String sEntryIdKey = "entry_id";
	private double mMediaPlayFrom = 0;

	private String mServerURL;
	private String mEntryId;
	private String mUiConfId;
	private String mPartnerId;
	private String mLocalContentId = "";
	private float mCacheSize = 100f;	// 100mb is a sane default.
	private String mKS;
	private String mAdMimeType;
	private int mAdPreferredBitrate;
	private int mContentPreferredBitrate;

	private Map<String, String> mExtraConfig = new HashMap<>();
	private boolean mAutoPlay = false;
	private boolean isWebDialogEnabled = false;
	private final CacheConfig mCacheConfig = new CacheConfig();

	static {
		// Use System.out to print even when Log.X() are disabled.
		System.out.println("Kaltura Player Android SDK, version " + BuildConfig.VERSION_NAME);
	}
	
	public static String getPlayerSdkVersion() {
		return BuildConfig.VERSION_NAME;
	}
	
	public CacheConfig getCacheConfig() {
		return mCacheConfig;
	}
	
	
	public String getPartnerId() {
		return mPartnerId;
	}

	public String getServerURL() {
		return mServerURL;
	}

	public KPPlayerConfig(String serverURL, String uiConfId, String partnerId) {
		mServerURL  = serverURL;
		mUiConfId   = uiConfId;
		mPartnerId  = partnerId;
		mAdMimeType = KMediaFormat.mp4_clear.mimeType;
		mAdPreferredBitrate = -1; // in bits
		mContentPreferredBitrate = -1; // in KBits
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
				//adding # so in native callout will have the hash of supported mimetypes
				return embedFrameURL + "#";
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
		
		KPPlayerConfig config = new KPPlayerConfig(
				base.getString("server"), 
				base.getString("uiConfId"), 
				base.getString("partnerId"));
		
		config.setEntryId(Utilities.optString(base, "entryId"));
		config.setKS(Utilities.optString(base, "ks"));
		
		if (!configJSON.isNull("extra")) {
			JSONObject extra = configJSON.getJSONObject("extra");

			for (Iterator<String> it = extra.keys(); it.hasNext(); ) {
				String key = it.next();
				Object value = extra.opt(key);
				if (value != null) {
					config.addConfig(key, value.toString());
				}
			}
		}

		return config;
	}

	public KPPlayerConfig addConfig(String key, String value) {
		if (key != null && key.length() > 0 && value != null && value.length() > 0) {
			if (key.equals("mediaProxy.mediaPlayFrom")) {
				mMediaPlayFrom = Double.parseDouble(value);
				return this;
			}
			if (key.equals("mediaProxy.preferedFlavorBR") || key.equals("mediaProxy.preferredFlavorBR")) { // in web it is preferedFlavorBR if it is fixed will keep working
				mContentPreferredBitrate = Integer.valueOf(value);
				return this;
			}
			mExtraConfig.put(key, value);
		}
		return this;
	}

	public boolean isAutoPlay() {
		return mAutoPlay;
	}

	public void setAutoPlay(boolean autoPlay) {
		mAutoPlay = autoPlay;
		addConfig("autoPlay", autoPlay ? "true" : "false");
	}

	public void setWebDialogEnabled(boolean isEnabled) {
		isWebDialogEnabled = isEnabled;
	}

	public boolean isWebDialogEnabled() {
		return isWebDialogEnabled;
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

	public String getLocalContentId() {
		return mLocalContentId;
	}
	
	public void setLocalContentId(String localContentId) {
        mLocalContentId = localContentId;
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

		return builder.build().toString() + "&" + getQueryString() + "#localContentId=" + mLocalContentId + "&";
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

	public double getMediaPlayFrom() {
		return mMediaPlayFrom;
	}
	
	public String getConfigValueString(String key) {
		return mExtraConfig.get(key);
	}

	/*
	This method give the ability to change the default MP4 ad plyback to
	some other mimetypes:
	       //mimeTypes.add("application/x-mpegURL");
           //mimeTypes.add("video/mp4");
           //mimeTypes.add("video/3gpp");
	*/
	public void setAdMimeType(String adMimeType) {
		mAdMimeType = adMimeType;
	}

	public String getAdMimeType() {
		return mAdMimeType;
	}


	/*
		This method defines the preferred bitrate threshold in bits 1Mbit = 1000000bit
		the IMAAdPlayer will taske bitratethat match this threshold and is <= from it
 	*/
	public void setAdPreferredBitrate(int adPreferredBitrate) {
		mAdPreferredBitrate = adPreferredBitrate;
	}

	public int getAdPreferredBitrate() {
		return mAdPreferredBitrate;
	}

	public int getContentPreferredBitrate() {
		return mContentPreferredBitrate;
	}
}
