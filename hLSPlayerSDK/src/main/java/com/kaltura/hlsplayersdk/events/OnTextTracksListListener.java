package com.kaltura.hlsplayersdk.events;

import java.util.List;

public interface OnTextTracksListListener {
	public void OnTextTracksList(List<String> list, int defaultTrackIndex);
}
