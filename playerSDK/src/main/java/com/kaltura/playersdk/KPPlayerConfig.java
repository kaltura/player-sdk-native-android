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
	static String sCacheStKey = "cache_st";
	static String sEntryIdKey = "entry_id";
	static String sPlayerIdKey = "playerId";
	static String sUridKey = "urid";
	static String sDebugKey = "debug";
	static String sForceHtml5Key = "forceMobileHTML5";
	static String sNativeAdIDKey = "&flashvars[nativeAdId]=";
	static String sEnableHoverKey = "&flashvars[controlBarContainer.hover]=true";
	static String sIFrameEmbedKey = "&iframeembed=true";

	public static enum Key{
		KP_PLAYER_CONFIG_NATIVE_AD_ID_KEY("nativeAdId"),
		KP_PLAYER_CONFIG_NATIVE_CALL_OUT_KEY("nativeCallout"),
		KP_PLAYER_CONFIG_CHROMECAST_KEY("chromecast.plugin"),
		KP_PLAYER_CONFIG_LEAD_ANDROID_HLS("Kaltura.LeadHLSOnAndroid");

		private String label;

		private Key(String str) {
			this.label = str;
		}

		public String toString() {
			return this.label;
		}

	}

	private Map<Key,String> mFlashvarsDict;

	private Map<String, String> mParamsHash;
	private String mUrl;

	private String mDomain;
	private String mWid;
	private String mCacheSt;
	private String mUrid;
	private String mAdvertiserID;
	private String mEntryId;
	private boolean mEnableHover;
	private boolean mDebug;
	private boolean mForceMobileHTML5;


	private String mUiConfId;
	private String mPlayerId;




	public KPPlayerConfig(){
		setupKPPlayerConfig();
	}

	public void setConfigKey(Key key, String value){
		mFlashvarsDict.put(key, value);
	}

	public List<Map.Entry<String,String>> getFlashVarsArray(){
		List<Map.Entry<String,String>> flashVarsArray = new ArrayList<Map.Entry<String,String>>();
		for (Entry<Key, String> pairs : mFlashvarsDict.entrySet()) {
			if(pairs.getKey() != null && !pairs.getValue().isEmpty()) {
				flashVarsArray.add(new AbstractMap.SimpleEntry<String, String>(String.format("flashvars[%s]",pairs.getKey()), pairs.getValue()));
			}

		}

		return flashVarsArray;
	}

	private void addDefaultFlags(){
		if (mFlashvarsDict != null){
			mFlashvarsDict.put(Key.KP_PLAYER_CONFIG_NATIVE_AD_ID_KEY, ""); //TODO:define value for advertiser id
		}
	}
	private void setupKPPlayerConfig(){
		mFlashvarsDict = new HashMap<Key,String>();
		addDefaultFlags();
	}

	public KPPlayerConfig(String domain, String uiConfId, String playerID) {
		mDomain = domain;
		mUiConfId = uiConfId;
		mPlayerId = playerID;
		mParamsHash = new HashMap<String, String>();
		mParamsHash.put(sUiConfIdKey, uiConfId);
		mParamsHash.put(sPlayerIdKey, playerID);
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

	public String getWid() {
		return mWid;
	}

	public void setWid(String wid) {
		mWid = wid;
		mParamsHash.put(sWidKey, wid);
	}

	public String getCacheSt() {
		return mCacheSt;
	}

	public void setCacheSt(String cacheSt) {
		mCacheSt = cacheSt;
		mParamsHash.put(sCacheStKey, cacheSt);
	}

	public String getUrid() {
		return mUrid;
	}

	public void setUrid(String urid) {
		mParamsHash.put(sUridKey, urid);
		mUrid = urid;
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

	public boolean isDebug() {
		return mDebug;
	}

	public void setDebug(boolean debug) {
		mDebug = debug;
		if (debug) {
			mParamsHash.put(sDebugKey, "true");
		}
	}

	public boolean isForceMobileHTML5() {
		return mForceMobileHTML5;
	}

	public void setForceMobileHTML5(boolean forceMobileHTML5) {
		mForceMobileHTML5 = forceMobileHTML5;
		if (forceMobileHTML5) {
			mParamsHash.put(sForceHtml5Key, "true");
		}
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

	public String getPlayerId() {
		return mPlayerId;
	}
}
