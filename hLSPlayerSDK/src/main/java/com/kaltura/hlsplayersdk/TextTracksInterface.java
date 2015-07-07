package com.kaltura.hlsplayersdk;

import com.kaltura.hlsplayersdk.events.OnTextTrackChangeListener;
import com.kaltura.hlsplayersdk.events.OnTextTrackTextListener;
import com.kaltura.hlsplayersdk.events.OnTextTracksListListener;


public interface TextTracksInterface {
	
	public void switchTextTrack( int newIndex );
	
	public void registerTextTracksList( OnTextTracksListListener listener );

	public void registerTextTrackChanged( OnTextTrackChangeListener listener );
	
	public void registerTextTrackText (OnTextTrackTextListener listener );

}