package com.kaltura.hlsplayersdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.os.SystemClock;
import android.util.Log;

import com.kaltura.hlsplayersdk.cache.HLSSegmentCache;
import com.kaltura.hlsplayersdk.cache.SegmentCachedListener;
import com.kaltura.hlsplayersdk.manifest.M2TSParser;
import com.kaltura.hlsplayersdk.manifest.ManifestEncryptionKey;
import com.kaltura.hlsplayersdk.manifest.ManifestParser;
import com.kaltura.hlsplayersdk.manifest.ManifestPlaylist;
import com.kaltura.hlsplayersdk.manifest.ManifestReloader;
import com.kaltura.hlsplayersdk.manifest.ManifestSegment;
import com.kaltura.hlsplayersdk.manifest.ManifestStream;
import com.kaltura.hlsplayersdk.subtitles.SubtitleHandler;
import com.kaltura.hlsplayersdk.types.ByteArray;
import com.kaltura.hlsplayersdk.types.TrackType;



// This is the confusingly named "HLSIndexHandler" from the flash HLSPlugin
// I'll change it, if anyone really hates the new name. It just makes more sense to me.
public class StreamHandler implements ManifestParser.ReloadEventListener, ManifestReloader.ManifestGetHandler, SegmentCachedListener {
	
	// Constant that allows modifying the behavior when starting a live stream. If the value is true (the intended state), it
	// should cause the player to start at the live edge of the stream. If the value is false, it will start at the earliest
	// part of the stream. It's only for testing purposes.
	private static final boolean SKIP_TO_END_OF_LIVE = true;
	
	public static int EDGE_BUFFER_SEGMENT_COUNT = 3;	// The number of segments to keep between playback and live edge.
	
	private KnowledgePrepHandler mKnowledgePrepHandler = null;
	public interface KnowledgePrepHandler
	{
		public void knowledgePrefetchComplete();
	}

	private class ErrorTimer
	{
		long mStartTime = 0;
		long mLastCheckTime = 0;

		private boolean mIsRunning = false;

		public void start()
		{
			mStartTime = SystemClock.elapsedRealtime();
		}

		public void stop()
		{
			mIsRunning = false;
		}

		public boolean isRunning()
		{
			return mIsRunning;
		}

		public long elapsedTime()
		{
			if (mIsRunning)
			{
				mLastCheckTime = SystemClock.elapsedRealtime();
			}
			return mLastCheckTime - mStartTime;
		}

	};

	public ManifestReloader reloader = new ManifestReloader();

	public static final int USE_DEFAULT_START = -999;

	public int lastSequence = 0;
	public int altAudioIndex = -1;
	private int reloadingAltAudioIndex = -1;
	public double lastKnownPlaylistStartTime = 0.0;
	public int lastQuality = 0;
	public int targetQuality = 0;
	public ManifestParser altAudioManifest = null;
	public ManifestParser baseManifest = null;
	public int reloadingQuality = 0;
	public String baseUrl = null;
	public String lastBadManifestUri = "";

	private final long recognizeBadStreamTime = 20000;

	private ErrorTimer mErrorSurrenderTimer = new ErrorTimer();
	private boolean mIsRecovering = false;

	private ManifestStream primaryStream = null;
	private boolean stalled = false;
	private boolean closed = false;
	/*
	 * BestEffortRequest
	 * 
	 * A container for keeping track of a segment used in determining the time base
	 * of a particular stream. 
	 *  
	 */
	public class BestEffortRequest
	{
		public final static int TYPE_VIDEO = 0;
		public final static int TYPE_AUDIO = 1;
		public final static int TYPE_AUDIO_VIDEO = 2;  // This is for when the segment has an alt audio segment as well as the base video segment
		
		public int type = TYPE_VIDEO;
		public ManifestSegment segment = null;
		public boolean downloadComplete = false;
		public boolean parsed = false;
		
		public BestEffortRequest(ManifestSegment seg, int segType)
		{
			segment = seg;
			
			type = segType;
			
			if (seg.altAudioSegment != null) // Don't trust the type they send to have knowledge of the alt audio stream
				type = TYPE_AUDIO_VIDEO;
		}
	}
	
	
	/*
	 * Returns the best effort request type from the string type of a manifest parser
	 * 
	 * May need to be extended to handle other types.
	 * 
	 */
	public int bestEffortTypeFromString(String t)
	{
		if (t.equals("AUDIO"))
		{
			return BestEffortRequest.TYPE_AUDIO;
		}
		return BestEffortRequest.TYPE_VIDEO;
	}
	
	
	private List<BestEffortRequest> _bestEffortRequests = new ArrayList<BestEffortRequest>(); // Active best effort requests


	public StreamHandler(ManifestParser parser)
	{
		baseManifest = parser;
		for (int i = 0; i < baseManifest.playLists.size(); ++i)
		{
			ManifestPlaylist mp = baseManifest.playLists.get(i);
			if (mp.isDefault)
			{
				altAudioManifest = mp.manifest;
				altAudioIndex = i;
				break;
			}
		}
	}
	
	public void doKnowledgePrep(KnowledgePrepHandler prepHandler)
	{
		mKnowledgePrepHandler = prepHandler;
		if (streamEnds())
		{
			//initiateBestEffortRequest(Integer.MAX_VALUE, lastQuality);
			initiateBestEffortRequest(0, lastQuality, true);
		}
		else
		{
			initiateBestEffortRequest(Integer.MAX_VALUE, lastQuality, true);
		}
	}
	
	// WE keep a list of witnesses of known PTS start values for segments.
	// This is indexed by segmentURL and returns the PTS start for that
	// seg if known. Since all segments are immutable, we can keep this
	// as a global cache.
	
	public Map<String, Double> startTimeWitnesses = new HashMap<String, Double>();
	
	public Vector<ManifestSegment> updateSegmentTimes(Vector<ManifestSegment> segments)
	{
		// Using our witnesses, fill in as much knowledge as we can about
		// segment start/end times.
		
		// Keep track of whatever segments we've assigned to.
		int setSegments[] = new int[segments.size()];
		
		// First, set any exactly known values.
		for (int i = 0; i < segments.size(); ++i)
		{
			// Skip unknowns.
			if (!startTimeWitnesses.containsKey(segments.get(i).uri))
				continue;
			
			segments.get(i).startTime = startTimeWitnesses.get(segments.get(i).uri);
			Log.i("StreamHandler.updateSegmentTimes", "Sentinal Segment=" + segments.get(i));
			setSegments[i] = 1;
		}
		
		if (segments.size() > 1)
		{
			// Then fill in any unknowns scanning forward....
			for (int i = 1; i < segments.size(); ++i)
			{
				if (setSegments[i] == 1)
					continue;
				
				// Skip unknowns
				if (setSegments[i-1] == 0)
					continue;
				
				segments.get(i).startTime = segments.get(i-1).startTime + segments.get(i-1).duration;
				setSegments[i] = 1;
			}
			
			// And scanning back...
			for (int i = segments.size() - 2; i >= 0; --i)
			{
				// Skip myself if I'm set
				if (setSegments[i] == 1)
					continue;
				
				// Skip unknowns.
				if (setSegments[i + 1] == 0)
					continue;
				
				segments.get(i).startTime = segments.get(i+1).startTime - segments.get(i).duration;
				setSegments[i] = 1;
			}
		}
		
//		Log.i("StreamHandler.updateSegmentTimes", "Segment Time Update Complete");
//		for (int i = 0; i < segments.size(); ++i)
//		{
//			ManifestSegment seg = segments.get(i);
//			Log.i("StreamHandler.updateSegmentTimes", "#" + i + " id=" + seg.id + " start=" + seg.startTime + " end=" + (seg.startTime + seg.duration));
//		}
		Log.i("StreamHandler.updateSegmentTimes", "Reconstructed manifest time with knowledge=" + checkAnySegmentKnowledge(segments) + " firstTime=" + (segments.size() > 1 ? segments.get(0).startTime : -1) + " lastTime=" + (segments.size() > 1 ? segments.get(segments.size() -1).startTime : -1));
		return segments;
	}
	
	public boolean checkAnySegmentKnowledge(Vector<ManifestSegment> segments)
	{
		// Find matches
		for (int i = 0; i < segments.size(); ++i)
		{
			if (startTimeWitnesses.containsKey(segments.get(i).uri))
				return true;
		}
		return false;
	}
	
	public ManifestSegment getSegmentBySequence(Vector<ManifestSegment> segments, int id)
	{
		// Find matches
		for (int i = 0; i < segments.size(); ++i)
		{
			ManifestSegment seg = segments.get(i);
			if (seg.id == id)
				return seg;
		}
		return null;
	}
	
	public double getSegmentStartTimeBySequence(Vector<ManifestSegment> segments, int id)
	{
		// Find matches
		for (int i = 0; i < segments.size(); ++i)
		{
			ManifestSegment seg = segments.get(i);
			if (seg.id == id)
				return seg.startTime;
		}
		return -1;
	}
	
	public ManifestSegment getSegmentContainingTime(Vector<ManifestSegment> segments, double time)
	{
		for (int i = 0; i < segments.size(); ++i)
		{
			ManifestSegment seg = segments.get(i);
			ManifestSegment segToReturn = null;
			if (time < seg.startTime && i == 0) // If we're the first one in the list, return us, even if time is less than our initial start time
				segToReturn = seg;
			else if (time < seg.startTime) // If we're not the first one in the list, if it's less than our start time, return the previous one
				segToReturn = segments.get(i - 1);
			else if (i == segments.size() - 1 && time >= seg.startTime && time < seg.startTime + seg.duration) // If we're the last one in the list, return us if the time is within our duration
				segToReturn = seg;
			
			if (segToReturn != null)
			{
				Log.i("StreamHandler.getSegmentContainingTime", "For Time " + time + ", returning segment: " + segToReturn);
				return segToReturn;
			}
		}
		
		// No match, dump to aid debug
		Log.i("StreamHandler.getSegmentContainingTime", "Looking for time: " + time);
		for (int i = 0; i < segments.size(); ++i)
		{
			ManifestSegment seg = segments.get(i);
			Log.i("StreamHandler.getSegmentContainingTime", "#" + i + " id=" + seg.id + " start=" + seg.startTime + " end=" + (seg.startTime + seg.duration) + " Segment=" + seg);
		}
		
		return null;
	}
	
	public int getSegmentSequenceContainingTime(Vector<ManifestSegment> segments, double time)
	{
		ManifestSegment seg = getSegmentContainingTime(segments, time);
		if (seg != null)
			return seg.id;
		return -1;
	}

	public boolean waitingForAudioReload = false;

	

	public int getAltAudioDefaultIndex()
	{
		for (int i = 0; i < baseManifest.playLists.size(); ++i)
		{
			ManifestPlaylist mp = baseManifest.playLists.get(i);
			if (mp.isDefault)
			{
				return i;
			}
		}
		return -1;
	}

	public int getAltAudioCurrentIndex()
	{
		return altAudioIndex;
	}

	public String[] getAltAudioLanguages()
	{
		if (baseManifest.playLists.size() == 0) return null;
		String[] languages = new String[baseManifest.playLists.size()];
		for (int i = 0; i < baseManifest.playLists.size(); ++i)
		{
			String lang = baseManifest.playLists.get(i).language;
			if (lang.length() == 0 )
				lang = baseManifest.playLists.get(i).name;
			languages[i] = lang;
		}
		return languages;
	}

	public List<String> getAltAudioLanguageList()
	{
		if (baseManifest.playLists.size() == 0) return null;
		List<String> languages = new ArrayList<String>();
		for (int i = 0; i < baseManifest.playLists.size(); ++i)
		{
			String lang = baseManifest.playLists.get(i).language;
			if (lang.length() == 0 )
				lang = baseManifest.playLists.get(i).name;

			languages.add(lang);
		}
		return languages;
	}


	public boolean hasAltAudio()
	{
		return baseManifest.playLists.size() > 0;
	}

	public List<QualityTrack> getQualityTrackList()
	{
		List<QualityTrack> tracks = new ArrayList<QualityTrack>();
		if (baseManifest.streams.size() > 0)
		{
			for (int i = 0; i < baseManifest.streams.size(); ++i)
			{
				ManifestStream s = baseManifest.streams.get(i);

				QualityTrack t = new QualityTrack();
				t.bitrate = s.bandwidth;
				t.width = s.width;
				t.height = s.height;
				t.trackId = i +  "|" + s.programId + "|" + s.uri;
				t.type = TrackType.VIDEO;
				tracks.add(t);

			}
		}
		else
		{
			QualityTrack t = new QualityTrack();
			t.trackId = "0|0|" + baseManifest.fullUrl;
			t.type = TrackType.VIDEO;
			tracks.add(t);
		}
		return tracks;
	}

	public void initialize(SubtitleHandler subtitleHandler)
	{
		ManifestParser man = getManifestForQuality(lastQuality);

		if (man.needsReloading())
		{
			reloader.setVideoSource(this, this);
			reloader.setAltAudioSource(this,  this);
			reloader.setSubtitleSource(subtitleHandler, subtitleHandler);
			reloader.start((long) man.segments.get(man.segments.size() - 1).duration * 1000 / 2);
		}
	}

	public void stopReloads()
	{
		reloader.stop();
	}

	public void close()
	{
		closed = true;
		stopReloads();
		stopListeningToBestEffortDownloads();
	}

	@Override
	public ManifestParser getVideoManifestToReload()
	{
		Log.i("StreamHandler.getVideoManifestToReload", "reloadingQuality=" + reloadingQuality + " mIsRecovering=" + mIsRecovering + " lastQuality=" + lastQuality);
		if (mIsRecovering)
		{
			attemptRecovery();
		}
		else if (reloadingQuality == -1)
		{
			// Need to make sure that the quality is set on the backup stream manifest before we try to reload it
			primaryStream.backupStream.manifest.quality = primaryStream.manifest.quality;
			return primaryStream.backupStream.manifest;
		}
		else
			return getManifestForQuality(lastQuality);
		return null;
	}

	private ManifestParser getAltAudioManifestForLanguage(int language)
	{
		if (baseManifest == null || language == -1) return null;
		if (baseManifest.playLists.size() < 1 || baseManifest.playLists.get(0).manifest == null) return null;
		else if (language < baseManifest.playLists.size())
			return baseManifest.playLists.get(language).manifest;
		return null;
	}

	@Override
	public ManifestParser getAltAudioManifestToReload()
	{
		Log.i("StreamHandler.getAltAudioManifestToReload", "hasAltAudio = " + hasAltAudio());
		if (hasAltAudio())
		{
			int index = altAudioIndex;
			if (reloadingAltAudioIndex  >= 0 && reloadingAltAudioIndex != altAudioIndex)
			{
				// We'll be reloading something different from what we have
				index = reloadingAltAudioIndex;
			}
			else
			{
				reloadingAltAudioIndex = altAudioIndex;
			}
			Log.i("StreamHandler.getAltAudioManifestToReload", "reloadingAltAudioIndex=" + reloadingAltAudioIndex + " index=" + index);
			if (index < baseManifest.playLists.size() && index >= 0)
				return baseManifest.playLists.get(index).manifest;
		}
		return null;
	}

	@Override
	public ManifestParser getSubtitleManifestToReload()
	{
		// Return null, as the StreamHandler doesn't handle subtitles
		return null;
	}

	private void reload(int quality)
	{
		reloadingQuality = quality;
		reloader.reload();
	}
	
	@Override
	public void onReloadComplete(ManifestParser parser)
	{
		Log.i("StreamHandler.onReloadComplete", "onReloadComplete quality/reloading: " + lastQuality + "/" + reloadingQuality + " | type=" + parser.type);

		if (closed)
		{
			Log.i("StreamHandler.onReloadComplete", "StreamHandler is closed. Returning.");
			return;
		}
		
		if (parser == null)
		{
			Log.e("StreamHandler.onReloadComplete", "Reload completed, but we don't have a manifest! It's null!");
			return;
		}
		
		ManifestParser currentManifest = parser;
		ManifestParser newManifest = parser.getReloadChild();
		
		if (newManifest == null)
		{
			Log.e("StreamHandler.onReloadComplete", "Reload completed, but we don't have a child manifest! It's null!");
		}
		
		if (newManifest.segments.size() == 0)
		{
			Log.e("StreamHandler.onReloadComplete", "Reload completed, but the manifest has zero segments");
		}
		
		boolean isAudio = newManifest.type.equals(ManifestParser.AUDIO);
		boolean isBackupStreamSwitch = (reloadingQuality == -1 && !isAudio);
		
		// Handle backup source swaps
		if (reloadingQuality == -1 && !isAudio)
		{
			for (int i = 0; i <= baseManifest.streams.size(); ++i)
			{
				if (i == baseManifest.streams.size())
				{
					Log.i("StreamHandler.onReloadComplete", "Backup Replacement Failed: Stream with URI " + primaryStream.uri + " not found");
					break;
				}
				
				if (baseManifest.streams.get(i) == primaryStream && baseManifest.streams.get(i).backupStream != null)
				{
					reloadingQuality = i;
					baseManifest.streams.set(i, baseManifest.streams.get(i).backupStream);
					break;
				}
			}
		}
		

		
		// Need to differentiate between audio and video streams for determining whether we need to do a timestamp request
		int rid = reloadingQuality;
		int lid = lastQuality;
		if (isAudio)
		{
			rid = reloadingAltAudioIndex;
			lid = altAudioIndex;
		}
		
		updateSegmentTimes(newManifest.segments);
		
		// Update our manifest for this quality level
		if (newManifest != null && isAudio)
		{
			Log.i("StreamHandler.onReloadComplete", "Setting alt audio to " + rid);
			if (baseManifest.playLists.size() > 0)
			{
				baseManifest.playLists.get(newManifest.quality).manifest = newManifest;
				if (newManifest.quality == lid)
					altAudioManifest = newManifest;
			}
			else
			{
				// uh - we shouldn't even be reloading this? what happened
				Log.e("StreamHandler.onReloadComplete", "Reloaded an alt audio manifest (supposedly), but the base manifest has no alt audio streams!!!");
			}
			
			// We don't restart the reloader based on alt audio streams
			// We don't update duration, either
			
		}
		else
		{
			Log.i("StreamHandler.onReloadComplete", "Setting quality to " + newManifest.quality);
			newManifest.logSegments();
			if (baseManifest.streams.size() > 0)
			{
				baseManifest.streams.get(newManifest.quality).manifest = newManifest;
			}
			else
			{
				// The reason this is okay is because if we don't have any submanifests, we don't have alt audio
				baseManifest = newManifest;
			}
			
			if (isBackupStreamSwitch)
			{
				Log.i("StreamHandler.onReloadComplete", "Restoring reloading quality to normal");
				HLSPlayerViewController.currentController.seekToCurrentPosition();
				reloadingQuality = lid; // restoring our quality since we're "done"
			}

			reloader.start();
			HLSPlayerViewController.currentController.postDurationChanged();
		}
	}

	private int mFailureCount = 0;

	public void altAudioReloadFailed(ManifestParser parser)
	{
		reloader.start((long)(getManifestForQuality(lastQuality).targetDuration * 1000  / 2));
	}

	@Override
	public void onReloadFailed(ManifestParser parser)
	{
		if (closed) return;

		if (parser != null && parser.type == ManifestParser.AUDIO)
		{
			altAudioReloadFailed(parser);
			return;
		}

		Log.i("StreamHandler.onReloadFailed", "onReloadFailed Manifest reload failed: " + parser.fullUrl);

		mIsRecovering = true;
		lastBadManifestUri = parser.fullUrl;

		if (mFailureCount == 0)
		{
			reloader.start((long)(getManifestForQuality(lastQuality).targetDuration * 1000  / 2));
			++mFailureCount;
			return;
		}
		else
		{
			mFailureCount = 0;
		}

		attemptRecovery();
	}

	private void attemptRecovery()
	{
		if (closed) return;
		Log.i("StreamHandler.attemptRecovery", "Attempting Recovery");
		mIsRecovering = false;

		if (!mErrorSurrenderTimer.isRunning())
			mErrorSurrenderTimer.start();

		// Shut everything down if we have had too many errors in a row
		if (mErrorSurrenderTimer.elapsedTime() >= recognizeBadStreamTime)
		{

			return;
		}

		if (!swapBackupStream(getStreamForQuality(lastQuality)))
			reload(lastQuality);

	}

	private boolean swapBackupStream(ManifestStream stream)
	{
		if (stream == null) return false;

		if (stream.backupStream != null)
		{
			Log.i("StreamHandler.swapBackupStream", "Swapping to backupstream: " + stream.backupStream.uri);
			primaryStream = stream;
			reload(-1);
			return true;
		}


		return false;
	}

	public boolean backupStreamExists()
	{
		ManifestStream curStream = getStreamForQuality(lastQuality);
		return (curStream != null && curStream.backupStream != null);
	}

	public boolean isStalled()
	{
		return stalled;
	}

	private int getWorkingQuality(int requestedQuality)
	{
		// We're basically always returning the last quality - if you want to switch qualities, now, you
		// must use the initiateQualityChange method.
		if (requestedQuality != lastQuality)
		{
			Log.w("StreamHandler.getWorkingQuality", "Requested quality doesn't match working quality. Please use initiateQualityChange method for switching qualities.");
		}
		return lastQuality;
	}
	
	private ManifestSegment getAltAudioSegmentForIndex(ManifestParser audioManifest, int index, int language)
	{
		if (audioManifest == null || audioManifest.segments == null || audioManifest.segments.size() == 0) return null;
		
		if (index < 0) index = 0;
		
		if (index >= 0 && index < audioManifest.segments.size())
		{
			if (audioManifest != null)
			{
				ManifestSegment seg = audioManifest.segments.get(index);
				seg.initializeCrypto(getKeyForSequence(seg.id, audioManifest.keys));
				return seg;
			}
		}
		return null;
	}

	private ManifestSegment getSegmentForIndex(ManifestParser curManifest, int index, int quality)
	{
		if (curManifest == null || curManifest.segments == null || curManifest.segments.size() == 0) return null;
		
		if (index < 0)
		{
			Log.w("StreamHandler.getSegmentForIndex", "Index of (" + index + ") requested. Resetting index to 0");
			index = 0;
		}
		
		ManifestSegment seg = curManifest.segments.get(index);

		seg.quality = quality;
		
		attachAltAudio(seg, altAudioManifest);
		
		seg.initializeCrypto(getKeyForSequence(seg.id, curManifest.keys));
		return seg;
	}
	


	/*
	 * testSegmentMatchByTime
	 * 
	 * Determines if an altAudio Segment (altAudioSeg), matches the the video segment
	 * by time
	 * 
	 */
	private boolean testSegmentMatchByTime(ManifestSegment seg, ManifestSegment altAudioSeg)
	{
		if (altAudioSeg == null) return false;
		
		if (seg.startTime < altAudioSeg.startTime)
		{
			if (seg.endTime() < altAudioSeg.endTime())
			{
				if (altAudioSeg.startTime - seg.startTime < seg.duration / 2)
				{
					// We likely have a match
					return true;
				}
			}
			
		}
		else if (seg.startTime > altAudioSeg.startTime)
		{
			if (seg.endTime() > altAudioSeg.endTime())
			{
				if (seg.startTime - altAudioSeg.startTime < seg.duration / 2)
				{
					// We likely have a match
					return true;
				}
			}
		}
		else if (seg.startTime == altAudioSeg.startTime)
		{
			return true;
		}
		return false;
	}
	
	private boolean attachAltAudio(ManifestSegment segment, ManifestParser audioManifest)
	{
		return attachAltAudio(segment, segment.startTime, audioManifest);
	}
	
	private boolean attachAltAudio(ManifestSegment segment, double time, ManifestParser audioManifest)
	{
		if (audioManifest != null)
		{
			updateSegmentTimes(audioManifest.segments);
			ManifestSegment audioSegment = getSegmentContainingTime(audioManifest.segments, time);
			if (audioSegment != audioManifest.segments.get(0))
			{
				if (audioSegment != null &&  !testSegmentMatchByTime(segment, audioSegment))
				{
					if (audioSegment.startTime < segment.startTime)
					{
						 ManifestSegment tempAudioSeg = getSegmentBySequence(audioManifest.segments, audioSegment.id + 1);
						 if (tempAudioSeg != null && testSegmentMatchByTime(segment, tempAudioSeg))
						 {
							 audioSegment = tempAudioSeg;
						 }
					}
					else if (audioSegment.startTime > segment.startTime)
					{
						 ManifestSegment tempAudioSeg = getSegmentBySequence(audioManifest.segments, audioSegment.id - 1);
						 if (tempAudioSeg != null && testSegmentMatchByTime(segment, tempAudioSeg))
						 {
							 audioSegment = tempAudioSeg;
						 }
						
					}
				}
			}
			
			Log.i("StreamHandler.attachAltAudio", "getting alt audio segment to match segment @ " + time + " : " + segment);
			Log.i("StreamHandler.attachAltAudio", "altAudioManifest.getSegmentContainingTime returned=" + audioSegment);
			if (audioSegment != null)
			{
				segment.altAudioSegment = audioSegment;
				segment.altAudioSegment.altAudioIndex = audioManifest.quality;
				segment.altAudioSegment.initializeCrypto(getKeyForSequence(segment.altAudioSegment.id, audioManifest.keys));
			}
			else
			{
				stalled = true;
				return false;
			}
		}
		return true; // we didn't fail (attaching null isn't a failure)
	}


	public ManifestSegment getFileForTime(double time, int quality)
	{
		Log.i("StreamHandler.getFileForTime", "time: " + time + " quality: " + quality);
		quality = getWorkingQuality(quality);

		double accum = 0.0;
		ManifestParser curManifest = getManifestForQuality(quality);
		Vector<ManifestSegment> segments = updateSegmentTimes(curManifest.segments);
				
		if (!checkAnySegmentKnowledge(segments) && _bestEffortRequests.size() == 0)
		{
			// We may also need to establish a timebase
			Log.i("StreamHandler.getFileForTime", "Seeking without timebase; initiating reuest.");
			initiateBestEffortRequest(Integer.MAX_VALUE, quality, false);
			
			while (!checkAnySegmentKnowledge(segments) && _bestEffortRequests.size() > 0)
			{
				try
				{
					Thread.sleep(50);
				}
				catch (Exception e)
				{
					
				}
			}
		}

		if (time == USE_DEFAULT_START && !streamEnds())
		{
			if (SKIP_TO_END_OF_LIVE)
			{
				int idx = Math.max(segments.size() - EDGE_BUFFER_SEGMENT_COUNT, 0);
				lastSequence = segments.get(idx).id;
				ManifestSegment seg = segments.get(idx);
				seg.quality = quality;
				attachAltAudio(seg, seg.startTime, altAudioManifest);
				seg.initializeCrypto(getKeyForSequence(seg.id, getManifestForQuality(lastQuality).keys)); // TODO: I don't think this is right. I think it will end up with a bug when working on non-current quality segments

				return seg;
			}
			else
			{
				time = 0;
			}
		}
		else if (time == USE_DEFAULT_START)
		{
			time = 0;
		}


		if (time < segments.get(0).startTime)
		{
			time = segments.get(0).startTime;
			Log.i("StreamHandler.getFileForTime", "SequenceSkip - time: " + time + " playlistStartTime: " + segments.get(0).startTime);
		}
		
		ManifestSegment segment = getSegmentContainingTime(segments, time);
		int seq = getSegmentSequenceContainingTime(segments, time);
		
		if (seq == -1 && segments.size() >= 2)
		{
			Log.i("StreamHandler.GetFileForTime", "Got out of bound timestamp for time " + time + ". Trying to recover...");
			
			ManifestSegment lastSeg = segments.get(segments.size() - 1);
			if (segments.size() >= EDGE_BUFFER_SEGMENT_COUNT + 1)
				lastSeg = segments.get(segments.size() - EDGE_BUFFER_SEGMENT_COUNT);
			
			if (time < segments.get(0).startTime)
			{
				seq = segments.get(0).id;
				Log.i("StreamHandler.getFileForTime", "Fell off oldest segment, going to end #" + seq);
			}
			else if (time > lastSeg.startTime)
			{
				seq = lastSeg.id;
				Log.i("StreamHandler.getFileForTime", "Fell off newest segment, going to end #" + seq);
			}
		}
		
		if (seq != -1)
		{
			ManifestSegment curSegment = getSegmentBySequence(segments, seq);
			
			curSegment.quality = quality;
			attachAltAudio(curSegment, time, altAudioManifest);
			curSegment.initializeCrypto(getKeyForSequence(curSegment.id, getManifestForQuality(lastQuality).keys)); // TODO: I don't think this is right. I think it will end up with a bug when working on non-current quality segments
			lastSequence = seq;
			
			return curSegment;
		}
		else
		{
			
		}


		if (!getManifestForQuality(quality).streamEnds)
		{
			Log.i("StreamHandler.getFileForTime", "Couldn't find a segment for the index. Stalling.");
			stalled = true;
		}

		return null;
	}

	public boolean streamEnds()
	{
		int quality = getWorkingQuality(lastQuality);
		return getManifestForQuality(quality).streamEnds;
	}

	public ManifestSegment getNextFile(int quality)
	{
		Log.i("StreamHandler.getNextFile", "Requesting Segment For Quality: " + quality + " lastQuality=" + lastQuality + " lastSequence=" + lastSequence);
		int requestedQuality = quality;
		quality = getWorkingQuality(quality);

		ManifestParser parser = getManifestForQuality(quality);
		Vector<ManifestSegment> segments = getSegmentsForQuality( quality );
		

		// Checking this here, as there's no need to do all the segment knowledge work if there isn't anything new
		if (segments.size() > 0 && lastSequence + 1 > (segments.get(segments.size() -1).id))
		{
			// There's nothing more to return.
			Log.i("StreamHandler.getNextFile", "No new segments to play!!! Looking for sequence id " + (lastSequence + 1));
			
			if (!streamEnds())
				stalled = true;
			return null;
		}
		
		if (!checkAnySegmentKnowledge(segments))
		{
			Log.i("StreamHandler.getNextFile", "Lack timebase for manifest " + parser.instance());
			
			if (_bestEffortRequests.size() == 0)
			{
				initiateBestEffortRequest(Integer.MAX_VALUE, quality, false);
				stalled = true;
				return null;
			}
			else
			{
				stalled = true;
				return null;
			}
		}
		
		// Recalculate the timebase
		updateSegmentTimes(segments);
		
		
		// Advance sequence number
		++lastSequence;
		Log.i("StreamHandler.getNextFile", "lastSequence = " + lastSequence);


		if (segments.size() > 0 && lastSequence < segments.get(0).id)
		{
			Log.i("StreamHandler.getNextFile", "Reseting too low sequence " + lastSequence + " to " + segments.get(0).id);
			lastSequence = segments.get(0).id;
		}
		
		
		ManifestSegment curSegment = getSegmentBySequence(segments, lastSequence);
		
		
		if (curSegment != null)
		{
			curSegment.quality = quality;
			if (attachAltAudio(curSegment, curSegment.startTime, altAudioManifest))
			{
				curSegment.initializeCrypto(getKeyForSequence(curSegment.id, getManifestForQuality(lastQuality).keys)); // TODO: I don't think this is right. I think it will end up with a bug when working on non-current quality segments
	
				stalled = false;
				return curSegment;
			}
		}

		Log.i("StreamHandler.getNextFile", "---- No New Segments Found. Last sequence = " + lastSequence + " | List Size = " + segments.size());

		if (!streamEnds())
		{
			Log.i("StreamHandler.getNextFile()", "No new segments to play!!! Stalling! Last sequence = " + lastSequence + " | List Size = " + segments.size());
			stalled = true;
		}

		return null;
	}
	
	public Vector<ManifestEncryptionKey> getAltAudioEncryptionKeys()
	{
		ManifestParser aaManifest = getAltAudioManifestForLanguage(altAudioIndex);
		if (aaManifest != null) return aaManifest.keys;
		return null;
	}

	public ManifestEncryptionKey getKeyForSequence( int seq, Vector<ManifestEncryptionKey> keys)
	{
		for ( ManifestEncryptionKey key : keys)
		{
			if (key.startSegmentId <= seq && key.endSegmentId >= seq)
				return key;
		}
		
		return null;
	}
	
	// Returns duration in ms
	public int getDuration()
	{
		double accum = 0.0f;

		if (baseManifest == null) return -1;

		Vector<ManifestSegment> segments = getSegmentsForQuality( lastQuality );
		updateSegmentTimes(segments);
		int i = segments.size() - 1;

		accum = (segments.get(i).startTime + segments.get(i).duration) - lastKnownPlaylistStartTime;

		return (int) (accum * 1000);

	}

	public int getQualityLevels()
	{
		if (baseManifest == null) return 0;
		if (baseManifest.streams.size() > 0 ) return baseManifest.streams.size();
		return 1;

	}

	private Vector<ManifestSegment> getSegmentsForQuality(int quality)
	{
		if ( baseManifest == null) return new Vector<ManifestSegment>();
		if (baseManifest.streams.size() < 1 || baseManifest.streams.get(0) == null) return baseManifest.segments;
		else if ( quality >= baseManifest.streams.size() ) return baseManifest.streams.get(0).manifest.segments;
		else return baseManifest.streams.get(quality).manifest.segments;
	}
	
	public ManifestParser getManifestForQuality(int quality)
	{
		if (baseManifest == null) return new ManifestParser();
		else if (baseManifest.streams.size() < 1 || baseManifest.streams.get(0).manifest == null) return baseManifest;
		else if ( quality >= baseManifest.streams.size() ) return baseManifest.streams.get(0).manifest;
		return baseManifest.streams.get(quality).manifest;
	}

	public ManifestStream getStreamForQuality(int quality)
	{
		if (baseManifest == null || baseManifest.streams.size() < 1 || quality >= baseManifest.streams.size()) return null;
		return baseManifest.streams.get(quality);
	}
	
	

	@Override
	public void onSegmentCompleted(String [] uri) {
		HLSSegmentCache.cancelCacheEvent(uri[0]);
		
		for (String url : uri)
		{
			ByteArray ba = new ByteArray(HLSSegmentCache.getByteArray(url));
			
			long pts = getPTS(ba, url);
			if (pts != -1)
			{
				double startTime = (double)((double)pts / (double)90000);
				startTimeWitnesses.put(url, startTime);
			}
		}
		
		


	}

	@Override
	public void onSegmentFailed(String uri, int errorCode) {
		HLSSegmentCache.cancelCacheEvent(uri);
		Log.i("StreamHandler.onSegmentFailed", "Failed Download - attempting recovery: " + errorCode + " " + uri);
		attemptRecovery();

	}
	

	private void initiateBestEffortRequest(int nextFragmentId, int quality, boolean wait)
	{
		initiateBestEffortRequest(nextFragmentId, quality, null, BestEffortRequest.TYPE_VIDEO, wait);
	}
	
	private void initiateBestEffortRequest(int nextFragmentId, int quality, ManifestParser newMan, int type, boolean wait)
	{
		/// if we had a pending BEF download, invalidate it
		stopListeningToBestEffortDownloads();
		
		// clean up best effort state
		Vector<ManifestSegment> segments = null;
		
		if (newMan != null)
			segments = newMan.segments;
		
		if (segments == null)
		{
			// Get the URL
			if (type == BestEffortRequest.TYPE_AUDIO)
			{
				newMan = getAltAudioManifestForLanguage(quality);
			}
			else
			{
				newMan = getManifestForQuality(quality);
			}
			if (newMan == null)
			{
				Log.i("StreamHandler.initiateBestEffortRequest", "No manifest found to best effort request quality level " + quality);
				return;
			}
			segments = newMan.segments;
		}
		
		if (segments == null)
		{
			Log.i("StreamHandler.initiateBestEffortRequest", "NO SEGMENTS FOUND, ABORTING initiateBestEffortRequest");
			return;
		}
		
		if (nextFragmentId > segments.size() - EDGE_BUFFER_SEGMENT_COUNT || nextFragmentId == Integer.MAX_VALUE)
		{
			Log.i("StreamHandler.initiateBestEffortRequest", "Capping to end of segment list " + (segments.size() - 1));
			nextFragmentId = segments.size() - EDGE_BUFFER_SEGMENT_COUNT;
		}
		
		ManifestSegment seg = null;
		if (type == BestEffortRequest.TYPE_AUDIO)
		{
			seg = getAltAudioSegmentForIndex(newMan, nextFragmentId, quality);
		}
		else
		{
			seg = getSegmentForIndex(newMan, nextFragmentId, quality);
		}
		_bestEffortRequests.add(new BestEffortRequest(seg, type));
		
		HLSSegmentCache.precache(seg, wait, bestEffortListener, HLSPlayerViewController.getHTTPResponseThreadHandler());
	}
	
	// Use this to clear ALL besteffort downloads
	private void stopListeningToBestEffortDownloads()
	{
		synchronized (_bestEffortRequests)
		{
			// we've got one going already
			while (_bestEffortRequests.size() > 0)
			{
				ManifestSegment seg = _bestEffortRequests.get(0).segment;
				HLSSegmentCache.cancelCacheEvent(seg.uri);
				_bestEffortRequests.remove(0);
			}
		}
	}
	
	private void stopListeningToCompletedBestEffortDownloads()
	{
		synchronized (_bestEffortRequests)
		{
			// we've got one going already
			for (int i = _bestEffortRequests.size() - 1; i >= 0; --i)
			{
				if (_bestEffortRequests.get(i).parsed )
				{
					HLSSegmentCache.cancelCacheEvent(_bestEffortRequests.get(i).segment.uri);
					_bestEffortRequests.remove(i);
				}
			}
		}
	}
	

	private final int _bufferCopySize = 0x4000;  
	private long getPTS(ByteArray segmentBytes, String uri)
	{
		M2TSParser tsParser = new M2TSParser();
					
		long pts = -1;
		int offset = 0;
		while (pts == -1 && offset < segmentBytes.length())
		{
			int len = Math.min(_bufferCopySize, segmentBytes.length() - offset);
			tsParser.appendBytes(segmentBytes, offset, len );
			pts = tsParser.pts;
			offset += len;
		}
		
		Log.i("StreamHandler.bestEffortListener.onSegmentCompleted", "Found PTS ( " + pts + " / " + ((long)(((double)pts / (double)90000) * (double)1000 * (double)1000)) + " / " + (double)((double)pts / 90000.0 )+  " )  in first " + offset + " bytes for " + uri);

		return pts;
	}

	private SegmentCachedListener bestEffortListener = new SegmentCachedListener()
	{
		

		
		
		@Override
		public void onSegmentCompleted(String [] uris)
		{
			Log.i("StreamHandler.bestEffortListener.onSegmentCompleted", "Completed for URL[0]: " + uris[0] + " _bestEffortRequests count = " + _bestEffortRequests.size());
			if (_bestEffortRequests.size() == 0) return; // There's nothing to work against
			
			for (String uri : uris)
			{
				synchronized (_bestEffortRequests)
				{
					for(BestEffortRequest v : _bestEffortRequests)
					{
						if (v.segment.uri.equals(uri))
						{
							v.downloadComplete = true;
						}
					}
				}
			}
			
			synchronized (_bestEffortRequests)
			{
				for (BestEffortRequest req : _bestEffortRequests)
				{
					if (!req.downloadComplete) continue;
					
					ByteArray ba = new ByteArray(HLSSegmentCache.getByteArray(req.segment.uri));
					
					long pts = getPTS(ba, req.segment.uri);
					
					if (req.type == BestEffortRequest.TYPE_VIDEO) // check the base - i should be 0
					{
						req.segment.startTime = (double)((double)pts / (double)90000);
						startTimeWitnesses.put(req.segment.uri, req.segment.startTime);
						req.parsed = true;
					}
					else if (req.type == BestEffortRequest.TYPE_AUDIO)
					{
						req.segment.startTime = (double)((double)pts / (double)90000);
						startTimeWitnesses.put(req.segment.uri, req.segment.startTime);
						req.parsed = true;
						
					}
					else if (req.type == BestEffortRequest.TYPE_AUDIO_VIDEO && uris.length == 2) // There should be two uris in the finished request
					{
						
						req.segment.startTime = (double)((double)pts / (double)90000);
						startTimeWitnesses.put(req.segment.uri, req.segment.startTime);
						
						// Have to get the PTS for the alt audio separately.
						ByteArray baa = new ByteArray(HLSSegmentCache.getByteArray(req.segment.altAudioSegment.uri));
						pts = getPTS(baa, req.segment.altAudioSegment.uri);
						
						req.segment.altAudioSegment.startTime = (double)((double)pts / (double)90000);
						startTimeWitnesses.put(req.segment.altAudioSegment.uri, req.segment.altAudioSegment.startTime);
						req.parsed = true;
					}
				}
			}
			
			if (mKnowledgePrepHandler != null)
			{
				mKnowledgePrepHandler.knowledgePrefetchComplete();
				mKnowledgePrepHandler = null;
			}
			stopListeningToCompletedBestEffortDownloads();
		}
		
		@Override
		public void onSegmentFailed(String uri, int errorCode)
		{
			// TODO: Not sure what to do here, yet...
			synchronized(_bestEffortRequests)
			{
				for (BestEffortRequest b : _bestEffortRequests)
				{
					if (b.segment.uri.equals(uri))
						b.parsed = true; // complete this!
					if (b.type == BestEffortRequest.TYPE_AUDIO_VIDEO && b.segment.altAudioSegment.uri.equals(uri))
						b.parsed = true; // Complete this, too!!! (failed audio in an audio+video should result in failure of the whole thing
						
				}
			}
			stopListeningToCompletedBestEffortDownloads(); // but this is a start
		}
		
		@Override
		public String toString()
		{
			return "StreamHandler.bestEffortListener@" + this.hashCode();
		}
	};


	/*
	 *  Quality changes
	 */
	
	public void initiateQualityChange(int newQuality)
	{
		Log.i("StreamHandler.initiateQualityChange", "Changing to quality level: " + newQuality);
		ManifestParser man = getManifestForQuality(newQuality);
		if (man != null)
		{
			man.quality = newQuality;
			man.reload(qualityChangeReloadListener);
		}
		else
		{
			Log.w("StreamHandler.initiateQualityChange", "Unable to switch to quality level: " + newQuality + ". Manifest not found.");
		}
	}
	
	int getQualityByManifest(ManifestParser parser)
	{
		if (parser.instance() == baseManifest.instance()) return 0;
		for (int i = 0; i < baseManifest.streams.size(); ++i)
		{
			ManifestStream stream = baseManifest.streams.get(i);
			
			if (stream != null && stream.manifest != null && stream.manifest.instance() == parser.instance())
			{
				return i;
			}
		}
		return 0;
	}
	
	private ManifestParser.ReloadEventListener qualityChangeReloadListener = new ManifestParser.ReloadEventListener()
	{
		
		@Override
		public void onReloadFailed(ManifestParser parser)
		{
			HLSPlayerViewController.currentController.postQualityTrackSwitchingEnd(lastQuality); // Tell 'em we're done, but haven't switched quality
		}
		
		@Override
		public void onReloadComplete(ManifestParser parser)
		{
			int quality = getQualityByManifest(parser);
			
			final ManifestParser currentManifest = parser;
			final ManifestParser newManifest = parser.getReloadChild();
			
			if (currentManifest.quality == lastQuality)
			{
				// We don't need to do anything since this one matches what we're already playing
				HLSPlayerViewController.currentController.postQualityTrackSwitchingEnd(lastQuality); // Tell 'em we're done, but haven't switched quality
				return;
			}
			
			// Make sure we have timebase knowledge			
			if (!checkAnySegmentKnowledge(newManifest.segments)) // I honestly wouldn't expect any, in many cases
			{
				Log.i("StreamHandler.onReloadComplete", "(A) Encountered a live/VOD manifest with no timebase knowledge. Requesting newest segment via best effort path for quality " + reloadingQuality);
				initiateBestEffortRequest(Integer.MAX_VALUE, quality, newManifest, bestEffortTypeFromString( newManifest.type) , false);
			}

			// If we don't have timebase knowledge, we need to wait until we have it.
			// What we can't do is wait in here, as it might block a thread, so we stick
			// it in a new thread!
			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					// We're only going to do so many checks for the new knowledge, so that we don't end up with zombie threads
					// that never complete.
					int checksLeft = 10000 / 100; // 10 seconds / sleep time
					while (!checkAnySegmentKnowledge(newManifest.segments) && checksLeft > 0)
					{
						--checksLeft;
						try
						{
							Thread.sleep(100);
						}
						catch (Exception e)
						{
							
						}
					}
					
					if (checksLeft == 0 && !checkAnySegmentKnowledge(newManifest.segments))
					{
						Log.e("StreamHandler.onReloadComplete", "We did not recieve timebase knowledge in a timely fashion. Giving up on quality change.");
						HLSPlayerViewController.currentController.postQualityTrackSwitchingEnd(lastQuality); // Tell 'em we're done, but haven't switched quality
						return;
					}
					
					updateSegmentTimes(currentManifest.segments);
					updateSegmentTimes(newManifest.segments);
					
					int newQuality = currentManifest.quality;
					
					if (newQuality == lastQuality)
					{
						// what the hell? I guess this is just a reload, maybe - but - why? Maybe they went forward, then back...
						Log.i("StreamHandler.onReloadComplete", "Our new quality matches the old (" + lastQuality + ", so not going to change anything - what's the point?");
						HLSPlayerViewController.currentController.postQualityTrackSwitchingEnd(lastQuality); // Tell 'em we're done, but haven't switched quality
						return;
					}
					else
					{
						// Swap the old manifest for the new one
						// We don't have to pause here because we haven't changed quality, yet
						if (currentManifest.instance() == baseManifest.instance())
						{
							// I'm not sure this should ever happen, as if you are in the base manifest, there aren't any other quality levels.
							baseManifest = newManifest; 
						}
						else
						{
							baseManifest.streams.get(newManifest.quality).manifest = newManifest;
						}
						
						lastQuality = newManifest.quality;						
						HLSPlayerViewController.currentController.seekToCurrentPosition();
						HLSPlayerViewController.currentController.postQualityTrackSwitchingEnd(newManifest.quality);
					}
				}
			} );
			t.start();
		}
	};

	/*
	 * Alt Audio changes
	 * 
	 */
	
	
	private void initiateAudioTrackChange(int trackIndex)
	{
		Log.i("StreamHandler.initiateAudioTrackChange", "Changing to track: " + trackIndex);
		ManifestParser man = getAltAudioManifestForLanguage(trackIndex);
		if (man != null)
		{
			man.quality = trackIndex;
			man.reload(altAudioChangeReloadListener);
		}
	}
	
	private ManifestParser.ReloadEventListener altAudioChangeReloadListener = new ManifestParser.ReloadEventListener()
	{
		@Override
		public void onReloadFailed(ManifestParser parser)
		{
			HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(altAudioIndex);
		}
		
		@Override
		public void onReloadComplete(ManifestParser parser)
		{
			final ManifestParser currentManifest = parser;
			final ManifestParser newManifest = parser.getReloadChild();
			
			if (currentManifest.quality == altAudioIndex)
			{
				// We don't need to do anything since this one matches what we're already playing
				HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(altAudioIndex); // Tell 'em we're done, but haven't switched tracks
				return;
			}
			
			// Make sure we have timebase knowledge
			if (!checkAnySegmentKnowledge(newManifest.segments)) // I honestly wouldn't expect any, in many cases
			{
				Log.i("StreamHandler.altAudioChangeReloadListener.onReloadComplete", "(A) Encountered an altAudio manifest with no timebase knowledge. Requesting newest segment via best effort path for index " + currentManifest.quality);
				initiateBestEffortRequest(Integer.MAX_VALUE, newManifest.quality, newManifest, bestEffortTypeFromString(newManifest.type), false);
			}
			
			// If we don't have timebase knowledge, we need to wait until we have it.
			// What we can't do is wait in here, as it might block a thread, so we stick
			// it in a new thread!
			
			Thread t = new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					// We're only going to do so many checks for the new knowledge, so that we don't end up with zombie threads
					// that never complete.
					int checksLeft = 10000 / 100; // 10 seconds / sleep time
					while (!checkAnySegmentKnowledge(newManifest.segments) && checksLeft > 0)
					{
						--checksLeft;
						try
						{
							Thread.sleep(100);
						}
						catch (Exception e)
						{
							
						}
					}
					
					if (checksLeft == 0 && !checkAnySegmentKnowledge(newManifest.segments))
					{
						Log.e("StreamHandler.altAudioChangeReloadListener.onReloadComplete", "We did not recieve timebase knowledge in a timely fashion. Giving up on audio track change.");
						HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(altAudioIndex); // Tell 'em we're done, but haven't switched quality
						return;
					}
					
					updateSegmentTimes(currentManifest.segments);
					updateSegmentTimes(newManifest.segments);
					
					// Swap the old manifest for the new one
					// We don't have to pause here because we haven't changed quality, yet
					if (currentManifest.instance() == baseManifest.instance())
					{
						Log.e("StreamHandler.altAudioChangeReloadListener.onReloadComplete", "Trying to set an alt audio manifest as the base manifest. This should never happen!!! Ignoring and giving up.");
						HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(altAudioIndex); // Tell 'em we're done, but haven't switched quality
						return;
					}
					else
					{
						baseManifest.playLists.get(newManifest.quality).manifest = newManifest;
						altAudioIndex = newManifest.quality;
						altAudioManifest = newManifest;
					}
					
					HLSPlayerViewController.currentController.seekToCurrentPosition();
					HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(newManifest.quality);

				}
			}, "altAudioSwitchTimeBaseListener");
			t.start();
			
			
		}
	};
	
	
	public void setAltAudioTrack(int index)
	{
		if (index < baseManifest.playLists.size())
		{
			if (index == altAudioIndex)
			{
				// This is our current index. No point in setting it again.
				HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(altAudioIndex);
				return;
			}
			if (index < 0)
			{
				altAudioManifest = null;
				altAudioIndex = -1;
				HLSPlayerViewController.currentController.seekToCurrentPosition();
				HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(altAudioIndex);
			}
			else
			{
				this.initiateAudioTrackChange(index);
			}
		}
		else
		{
			HLSPlayerViewController.currentController.postAudioTrackSwitchingEnd(altAudioIndex);
		}
	}
}
