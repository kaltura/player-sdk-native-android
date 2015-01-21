package com.kaltura.hlsplayersdk;

import com.kaltura.hlsplayersdk.events.OnQualitySwitchingListener;
import com.kaltura.hlsplayersdk.events.OnQualityTracksListListener;

public interface QualityTracksInterface {
	
	/**
	 * 
	 * @param newTime set buffer time
	 */
	public void setBufferTime( int newTime );
	
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
	
	 /**
	 * Calls listener when quality list is available 
	 * @param listener
	 */
	 public void registerQualityTracksList( OnQualityTracksListListener listener );

	 /**
	  * Calls listeners when quality switch starts / ends
	  * @param listener
	  */
	 public void registerQualitySwitchingChange( OnQualitySwitchingListener listener );
	 
	 /**
	  * 
	  * @return transfer rate of last download (KBPS)
	  */
	 public float getLastDownloadTransferRate();
	 
	 /**
	  * 
	  * @return dropped frame per second
	  */
	 public float getDroppedFramesPerSecond();
	 
	 /**
	  * 
	  * @return buffer full percentage
	  */
	 public float getBufferPercentage();
	 
	 /**
	  * 
	  * @return current quality index
	  */
	 public int getCurrentQualityIndex();

}