package com.kaltura.playersdk.tracks;

import java.util.List;

public interface KTrackActions {
	 List<TrackFormat> getAudioTrackList();
	 List<TrackFormat> getTextTrackList();
	 List<TrackFormat> getVideoTrackList();
	 TrackFormat       getCurrentTrack(TrackType trackType);
	 void              switchTrack(TrackType trackType, int newIndex);
	 void              switchTrackByBitrate(TrackType trackType, int preferredBitrateKBit);

	interface EventListener {
		void onTracksUpdate(KTrackActions tracksManager);
	}
	interface VideoTrackEventListener {
		void onVideoTrackChanged(int currentTrack);
	}
	interface AudioTrackEventListener {
		void onAudioTrackChanged(int currentTrack);
	}
	interface TextTrackEventListener {
		void onTextTrackChanged(int currentTrack);
	}
}
