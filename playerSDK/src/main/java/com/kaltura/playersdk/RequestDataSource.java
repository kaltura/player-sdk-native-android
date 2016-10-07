package com.kaltura.playersdk;

import com.kaltura.playersdk.config.KPPlayerConfig;

/*
 * This class will be implemented by the SDK User and will be given to the SDK upon setComponents call
 */
public interface RequestDataSource {
	
	public String getServerAddress();
	public String getWid();
	public String getUiConfId();
	public String getCacheStr();
	public String getEntryId();
	public KPPlayerConfig getFlashVars();
	public String getUrid();
    public boolean isSpecificVersionTemplate();
	
}
