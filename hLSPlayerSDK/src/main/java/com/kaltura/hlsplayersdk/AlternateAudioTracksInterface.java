package com.kaltura.hlsplayersdk;

import com.kaltura.hlsplayersdk.events.OnAudioTrackSwitchingListener;
import com.kaltura.hlsplayersdk.events.OnAudioTracksListListener;


public interface AlternateAudioTracksInterface {
	
	 public void hardSwitchAudioTrack( int newAudioIndex );

	 public void softSwitchAudioTrack( int newAudioIndex );
	
	 public void registerAudioTracksList( OnAudioTracksListListener listener );

	 public void registerAudioSwitchingChange( OnAudioTrackSwitchingListener listener );

}