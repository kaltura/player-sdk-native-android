package com.kaltura.playersdk;

import com.kaltura.playersdk.events.OnTextTrackChangeListener;
import com.kaltura.playersdk.events.OnTextTracksListListener;


public interface TextTracksInterface {
	
	public void switchTextTrack( int newIndex );
	
	 public void registerTextTracksList( OnTextTracksListListener listener );

	 public void registerTextTrackChanged( OnTextTrackChangeListener listener );

}
