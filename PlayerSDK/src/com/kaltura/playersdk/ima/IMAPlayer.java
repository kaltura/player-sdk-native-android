package com.kaltura.playersdk.ima;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.SimpleVideoPlayer;
import com.google.android.libraries.mediaframework.layeredvideo.Util;
import com.kaltura.playersdk.VideoPlayerInterface;
import com.kaltura.playersdk.events.KPlayerEventListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;

public class IMAPlayer extends RelativeLayout implements VideoPlayerInterface {

	public static int PLAYHEAD_UPDATE_INTERVAL = 200;

	private OnPlayerStateChangeListener mPlayerStateListener;
	private OnPlayheadUpdateListener mPlayheadUpdateListener;
	private OnProgressListener mProgressListener;
	private KPlayerEventListener mKPlayerEventListener;
	/**
	 * Responsible for requesting the ad and creating the
	 * {@link com.google.ads.interactivemedia.v3.api.AdsManager}
	 */
	private AdsLoader adsLoader;


	/**
	 * Responsible for containing listeners for processing the elements of the ad.
	 */
	private AdsManager adsManager;

	/**
	 * URL of the ad
	 */
	private Uri mAdTagUrl;

	/**
	 * These callbacks are notified when the video is played and when it ends. The IMA SDK uses this
	 * to poll for video progress and when to stop the ad.
	 */
	private List<VideoAdPlayer.VideoAdPlayerCallback> callbacks;

	FrameLayout adPlayerContainer;
	FrameLayout adUiContainer;


	private VideoPlayerInterface mContentPlayer;
	private SimpleVideoPlayer mAdPlayer;
	private boolean isInSequence;
	private Activity mActivity;

	private Handler mHandler;
	private JSONObject mTimeRemainingObj = new JSONObject();
	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if ( isInSequence && mKPlayerEventListener!=null ) {
				try {
					mTimeRemainingObj.put("time", videoAdPlayer.getProgress().getCurrentTime() );
					mTimeRemainingObj.put("duration", videoAdPlayer.getProgress().getDuration() );
					float remain = videoAdPlayer.getProgress().getDuration() - videoAdPlayer.getProgress().getCurrentTime();
					mTimeRemainingObj.put("remain", remain );
					mKPlayerEventListener.onKPlayerEvent( new Object[]{"adRemainingTimeChange", mTimeRemainingObj} );

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.d(this.getClass().getSimpleName(), "failed to send adRemainingTimeChange!");
				}

			}
			mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
		}
	};


	public IMAPlayer(Context context) {
		super(context);
	}

	public IMAPlayer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public IMAPlayer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}


	public void setParams( VideoPlayerInterface contentPlayer, String adTagUrl, Activity activity, KPlayerEventListener listener ) {
		mActivity = activity;
		mKPlayerEventListener = listener;
		mContentPlayer = contentPlayer;
		mAdTagUrl = Uri.parse(adTagUrl);
		//		mAdTagUrl = Uri.parse("http://pubads.g.doubleclick.net/gampad/ads?sz=400x300&iu=%2F6062%2Fgmf_demo&ciu_szs&impl=s&gdfp_req=1&env=vp&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&correlator=[timestamp]&cust_params=gmf_format%3Dskip");
		isInSequence = false;

		if ( contentPlayer instanceof View ) {
			addView( (View) contentPlayer );
		}

		adsLoader = ImaSdkFactory.getInstance().createAdsLoader(activity, ImaSdkFactory.getInstance().createImaSdkSettings());
		AdListener adListener = new AdListener();
		adsLoader.addAdErrorListener(adListener);
		adsLoader.addAdsLoadedListener(adListener);


		callbacks = new ArrayList<VideoAdPlayer.VideoAdPlayerCallback>();

	}

	public void setAdTagUrl( String adTagUrl ) {
		mAdTagUrl = Uri.parse(adTagUrl);
	}

	@Override
	public String getVideoUrl() {
		if ( mContentPlayer!= null ) {
			return mContentPlayer.getVideoUrl();
		}

		return null;
	}

	@Override
	public void setVideoUrl(String url) {
		if ( mContentPlayer != null ) {
			mContentPlayer.setVideoUrl( url );
		}

	}

	@Override
	public int getDuration() {
		if ( mContentPlayer!=null ) {
			if ( isInSequence ) {
				return mAdPlayer.getDuration();
			} else {
				return mContentPlayer.getDuration();
			}
		}

		return 0;
	}

	@Override
	public boolean getIsPlaying() {
		return isPlaying();
	}

	@Override
	public void play() {
		if (mAdTagUrl != null) {
			requestAd();
		} else {
			mContentPlayer.play();
		}

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void seek(int msec) {
		//TODO
		/*	if ( isInSequence ) {
			mAdPlayer.seekTo( msec );
		} else {
			mContentPlayer.seek(msec);
		}*/

	}

	@Override
	public boolean isPlaying() {
		if ( isInSequence )
			return true;
		if ( mContentPlayer!= null ) {
			return ( mContentPlayer.isPlaying() );
		}
		return false;

	}

	@Override
	public boolean canPause() {
		if ( mContentPlayer!= null ) {
			if ( isInSequence ) {
				return false;
			} else {
				return mContentPlayer.canPause();
			}
		}

		return false;		
	}


	@Override
	public void registerPlayerStateChange( OnPlayerStateChangeListener listener) {
		mPlayerStateListener = listener;
		if ( !isInSequence && mContentPlayer!=null ) {
			mContentPlayer.registerPlayerStateChange ( listener );
		}
	}

	@Override
	public void registerReadyToPlay( MediaPlayer.OnPreparedListener listener) {
		//TODO
	}

	@Override
	public void registerError( MediaPlayer.OnErrorListener listener) {
		//TODO

	}

	@Override
	public void registerPlayheadUpdate( OnPlayheadUpdateListener listener ) {
		mPlayheadUpdateListener = listener;
		if ( !isInSequence && mContentPlayer!=null ) {
			mContentPlayer.registerPlayheadUpdate ( listener );
		}
	}

	@Override
	public void removePlayheadUpdateListener() {
		mPlayheadUpdateListener = null;
		if ( !isInSequence && mContentPlayer!=null ) {
			mContentPlayer.removePlayheadUpdateListener ();
		}
	}

	@Override
	public void registerProgressUpdate ( OnProgressListener listener ) {
		mProgressListener = listener;
		if ( !isInSequence && mContentPlayer!=null ) {
			mContentPlayer.registerProgressUpdate ( listener );
		}
	}

	private void hideContentPlayer() {
		if ( !isInSequence ) {
			isInSequence = true;
			if ( mContentPlayer.canPause() )
				mContentPlayer.pause();
			if ( mContentPlayer instanceof View )
				( (View) mContentPlayer ).setVisibility(View.GONE);

			//unregister events
			mContentPlayer.registerPlayerStateChange( null );
			mContentPlayer.registerPlayheadUpdate( null );
			mContentPlayer.registerProgressUpdate( null );
		} 
	}

	private void showContentPlayer() {
		if ( isInSequence ) {
			isInSequence = false;
			destroyAdPlayer();

			if ( mContentPlayer instanceof View )
				addView( (View) mContentPlayer );

			//register events
			mContentPlayer.registerPlayerStateChange( mPlayerStateListener );
			mContentPlayer.registerPlayheadUpdate( mPlayheadUpdateListener );
			mContentPlayer.registerProgressUpdate( mProgressListener );

			mContentPlayer.play();

		} 
	}

	/**
	 * Destroy the {@link SimpleVideoPlayer} responsible for playing the ad and rmeove it.
	 */
	private void destroyAdPlayer(){

		if(adPlayerContainer != null){
			removeView(adPlayerContainer);
		}
		if (adUiContainer != null) {
			removeView(adUiContainer);
		}
		if(mAdPlayer != null){
			mAdPlayer.release();
		}
		adPlayerContainer = null;
		mAdPlayer = null;

	}

	private void createAdPlayer(){
		destroyAdPlayer();

		adPlayerContainer = new FrameLayout(mActivity);
		addView(adPlayerContainer);

		Video adVideo = new Video(mAdTagUrl.toString(), Video.VideoType.MP4);
		mAdPlayer = new SimpleVideoPlayer(mActivity,
				adPlayerContainer,
				adVideo,
				"",
				true);

		mAdPlayer.addPlaybackListener(playbackListener);

		// Move the ad player's surface layer to the foreground so that it is overlaid on the content
		// player's surface layer (which is in the background).
		mAdPlayer.moveSurfaceToForeground();
		mAdPlayer.play();
		mAdPlayer.disableSeeking();
		//  mAdPlayer.setSeekbarColor(Color.YELLOW);
		mAdPlayer.hideTopChrome();
		//TODO? mAdPlayer.setFullscreen(contentPlayer.isFullscreen());


		// Move the ad player's surface layer to the foreground so that it is overlaid on the content
		// player's surface layer (which is in the background).
		isInSequence = true;

		adPlayerContainer.setLayoutParams(Util.getLayoutParamsBasedOnParent(
				adPlayerContainer,
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
				));


		if ( adUiContainer.getParent() != null ) {
			removeView ( adUiContainer );			
		}
		addView ( adUiContainer );

		// Notify the callbacks that the ad has begun playing.
		/*for (VideoAdPlayer.VideoAdPlayerCallback callback : callbacks) {
			callback.onPlay();
		}*/
	}

	/**
	 * Create an ads request which will request the VAST document with the given ad tag URL.
	 * @param tagUrl URL pointing to a VAST document of an ad.
	 * @return a request for the VAST document.
	 */
	private AdsRequest buildAdsRequest(String tagUrl) {
		// Create the ad adDisplayContainer UI which will be used by the IMA SDK to overlay ad controls.
		adUiContainer = new FrameLayout(mActivity);
		addView(adUiContainer);

		adUiContainer.setLayoutParams(Util.getLayoutParamsBasedOnParent(
				adUiContainer,
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		AdDisplayContainer adDisplayContainer = ImaSdkFactory.getInstance().createAdDisplayContainer();
		adDisplayContainer.setPlayer(videoAdPlayer);
		adDisplayContainer.setAdContainer(adUiContainer);
		AdsRequest request = ImaSdkFactory.getInstance().createAdsRequest();
		request.setAdTagUrl(tagUrl);

		request.setAdDisplayContainer(adDisplayContainer);
		return request;
	}

	/**
	 * Make the ads loader request an ad with the ad tag URL which this {@link ImaPlayer} was
	 * created with
	 */
	private void requestAd() {
		adsLoader.requestAds(buildAdsRequest(mAdTagUrl.toString()));
	}



	/**
	 * Handles loading, playing, retrieving progress, pausing, resuming, and stopping ad.
	 */
	private final VideoAdPlayer videoAdPlayer = new VideoAdPlayer() {
		@Override
		public void playAd() {
			hideContentPlayer();
		}

		@Override
		public void loadAd(String mediaUri) {
			mAdTagUrl = Uri.parse(mediaUri);
			createAdPlayer();
		}

		@Override
		public void stopAd() {
			//hideAdPlayer();
			showContentPlayer();
		}

		@Override
		public void pauseAd() {
			if (mAdPlayer != null){
				mAdPlayer.pause();
			}
		}

		@Override
		public void resumeAd() {
			if(mAdPlayer != null) {
				mAdPlayer.play();
			}
		}

		@Override
		public void addCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
			callbacks.add(videoAdPlayerCallback);
		}

		@Override
		public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
			callbacks.remove(videoAdPlayerCallback);
		}

		private VideoProgressUpdate oldVpu;
		/**
		 * Reports progress in ad player or content player (whichever is currently playing).
		 *
		 * NOTE: When the ad is buffering, the video is paused. However, when the buffering is
		 * complete, the ad is resumed. So, as a workaround, we will attempt to resume the ad, by
		 * calling the start method, whenever we detect that the ad is buffering. If the ad is done
		 * buffering, the start method will resume playback. If the ad has not finished buffering,
		 * then the start method will be ignored.
		 */
		@Override
		public VideoProgressUpdate getProgress() {
			VideoProgressUpdate vpu = null;

			if ( isInSequence && mAdPlayer!=null ) {
				vpu = new VideoProgressUpdate(mAdPlayer.getCurrentPosition(),
						mAdPlayer.getDuration());
			} else {
				vpu = VideoProgressUpdate.VIDEO_TIME_NOT_READY;
			}


			/*if (oldVpu == null) {
				oldVpu = vpu;
			} else if ((!vpu.equals(VideoProgressUpdate.VIDEO_TIME_NOT_READY))
					&& vpu.getCurrentTime() == oldVpu.getCurrentTime()) {
				// TODO(hsubrama): Find better method for detecting ad pause and resuming ad playback.
				// Resume the ad player if it has paused due to buffering.
				/*	if (mAdPlayer != null && mAdPlayer.shouldBePlaying()) {
					adPlayer.pause();
					adPlayer.play();
				}
			}*/



			oldVpu = vpu;
			return vpu;
		}
	};

	/**
	 * Notifies callbacks when the ad finishes.
	 */
	private final ExoplayerWrapper.PlaybackListener playbackListener
	= new ExoplayerWrapper.PlaybackListener() {

		/**
		 * @param e The error.
		 */
		@Override
		public void onError(Exception e) {

		}

		/**
		 * We notify all callbacks when the ad ends.
		 * @param playWhenReady Whether the video should play as soon as it is loaded.
		 * @param playbackState The state of the Exoplayer instance.
		 */
		@Override
		public void onStateChanged(boolean playWhenReady, int playbackState) {
			if (playbackState == ExoPlayer.STATE_ENDED) {
				adsLoader.contentComplete();
				for (VideoAdPlayer.VideoAdPlayerCallback callback : callbacks) {
					callback.onEnded();
				}
			}
		}

		/**
		 * We don't respond to size changes.
		 * @param width The new width of the player.
		 * @param height The new height of the player.
		 */
		@Override
		public void onVideoSizeChanged(int width, int height) {

		}
	};


	/**
	 * Sets up ads manager, responds to ad errors, and handles ad state changes.
	 */
	private class AdListener implements AdErrorEvent.AdErrorListener,
	AdsLoader.AdsLoadedListener, AdEvent.AdEventListener {
		@Override
		public void onAdError(AdErrorEvent adErrorEvent) {
			// If there is an error in ad playback, log the error and resume the content.
			Log.d(this.getClass().getSimpleName(), adErrorEvent.getError().getMessage());
			if ( mKPlayerEventListener!=null ) {
				mKPlayerEventListener.onKPlayerEvent( "adsLoadError" );
			}

		}

		@Override
		public void onAdEvent(AdEvent event) {
			Object[] eventObject = null;
			try {
				JSONObject obj = new JSONObject();
				switch (event.getType()) {
				case LOADED:
					adsManager.start();				
					obj.put("isLinear", event.getAd().isLinear());				
					obj.put("adID", event.getAd().getAdId());
					obj.put("adSystem", event.getAd().getAdSystem());
					obj.put("adPosition", event.getAd().getAdPodInfo().getAdPosition());
					eventObject = new Object[]{"adLoaded", obj};

					if ( mHandler == null ) {
						mHandler = new Handler();
					}
					mHandler.postDelayed(runnable, PLAYHEAD_UPDATE_INTERVAL);


					if ( mKPlayerEventListener!=null && eventObject!=null ) {
						mKPlayerEventListener.onKPlayerEvent( eventObject );
					}

					//handle bug that "STARTED" is not sent
					obj.put("context", null);
					obj.put("duration", event.getAd().getDuration());
					//obj.put("adID", event.getAd().getAdId());
					eventObject = new Object[]{"adStart", obj};

					break;

				case STARTED:
					//not being sent?
					//					obj.put("context", null);
					//					obj.put("duration", event.getAd().getDuration());
					//					obj.put("adID", event.getAd().getAdId());
					//					eventObject = new Object[]{"adStart", obj};

					break;

				case COMPLETED:
					obj.put("adID", event.getAd().getAdId());
					eventObject = new Object[]{"adCompleted", obj};
					if ( mHandler != null ) {
						mHandler.removeCallbacks( runnable );
					}
					break;

				case ALL_ADS_COMPLETED:
					eventObject = new Object[]{"allAdsCompleted"};
					break;

				case CONTENT_PAUSE_REQUESTED:
					eventObject = new Object[]{"contentPauseRequested"};
					hideContentPlayer();
					break;

				case CONTENT_RESUME_REQUESTED:
					eventObject = new Object[]{"contentResumeRequested"};
					destroyAdPlayer();
					showContentPlayer();
					break;

				case FIRST_QUARTILE:
					eventObject = new Object[]{"firstQuartile"};
					break;

				case MIDPOINT:
					eventObject = new Object[]{"midpoint"};
					break;

				case THIRD_QUARTILE:
					eventObject = new Object[]{"thirdQuartile"};
					break;

				default:
					break;
				}

				if ( mKPlayerEventListener!=null && eventObject!=null ) {
					mKPlayerEventListener.onKPlayerEvent( eventObject );
				}

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Log.d(this.getClass().getSimpleName(), "failed to handle ad events!");
			}
		}

		@Override
		public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
			adsManager = adsManagerLoadedEvent.getAdsManager();
			adsManager.addAdErrorListener(this);
			adsManager.addAdEventListener(this);
			adsManager.init();
			if ( mKPlayerEventListener!=null ) {
				mKPlayerEventListener.onKPlayerEvent( "adLoadedEvent" );
			}
		}
	}


	@Override
	public void setStartingPoint(int point) {
		if ( mContentPlayer!= null ) {
			mContentPlayer.setStartingPoint(point);
		}

	}


}
