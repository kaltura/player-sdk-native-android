package com.kaltura.playersdk;

import java.util.Map;

import android.net.Uri;

public class RequestHandler {
	private static final String KP_PLAYER_ADATA_SOURCE_WidKey = "wid";
	private static final String KP_PLAYER_ADATA_SOURCE_UiConfIdKey = "uiconf_id";
	private static final String KP_PLAYER_ADATA_SOURCE_CacheStKey = "cache_st";
	private static final String KP_PLAYER_ADATA_SOURCE_EntryId = "entry_id";
	private static final String KP_PLAYER_ADATA_SOURCE_UridKey = "urid";
	
	public static String videoRequestURL(RequestDataSource requestDataSource){
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
	    	builder.appendQueryParameter(KP_PLAYER_ADATA_SOURCE_WidKey, wid);
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
	
}
