package com.kaltura.playersdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.example.kplayersdk.BuildConfig;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class KPPlayerConfig {
	
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

    static public String nativeVersion(Context context) {
        String version = "";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "&flashvars[nativeVersion]=" + "Android_" + version;
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
}
