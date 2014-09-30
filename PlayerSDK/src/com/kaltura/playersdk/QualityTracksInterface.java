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
	  * @return transfer rate of last download
	  */
	 public float getLastDownloadTransferRate();
	 
	 /**
	  * 
	  * @return dropped frame count
	  */
	 public float getDroppedFrameCount();
	 
	 /**
	  * 
	  * @return buffer empting rate
	  */
	 public float getBufferEmptyRate();
	 
	 /**
	  * 
	  * @return current quality index
	  */
	 public int getCurrentQualityIndex();

}
