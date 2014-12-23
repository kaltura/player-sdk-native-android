package com.kaltura.hlsplayersdk.events;

public interface OnAudioTrackSwitchingListener {
	
	public void onAudioSwitchingStart( int oldTrackIndex, int newTrackIndex );

	public void onAudioSwitchingEnd( int newTrackIndex );
}
