package com.kaltura.playersdk;

import com.kaltura.playersdk.events.OnQualitySwitchingListener;
import com.kaltura.playersdk.events.OnQualityTracksListListener;

public interface QualityTracksInterface {
	
	/**
	 * Will turn autoSwitch off and select given index
	 * @param newIndex
	 */
	 public void switchQualityTrack( int newIndex );
	 
	 /**
	  * @param autoSwitch when set to true player will automatically switch
	  *  quality
	  *  
	  */
	 public void setAutoSwitch( boolean autoSwitch );
	
	 public void registerQualityTracksList( OnQualityTracksListListener listener );

	 public void registerQualitySwitchingChange( OnQualitySwitchingListener listener );

}
