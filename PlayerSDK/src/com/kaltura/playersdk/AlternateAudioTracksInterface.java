package com.kaltura.playersdk;

import com.kaltura.playersdk.events.OnAudioTrackSwitchingListener;
import com.kaltura.playersdk.events.OnAudioTracksListListener;


public interface AlternateAudioTracksInterface {
	
	 public void hardSwitchAudioTrack( int newAudioIndex );

	 public void softSwitchAudioTrack( int newAudioIndex );
	
	 public void registerAudioTracksList( OnAudioTracksListListener listener );

	 public void registerAudioSwitchingChange( OnAudioTrackSwitchingListener listener );

}
