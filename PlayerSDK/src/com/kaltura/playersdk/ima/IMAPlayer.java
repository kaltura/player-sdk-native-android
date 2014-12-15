package com.kaltura.playersdk.ima;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

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
import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.events.OnWebViewMinimizeListener;

public class IMAPlayer extends FrameLayout implements VideoPlayerInterface {

	public static int PLAYHEAD_UPDATE_INTERVAL = 500;
	public static int MAX_AD_BUFFER_COUNT = 30;

	private OnPlayerStateChangeListener mPlayerStateListener;
	private OnPlayheadUpdateListener mPlayheadUpdateListener;
	private OnProgressListener mProgressListener;
	private KPlayerEventListener mKPlayerEventListener;
	private OnWebViewMinimizeListener mWebViewMinimizeListener;
	/**
	 * Responsible for requesting the ad and creating the
	 * {@link com.google.ads.interactivemedia.v3.api.AdsManager}
	 */
	private AdsLoader mAdsLoader;


	/**
	 * Responsible for containing listeners for processing the elements of the ad.
	 */
	private AdsManager mAdsManager;

	/**
	 * URL of the ad
	 */
	private Uri mAdTagUrl;
	
	private Uri mCurrentAdUrl;

	/**
	 * These callbacks are notified when the video is played and when it ends. The IMA SDK uses this
	 * to poll for video progress and when to stop the ad.
	 */
	private List<VideoAdPlayer.VideoAdPlayerCallback> callbacks;

	private FrameLayout adPlayerContainer;
	private FrameLayout adUiContainer;
	/**
	 * Notifies callbacks when the ad finishes.
	 */
	private ExoplayerWrapper.PlaybackListener mPlaybackListener;

	private VideoPlayerInterface mContentPlayer;
	private SimpleVideoPlayer mAdPlayer;
	private boolean mIsInSequence;
	private Activity mActivity;
	private int mContentCurrentPosition = 0;
	private boolean mAdRequestProgress = false;
	private boolean mIsAdPlaying = false;
	
	private OnPlayheadUpdateListener mPlayheadListener = new OnPlayheadUpdateListener() {

		@Override
		public void onPlayheadUpdated(int msec) {
			mContentCurrentPosition = msec;
			if ( mPlayheadUpdateListener!=null )
				mPlayheadUpdateListener.onPlayheadUpdated(msec);			
		}		
	};

	private Handler mHandler;
	private JSONObject mTimeRemainingObj = new JSONObject();
	private Runnable runnable = new Runnable() {
		@Override
		public void run() {
			if ( mIsInSequence && mKPlayerEventListener!=null ) {
				try {
					VideoProgressUpdate progress = videoAdPlayer.getProgress();
					mTimeRemainingObj.put("time", progress.getCurrentTime() );
					mTimeRemainingObj.put("duration", progress.getDuration() );
					float remain = progress.getDuration() - progress.getCurrentTime();
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

	private int mAdBufferCount = 0;


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
//		adTagUrl = "http://pubads.g.doubleclick.net/gampad/ads?sz=640x360&iu=/6062/iab_vast_samples/skippable&ciu_szs=300x250,728x90&impl=s&gdfp_req=1&env=vp&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&correlator=[timestamp]";
//		adTagUrl = "http://pubads.g.doubleclick.net/gampad/ads?sz=400x300&iu=%2F6062%2Fgmf_demo&ciu_szs&impl=s&gdfp_req=1&env=vp&output=xml_vast2&unviewed_position_start=1&" +
//	            "url=[referrer_url]&correlator=[timestamp]&ad_rule=1&cmsid=11924&vid=cWCkSYdFlU0&cust_params=gmf_format%3Dstd%2Cskip";
//		
		//adTagUrl = "http://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=%2F3510761%2FadRulesSampleTags&ciu_szs=160x600%2C300x250%2C728x90&cust_params=adrule%3Dpremidpostpodandbumpers&impl=s&gdfp_req=1&env=vp&ad_rule=1&vid=47570401&cmsid=481&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&correlator=[timestamp]";
		if ( adTagUrl != null ) {
			mAdTagUrl = Uri.parse(adTagUrl);
			mIsInSequence = true;
		}
		else {
			mIsInSequence = false;
			
		}
		addContentPlayerView();
	
		// Create the ad adDisplayContainer UI which will be used by the IMA SDK to overlay ad controls.
		adUiContainer = new FrameLayout(mActivity);
		addView(adUiContainer);

		adUiContainer.setLayoutParams(Util.getLayoutParamsBasedOnParent(
				adUiContainer,
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
			

		mAdsLoader = ImaSdkFactory.getInstance().createAdsLoader(activity, ImaSdkFactory.getInstance().createImaSdkSettings());
		AdListener adListener = new AdListener();
		mAdsLoader.addAdErrorListener(adListener);
		mAdsLoader.addAdsLoadedListener(adListener);
		mPlaybackListener = new ExoplayerWrapper.PlaybackListener() {

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
					mAdsLoader.contentComplete();
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

		callbacks = new ArrayList<VideoAdPlayer.VideoAdPlayerCallback>();

	}

	public void setAdTagUrl( String adTagUrl ) {
		mAdTagUrl = Uri.parse(adTagUrl);
	}
	
	public void registerWebViewMinimize( OnWebViewMinimizeListener listener ) {
		mWebViewMinimizeListener = listener;
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
		if ( mIsInSequence && mAdPlayer!=null ) {
			return mAdPlayer.getDuration();
		} else if ( !mIsInSequence && mContentPlayer!=null ){
			return mContentPlayer.getDuration();
		}
		
		return 0;
	}

	@Override
	public void play() {
		if ( mAdTagUrl != null ) {
			if ( !mAdRequestProgress)
				requestAd();
		} else if ( mCurrentAdUrl == null ) {
			if ( mIsInSequence ) 
				showContentPlayer();
			else 
				mContentPlayer.play();
		} else if ( mIsInSequence && !mIsAdPlaying ) {
			mIsAdPlaying = true;
			videoAdPlayer.resumeAd();
		}

	}

	@Override
	public void pause() {
		if ( mIsInSequence && mIsAdPlaying ) {
			mIsAdPlaying = false;
			videoAdPlayer.pauseAd();
		} else if ( mContentPlayer!=null ){
			mContentPlayer.pause();
		}
	}

	@Override
	public void stop() {
		if ( mAdPlayer!=null ) {
			mAdPlayer.pause();
			mAdPlayer.release();
		}
		if ( mContentPlayer!=null ) {
			mContentPlayer.stop();
		}
	}

	@Override
	public void seek(int msec) {
		if ( !mIsInSequence ) {
			mContentPlayer.seek(msec);
		}
		//can't seek adPlayer
	}

	@Override
	public boolean isPlaying() {
		if ( mIsInSequence ) {
			return mIsAdPlaying;
		}
		else if ( mContentPlayer!= null ) {
			return ( mContentPlayer.isPlaying() );
		}
		
		return false;
	}

	@Override
	public boolean canPause() {
		if ( mIsInSequence && mAdPlayer!=null ) {
			return true;
		} else if ( !mIsInSequence && mContentPlayer!=null ) {
			return mContentPlayer.canPause();
		}
		
		return false;		
	}


	@Override
	public void registerPlayerStateChange( OnPlayerStateChangeListener listener) {
		mPlayerStateListener = listener;
		if ( !mIsInSequence && mContentPlayer!=null ) {
			mContentPlayer.registerPlayerStateChange ( listener );
		}
	}


	@Override
	public void registerPlayheadUpdate( OnPlayheadUpdateListener listener ) {
		mPlayheadUpdateListener = listener;
	}

	@Override
	public void removePlayheadUpdateListener() {
		mPlayheadUpdateListener = null;
		if ( !mIsInSequence && mContentPlayer!=null ) {
			mContentPlayer.removePlayheadUpdateListener ();
		}
	}

	@Override
	public void registerProgressUpdate ( OnProgressListener listener ) {
		mProgressListener = listener;
		if ( !mIsInSequence && mContentPlayer!=null ) {
			mContentPlayer.registerProgressUpdate ( listener );
		}
	}

	private void hideContentPlayer() {
		if ( !mIsInSequence ) {
			mIsInSequence = true;

			//unregister events
			mContentPlayer.registerPlayerStateChange( null );
			mContentPlayer.registerPlayheadUpdate( null );
			mContentPlayer.registerProgressUpdate( null );
		} 
	}
	
	private void notifyAdError() {
		mAdTagUrl = null;
		mCurrentAdUrl = null;
		
		//stop checking for ad progress
		if ( mHandler != null ) {
			mHandler.removeCallbacks( runnable );
		}
		if ( mKPlayerEventListener!=null ) {	
			mKPlayerEventListener.onKPlayerEvent( "adsLoadError" );		
		}
		mIsAdPlaying = false;
		if (mAdsManager != null) {
			mAdsManager.destroy();
		}
		
		callResume();

	}
	
	private void addContentPlayerView() {
		if ( mContentPlayer instanceof View ) {			
			LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);
			addView( (View) mContentPlayer, lp );	
		}
	}

	private void showContentPlayer() {
		if ( mIsInSequence ) {
			mIsInSequence = false;
				
			mActivity.runOnUiThread(new Runnable() {
				public void run() {
					destroyAdPlayer();
					
					//register events
					mContentPlayer.registerPlayerStateChange( mPlayerStateListener );
					mContentPlayer.registerPlayheadUpdate( mPlayheadListener );
					mContentPlayer.registerProgressUpdate( mProgressListener );
					
					if ( mWebViewMinimizeListener != null ) {
						mWebViewMinimizeListener.setMinimize(false);
					}

					mContentPlayer.play();
				}
			});	
		} 
	}
	
	 /**
	   * Notify the ad callbacks that the content has resumed.
	   */
	  private void callResume(){
	    for (VideoAdPlayer.VideoAdPlayerCallback callback : callbacks) {
	      callback.onResume();
	    }
	  }
	
	
	/**
	 * Destroy the {@link SimpleVideoPlayer} responsible for playing the ad and rmeove it.
	 */
	private void destroyAdPlayer(){

		if(adPlayerContainer != null && adPlayerContainer.getParent() != null){
			removeView(adPlayerContainer);
		}
		if (adUiContainer != null && adUiContainer.getParent() != null) {
			removeView(adUiContainer);
		}
		
		if(mAdPlayer != null){
			mAdPlayer.release();
			mAdPlayer.moveSurfaceToBackground();
		}
		
		mIsAdPlaying = false;
		adPlayerContainer = null;
		mAdPlayer = null;

	}

	private void createAdPlayer( boolean shouldPlay ){
		destroyAdPlayer();

		adPlayerContainer = new FrameLayout(mActivity);
		addView(adPlayerContainer);

		adPlayerContainer.setLayoutParams(Util.getLayoutParamsBasedOnParent(
				adPlayerContainer,
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT
				));

		if ( adUiContainer.getParent() != null ) {
			removeView ( adUiContainer );			
		}
		
		addView ( adUiContainer );
		Video adVideo = new Video(mCurrentAdUrl.toString(), Video.VideoType.MP4);
		mAdPlayer = new SimpleVideoPlayer(mActivity,
				adPlayerContainer,
				adVideo,
				"",
				true);

		mIsInSequence = true;
		
		mAdPlayer.addPlaybackListener( mPlaybackListener );

		// Move the ad player's surface layer to the foreground so that it is overlaid on the content
		// player's surface layer (which is in the background).
		mAdPlayer.moveSurfaceToForeground();
		
		if ( shouldPlay )
			mAdPlayer.play();
		mIsAdPlaying = true;
		mAdPlayer.disableSeeking();
		mAdPlayer.hideTopChrome();
		
		// Notify the callbacks that the ad has begun playing.
		for (VideoAdPlayer.VideoAdPlayerCallback callback : callbacks) {
			callback.onPlay();
		}
		
		if ( mWebViewMinimizeListener != null ) {
			mWebViewMinimizeListener.setMinimize(true);
		}
	}

	/**
	 * Create an ads request which will request the VAST document with the given ad tag URL.
	 * @param tagUrl URL pointing to a VAST document of an ad.
	 * @return a request for the VAST document.
	 */
	private AdsRequest buildAdsRequest(String tagUrl) {
		
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
		mAdRequestProgress = true;
		mAdsLoader.requestAds(buildAdsRequest(mAdTagUrl.toString()));
	}



	/**
	 * Handles loading, playing, retrieving progress, pausing, resuming, and stopping ad.
	 */
	private final VideoAdPlayer videoAdPlayer = new VideoAdPlayer() {
		@Override
		public void playAd() {
			mContentPlayer.pause();
			hideContentPlayer();
		}

		@Override
		public void loadAd(String mediaUri) {
			mCurrentAdUrl = Uri.parse(mediaUri);
			createAdPlayer( true );
			mAdTagUrl = null;
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

			if ( mIsInSequence ) {
				if ( mAdPlayer!=null ) 
					vpu = new VideoProgressUpdate(mAdPlayer.getCurrentPosition(),
						mAdPlayer.getDuration());
				else 
					vpu = VideoProgressUpdate.VIDEO_TIME_NOT_READY;
			} else {
				if ( mContentPlayer!=null ) 
					vpu = new VideoProgressUpdate(mContentCurrentPosition,
							mContentPlayer.getDuration());
				else 
					vpu = VideoProgressUpdate.VIDEO_TIME_NOT_READY;
			}


			if (oldVpu == null) {
				oldVpu = vpu;
				mAdBufferCount = 0;				
			} else if ( mIsInSequence && (!vpu.equals(VideoProgressUpdate.VIDEO_TIME_NOT_READY))
					&& vpu.getCurrentTime() == oldVpu.getCurrentTime()) {
				mAdBufferCount++;
				if (mAdBufferCount == MAX_AD_BUFFER_COUNT && mAdPlayer != null && mIsInSequence) {
					//try to recover
						mAdPlayer.pause();
						mAdPlayer.play();
				} else if ( mAdBufferCount > MAX_AD_BUFFER_COUNT ) {
					Log.w(this.getClass().getSimpleName(), "ad is buffering and can't recover!");
					notifyAdError();
					
				}
			} else {
				oldVpu = vpu;
				mAdBufferCount = 0;
			}

			return vpu;
		}
	};



	/**
	 * Sets up ads manager, responds to ad errors, and handles ad state changes.
	 */
	private class AdListener implements AdErrorEvent.AdErrorListener,
	AdsLoader.AdsLoadedListener, AdEvent.AdEventListener {
		@Override
		public void onAdError(AdErrorEvent adErrorEvent) {
			mAdRequestProgress = false;
			// If there is an error in ad playback, log the error and resume the content.
			Log.w(this.getClass().getSimpleName(), adErrorEvent.getError().getMessage());
			notifyAdError();

		}

		@Override
		public void onAdEvent(AdEvent event) {
			mAdRequestProgress = false;
			Object[] eventObject = null;
			try {
				JSONObject obj = new JSONObject();
				switch (event.getType()) {
				case LOADED:
					mAdsManager.start();				
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
					mCurrentAdUrl = null;
					break;

				case ALL_ADS_COMPLETED:
					eventObject = new Object[]{"allAdsCompleted"};
					mCurrentAdUrl = null;
					break;

				case CONTENT_PAUSE_REQUESTED:
					eventObject = new Object[]{"contentPauseRequested"};
					mContentPlayer.pause();
					hideContentPlayer();
					for (VideoAdPlayer.VideoAdPlayerCallback callback : callbacks) {
					      callback.onPause();
					}
					break;

				case CONTENT_RESUME_REQUESTED:
					eventObject = new Object[]{"contentResumeRequested"};
					showContentPlayer();
					callResume();
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
			mAdsManager = adsManagerLoadedEvent.getAdsManager();
			mAdsManager.addAdErrorListener(this);
			mAdsManager.addAdEventListener(this);
			mAdsManager.init();
			if ( mKPlayerEventListener!=null ) {
				Log.d(this.getClass().getSimpleName(), "Calling AdLoadedEvent!");
				mKPlayerEventListener.onKPlayerEvent( "adLoadedEvent" );
			}
		}
	}

	@Override
	public void setStartingPoint(int point) {
		if ( !mIsInSequence && mContentPlayer!= null ) {
			mContentPlayer.setStartingPoint(point);
		}
		
	}

	@Override
	public void registerError(OnErrorListener listener) {
		if ( mContentPlayer!=null ) {
			mContentPlayer.registerError(listener);
		}		
	}

	@Override
	public void release() {
		if ( mIsInSequence && mAdPlayer!=null ) {
			mAdPlayer.pause();
			mAdPlayer.release();
			mAdPlayer = null;
			if ( mHandler != null ) {
				mHandler.removeCallbacks( runnable );
			}
		} else if ( !mIsInSequence && mContentPlayer!=null ) {	
			mContentPlayer.release();
		}
	}

	@Override
	public void recoverRelease() {
		if ( mIsInSequence ) {
			createAdPlayer( mIsAdPlaying );
			if ( mHandler == null ) {
				mHandler = new Handler();
			}
			mHandler.postDelayed(runnable, PLAYHEAD_UPDATE_INTERVAL);
			
		} else if ( mContentPlayer!=null ) {
			mContentPlayer.recoverRelease();
		}
	}

}
