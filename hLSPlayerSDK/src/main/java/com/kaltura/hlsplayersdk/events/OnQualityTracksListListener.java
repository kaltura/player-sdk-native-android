package com.kaltura.hlsplayersdk.events;

import java.util.List;

import com.kaltura.hlsplayersdk.QualityTrack;

public interface OnQualityTracksListListener {
	public void OnQualityTracksList( List<QualityTrack> list, int defaultTrackIndex );
}
