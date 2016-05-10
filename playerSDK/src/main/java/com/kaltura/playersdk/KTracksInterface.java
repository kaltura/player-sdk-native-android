package com.kaltura.playersdk;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.kaltura.playersdk.types.TrackType;

import java.util.List;

public interface KTracksInterface {


	 public List<String> getTracksList(TrackType trackType);
	 public int          getTrackCount(TrackType trackType);
	 public int          getCurrentTrackIndex(TrackType trackType);
	 public void         switchTrack(TrackType trackType, int newIndex);
	 public MediaFormat getTrackFormat(TrackType trackType, int index);
	 public String       getTrackName(MediaFormat format);
	 public void         setCaptionListener(ExoplayerWrapper.CaptionListener listener);
	 public void         setMetadataListener(ExoplayerWrapper.Id3MetadataListener listener);
//	 public void setBufferTime( int newTime );
//
//	 public void switchQualityTrack( int newIndex );
//
//	 public void setAutoSwitch( boolean autoSwitch );
//
//	 public float getLastDownloadTransferRate();
//
//	 public float getDroppedFramesPerSecond();
//
//	 public float getBufferPercentage();
}
