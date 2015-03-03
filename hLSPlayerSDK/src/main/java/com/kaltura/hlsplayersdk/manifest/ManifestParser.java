package com.kaltura.hlsplayersdk.manifest;


import java.util.Vector;

import android.util.Log;
import android.util.EventLog.Event;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.hlsplayersdk.URLLoader;
import com.kaltura.hlsplayersdk.subtitles.*;
import com.kaltura.hlsplayersdk.events.OnErrorListener;
import com.kaltura.hlsplayersdk.manifest.events.*;

public class ManifestParser implements OnParseCompleteListener, URLLoader.DownloadEventListener {
	
	private static int instanceCounter = 0;
	private int instanceCount = 0;
	public int instance() { return instanceCount; }
	
	public ManifestParser()
	{
		++instanceCounter;
		instanceCount = instanceCounter;
	}
	
	public static final String DEFAULT = "DEFAULT";
	public static final String AUDIO = "AUDIO";
	public static final String VIDEO = "VIDEO";
	public static final String SUBTITLES = "SUBTITLES";
	public static final String SEGMENT = "SEGMENT";
	
	
	public String type = DEFAULT;
	public int version;
	public String baseUrl;
	public String fullUrl;
	public int mediaSequence;
	public boolean allowCache;
	public double targetDuration;
	public boolean streamEnds = false;
	public Vector<ManifestPlaylist> playLists = new Vector<ManifestPlaylist>();
	public Vector<ManifestStream> streams = new Vector<ManifestStream>();
	public Vector<ManifestSegment> segments = new Vector<ManifestSegment>();
	public Vector<ManifestPlaylist> subtitlePlayLists = new Vector<ManifestPlaylist>();
	public Vector<SubTitleSegment> subtitles = new Vector<SubTitleSegment>();
	public Vector<ManifestEncryptionKey> keys = new Vector<ManifestEncryptionKey>();
	
	public Vector<URLLoader> manifestLoaders = new Vector<URLLoader>();
	public Vector<ManifestParser> manifestParsers = new Vector<ManifestParser>();
	public boolean goodManifest = true;
	private int mReloadFailureCount = 0;
	
	public int videoPlayId = 0; // Used for tracking which video play we're on. Only the base manifest parser will have this set to anything other than 0.
	
	public int continuityEra = 0;
	private int _subtitlesLoading = 0;
	
	private ManifestParser mReloadingManifest = null; 	// If this is the parent, mReloadingManifest is the child. If this is the child, mReloadingManifest is the parent
	
	private boolean mReloadParent = true;
	
	
	public ManifestParser getReloadParent()
	{
		if (mReloadParent) return this;
		return mReloadingManifest;
	}
	
	public ManifestParser getReloadChild()
	{
		if (mReloadParent) return mReloadingManifest;
		return this;		
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("\nManifest instance : " + instanceCount + "\n-----------------------------\n");
		sb.append("type : " + type + "\n");
		sb.append("version : " + version + "\n");
		sb.append("baseUrl : " + baseUrl + "\n");
		sb.append("fullUrl : " + fullUrl + "\n");
		sb.append("mediaSequence : " + mediaSequence + "\n");
		sb.append("allowCache : " + allowCache + "\n");
		sb.append("targetDuration : " + targetDuration + "\n");
		sb.append("streamEnds : " + streamEnds + "\n");
		
		for (int i = 0; i < segments.size(); ++i)
		{
			sb.append("---- ManifestSegment ( " + i  + " )\n");
			sb.append(segments.get(i).toString());
		}
		
		
		for (int i = 0; i < streams.size(); ++i)
		{
			sb.append("---- Stream ( " + i  + " )\n");
			sb.append(streams.get(i).toString());
		}
		
		return sb.toString();
	}
	
	public double estimatedWindowDuration()
	{
		return segments.size() * targetDuration;
	}
	
	public boolean isDVR()
	{
		return allowCache && !streamEnds;
	}
	
	public boolean needsReloading()
	{
		return !streamEnds && (segments.size()  > 0 || subtitles.size() > 0);
	}
	
	public void dumpToLog()
	{
		Log.i("ManifestParser.dumpToLog", this.toString());
	}
	
	public static String getNormalizedUrl( String baseUrl, String uri)
	{
		return ( uri.substring(0, 5).equals("http:") || uri.substring(0, 6).equals("https:") || uri.substring(0, 5).equals("file:")) ? uri : baseUrl + uri;
	}
	
	public static <T> T as(Class<T> t, Object o) {
		  return t.isInstance(o) ? t.cast(o) : null;
		}

	private Object lastHint = null;
	
	public boolean hasSegments()
	{
		if (segments.size() > 0) return true;
		for (int i = 0; i < streams.size(); ++i)
		{
			if (streams.get(i).manifest != null && streams.get(i).manifest.hasSegments()) return true;
		}
		return false;
	}

	public void parse(String input, String _fullUrl)
	{
		lastHint = null;
		fullUrl = _fullUrl;
		baseUrl = _fullUrl.substring(0, _fullUrl.lastIndexOf('/') + 1);
		
		// Normalize line endings
		input = input.replace("\r\n", "\n");
		
		// split into an array
		String [] lines = input.split("\n");
		
		// process each line
		
		int nextByteRangeStart = 0;
		
		if (lines.length == 0)
		{
			goodManifest = false;
		}
		
		int i = 0;
		for ( i = 0; i < lines.length; ++i)
		{
			String curLine = lines[i];

			// Ignore empty lines;
			if (curLine.length() == 0) continue;
			
			String curPrefix = curLine.substring(0, 1);
			
			if (i == 0 && !curLine.contains("#EXTM3U"))
			{
				Log.i("ManifestParser.parse()", "Bad Stream! #EXTM3U is missing from the first line");
				goodManifest = false;
				HLSPlayerViewController.currentController.postError(OnErrorListener.MEDIA_ERROR_MALFORMED, "#EXTM3U is missing from the first line. " + this.fullUrl);
				break;
			}
			
			if (!curPrefix.endsWith("#") && curLine.length() > 0)
			{
				// Specifying a media file, note it
				if ( !type.equals(SUBTITLES ))
				{
					String targetUrl = getNormalizedUrl(baseUrl, curLine);
					ManifestSegment segment = as(ManifestSegment.class, lastHint);
					if (segment != null && segment.byteRangeStart != -1)
					{
						// Append akamai ByteRange properties to URL
						String urlPostFix = targetUrl.indexOf( "?" ) == -1 ? "?" : "&";
						targetUrl += urlPostFix + "range=" + segment.byteRangeStart + "-" + segment.byteRangeEnd;
					}
					
					BaseManifestItem mi = as(BaseManifestItem.class, lastHint);
					if (mi != null)
						mi.uri = targetUrl;
					else
					{
						Log.e("ManifestParser.parse", "UnknownType. Can't Set URI: " + lastHint.toString());
					}
						
				}
				else
				{
					// SUBTITLES
					String targetUrl = getNormalizedUrl(baseUrl, curLine); 
					SubTitleSegment sp = as(SubTitleSegment.class, lastHint);
					if (sp != null)
						sp.setUrl(targetUrl);
					else
					{
						Log.e("ManifestParser.parse", "UnknownType. Can't call SubTitleSegment.setUrl(): " + lastHint.toString());
					}
				}
				continue;
			}
			
			// Otherwise, we are processing a tag.
			int colonIndex = curLine.indexOf(':');
			String tagType = colonIndex > -1 ? curLine.substring(1, colonIndex) : curLine.substring(1);
			String tagParams = colonIndex > -1 ? curLine.substring( colonIndex + 1) : "";
			
			if (tagType.equals("EXTM3U")) 
			{
				if (i != 0)
					Log.w("ManifestParser.parse", "Saw EXTM3U out of place! Ignoring...");
			}
			else if (tagType.equals("EXT-X-TARGETDURATION")) 
			{
				targetDuration = Integer.parseInt(tagParams);
			}
			else if (tagType.equals("EXT-X-ENDLIST"))
			{
				// This will only show up in live streams if the stream is over.
				// This MUST (according to the spec) show up in any stream in which no more
				//     segments will be made available.
				streamEnds = true;
			}
			else if (tagType.equals("EXT-X-KEY"))
			{
				if (keys.size() > 0) keys.get(keys.size() - 1).endSegmentId = segments.size() - 1;
				ManifestEncryptionKey key = ManifestEncryptionKey.fromParams(tagParams);
				key.startSegmentId = segments.size();
				if (!key.url.contains("://"))
				{
					key.url = getNormalizedUrl(baseUrl, key.url);
				}
				keys.add(key);
			}
			else if (tagType.equals("EXT-X-VERSION"))
			{
				version = Integer.parseInt(tagParams);
			}
			else if (tagType.equals("EXT-X-MEDIA-SEQUENCE"))
			{
				mediaSequence = Integer.parseInt(tagParams);
				Log.i("ManifestParser(" + instanceCount + ")", "Type=" + type + " MediaSequence=" + mediaSequence);
			}
			else if (tagType.equals("EXT-X-ALLOW-CACHE"))
			{
				allowCache = tagParams.equals("YES") ? true : false;
			}
			else if (tagType.equals("EXT-X-MEDIA"))
			{
				if ( tagParams.indexOf( "TYPE=AUDIO" ) != -1 )
				{
					ManifestPlaylist playList = ManifestPlaylist.fromString( tagParams ); 
					playList.uri = getNormalizedUrl( baseUrl, playList.uri );
					playLists.add( playList );
				}
				else if ( tagParams.indexOf( "TYPE=SUBTITLES" ) != -1 )
				{
					ManifestPlaylist subtitleList = ManifestPlaylist.fromString( tagParams );
					subtitleList.uri = getNormalizedUrl( baseUrl, subtitleList.uri );
					subtitlePlayLists.add( subtitleList );
				}
				else Log.w("ManifestParser.parse", "Encountered " + tagType + " tag that is not supported, ignoring." );				
			}
			else if (tagType.equals("EXT-X-STREAM-INF"))
			{
				streams.add(ManifestStream.fromString(tagParams));
				lastHint = streams.get(streams.size() - 1);
			}
			else if (tagType.equals("EXTINF"))
			{
				if ( type.equals(SUBTITLES ))
				{
					SubTitleSegment subTitle = new SubTitleSegment();
					String[] valueSplit = tagParams.split(",");
					subTitle.segmentTimeWindowDuration = Double.parseDouble(valueSplit[0]);
					subtitles.add( subTitle );
					lastHint = subTitle;
					
				}
				else
				{
					lastHint = new ManifestSegment();
					segments.add((ManifestSegment)lastHint);
					lastHint = segments.get(segments.size()-1);
					String [] valueSplit = tagParams.split(",");
					
					if (valueSplit.length > 0) 
						((ManifestSegment)lastHint).duration =  Double.parseDouble(valueSplit[0]);
					else 
						((ManifestSegment)lastHint).duration = targetDuration;
					
					((ManifestSegment)lastHint).continuityEra = continuityEra;
					
					if(valueSplit.length > 1)
					{
						((ManifestSegment)lastHint).title = valueSplit[1];
					}
				}				
			}
			else if (tagType.equals("EXT-X-BYTERANGE"))
			{
				ManifestSegment hintAsSegment = as(ManifestSegment.class, lastHint);
				if ( hintAsSegment == null ) break;
				String [] byteRangeValues = tagParams.split("@");
				hintAsSegment.byteRangeStart = byteRangeValues.length > 1 ? Integer.parseInt( byteRangeValues[ 1 ] ) : nextByteRangeStart;
				hintAsSegment.byteRangeEnd = hintAsSegment.byteRangeStart + Integer.parseInt( byteRangeValues[ 0 ] );
				nextByteRangeStart = hintAsSegment.byteRangeEnd + 1;
			}
			else if (tagType.equals("EXT-X-DISCONTINUITY"))
			{
				++continuityEra;
			}
			else if (tagType.equals("EXT-X-PROGRAM-DATE-TIME"))
			{
				
			}
			else
			{
				Log.w("ManifestParser.parse", "Unknown tag '" + tagType + "', ignoring...");
			}
		}
		
		// Process any other manifests referenced
		Vector<BaseManifestItem> manifestItems = new Vector<BaseManifestItem>();
		manifestItems.addAll(streams);
		manifestItems.addAll(playLists);
		manifestItems.addAll(subtitlePlayLists);
		
		for (int k = 0; k < manifestItems.size(); ++k)
		{
			BaseManifestItem curItem = manifestItems.get(k);
			if (curItem.uri.lastIndexOf("m3u8") != -1)
			{
				// Request and parse the manifest.
				addItemToManifestLoader(curItem);
			}
		}
		
		// update start time for the segments we own
		double timeAccum = 0.0;
		for (int m = 0; m < segments.size(); ++m)
		{
			segments.get(m).id = mediaSequence + m; // set the id based on the media sequence
			segments.get(m).startTime = timeAccum;
			Log.i("ManifestParser(" + instanceCount + ").foundSegment", "SegmentURI=" + segments.get(m).uri);
			timeAccum += segments.get(m).duration;
		}
		
		// update start time for the subtitles we own
		timeAccum = 0.0;
		for (int m = 0; m < subtitles.size(); ++m)
		{
			subtitles.get(m).id = mediaSequence + m;
			subtitles.get(m).setTimeWindowStart(timeAccum);
			timeAccum += subtitles.get(m).segmentTimeWindowDuration;
		}
		
		if (manifestLoaders.size() == 0) postParseComplete(this);
			
	}
	
	private void verifyManifestItemIntegrity()
	{
		// work through the streams and remove any broken ones
		for (int i = streams.size() - 1; i >= 0; --i)
		{
			if (streams.get(i).manifest == null || streams.get(i).manifest.goodManifest == false)
				streams.remove(i);
		}
		
		// Make our streamEnds value match the value of the first stream
		if (streams.size() > 0)
			streamEnds = streams.get(0).manifest.streamEnds;
		else if (subtitlePlayLists.size() > 0)
			streamEnds = subtitlePlayLists.get(0).manifest.streamEnds;
		
		// Work through the streams and set up the backup streams
		int backupCount = 0;
		for (int i = streams.size() - 1; i >= 0; --i)
		{
			// skip the last item in the list because we don't know yet if it's a backup
			if (i == streams.size() - 1)
				continue;
			
			if (streams.get(i).bandwidth == streams.get(i+1).bandwidth)
			{
				backupCount++;
			}
			else if (backupCount > 0)
			{
				// link the main stream with its backup stream(s)
				linkBackupStreams(i+1, backupCount);
				backupCount = 0;
			}
		}
		
		// Check for leftovers
		if (backupCount > 0)
			linkBackupStreams(0, backupCount);
		
		// Remove any dead manifests
		for (int i = playLists.size() - 1; i >= 0; --i)
		{
			if (playLists.get(i).manifest == null)
				playLists.remove(i);
		}
	}
	
	private void linkBackupStreams(int startIndex, int count)
	{
		// store the index of the last item so we don't have to do the math more than once
		int lastIndex = startIndex + count;

		// link the last stream to the first stream
		streams.get(lastIndex).backupStream = streams.get(startIndex);
		streams.get(startIndex).numBackups = count;
		
		for (int i = lastIndex; i > startIndex; --i)
		{
			streams.get(i - 1).backupStream = streams.get(i);
			streams.get(i).numBackups = count;
			streams.remove(i);
		}
	}
	
	private void addItemToManifestLoader(BaseManifestItem item)
	{
		URLLoader manifestLoader = new URLLoader("ManifestParser.addItemToManifestLoader", this, item);
		manifestLoaders.add(manifestLoader);
		manifestLoader.get(item.uri);
	}
	
	@Override
	public void onDownloadFailed(URLLoader loader, String response) {
		if (loader.manifestItem != null)
		{
			Log.w("ManifestParser.onManifestError", "ERROR loading manifest " + response);
			manifestLoaders.remove(loader);
			announceIfComplete();
		}
		else
		{
			
		}
		HLSPlayerViewController.currentController.postError(OnErrorListener.MEDIA_ERROR_IO, loader.uri + "(" + response + ")");
		postReloadFailed(this);
	}

	@Override
	public void onDownloadComplete(URLLoader loader, String response) {
		
		Log.i("ManifestParser.onDownloadComplete(" + instanceCount + ")", "Reloading type=" + type + " loader.manifestItem=" + (loader.manifestItem == null ? null : loader.manifestItem.hashCode()) + " listenerHash=" + ( mReloadEventListener != null ? mReloadEventListener.hashCode() : null )+ " URI=" + fullUrl);

		if (loader.manifestItem != null) // this is a load of a submanifest
		{
			String resourceData = response;
			BaseManifestItem manifestItem = loader.manifestItem;
			manifestLoaders.remove(loader);
			
			ManifestParser parser = new ManifestParser();
			parser.type = manifestItem.type;
			manifestItem.manifest = parser;
			manifestParsers.add(parser);
	
			parser.setOnParseCompleteListener(this);
			parser.parse(resourceData, getNormalizedUrl(baseUrl, manifestItem.uri));
		}
		else // this is a reload!
		{
			String resourceData = response;
			if (mOnParseCompleteListener == null) setOnParseCompleteListener(this);
			parse(resourceData, fullUrl);
		}
		
	}

	@Override
	public void onParserComplete(ManifestParser parser)
	{
		Log.i("ManifestParser.onParserComplete(" + instanceCount + ")", "Parse Complete - parser=" + parser.instance() + " type=" + type + " reloadListenerHash=" + ( mReloadEventListener != null ? mReloadEventListener.hashCode() : null ) + " URI=" + fullUrl);
		if (parser == this && mReloadEventListener != null) // We're reloading
		{
			postReloadComplete(this);
		}
		else
		{
			manifestParsers.remove(parser);
			announceIfComplete();
		}
	}
	
	// This happens in the parent
	public void reload(ReloadEventListener reloadListener)
	{
		Log.i("ManifestParser.reload(" + instanceCount + ")", "Reloading type=" + type + " listenerHash=" + reloadListener.hashCode() + " URI=" + fullUrl);
		mReloadParent = true; // We are the parent
		
		if (mReloadingManifest != null) 
		{
			mReloadingManifest.setReloadEventListener(null);
			mReloadingManifest = null;
		}
		
		mReloadingManifest = new ManifestParser(); // This is creating the child that will be used to actually parse the update
		mReloadingManifest.type = type;
		mReloadingManifest.setReloadEventListener(reloadListener);
		mReloadingManifest.reload(this);
	}

	// This happens in the child
	private void reload(final ManifestParser manifest)
	{
		// When the URLLoader finishes, it should set the parseComplete listener to *this*, and
		// when that completes, it should call the reloadCompleteListener
		mReloadParent = false; // we are not the parent
		mReloadingManifest = manifest; // This is setting the parent - the one we're trying to reload
		fullUrl = manifest.fullUrl;
		final ManifestParser self = this;
		
		HLSPlayerViewController.postToHTTPResponseThread( new Runnable() 
		{
			@Override
			public void run()
			{
				URLLoader manifestLoader = new URLLoader("ManifestParser(" + instance() + ").reload(" + manifest.instance() + ")", self, null);
				manifestLoader.get(fullUrl);
				
			}
		} );
	}
	
	private void announceIfComplete()
	{
		Log.i("ManifestParser.announceIfComplete()", "_subtitles = " + _subtitlesLoading);
		if (manifestParsers.size() == 0 && manifestLoaders.size() == 0)
		{
			verifyManifestItemIntegrity();
			postParseComplete(this);
		}
	}

	public int getReloadFailureCount()
	{
		return mReloadFailureCount;
	}
	
	public void incrementReloadFailureCount(int count)
	{
		++mReloadFailureCount;
	}
	
	public void clearReloadFailureCount()
	{
		mReloadFailureCount = 0;
	}
	
	public interface ReloadEventListener
	{
		void onReloadComplete(ManifestParser parser);
		void onReloadFailed(ManifestParser parser);
	}
	
	private ReloadEventListener mReloadEventListener = null;
	
	public void setReloadEventListener(ReloadEventListener listener)
	{
		mReloadEventListener = listener;
	}
	public void postReloadComplete(ManifestParser parser)
	{
		Log.i("ManifestParser(" + instanceCount + ").postReloadComplete", "Reload Complete for Parent=" + mReloadingManifest.instance());
		if (mReloadEventListener != null) mReloadEventListener.onReloadComplete(parser.getReloadParent()); // We're passing out the parent
	}
	
	public void postReloadFailed(ManifestParser parser)
	{
		Log.i("ManifestParser(" + instanceCount + ").reloadFailed", "Reloading type=" + type + " listenerHash=" + ( mReloadEventListener != null ? mReloadEventListener.hashCode() : null )+ " URI=" + fullUrl);
		if (mReloadEventListener != null) mReloadEventListener.onReloadFailed(parser.getReloadChild()); // We're passing out the parent
 	}
	
	public void notifyReloadComplete()
	{
		if (mReloadParent)
			mReloadingManifest = null;
	}
	
	// Event Listeners
	public void setOnParseCompleteListener(OnParseCompleteListener listener)
	{
		mOnParseCompleteListener = listener;
	}
	
	
	public void setOnParseCompleteListener(OnParseCompleteListener listener, int playId)
	{
		videoPlayId = playId;
		mOnParseCompleteListener = listener;
	}
	private OnParseCompleteListener mOnParseCompleteListener;
	public void postParseComplete(ManifestParser parser)
	{
		if (mOnParseCompleteListener != null) mOnParseCompleteListener.onParserComplete(parser);
	}
	
	
	
	public ManifestSegment findSegmentByID(int id)
	{
		for (int i = segments.size() - 1; i >= 0; --i)
		{
			if (segments.get(i).id == id)
				return segments.get(i);
		}
		return null;
	}

}
