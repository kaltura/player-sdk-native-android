package com.kaltura.playersdk;


public interface RequestDataSource {
	
	public String getServerAddress();
	public String getWid();
	public String getUiConfId();
	public String getCacheStr();
	public String getEntryId();
	public KPPlayerConfig getFlashVars();
	public String getUrid();
	
}
