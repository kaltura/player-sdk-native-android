package com.kaltura.playersdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.util.Map;

public class RequestHandler {
	private static final String KP_PLAYER_ADATA_SOURCE_WidKey = "wid";
	private static final String KP_PLAYER_ADATA_SOURCE_UiConfIdKey = "uiconf_id";
	private static final String KP_PLAYER_ADATA_SOURCE_CacheStKey = "cache_st";
	private static final String KP_PLAYER_ADATA_SOURCE_EntryId = "entry_id";
	private static final String KP_PLAYER_ADATA_SOURCE_UridKey = "urid";


    public static String videoRequestURL(RequestDataSource requestDataSource){
        if(requestDataSource.isSpecificVersionTemplate()){
            return videoRequestURLSpecific(requestDataSource);
        }else {
            return videoRequestURLProduction(requestDataSource);
        }
    }

    private static String videoRequestURLProduction(RequestDataSource requestDataSource){
        String serverAddress = requestDataSource.getServerAddress();
        if (serverAddress == null || serverAddress.length() == 0) {
            return null;
        }

        String wid = requestDataSource.getWid();
        String uiConfId = requestDataSource.getUiConfId();
        String cacheStr = requestDataSource.getCacheStr();
        String entryId = requestDataSource.getEntryId();
        String urid = requestDataSource.getUrid();
        KPPlayerConfig configFlags = requestDataSource.getFlashVars();
        String url = String.format("%s/p/%s/sp/%s00/embedIframeJs/uiconf_id/%s/partner_id/%s",serverAddress,wid,wid,uiConfId,wid);

        Uri.Builder builder = Uri.parse(url).buildUpon();

        if (entryId != null && !entryId.isEmpty()){
            builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_EntryId, entryId);
        }

        if (cacheStr != null && !cacheStr.isEmpty()){
            builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_CacheStKey, cacheStr);
        }

        if (configFlags != null) {
            for (Map.Entry<String,String> flashEntry : configFlags.getFlashVarsArray()) {
                builder.appendQueryParameter(flashEntry.getKey(), flashEntry.getValue());
            }
        }

        builder.appendQueryParameter("iframeembed", "true");

        return builder.build().toString().replace("%5B", "[").replace("%5D", "]");
    }



	private static String videoRequestURLSpecific(RequestDataSource requestDataSource){
		String url = requestDataSource.getServerAddress();
	    if (url == null || url.length() == 0) {
	        return null;
	    }
	    
	    String wid = requestDataSource.getWid();
	    String uiConfId = requestDataSource.getUiConfId();
	    String cacheStr = requestDataSource.getCacheStr();
	    String entryId = requestDataSource.getEntryId();
	    String urid = requestDataSource.getUrid();
	    KPPlayerConfig configFlags = requestDataSource.getFlashVars();
	    
	    Uri.Builder builder = Uri.parse(url).buildUpon();
	    if (wid != null && wid.length() > 0) {
	    	builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_WidKey, "_"+wid);
	    }
	    
	    if (uiConfId != null && !uiConfId.isEmpty()){
	    	builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_UiConfIdKey, uiConfId);
	    }
	    
	    if (cacheStr != null && !cacheStr.isEmpty()){
	    	builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_CacheStKey, cacheStr);
	    }
	    
	    if (entryId != null && !entryId.isEmpty()){
	    	builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_EntryId, entryId);
	    }
	    
	    if (urid != null && !urid.isEmpty()){
	    	builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_UridKey, urid);
	    }
	    
	    if (configFlags != null) {
	    	for (Map.Entry<String,String> flashEntry : configFlags.getFlashVarsArray()) {
				builder.appendQueryParameter(flashEntry.getKey(), flashEntry.getValue());
			}
	    }

	    return builder.build().toString().replace("%5B", "[").replace("%5D", "]");
	}

    public static String getIframeUrlWithNativeVersion(String iframeUrl, Context context) {
        if(iframeUrl == null || iframeUrl.length() == 0){
            return null;
        }

        String version = "";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return iframeUrl + "&flashvars[nativeVersion]=" + "Android_" + version;
    }
	
}
