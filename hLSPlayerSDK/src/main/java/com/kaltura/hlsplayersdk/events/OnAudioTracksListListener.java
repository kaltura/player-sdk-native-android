package com.kaltura.hlsplayersdk.events;

import java.util.List;

public interface OnAudioTracksListListener {
	public void OnAudioTracksList( List<String> list, int defaultTrackIndex );
}
