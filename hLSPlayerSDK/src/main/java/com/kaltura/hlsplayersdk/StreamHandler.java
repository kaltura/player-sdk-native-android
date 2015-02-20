package com.kaltura.hlsplayersdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.os.SystemClock;
import android.util.Log;

import com.kaltura.hlsplayersdk.cache.HLSSegmentCache;
import com.kaltura.hlsplayersdk.cache.SegmentCachedListener;
import com.kaltura.hlsplayersdk.manifest.ManifestEncryptionKey;
import com.kaltura.hlsplayersdk.manifest.ManifestParser;
import com.kaltura.hlsplayersdk.manifest.ManifestPlaylist;
import com.kaltura.hlsplayersdk.manifest.ManifestSegment;
import com.kaltura.hlsplayersdk.manifest.ManifestStream;
import com.kaltura.hlsplayersdk.types.TrackType;


// This is the confusingly named "HLSIndexHandler" from the flash HLSPlugin
// I'll change it, if anyone really hates the new name. It just makes more sense to me.
public class StreamHandler implements ManifestParser.ReloadEventListener, SegmentCachedListener {
	
	private static final boolean SKIP_TO_END_OF_LIVE = true;
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
	
	public static final int USE_DEFAULT_START = -999;
	
	public int lastSegmentIndex = 0;
	public int altAudioIndex = -1;
	public double lastKnownPlaylistStartTime = 0.0;
	public int lastQuality = 0;
	public int targetQuality = 0;
	public ManifestParser altAudioManifest = null;
	public ManifestParser manifest = null;
	public ManifestParser reloadingManifest = null;
	public int reloadingQuality = 0;
	public String baseUrl = null;
	public String lastBadManifestUri = "";
	
	private final long recognizeBadStreamTime = 20000;
	
	private ErrorTimer mErrorSurrenderTimer = new ErrorTimer();
	private boolean mIsRecovering = false;
	
	private boolean reloadingFromBackup = false;
	private ManifestStream primaryStream = null;
	private Timer reloadTimer = null;
	private int sequenceSkips = 0;
	private boolean stalled = false;
	private boolean closed = false;
	private HashMap<String, Integer> badManifestMap = new HashMap<String, Integer>();
	private static final int maxFailedManifestTries = 3; // The number of retries a manifest may occur before we give up on it and remove it from our manifest list.
	private static final int isTooFarBehind = 5; // How far behind a stream can be before we log a message warning of significant delays
	
	private long mTimerDelay = 10000;
	
	
	public StreamHandler(ManifestParser parser)
	{
		manifest = parser;
		for (int i = 0; i < manifest.playLists.size(); ++i)
		{
			ManifestPlaylist mp = manifest.playLists.get(i);
			if (mp.isDefault)
			{
				altAudioManifest = mp.manifest;
				altAudioIndex = i;
				break;
			}
		}
	}
	
	public boolean setAltAudioTrack(int index)
	{
		if (index < manifest.playLists.size())
		{
			if (index < 0)
			{
				altAudioManifest = null;
				altAudioIndex = -1;
			}
			else 
			{
				altAudioIndex = index;
				altAudioManifest = manifest.playLists.get(altAudioIndex).manifest;
			}
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public int getAltAudioDefaultIndex()
	{
		for (int i = 0; i < manifest.playLists.size(); ++i)
		{
			ManifestPlaylist mp = manifest.playLists.get(i);
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
		if (manifest.playLists.size() == 0) return null;
		String[] languages = new String[manifest.playLists.size()];
		for (int i = 0; i < manifest.playLists.size(); ++i)
		{
			String lang = manifest.playLists.get(i).language;
			if (lang.length() == 0 )
				lang = manifest.playLists.get(i).name;
			languages[i] = lang;
		}
		return languages;
	}
	
	public List<String> getAltAudioLanguageList()
	{
		if (manifest.playLists.size() == 0) return null;
		List<String> languages = new ArrayList<String>();
		for (int i = 0; i < manifest.playLists.size(); ++i)
		{
			String lang = manifest.playLists.get(i).language;
			if (lang.length() == 0 )
				lang = manifest.playLists.get(i).name;

			languages.add(lang);
		}
		return languages;
	}
	
	
	public boolean hasAltAudio()
	{
		return manifest.playLists.size() > 0;
	}
	
	public List<QualityTrack> getQualityTrackList()
	{
		List<QualityTrack> tracks = new ArrayList<QualityTrack>();
		if (manifest.streams.size() > 0)
		{
			for (int i = 0; i < manifest.streams.size(); ++i)
			{
				ManifestStream s = manifest.streams.get(i);

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
			t.trackId = "0|0|" + manifest.fullUrl;
			t.type = TrackType.VIDEO;
			tracks.add(t);	
		}
		return tracks;
	}
	
	public void initialize()
	{
		ManifestParser man = getManifestForQuality(lastQuality);
		if (man != null && !man.streamEnds && man.segments.size() > 0)
		{
			mTimerDelay = (long) man.segments.get(man.segments.size() - 1).duration * 1000 / 2;
			startReloadTimer();
		}
	}
	
	public void stopReloads()
	{
		if (reloadingManifest != null)
		{
			reloadingManifest.setReloadEventListener(null);
			reloadingManifest = null;
		}
		killTimer();		
	}
	
	public void close()
	{
		closed = true;
		stopReloads();
	}
	
	private void reload(int quality)
	{
		reload(quality, null);
	}
	
	private void killTimer()
	{
		if (reloadTimer != null)
		{
			reloadTimer.cancel();
			reloadTimer = null;
		}
	}
	
	private void reload(int quality, ManifestParser manifest )
	{
		if (closed) return;
		killTimer(); // In case the timer is active - don't want to do another reload in the middle of it
		
		
		reloadingQuality = quality;
		
		ManifestParser manToReload = manifest != null ? manifest : getManifestForQuality(reloadingQuality);
		reloadingManifest = new ManifestParser();
		reloadingManifest.type = manToReload.type;
		reloadingManifest.setReloadEventListener(this);
		reloadingManifest.reload(manToReload);
		
	}
	
	private void startReloadTimer()
	{
		if (closed) return;
		killTimer(); 
		
		reloadTimer = new Timer();
		reloadTimer.schedule(new TimerTask()
		{
			public void run()
			{
				Log.i("StreamHandler.reloadTimerComplete.run", "Reload Timer Complete!");
				if (mIsRecovering)
				{
					attemptRecovery();
				}
				else if (targetQuality != lastQuality)
					reload(targetQuality);
				else
					reload(lastQuality);
			}
			
		}, mTimerDelay);
	}
	
	@Override
	public void onReloadComplete(ManifestParser parser) {
		Log.i("StreamHandler.onReloadComplete", "onReloadComplete last/reload/target: " + lastQuality + "/" + reloadingQuality + "/" + targetQuality);
		if (closed)
		{
			Log.i("StreamHandler.onReloadComplete", "StreamHandler is closed. Returning.");
			return;
		}
		ManifestParser newManifest = parser;
		mFailureCount = 0; // reset the failure count since we had a success
		if (newManifest == null || newManifest.segments.size() == 0)
		{
			Log.e("StreamHandler.onReloadComplete", "newManifest = " + newManifest + " newManifest.segements.size == " + newManifest.segments.size());
		}
		if (newManifest != null && newManifest.segments.size() > 0)
		{
			// Set the timer delay to the most likely possible delay
			mTimerDelay = (long)(newManifest.segments.get(newManifest.segments.size() - 1).duration * 1000);
			
			// remove the reload completed listener since this might become the new manifest
			newManifest.setReloadEventListener(null);
			
			ManifestParser currentManifest = reloadingQuality != -1 ? getManifestForQuality(reloadingQuality) : null;
			
			long timerOnErrorDelay = (long)(newManifest.targetDuration * 1000  / 2);
			
			// If we're not switching quality
			if (reloadingQuality == lastQuality)
			{				
				if (newManifest.mediaSequence > currentManifest.mediaSequence)
				{
					updateManifestSegments(newManifest, reloadingQuality);
				}
				else if (newManifest.mediaSequence == currentManifest.mediaSequence && newManifest.segments.size() != currentManifest.segments.size())
				{
					updateManifestSegments(newManifest, reloadingQuality);
				}
				else
				{
					// the media sequence is earlier than the one we currently have, which isn't
					// allowed by the spec, or there are no changes. So do nothing, but check again as quickly as allowed
					mTimerDelay = timerOnErrorDelay;
				}
			}
			else if (reloadingQuality == targetQuality || reloadingQuality == -1)
			{
				if (!updateManifestSegmentsQualityChange(newManifest, reloadingQuality) && reloadTimer != null)
					mTimerDelay = timerOnErrorDelay;
			}

		}
		

		reloadingFromBackup = false;
		reloadingManifest = null; // don't want to hang on to it
		if (reloadingQuality == -1)
		{
			HLSPlayerViewController.currentController.seekToCurrentPosition();
		}
		startReloadTimer();
		HLSPlayerViewController.currentController.postDurationChanged();
		
	}
	
	private int mFailureCount = 0;

	@Override
	public void onReloadFailed(ManifestParser parser) 
	{
		if (closed) return;
		Log.i("StreamHandler.onReloadFailed", "onReloadFailed Manifest reload failed: " + parser.fullUrl);
		
		mIsRecovering = true;
		lastBadManifestUri = parser.fullUrl;

		if (mFailureCount == 0)
		{
			mTimerDelay = (long)(getManifestForQuality(lastQuality).targetDuration * 1000  / 2);
			startReloadTimer();
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
		
		
		// This might just be a bad manifest, so try swapping it with a backup if we can and reload the manifest immediately
		int quality = targetQuality != lastQuality ? targetQuality : lastQuality;
		
		if (!swapBackupStream(getStreamForQuality(quality)))
			reload(quality);
		
	}
	
	private boolean swapBackupStream(ManifestStream stream)
	{
		if (stream == null) return false;
		
		if (stream.backupStream != null)
		{
			Log.i("StreamHandler.swapBackupStream", "Swapping to backupstream: " + stream.backupStream.uri);
			primaryStream = stream;
			reload( -1, stream.backupStream.manifest );
			return true;
		}
		
		
		return false;
	}
	
	public boolean backupStreamExists()
	{
		ManifestStream curStream = getStreamForQuality(lastQuality);
		return (curStream != null && curStream.backupStream != null);
	}
	
	private boolean updateManifestSegmentsQualityChange(ManifestParser newManifest, int quality)
	{
		if (newManifest == null || newManifest.segments.size() == 0) return true;
		
		ManifestParser lastQualityManifest = getManifestForQuality(lastQuality);
		ManifestParser targetManifest = quality == -1 ? primaryStream.backupStream.manifest : getManifestForQuality(quality);

		if (newManifest.isDVR() != lastQualityManifest.isDVR())
		{
			// If the new manifest's DVR status does not match the current DVR status, don't switch qualities
			targetQuality = lastQuality;
			return false;
		}

		Vector<ManifestSegment> lastQualitySegments = lastQualityManifest.segments;
		Vector<ManifestSegment> targetSegments = targetManifest.segments;
		Vector<ManifestSegment> newSegments = newManifest.segments;

		ManifestSegment matchSegment = lastQualitySegments.get(lastSegmentIndex);

		// Add the new manifest segments to the targetManifest
		// Tasks: (in order)
		// 	1) Append the new segments to the target segment list and determine the new last known playlist start time
		//	2) Determine the last segment index in the new segment list
		
		// Find the point where the new segments id matches the old segment id
		int matchId = targetSegments.get(targetSegments.size() - 1).id;
		int matchIndex = -1;
		double matchStartTime = lastKnownPlaylistStartTime;
		for (int i = newSegments.size() - 1; i >= 0; --i)
		{
			if (newSegments.get(i).id == matchId)
			{
				matchIndex = i;
				break;
			}
		}
		
		// We only need to make additional calculations if we were able to find a point where the segments matched up
		if (matchIndex >= 0 && matchIndex != newSegments.size() - 1)
		{
			// Fix the start times
			double nextStartTime = targetSegments.get(targetSegments.size()-1).startTime;
			for (int i = matchIndex; i < newSegments.size(); ++i)
			{
				newSegments.get(i).startTime = nextStartTime;
				nextStartTime += newSegments.get(i).duration;
			}
			
			// Append the new manifest segments to the targetManifest
			for (int i = matchIndex + 1; i < newSegments.size(); ++i)
			{
				targetSegments.add(newSegments.get(i));
			}
			
			// Now we need to calculate the last known playlist start time
			int matchStartId = newSegments.get(0).id;
			for (int i = 0; i < targetSegments.size(); ++i)
			{
				if (targetSegments.get(i).id == matchStartId)
					matchStartTime = targetSegments.get(i).startTime;
			}
		}
		else if (matchIndex < 0)
		{
			// The last playlist start time is at the start of the newest segment, the best we can do here is estimate
			matchStartTime += targetSegments.get(targetSegments.size() - 1).duration;
			
			// No matches were foun so we add all the new segments to the playlist, also adjust their start times
			double nextStartTime = matchStartTime;
			for (int i = 0; i < newSegments.size(); ++i)
			{
				newSegments.get(i).startTime = nextStartTime;
				nextStartTime += newSegments.get(i).duration;
				targetSegments.add(newSegments.get(i));
			}
		}
		else
		{
			// In this case, there are no new segments, and we don't acually need to do anything to the playlist
		}
		
		// This is now where our new playlist starts
		lastKnownPlaylistStartTime = matchStartTime;
		
		// Figure out what the new lastSegmentIndex is
		boolean found = false;
		double matchTime = lastQualitySegments.get(lastSegmentIndex).startTime;
		for (int i = 0; i < targetSegments.size(); ++i)
		{
			if (targetSegments.get(i).startTime <= matchTime && targetSegments.get(i).startTime > matchTime - targetSegments.get(i).duration)
			{
				lastSegmentIndex = i;
				found = true;
				stalled = false;
				break;
			}
		}
		
		if (!found && targetSegments.get(targetSegments.size() - 1).startTime < matchSegment.startTime)
		{
			Log.i("StreamHandler.updateManifestSegmentsQualityChange()", "***STALL*** Target Sart Time: " + targetSegments.get(targetSegments.size() - 1).startTime + " Match Start Time: " + matchSegment.startTime);
			
			stalled = true; // We want to stall because we don't know what the index should be as our new playlist is not quite caught up
			return found; // returning early so that we don't hcange lastQuality (we still need that value around)
		}
		
		
		// Either set lastQuality to targetQulity or switch to the backup since we're finally all matched up
		if (quality == -1)
		{
			// Find the stream to replace with its backup
			for (int i = 0; i <= manifest.streams.size(); ++i)
			{
				if (i == manifest.streams.size())
				{
					Log.w("StreamHandler.updateManifestSegmentsQualityChange", " WARNING - Backup Replacement failed: stream with URI " + primaryStream.uri + " not found");
					break;
				}
				
				if (manifest.streams.get(i) == primaryStream && primaryStream.backupStream != null)
				{
					manifest.streams.set(i, primaryStream.backupStream);
					break;
				}
			}
		}
		else
		{
			lastQuality = quality;
		}
		
		stalled = false;
		return found;
		
	}
	
	private void updateManifestSegments(ManifestParser newManifest, int quality)
	{
		Log.i("StreamHandler.updateManifestSegments", "Updating manifest segments for quality " + quality);
		// NOTE: If a stream uses byte ranges, the algorithm in this method will not
		// take note of them, and will likely return the same file every time. An effort
		// could also be made to do more stringent testing on the list of segments (beyond just the URI),
		// perhaps by comparing timestamps.
		
		if (newManifest == null || newManifest.segments.size() == 0) return;
		Vector<ManifestSegment> segments = getSegmentsForQuality( quality );
		ManifestParser curManifest = getManifestForQuality(quality);
		int segId = segments.get(segments.size() - 1).id;

		
		// Seek forward from the lastindex of the list (no need to start from 0) to match continuity eras
		int i = 0;
		int k = 0;
		int continuityOffset = 0;
		double newStartTime = 0;
		for (i = 0; i < segments.size(); ++i)
		{
			if (newManifest.segments.get(0).id == segments.get(i).id)
			{
				// Found the match. Now offset the eras in the new segment list by the era in the match
				continuityOffset = segments.get(i).continuityEra;
				newStartTime = segments.get(i).startTime;
				break;
			}
		}
		
		if (i == segments.size()) // we didn't find a match so force a discontinuity
		{
			if (segments.size() > 0)
			{
				continuityOffset = segments.get(segments.size()-1).continuityEra + 1;
				newStartTime = segments.get(segments.size()-1).startTime + segments.get(segments.size()-1).duration;
			}
		}

		// store the playlist start time
		lastKnownPlaylistStartTime = newStartTime;

		// run through the new playlist and adjust the start times and continuityEras
		for (k = 0; k < newManifest.segments.size(); ++k)
		{
			newManifest.segments.get(k).continuityEra += continuityOffset;
			newManifest.segments.get(k).startTime = newStartTime;
			newStartTime += newManifest.segments.get(k).duration;
		}
		
		// Seek backward through the new segment list until we find the one that matches
		// the last segment of the current list
		for (i = newManifest.segments.size() - 1; i >= 0; --i)
		{
			if (newManifest.segments.get(i).id == segId)
			{
				break;
			}
		}
		
		if (i + 1 >= newManifest.segments.size())
		{
			Log.i("StreamHandler.updateManifestSegments", "Did not find any new segments");
		}
		
		// append the remaining segments to the existing segment list
		int lastSize = segments.size();
		for (k = i+1; k < newManifest.segments.size(); ++k)
		{
			segments.add(newManifest.segments.get(k));
		}
		if (segments.size() != lastSize)
		{
			Log.i("StreamHandler.updateManifestSegments", "We have new segments. We shouldn't be stalled any longer");
			stalled = false;
		}
		
		// Match the new manifest's and the old manifest's DVR status
		getManifestForQuality(quality).streamEnds = newManifest.streamEnds;
		manifest.streamEnds = newManifest.streamEnds;

	}
	
	public boolean isStalled()
	{
		return stalled;
	}
	
	private int getWorkingQuality(int requestedQuality)
	{
		// Note that this method always returns lastQuality. It triggers a reload if it needs to, and
		// 	lastQuality will be set once the reload is complete.
		
		// If the requested quality is the same as what we're currently using, return that
		if (requestedQuality == lastQuality) return lastQuality;
		
		// If the requsted quality is the same as the target quality, we've already asked for a reload, so return the last quality
		if (requestedQuality == targetQuality) return lastQuality;
		
		// The requested quality doesn't match eithe the targetQuality or the lastQuality, which means this is new territory.
		// So we will reload the manifest for the requested quality
		targetQuality = requestedQuality;
		Log.i("StreamHandler.getWorkingQuality", "Quality Change: " + lastQuality + " --> " + requestedQuality);
		reload(targetQuality);			
		return lastQuality;
	}
	
	private ManifestSegment getSegmentForIndex(Vector<ManifestSegment> segments, int index, int quality)
	{
		ManifestSegment seg = segments.get(index);
		seg.quality = quality;
		if (altAudioManifest != null)
		{
			if (altAudioManifest.segments.size() > index)
			{
				seg.altAudioSegment = altAudioManifest.segments.get(index);
				seg.altAudioSegment.altAudioIndex = altAudioIndex;
			}
		}
		seg.key = getKeyForIndex(index);
		seg.initializeCrypto();
		return seg;
	}
	
	
	public ManifestSegment getFileForTime(double time, int quality)
	{
		quality = getWorkingQuality(quality);
		
		double accum = 0.0;
		Vector<ManifestSegment> segments = getSegmentsForQuality(quality);
		
		if (time == USE_DEFAULT_START && !streamEnds())
		{
			if (SKIP_TO_END_OF_LIVE)
			{
				lastSegmentIndex = Math.max(segments.size() - 2, 0);
				return getSegmentForIndex(segments, lastSegmentIndex, quality);
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

		
		if (time < lastKnownPlaylistStartTime)
		{
			time = lastKnownPlaylistStartTime;
			++sequenceSkips;
			Log.i("StreamHandler.getFileForTime", "SequenceSkip - time: " + time + " playlistStartTime: " + lastKnownPlaylistStartTime);
		}
		
		int i = 0;
		for (i = 0; i < segments.size(); ++i)
		{
			ManifestSegment curSegment = segments.get(i);
			
			if (curSegment.duration > time - accum)
			{
				lastSegmentIndex = i;
				return getSegmentForIndex(segments, lastSegmentIndex, quality);
			}
			
			accum += curSegment.duration;
		}
		
		lastSegmentIndex = i;
		
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
		
		quality = getWorkingQuality(quality);
		
		Vector<ManifestSegment> segments = getSegmentsForQuality( quality );

		if (lastSegmentIndex < segments.size() && stalled)
		{
			Log.i("StreamHandler.getNextFile()", "We have more segments than our current index - toggling off Stalled");
			stalled = false;
		}
		if (stalled)
		{
			Log.i("StreamHandler.getNextFile()", "---- Stalled ----- Segment Count=" + segments.size() + " lastSegmentIndex=" + lastSegmentIndex);
			return null;
		}

		
		int targetIndex = lastSegmentIndex + 1;
		
		
		if ( targetIndex < segments.size())
		{
			lastSegmentIndex = targetIndex;
			ManifestSegment lastSegment = segments.get(lastSegmentIndex);
			lastSegment.key = getKeyForIndex(lastSegmentIndex);
			if (altAudioManifest != null)
			{
				if (altAudioManifest.segments.size() > lastSegmentIndex)
				{
					lastSegment.altAudioSegment = altAudioManifest.segments.get(lastSegmentIndex);
					lastSegment.altAudioSegment.altAudioIndex = altAudioIndex;
				}
			}
			if (lastSegment.startTime + lastSegment.duration < lastKnownPlaylistStartTime)
			{
				Log.i("StreamHandler.getNextFile", "SequenceSkip - startTime: " + lastSegment.startTime + " + duration: " + lastSegment.duration  + " playlistStartTime: " + lastKnownPlaylistStartTime);
				lastSegmentIndex = getSegmentIndexForTime(lastKnownPlaylistStartTime);
				++sequenceSkips;				
			}
			Log.i("StreamHandler.getNextFile", "Getting Next Segment[" + lastSegmentIndex + "]\n" + lastSegment.toString());

			lastSegment.quality = quality;
			lastSegment.initializeCrypto();
			return lastSegment;
			
		}
		
		Log.i("StreamHandler.getNextFile", "---- No New Segments Found. Last index = " + lastSegmentIndex + " | List Size = " + segments.size());
		
		if (!streamEnds())
		{
			Log.i("StreamHandler.getNextFile()", "No new segments to play!!! Stalling! Last index = " + lastSegmentIndex + " | List Size = " + segments.size());
			stalled = true;
		}
		
		return null;
	}
	
	//TODO: MAKE THIS WORK
	public ManifestEncryptionKey getKeyForIndex( int index )
	{
		Vector<ManifestEncryptionKey> keys = null;
		
		
		// Make sure we accessing returning the correct key list for the manifest type
		if ( manifest.type == ManifestParser.AUDIO ) keys = manifest.keys;
		else keys = getManifestForQuality( lastQuality ).keys;
		
		for ( int i = 0; i < keys.size(); i++ )
		{
			ManifestEncryptionKey key = keys.get( i );
			if ( key.startSegmentId <= index && key.endSegmentId >= index )
			{
				return key;
			}
		}
		
		return null;
	}
	
	
	// Returns duration in ms
	public int getDuration()
	{
		double accum = 0.0f;
		
		if (manifest == null) return -1;
		
		Vector<ManifestSegment> segments = getSegmentsForQuality( lastQuality );
		ManifestParser activeManifest = getManifestForQuality(lastQuality);
		int i = segments.size() - 1;
		
		// Test can be removed in future if there's no problems with returning duration all the time
		//if (i >= 0 && (activeManifest.allowCache || activeManifest.streamEnds))  
		{
			accum = (segments.get(i).startTime + segments.get(i).duration) - lastKnownPlaylistStartTime;
		}
		
		return (int) (accum * 1000);
		
	}
	
	private int getSegmentIndexForTime(double time)
	{
		return getSegmentIndexForTimeAndQuality(time, lastQuality);
	}
	
	private int getSegmentIndexForTimeAndQuality(double time, int quality)
	{
		if (manifest != null)
			return -1;
		
		Vector<ManifestSegment> segments = getSegmentsForQuality( lastQuality );
		
		for (int i = segments.size() - 1; i >= 0; --i)
		{
			if (segments.get(i).startTime < time)
				return i;
		}
		return 0;
	}
	
	public int getQualityLevels()
	{
		if (manifest == null) return 0;
		if (manifest.streams.size() > 0 ) return manifest.streams.size();
		return 1;
		
	}
	
	private Vector<ManifestSegment> getSegmentsForQuality(int quality)
	{
		if ( manifest == null) return new Vector<ManifestSegment>();
		if (manifest.streams.size() < 1 || manifest.streams.get(0) == null) return manifest.segments;
		else if ( quality >= manifest.streams.size() ) return manifest.streams.get(0).manifest.segments;
		else return manifest.streams.get(quality).manifest.segments;
	}
	
	public ManifestParser getManifestForQuality(int quality)
	{
		if (manifest == null) return new ManifestParser();
		if (manifest.streams.size() < 1 || manifest.streams.get(0).manifest == null) return manifest;
		else if ( quality >= manifest.streams.size() ) return manifest.streams.get(0).manifest;
		return manifest.streams.get(quality).manifest;
	}
	
	public ManifestStream getStreamForQuality(int quality)
	{
		if (manifest == null || manifest.streams.size() < 1 || quality >= manifest.streams.size()) return null;
		return manifest.streams.get(quality);
	}

	@Override
	public void onSegmentCompleted(String uri) {
		HLSSegmentCache.cancelCacheEvent(uri);
		
	}

	@Override
	public void onSegmentFailed(String uri, int errorCode) {
		HLSSegmentCache.cancelCacheEvent(uri);
		Log.i("StreamHandler.onSegmentFailed", "Failed Download - attempting recovery: " + errorCode + " " + uri);
		attemptRecovery();
		
	}




	
	
	
	
	
	
	
	
}
