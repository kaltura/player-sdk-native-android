package com.kaltura.playersdk.events;

public interface OnQualitySwitchingListener {	
	
	public void onQualitySwitchingStart( int oldTrackIndex, int newTrackIndex );

	public void onQualitySwitchingEnd( int newTrackIndex );
}
