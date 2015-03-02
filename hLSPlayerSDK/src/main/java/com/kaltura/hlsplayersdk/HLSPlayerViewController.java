package com.kaltura.hlsplayersdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.kaltura.hlsplayersdk.cache.HLSSegmentCache;
import com.kaltura.hlsplayersdk.cache.SegmentCachedListener;
import com.kaltura.hlsplayersdk.events.OnAudioTrackSwitchingListener;
import com.kaltura.hlsplayersdk.events.OnAudioTracksListListener;
import com.kaltura.hlsplayersdk.events.OnDurationChangedListener;
import com.kaltura.hlsplayersdk.events.OnErrorListener;
import com.kaltura.hlsplayersdk.events.OnPlayerStateChangeListener;
import com.kaltura.hlsplayersdk.events.OnPlayheadUpdateListener;
import com.kaltura.hlsplayersdk.events.OnProgressListener;
import com.kaltura.hlsplayersdk.events.OnQualitySwitchingListener;
import com.kaltura.hlsplayersdk.events.OnQualityTracksListListener;
import com.kaltura.hlsplayersdk.events.OnTextTrackChangeListener;
import com.kaltura.hlsplayersdk.events.OnTextTrackTextListener;
import com.kaltura.hlsplayersdk.events.OnTextTracksListListener;
import com.kaltura.hlsplayersdk.events.OnToggleFullScreenListener;
import com.kaltura.hlsplayersdk.manifest.ManifestParser;
import com.kaltura.hlsplayersdk.manifest.ManifestSegment;
import com.kaltura.hlsplayersdk.manifest.events.OnParseCompleteListener;
import com.kaltura.hlsplayersdk.subtitles.SubtitleHandler;
import com.kaltura.hlsplayersdk.subtitles.TextTrackCue;
import com.kaltura.hlsplayersdk.types.PlayerStates;

/**
 * Main class for HLS video playback on the Java side.
 * 
 * PlayerViewController is responsible for integrating the JNI/Native side
 * with the Java APIs and interfaces. This is the central point for HLS
 * video playback!
 */
public class HLSPlayerViewController extends RelativeLayout implements
		VideoPlayerInterface, URLLoader.DownloadEventListener, OnParseCompleteListener, 
		TextTracksInterface, AlternateAudioTracksInterface, QualityTracksInterface, SegmentCachedListener {

	// State constants.
	private final int STATE_STOPPED = 1;
	private final int STATE_PAUSED = 2;
	private final int STATE_PLAYING = 3;
	private final int STATE_SEEKING = 4;
	private final int STATE_FORMAT_CHANGING = 5;
	private final int STATE_FOUND_DISCONTINUITY = 6;
	private final int STATE_WAITING_FOR_DATA = 7;
	private final int STATE_CUE_STOP = 8;
	
	private final int THREAD_STATE_STOPPED = 0;
	private final int THREAD_STATE_RUNNING = 1;
	
	private final int STARTUP_STATE_STARTED = 0;
	private final int STARTUP_STATE_LOADING = 1;
	private final int STARTUP_STATE_LOADED = 2;
	private final int STARTUP_STATE_PLAY_QUEUED = 3;
	private final int STARTUP_STATE_WAITING_TO_START = 4;

	// Native methods
	private native int GetState();
	private native void InitNativeDecoder();
	private native void CloseNativeDecoder();
	private native void ResetPlayer();
	private native void PlayFile(double timeInSeconds);
	private native void StopPlayer();
	private native void Pause(boolean pause);
	public native void SetSurface(Surface surface);
	private native int NextFrame();
	private native void FeedSegment(String url, int quality, int continuityEra, String altAudioURL, int altAudioIndex, double startTime, int cryptoId, int altCryptoId);
	private native void SeekTo(double timeInSeconds);
	private native void ApplyFormatChange();
	private native void SetSegmentCountToBuffer(int segmentCount);
	private native int DroppedFramesPerSecond();
	public native boolean AllowAllProfiles();

	// Static interface.
	// TODO Allow multiple active PlayerViewController instances.
	public static HLSPlayerViewController currentController = null;
	private static int mQualityLevel = 0;
	private static int mSubtitleLanguage = 0;
	private static int mAltAudioIndex = 0;
	
	private static boolean noMoreSegments = false;
	private static int videoPlayId = 0;


	/**
	 * Get the next segment in the stream.
	 */
	public static void requestNextSegment() {
		if (currentController == null)
			return;
		
		ManifestSegment seg = currentController.getStreamHandler().getNextFile(mQualityLevel);
		if(seg == null)
		{
			if (currentController.getStreamHandler().streamEnds() == true)
				noMoreSegments = true;
			Log.i("HLSPlayerViewController.requestNextSegment", "---- Did not receive a valid segment ----- ");
			return;
		}
		
		Log.i("HLSPlayerViewController.requestNextSegment", "---- Feeding segment '" + seg.uri + "'");
			

		if (seg.altAudioSegment != null)
		{
			HLSSegmentCache.precache(new String[] {seg.uri, seg.altAudioSegment.uri}, new int [] { seg.cryptoId, seg.altAudioSegment.cryptoId }, false, currentController.getStreamHandler(), getInterfaceThreadHandler());
			currentController.FeedSegment(seg.uri, seg.quality, seg.continuityEra, seg.altAudioSegment.uri, seg.altAudioSegment.altAudioIndex, seg.startTime, seg.cryptoId, seg.altAudioSegment.cryptoId);
		}
		else
		{
			HLSSegmentCache.precache(seg.uri, seg.cryptoId, false, currentController.getStreamHandler(), getInterfaceThreadHandler());
			currentController.FeedSegment(seg.uri, seg.quality, seg.continuityEra, null, -1, seg.startTime, seg.cryptoId, -1);
		}
	}

	/**
	 * Initiate loading of the segment corresponding to the specified time.
	 * @param time The time in seconds to request.
	 * @return Offset into the segment to get to exactly the requested time.
	 */
	public static double requestSegmentForTime(double time) {
		Log.i("PlayerViewController.requestSegmentForTime", "Requested Segment Time: " + time);
		if(currentController == null)
			return 0;
		
		ManifestSegment seg = currentController.getStreamHandler().getFileForTime(time, mQualityLevel);
		if(seg == null)
		{
			Log.i("HLSPlayerViewController.requestSegmentForTime", "Did not recieve a segment. StreamHandler.isStalled() = " + currentController.getStreamHandler().isStalled());
			return 0;
		}
		
		if (seg.altAudioSegment != null)
		{
			HLSSegmentCache.precache(new String[] {seg.uri, seg.altAudioSegment.uri}, new int [] { seg.cryptoId, seg.altAudioSegment.cryptoId }, false, currentController.getStreamHandler(), getInterfaceThreadHandler());
			currentController.FeedSegment(seg.uri, seg.quality, seg.continuityEra, seg.altAudioSegment.uri, seg.altAudioSegment.altAudioIndex, seg.startTime, seg.cryptoId, seg.altAudioSegment.cryptoId);
		}
		else
		{
			HLSSegmentCache.precache(seg.uri, seg.cryptoId, false, currentController.getStreamHandler(), getInterfaceThreadHandler());
			currentController.FeedSegment(seg.uri, seg.quality, seg.continuityEra, null, -1, seg.startTime, seg.cryptoId, -1);
		}


		return seg.startTime;
	}

	/**
	 * Internal helper. Creates a SurfaceView with proper parameters for display.
	 * This is needed for compatibility with older devices. When the surface is
	 * ready, SetSurface() is called back from the SurfaceView.
	 * 
	 * @param enablePushBuffers Use the PUSH_BUFFERS surface type?
	 * @param w Desired surface width.
	 * @param h Desired surface height.
	 * @param colf Desired color format.
	 */
	public static void enableHWRendererMode(boolean enablePushBuffers, int w,
			int h, int colf) {

		final boolean epb = enablePushBuffers;

		
		Log.i("PlayerViewController", "Initializing hw surface.");
		
		currentController.post(new Runnable() {
			@Override
			public void run() {
				currentController.SetSurface(null);
				
				if (currentController.mPlayerView != null) {
					currentController.removeView(currentController.mPlayerView);
				}
		
				@SuppressWarnings("deprecation")
				LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
						ViewGroup.LayoutParams.FILL_PARENT);
				lp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
				currentController.mPlayerView = new PlayerView(
						currentController.getContext(), currentController,
						epb);
				currentController.addView(currentController.mPlayerView, lp);
		
				Log.w("addComponents", "Surface Holder is " + currentController.mPlayerView.getHolder());
				if (currentController.mPlayerView.getHolder() != null)
					Log.w("addComponents", "Surface Holder is " + currentController.mPlayerView.getHolder().getSurface());
		
				// Preserve resolution info for layout.
				setVideoResolution(currentController.mVideoWidth, currentController.mVideoHeight);

			}
		});
		
	}

	/**
	 * Handle changes in the video resolution. Primarily for correct layout.
	 * @param w Actual width of video.
	 * @param h Actual height of video.
	 */
	public static void setVideoResolution(int w, int h) {
		final int ww = w;
		final int hh = h;
		if (currentController != null) 
		{
			currentController.post(new Runnable() {
				@Override
				public void run() {
					currentController.mVideoWidth = ww;
					currentController.mVideoHeight = hh;
					
					if(currentController.mPlayerView != null)
					{
						currentController.mPlayerView.mVideoWidth = ww;
						currentController.mPlayerView.mVideoHeight = hh;
						currentController.mPlayerView.requestLayout();
		
					}
			
					currentController.requestLayout();
				}
			});
		}
	}
	
	/**
	 *  Provides a method for the native code to notify us that a format change event has occurred
	 */
	public static void notifyAudioTrackChangeComplete(int audioTrack)
	{
		if (currentController != null)
		{
			currentController.postAudioTrackSwitchingEnd(audioTrack);
				
		}
	}
	
	/**
	 *  Provides a method for the native code to notify us that a format change event has occurred
	 */
	public static void notifyFormatChangeComplete(int qualityLevel)
	{
		if (currentController != null)
		{
			currentController.postQualityTrackSwitchingEnd(qualityLevel);
		}
	}
	
	/**
	 * Provides a method for the native code to notify us of errors
	 * 
	 * @param error - These are the codes from the OnErrorListener
	 * @param msg
	 */
	public static void postNativeError(int error, boolean fatal, String msg)
	{
		if (currentController != null)
		{
			if (!fatal)
				currentController.postError(error, msg);
			else
				currentController.postFatalError(error, msg);
		}
	}
	
	// HTTPResponse thread
	static class HTTPResponseThread extends HandlerThread
	{
		private Handler mHandler = null;
		
		HTTPResponseThread()
		{
			super("HTTPResponseThread");
			start();
			setHandler(new Handler(getLooper()));
		}

		public Handler getHandler() {
			return mHandler;
		}

		private void setHandler(Handler mHandler) {
			this.mHandler = mHandler;
		}
	}
	
	private HTTPResponseThread mHTTPResponseThread = null;
	
	public static HTTPResponseThread getHTTPResponseThread()
	{
		return currentController != null ? currentController.mHTTPResponseThread : null;
	}
	
	public static void postToHTTPResponseThread(Runnable runnable)
	{
		Handler handler = getHTTPResponseThreadHandler();
		if (handler != null)
		{
			handler.post(runnable);
		}
	}
	
	public static Handler getHTTPResponseThreadHandler()
	{
		return (getHTTPResponseThread() != null) ? getHTTPResponseThread().getHandler() : null;
	}
	
	
	
	// Interface thread
	static class InterfaceThread extends HandlerThread
	{
		private Handler mHandler = null;
		
		InterfaceThread()
		{
			super("InterfaceThread");
			start();
			setHandler(new Handler(getLooper()));
		}

		public Handler getHandler() {
			return mHandler;
		}

		private void setHandler(Handler mHandler) {
			this.mHandler = mHandler;
		}
	}
	
	private InterfaceThread mInterfaceThread = null;
	
	public static InterfaceThread getInterfaceThread()
	{
		return currentController != null ? currentController.mInterfaceThread : null;
	}
	
	public static void postToInterfaceThread(Runnable runnable)
	{
		Handler handler = getInterfaceThreadHandler();
		if (handler != null)
		{
			handler.post(runnable);
		}
	}
	
	public static Handler getInterfaceThreadHandler()
	{
		return (getInterfaceThread() != null) ? getInterfaceThread().getHandler() : null;
	}

	// Instance members.
	private PlayerView mPlayerView;

	// This is our root manifest
	private ManifestParser mManifest = null;
	private URLLoader manifestLoader;
	private StreamHandler mStreamHandler = null;
	private SubtitleHandler mSubtitleHandler = null;


	private OnPlayheadUpdateListener mPlayheadUpdateListener = null;
	private OnProgressListener mOnProgressListener = null;

	// Video state.
	public int mVideoWidth = 640, mVideoHeight = 480;
	private int mTimeMS = 0;
	
	// Restart details
	private String mLastUrl = "";
	private int mStartingMS = 0;
	private int mInitialPlayState = 0;
	private int mInitialQualityLevel = 0;
	private int mInitialAudioTrack = 0;
	private int mInitialSubtitleTrack = 0;
	private boolean mRestoringState = false;
	
	// Startup details
	private int mStartupState = STARTUP_STATE_WAITING_TO_START;

	// Thread to run video rendering.
	private boolean stopVideoThread = false;
	private int mRenderThreadState = THREAD_STATE_STOPPED;
	private Thread mRenderThread;
	private Runnable renderRunnable = new Runnable() {
		private int lastState = STATE_STOPPED;
		private int lastTimeStamp = -1;
		public void run() {
			mRenderThreadState = THREAD_STATE_RUNNING;
			while (mRenderThreadState == THREAD_STATE_RUNNING) {
				if (stopVideoThread)
				{
					Log.i("videoThread", "Stopping video render thread");
					mRenderThreadState = THREAD_STATE_STOPPED;
					continue;
				}
				int state = GetState();
				if (state == STATE_PLAYING || state == STATE_FOUND_DISCONTINUITY || state == STATE_WAITING_FOR_DATA) {
					int rval = NextFrame();
					if (rval >= 0) { mTimeMS = rval; Log.i("RunThread", "mTimeMS = " + mTimeMS); }
					if (rval < 0 && state != lastState)
					{
						Log.i("videoThread", "State Changed -- NextFrame() returned " + rval + " : state = " + 
								(state == STATE_PLAYING ? "STATE_PLAYING" : 
								(state == STATE_FOUND_DISCONTINUITY ? "STATE_FOUND_DISCONTINUITY" : 
								(state == STATE_WAITING_FOR_DATA ? "STATE_WAITING_FOR_DATA" : "UNKNOWN STATE"))));
					}
					lastState = state;
					if (rval == -1 && noMoreSegments) currentController.stop();
					if (rval == -1013) // INFO_DISCONTINUITY
					{
						Log.i("videoThread", "Ran into a discontinuity (INFO_DISCONTINUITY)");
						HandleFormatChange();
					}
					else
					{
						if (lastTimeStamp != mTimeMS)
						{
							postPlayheadUpdate(mTimeMS);
							lastTimeStamp = mTimeMS;
						}
					}

					// SUBTITLES!
					
					if (mSubtitleHandler != null)
					{
						double timeSecs = ( (double)mTimeMS / 1000.0);
						Vector<TextTrackCue> cues = mSubtitleHandler.update(timeSecs, mSubtitleLanguage);
						if (cues != null && mSubtitleTextListener != null)
						{
							for (int i = 0; i < cues.size(); ++i)
							{
								TextTrackCue cue = cues.get(i);
								postTextTrackText(cue.startTime, cue.endTime - cue.startTime, cue.lineAlignment, cue.text);
							}
						}
					}
					
					try {
						Thread.yield();
					} catch (Exception e) {
						Log.i("video run", "Video thread sleep interrupted!");
					}
					
					//Log.i("PlayerViewController", "Dropped Frames Per Sec: " + DroppedFramesPerSecond());

				} 
				else if (state == STATE_CUE_STOP)
				{
					stop();
				}
				else
				{
					try {
						Thread.sleep(30);
					} catch (InterruptedException ie) {
						Log.i("video run", "Video thread sleep interrupted!");
					}
				}

			}
			stopVideoThread = false;
		}
	};
	
	// Handle discontinuity/format change
	public void HandleFormatChange()
	{
		post(new Runnable()
			{
				public void run() {
					Log.i("HandleFormatChange", "UI Thread calling ApplyFormatChange()");
					ApplyFormatChange();
				}
			}
		);
	}

	public HLSPlayerViewController(Context context) {
		super(context);
	}

	public HLSPlayerViewController(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public HLSPlayerViewController(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
	}

	/**
	 * Load JNI libraries and set up the render thread.
	 */
	private void initializeNative() {
		try {
			System.loadLibrary("HLSPlayerSDK");
			InitNativeDecoder();
			mInterfaceThread = new InterfaceThread();
			mHTTPResponseThread = new HTTPResponseThread();
			

		} catch (Exception e) {
			Log.e("PlayerViewController", "Failed to initialize native video library.");
		}
		
		// Note the active controller.
		currentController = this;

	}

	/**
	 * Terminate render thread and shut down JNI resources.
	 */
	public void close() {
		Log.i("PlayerViewController", "Closing resources.");
		if (mRenderThread != null)
		{
			mRenderThread.interrupt();
			mRenderThread = null;
		}
		if (mInterfaceThread != null) 
		{ 
			mInterfaceThread.interrupt();
			mInterfaceThread = null;
		}
		if (mHTTPResponseThread != null)
		{
			mHTTPResponseThread.interrupt();
			mHTTPResponseThread = null;
		}
		CloseNativeDecoder();
		if (mStreamHandler != null)
		{
			mStreamHandler.close();
			mStreamHandler = null;
		}
		currentController = null;
		
	}
	
	@Override
	public void release() {
		
		SharedPreferences sp = getContext().getSharedPreferences("hlsplayersdk", Context.MODE_PRIVATE);
		Editor spe = sp.edit();
		spe.clear();
		spe.putString("lasturl", mLastUrl);
		spe.putInt("playstate", GetState());
		spe.putInt("startms", mStartingMS);
		spe.putInt("initialquality", mQualityLevel);
		spe.putInt("initialaudiotrack", mStreamHandler != null ? mStreamHandler.altAudioIndex : 0);
		spe.putInt("initialsubtitletrack", mSubtitleLanguage);
		spe.commit();
		
		close();
		
		// Serialize position, playstate, and url
	}
	@Override
	public void recoverRelease() {
		// Deserialize postion, playstate, and url
		SharedPreferences sp = getContext().getSharedPreferences("hlsplayersdk", Context.MODE_PRIVATE);
		mLastUrl = sp.getString("lasturl", "");
		mInitialPlayState = sp.getInt("playstate", 0);
		mStartingMS = sp.getInt("startms", 0);
		mInitialQualityLevel = sp.getInt("initialquality", 0);
		mInitialAudioTrack = sp.getInt("initialaudiotrack", 0);
		mInitialSubtitleTrack = sp.getInt("initialsubtitletrack", 0);
		
		if (mStartingMS > 0)
			mRestoringState = true;
		
		if (mRestoringState)
		{
			// we need to resume, somehow
			post(new Runnable()
			{
				@Override
				public void run() {
					
					Log.i("VideoPlayer UI", " -----> Play " + mLastUrl);
		            setVideoUrl(mLastUrl);
		        	setVisibility(View.VISIBLE);
		        	play();
				}
				
			});
		}
		
	}




	
	/**
	 *  Reset any state that we have
	 */
	public void reset()
	{
		mTimeMS = 0;
		HLSSegmentCache.resetProgress();
	}

	/**
	 * Called when the manifest parser is complete. Once this is done, play can
	 * actually start.
	 */
	public void onParserComplete(ManifestParser parser) {
		if (parser == null || parser.hasSegments() == false)
		{
			Log.w("PlayerViewController", "Manifest is null. Ending playback.");
			postFatalError(OnErrorListener.MEDIA_ERROR_NOT_VALID, "No Valid Manifest");
			return;
		}
		
		// If we're not on the currently requested video play - bail out. No need to start anything up.
		if (parser.videoPlayId != videoPlayId) return;
		
		noMoreSegments = false;
		Log.i(this.getClass().getName() + ".onParserComplete", "Entered");
		mStreamHandler = new StreamHandler(parser);
		mSubtitleHandler = new SubtitleHandler(parser);
		
		double startTime = StreamHandler.USE_DEFAULT_START; // This is a trigger to let getFileForTime know to start a live stream 
		int subtitleIndex = 0;
		int qualityLevel = 0;
		int textTrackIndex = mSubtitleHandler.hasSubtitles() ? mSubtitleHandler.getDefaultLanguageIndex() : 0;
		if (mRestoringState)
		{
			getStreamHandler().setAltAudioTrack(mAltAudioIndex);
			startTime = mStreamHandler.streamEnds() ? (double)mStartingMS / 1000.0 : StreamHandler.USE_DEFAULT_START; // Always go to the end on a live stream, even when we resume
			qualityLevel = mInitialQualityLevel;
			textTrackIndex = mInitialSubtitleTrack;
		}
		
		ManifestSegment seg = getStreamHandler().getFileForTime(startTime, qualityLevel);
		if (seg == null)
		{
			postError(OnErrorListener.MEDIA_ERROR_NOT_VALID, "Manifest is not valid. There aren't any segments.");
			Log.w("PlayerViewController", "Manifest is not valid. There aren't any segments. Ending playback.");
			return;
		}
		
		if (startTime == StreamHandler.USE_DEFAULT_START) startTime = seg.startTime; // If our time isn't set for us (ie. on a resume), 
																					 // we'll be starting at the front of the segment, 
																					 // so use that time when feeding the segment and
																					 // getting text tracks.
		
	
		if (mSubtitleHandler.hasSubtitles())
		{
			postTextTracksList(mSubtitleHandler.getLanguageList(), textTrackIndex);
			
			mSubtitleLanguage = textTrackIndex;
			mSubtitleHandler.precacheSegmentAtTime(startTime, mSubtitleLanguage );
			postTextTrackChanged(mSubtitleLanguage);
		}
		else
		{
			mSubtitleHandler = null;
			postTextTracksList(new ArrayList<String>(), -1);
			
		}
		
		if (mStreamHandler.hasAltAudio())
		{
			postAudioTracksList(mStreamHandler.getAltAudioLanguageList(), mStreamHandler.getAltAudioDefaultIndex());
		}
		else
		{
			postAudioTracksList(new ArrayList<String>(), -1);
		}
		
		postQualityTracksList(mStreamHandler.getQualityTrackList(), 0);
		

		if (seg.altAudioSegment != null)
		{
			// We need to feed the segment before calling precache so that the datasource can be initialized before we
			// supply the event handler to the segment cache. In the case where the segment is already in the cache, the
			// event handler can be called immediately.
			FeedSegment(seg.uri, seg.quality, seg.continuityEra, seg.altAudioSegment.uri, seg.altAudioSegment.altAudioIndex, seg.startTime, seg.cryptoId, seg.altAudioSegment.cryptoId);
			HLSSegmentCache.precache(new String[] {seg.uri, seg.altAudioSegment.uri}, new int [] { seg.cryptoId, seg.altAudioSegment.cryptoId }, true, this, getInterfaceThreadHandler());
			postAudioTrackSwitchingStart(-1, seg.altAudioSegment.altAudioIndex);
			postAudioTrackSwitchingEnd(seg.altAudioSegment.altAudioIndex);
		}
		else
		{
			// We need to feed the segment before calling precache so that the datasource can be initialized before we
			// supply the event handler to the segment cache. In the case where the segment is already in the cache, the
			// event handler can be called immediately.
			FeedSegment(seg.uri, seg.quality, seg.continuityEra, null, -1, seg.startTime, seg.cryptoId, -1);
			HLSSegmentCache.precache(seg.uri, seg.cryptoId, true, this, getInterfaceThreadHandler());
		}
		
		// Kick off render thread.
		if (mRenderThreadState == THREAD_STATE_STOPPED);
		{
			mRenderThread = new Thread(renderRunnable, "RenderThread");
			mRenderThread.start();
		}
		
		postDurationChanged();
		
	}
	
	@Override
	public void onSegmentCompleted(String uri) {
		HLSSegmentCache.cancelCacheEvent(uri);
		postPlayerStateChange(PlayerStates.START);
		
		if (mStartupState == STARTUP_STATE_PLAY_QUEUED)
			initiatePlay();
		else
			setStartupState(STARTUP_STATE_LOADED);
	}

	@Override
	public void onSegmentFailed(String uri, int responseCode) {

		HLSSegmentCache.cancelCacheEvent(uri);
		setStartupState(STARTUP_STATE_WAITING_TO_START);
		
	}

	@Override
	public void onDownloadComplete(URLLoader loader, String response) {
		if (loader.videoPlayId != videoPlayId) return;
		if (mManifest != null)
		{
			Log.e("PlayerViewController.onDownloadComplete", "Manifest is not NULL! Killing the old one and starting a new one.");
			mManifest.setOnParseCompleteListener(null);
			mManifest = null;
		}
		mManifest = new ManifestParser();
		mManifest.setOnParseCompleteListener(this, loader.videoPlayId);
		mManifest.parse(response, loader.getRequestURI().toString());
	}

	public void onDownloadFailed(URLLoader loader, String response) {
		Log.i("PlayerViewController", "Download failed: " + response);
		postFatalError(OnErrorListener.MEDIA_ERROR_IO, loader.uri + " (" + response + ")");
	}

	protected StreamHandler getStreamHandler() {
		return mStreamHandler;
	}

	public void setOnFullScreenListener(OnToggleFullScreenListener listener) {

	}

	public boolean getIsPlaying() {
		return GetState() == STATE_PLAYING;
	}

	public void initialize() {
		//mActivity = activity;
		setBackgroundColor(0xFF000000);
		initializeNative();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		Log.i("PlayerViewController.onSizeChanged", "Set size to " + w + "x" + h);
	}

	public void destroy() 
	{
		Log.i("PlayerViewController", "Destroying...");

		if (mPlayerView == null)
			return;

		stop();
		close();
	}

	public void incrementQuality()
	{
		switchQualityTrack(mQualityLevel + 1); 
	}
	
	public void decrementQuality()
	{
		switchQualityTrack(mQualityLevel - 1); 
	}

	// /////////////////////////////////////////////////////////////////////////////////////////////
	// VideoPlayerInterface methods
	// /////////////////////////////////////////////////////////////////////////////////////////////
	public boolean isPlaying() {
		return getIsPlaying();
	}

	public int getDuration() {
		if (mStreamHandler != null)
			return mStreamHandler.getDuration();
		return -1;
	}

	public String getVideoUrl() {
		return "Not Implemented";
	}
	
	private void initiatePlay()
	{
		setStartupState(STARTUP_STATE_STARTED);
		mStreamHandler.initialize();
		PlayFile(((double)mStartingMS) / 1000.0f);
		postPlayerStateChange(PlayerStates.PLAY);

		if (mRestoringState)
		{
			if (this.mInitialPlayState == STATE_PAUSED)
			{
				pause();
			}
			
			// Reset so that the next video doesn't start in the middle
			mStartingMS = 0; 
			mRestoringState = false;
		}

		
		
	}

	public void play() {
		int state = GetState();
		if (state == STATE_PAUSED)
		{
			postToInterfaceThread(new Runnable() {
				public void run()
				{
					Pause(false);
					int state = GetState();
					if (state == STATE_PAUSED) postPlayerStateChange(PlayerStates.PAUSE);
					else if (state == STATE_PLAYING) postPlayerStateChange(PlayerStates.PLAY);
				}
			});
			return;			
		}
		
		
		if (mStartupState == STARTUP_STATE_LOADED)
			initiatePlay();
		else if (mStartupState != STARTUP_STATE_STARTED)
			setStartupState(STARTUP_STATE_PLAY_QUEUED);
	}

	public void pause() {
		postToInterfaceThread(new Runnable() {
			public void run()
			{
				Pause(true);
				int state = GetState();
				if (state == STATE_PAUSED) postPlayerStateChange(PlayerStates.PAUSE);
				else if (state == STATE_PLAYING) postPlayerStateChange(PlayerStates.PLAY);
			}
		});
	}

	public void stop() {
		HLSSegmentCache.cancelDownloads();
		if (mStreamHandler != null) mStreamHandler.stopReloads();
		StopPlayer();
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		postPlayerStateChange(PlayerStates.END);
	}

	public int getCurrentPosition() {
		return mTimeMS;
	}

	private int targetSeekMS = 0;
	private boolean targetSeekSet = false;
	
	public void goToLive()
	{
		seek(StreamHandler.USE_DEFAULT_START, false);
	}
	
	/***
	 * seekToCurrentPosition()
	 * 
	 * It's pretty obvious what it does. However, why it exists
	 * probably isn't clear.
	 * 
	 * When we switch between backup streams, we need to seek to the
	 * last known position instead of waiting for the player to just run
	 * into the new segment, due to the chance that the new segment from
	 * the alternate stream might not match exactly with the original stream.
	 * 
	 * Unfortunately, because backup streams are of the same quality level as
	 * the primary stream, the native player won't notice a discontinuity, and
	 * won't reset everything for the new stream unless we do the seek.
	 * 
	 */
	public void seekToCurrentPosition()
	{
		seek(mTimeMS, false);
	}
	
	public void seek(final int msec)
	{
		seek(msec, true);
	}
	
	public void seek(final int msec, final boolean notify) {
		HLSSegmentCache.cancelAllCacheEvents();
		
		targetSeekSet = true;
		targetSeekMS = msec;	
				
		postToInterfaceThread( new Runnable() {
			public void run()
			{
				boolean tss = targetSeekSet;
				int tsms = targetSeekMS;
				int state = GetState();
				if (tss && state != STATE_STOPPED)
				{
					if (notify) postPlayerStateChange(PlayerStates.SEEKING);
					targetSeekSet = false;
					targetSeekMS = 0;
					if (tsms != StreamHandler.USE_DEFAULT_START)
						SeekTo(((double)tsms) / 1000.0f);
					else
						SeekTo((double)tsms);
					if (notify) postPlayerStateChange(PlayerStates.SEEKED);
				}
				else if (tss && state == STATE_STOPPED && mRenderThreadState == THREAD_STATE_RUNNING)
				{
					Log.i("PlayerViewController.Seek().Runnable()", "Seeking while player is stopped.");
					mStreamHandler.initialize(); // Need to restart the reload manifest process
					if (notify) postPlayerStateChange(PlayerStates.SEEKING);
					targetSeekSet = false;
					targetSeekMS = 0;
					
					if (tsms != StreamHandler.USE_DEFAULT_START)
						SeekTo(((double)tsms) / 1000.0f);
					else
						SeekTo((double)tsms);
					if (notify) postPlayerStateChange(PlayerStates.SEEKED);
				}
				else
				{
					Log.i("PlayerViewController.Seek().Runnable()", "No More Seeks Queued");
				}
			}
		});
	}

	// Helper to check network status.
	public boolean isOnline() {
	    ConnectivityManager connMgr = (ConnectivityManager) 
	            getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = null;
	    try
	    {
	    	networkInfo = connMgr.getActiveNetworkInfo();
	    }
	    catch (Exception e)
	    {
	    	Log.i("PlayerViewController.isOnline()", e.toString());
	    	Log.i("PlayerViewController.isOnline()", "This is possibly because the permission 'android.permission.ACCESS_NETWORK_STATE' is missing from the manifest.");
	    }
	    return (networkInfo != null && networkInfo.isConnected());
	}
	
	public void stopAndReset()
	{
		if (mRenderThreadState == THREAD_STATE_RUNNING)
			stopVideoThread = true;
		try {
			if (mRenderThread != null) mRenderThread.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		StopPlayer();
		ResetPlayer();
		if (mStreamHandler != null)
		{
			mStreamHandler.close();
			mStreamHandler = null;
		}
		reset();
		try {
			Thread.yield();
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String getStartupStateText(int state)
	{
		switch (state)
		{
		case STARTUP_STATE_STARTED:
			return "STARTUP_STATE_STARTED";
		case STARTUP_STATE_LOADING:
			return "STARTUP_STATE_LOADING";
		case STARTUP_STATE_LOADED:
			return "STARTUP_STATE_LOADED";
		case STARTUP_STATE_PLAY_QUEUED:
			return "STARTUP_STATE_PLAY_QUEUED";
		case STARTUP_STATE_WAITING_TO_START:
			return "STARTUP_STATE_WATING_TO_START";
		}
		return "Unknown";
	}
	
	public void setStartupState(int newState)
	{
		Log.i("PlayerViewController.setStartupState", "Old State=" + getStartupStateText(mStartupState) + " New State=" + getStartupStateText(newState));
		mStartupState = newState;
	}
	
	public void setVideoUrl(String url) {
		Log.i("PlayerView.setVideoUrl", url);
		if (manifestLoader != null)
		{
			manifestLoader.setDownloadEventListener(null);
			manifestLoader = null;
		}
		if (mManifest != null)
		{
			Log.i("PlayerViewController.setVideoURL", "Manifest is not NULL. Killing the old one and starting a new one.");
			mManifest.setOnParseCompleteListener(null);
			mManifest = null;
		}


		HLSSegmentCache.cancelAllCacheEvents();
		HLSSegmentCache.cancelDownloads();
		targetSeekMS = 0;
		targetSeekSet = false;
		stopAndReset();
		
		mLastUrl = url;

		postPlayerStateChange(PlayerStates.LOAD);

		// Confirm network is ready to go.
		if(!isOnline())
		{
			Toast.makeText(getContext(), "Not connnected to network; video may not play.", Toast.LENGTH_LONG).show();
		}
		

		setStartupState(STARTUP_STATE_LOADING);
		

		// Incrementing the videoPlayId. This will keep us from starting videos delayed
		// by slow manifest downloads when the user tries to start a new video (meaning
		// that we'll only start the latest request once the parsers are finished).
		++videoPlayId;
		
		// Init loading.
		manifestLoader = new URLLoader("HLSPlayerViewController.setVideoUrl", this, null, videoPlayId);
		manifestLoader.get(url);
	}


	private OnPlayerStateChangeListener mPlayerStateChangeListener = null;
	
	@Override
	public void registerPlayerStateChange(OnPlayerStateChangeListener listener) {
		mPlayerStateChangeListener = listener;
	}

	private void postPlayerStateChange(final PlayerStates state)
	{
		if (mPlayerStateChangeListener != null)
		{
			post(new Runnable()
			{

				@Override
				public void run() {
					mPlayerStateChangeListener.onStateChanged(state);					
				}
				
			});
		}
	}
	
	private OnDurationChangedListener mDurationChangedListener = null;
	
	@Override
	public void registerDurationChanged(OnDurationChangedListener listener)
	{
		mDurationChangedListener = listener;		
	}
	
	public void postDurationChanged()
	{
		if (mDurationChangedListener != null)
		{
			post( new Runnable()
			{
				@Override
				public void run() {
					mDurationChangedListener.onDurationChanged(getDuration());
				}
			} );
		}
	}
	
	@Override
	public void registerPlayheadUpdate(OnPlayheadUpdateListener listener) {
		mPlayheadUpdateListener = listener;
	}
	
	private void postPlayheadUpdate(final int msec)
	{
		if (mPlayheadUpdateListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mPlayheadUpdateListener.onPlayheadUpdated(msec);					
				}
				
			});
		}
	}

	
	@Override
	public void registerProgressUpdate(OnProgressListener listener) {
		mOnProgressListener = listener;

	}
	public void postProgressUpdate(final int progress)
	{
		if (mOnProgressListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnProgressListener.onProgressUpdate(progress);					
				}
				
			});
		}
	}
	
	OnErrorListener mErrorListener = null;
	@Override
	public void registerError(OnErrorListener listener) {
		mErrorListener = listener;
		
	}
	
	public void postFatalError(final int errorCode, final String errorMessage)
	{
		Log.e("HLSPlayerSDK.FatalError", "(" + errorCode + ")" + errorMessage);
		if (mErrorListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mErrorListener.onFatalError(errorCode, errorMessage);
				}
			});
		}
	}
	
	public void postError(final int errorCode, final String errorMessage)
	{
		Log.e("HLSPlayerSDK.Error", "(" + errorCode + ")" + errorMessage);
		if (mErrorListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mErrorListener.onError(errorCode, errorMessage);
				}
			});
		}
	}
	
	@Override
	public void removePlayheadUpdateListener() {
		if (mPlayheadUpdateListener != null)
			mPlayheadUpdateListener = null;
		
	}
	
	@Override
	public void setStartingPoint(int point) {
		mStartingMS = point;
	}
	
	//////////////////////////////////////////////////////////
	// Subtitle interface
	//////////////////////////////////////////////////////////
	private OnTextTracksListListener mOnTextTracksListListener = null;
	private OnTextTrackChangeListener mOnTextTrackChangeListener = null;
	private OnTextTrackTextListener mSubtitleTextListener = null;

	public void registerTextTrackText(OnTextTrackTextListener listener)
	{
		mSubtitleTextListener = listener;
	}
	private void postTextTrackText(final double startTime, final double length, final String align, final String buffer)
	{
		if (mSubtitleTextListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mSubtitleTextListener.onSubtitleText(startTime, length, align, buffer);					
				}
			});
		}
	}
	
	@Override
	public void switchTextTrack(int newIndex) {
		if (mSubtitleHandler != null && newIndex < mSubtitleHandler.getLanguageCount())
		{
			mSubtitleLanguage = newIndex;
			postTextTrackChanged(mSubtitleLanguage);
		}
		
	}

	@Override
	public void registerTextTracksList(OnTextTracksListListener listener) {
		mOnTextTracksListListener = listener;		
	}
	private void postTextTracksList(final List<String> list, final int defaultTrackIndex)
	{
		if (mOnTextTracksListListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnTextTracksListListener.OnTextTracksList(list, defaultTrackIndex);					
				}				
			});
		}
	}
	
	@Override
	public void registerTextTrackChanged(OnTextTrackChangeListener listener) {
		mOnTextTrackChangeListener = listener;
	}
	private void postTextTrackChanged(final int newTrackIndex )
	{
		if (mOnTextTrackChangeListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnTextTrackChangeListener.onOnTextTrackChanged(newTrackIndex);					
				}				
			});
		}
	}
	
	//////////////////////////////////////////////////////////
	// Alternate Audio interface
	//////////////////////////////////////////////////////////
	
	@Override
	public void hardSwitchAudioTrack(int newAudioIndex) {
		if (getStreamHandler() == null)
		{
			postError(OnErrorListener.MEDIA_ERROR_NOT_VALID, "The media is not yet ready.");
			return; // We haven't started yet
		}
		
		final int newIndex = newAudioIndex;
		
		postToInterfaceThread(new Runnable() {
			public void run()
			{
				postAudioTrackSwitchingStart( getStreamHandler().getAltAudioCurrentIndex(), newIndex);
				
				boolean success = getStreamHandler().setAltAudioTrack(newIndex);
				
				if (!success) return; // Don't bother trying to change when there's nothing to change to
				
				while (getStreamHandler() != null && getStreamHandler().waitingForAudioReload)
				{
					try
					{
						Thread.sleep(30);
					}
					catch (InterruptedException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				if (getStreamHandler() == null) return; // don't seek if the app has exited while waiting for reload.
				
				seekToCurrentPosition();

				postAudioTrackSwitchingEnd( getStreamHandler().getAltAudioCurrentIndex());
			}
		});
		
	}
	
	@Override
	public void softSwitchAudioTrack(int newAudioIndex) {
		if (getStreamHandler() == null)
		{
			postError(OnErrorListener.MEDIA_ERROR_NOT_VALID, "The media is not yet ready.");
			return; // We haven't started yet
		}

		postAudioTrackSwitchingStart( getStreamHandler().getAltAudioCurrentIndex(), newAudioIndex);
		
		boolean success = getStreamHandler().setAltAudioTrack(newAudioIndex); 

		postAudioTrackSwitchingEnd( getStreamHandler().getAltAudioCurrentIndex());
	}
	
	private OnAudioTracksListListener mOnAudioTracksListListener = null;
	@Override
	public void registerAudioTracksList(OnAudioTracksListListener listener) {
		mOnAudioTracksListListener = listener;
	}
	private void postAudioTracksList(final  List<String> list, final int defaultTrackIndex  )
	{
		if (mOnAudioTracksListListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnAudioTracksListListener.OnAudioTracksList(list, defaultTrackIndex);					
				}				
			});
		}
	}
	
	private OnAudioTrackSwitchingListener mOnAudioTrackSwitchingListener = null;
	@Override
	public void registerAudioSwitchingChange( OnAudioTrackSwitchingListener listener) {
		mOnAudioTrackSwitchingListener = listener;
	}
	
	private void postAudioTrackSwitchingStart(final  int oldTrackIndex, final int newTrackIndex  )
	{
		if (mOnAudioTrackSwitchingListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnAudioTrackSwitchingListener.onAudioSwitchingStart(oldTrackIndex, newTrackIndex);					
				}				
			});
		}
	}
	
	private void postAudioTrackSwitchingEnd(final int newTrackIndex  )
	{
		if (mOnAudioTrackSwitchingListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnAudioTrackSwitchingListener.onAudioSwitchingEnd(newTrackIndex);					
				}				
			});
		}
	}


	//////////////////////////////////////////////////////////
	// Quality Change interface
	//////////////////////////////////////////////////////////

	@Override
	public void switchQualityTrack(int newIndex) {
		if (mStreamHandler != null)
		{
			int ql = mStreamHandler.getQualityLevels();
			if (newIndex >= 0 && newIndex < ql -1)
			{
				postQualityTrackSwitchingStart(mQualityLevel, newIndex);
				mQualityLevel = newIndex;
			}
			else
			{
				postQualityTrackSwitchingEnd(mQualityLevel);
			}
		}
	}
	@Override
	public void setAutoSwitch(boolean autoSwitch) {
		postError(OnErrorListener.MEDIA_ERROR_UNSUPPORTED, "setAutoSwitch");
		
	}
	
	private OnQualityTracksListListener mOnQualityTracksListListener = null;
	@Override
	public void registerQualityTracksList(OnQualityTracksListListener listener) {
		mOnQualityTracksListListener = listener;
	}
	
	private void postQualityTracksList(final  List<QualityTrack> list, final int defaultTrackIndex  )
	{
		if (mOnQualityTracksListListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnQualityTracksListListener.OnQualityTracksList(list, defaultTrackIndex);					
				}				
			});
		}
	}
	
	
	private OnQualitySwitchingListener mOnQualitySwitchingListener = null;
	@Override
	public void registerQualitySwitchingChange( OnQualitySwitchingListener listener) {
		mOnQualitySwitchingListener = listener;		
	}
	
	private void postQualityTrackSwitchingStart(final  int oldTrackIndex, final int newTrackIndex  )
	{
		if (mOnQualitySwitchingListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnQualitySwitchingListener.onQualitySwitchingStart(oldTrackIndex, newTrackIndex);					
				}				
			});
		}
	}
	
	private void postQualityTrackSwitchingEnd(final int newTrackIndex  )
	{
		if (mOnQualitySwitchingListener != null)
		{
			post(new Runnable()
			{
				@Override
				public void run() {
					mOnQualitySwitchingListener.onQualitySwitchingEnd(newTrackIndex);					
				}				
			});
		}
	}
	
	private int mTimeToBuffer = 10;
	private int SetSegmentsToBuffer()
	{
		ManifestParser m = mStreamHandler.getManifestForQuality(mQualityLevel);
		int segments = 1;
		if (m != null)
		{
			segments = mTimeToBuffer / (int)m.targetDuration;
			int rem = mTimeToBuffer % (int)m.targetDuration;
			if (rem != 0) ++segments;
			if (segments > 1) segments = 1;
			SetSegmentCountToBuffer(segments);
			
		}
		else
			SetSegmentCountToBuffer(segments);
		
		return segments;
	}
	
	@Override
	public void setBufferTime(int newTime) {
		mTimeToBuffer = newTime;
		if (mStreamHandler != null && mStreamHandler.manifest != null)
		{
			SetSegmentsToBuffer();
		}
	}
	@Override
	public float getLastDownloadTransferRate() {
		return (float)HLSSegmentCache.lastDownloadDataRate;
	}
	@Override
	public float getDroppedFramesPerSecond() {
		return DroppedFramesPerSecond();
	}
	@Override
	public float getBufferPercentage() {
		return HLSSegmentCache.lastBufferPct;
	}
	@Override
	public int getCurrentQualityIndex() {
		if (mStreamHandler != null) return mStreamHandler.lastQuality;
		return 0;
	}

}
