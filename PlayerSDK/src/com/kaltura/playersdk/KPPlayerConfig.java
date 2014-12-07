package com.kaltura.playersdk;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KPPlayerConfig {
	public final static String KP_PLAYER_CONFIG_NATIVE_AD_ID_KEY = "nativeAdId";
	public final static String KP_PLAYER_CONFIG_NATIVE_CALL_OUT_KEY = "nativeCallout";
	public final static String KP_PLAYER_CONFIG_CHROMECAST_KEY = "chromecast.plugin";
	public final static String KP_PLAYER_CONFIG_LEAD_ANDROID_HLS = "Kaltura.LeadHLSOnAndroid";
	
	private Map<String,String> mFlashvarsDict;
	
	public KPPlayerConfig(){
		setupKPPlayerConfig();
	}
	
	public void setConfigKey(String key, String value){
		mFlashvarsDict.put(key, value);
	}
	
	public List<Map.Entry<String,String>> getFlashVarsArray(){
		List<Map.Entry<String,String>> flashVarsArray = new ArrayList<Map.Entry<String,String>>();
		for (Map.Entry<String,String> pairs : mFlashvarsDict.entrySet()) {
			if(!pairs.getKey().isEmpty() && !pairs.getValue().isEmpty()) {
				flashVarsArray.add(new AbstractMap.SimpleEntry<String, String>(String.format("flashvars[%s]",pairs.getKey()), pairs.getValue()));
			}
			
		}
		
		return flashVarsArray;
	}
	
	private void addDefaultFlags(){
		if (mFlashvarsDict != null){
			mFlashvarsDict.put(KP_PLAYER_CONFIG_NATIVE_AD_ID_KEY, ""); //TODO:define value for advertiser id
		}
	}
	private void setupKPPlayerConfig(){
		mFlashvarsDict = new HashMap<String,String>();
		addDefaultFlags();
	}
}
