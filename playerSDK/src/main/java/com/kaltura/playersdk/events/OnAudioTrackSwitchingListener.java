package com.kaltura.playersdk.events;

public interface OnAudioTrackSwitchingListener {
	
	public void onAudioSwitchingStart( int oldTrackIndex, int newTrackIndex );

	public void onAudioSwitchingEnd( int newTrackIndex );
}
