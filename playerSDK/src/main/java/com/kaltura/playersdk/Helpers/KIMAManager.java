package com.kaltura.playersdk.Helpers;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.kaltura.playersdk.players.KPlayer;
import com.kaltura.playersdk.players.KPlayerController;
import com.kaltura.playersdk.players.KPlayerListener;

/**
 * Created by nissopa on 6/30/15.
 */
public class KIMAManager implements AdErrorEvent.AdErrorListener,
        AdsLoader.AdsLoadedListener, AdEvent.AdEventListener,
        KIMAAdapter.OnContentCompleteListener {
    // Container with references to video player and ad UI ViewGroup.
    private AdDisplayContainer mAdDisplayContainer;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader mAdsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager mAdsManager;

    // Factory class for creating SDK objects.
    private ImaSdkFactory mSdkFactory;

    // Ad-enabled video player.
    private KIMAAdapter imaAdapter;

    // Default VAST ad tag; more complex apps might select ad tag based on content video criteria.
    private String mDefaultAdTagUrl;

    private KPlayerListener mPlayerListener;


    public KIMAManager(Context context, KPlayerController.KPlayer player, ViewGroup adUiContainer, String adTagURL) {
        imaAdapter = new KIMAAdapter(player, adUiContainer);
        imaAdapter.init();
        imaAdapter.setOnContentCompleteListener(this);
        mDefaultAdTagUrl = adTagURL;

        // Create an AdsLoader.
        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context);
        mAdsLoader.addAdErrorListener(this);
        mAdsLoader.addAdsLoadedListener(this);
    }

    /**
     * Request video ads using the default VAST ad tag. Typically, you would change your ad tag
     * URL based on the current content being played.
     */
    public void requestAds() {
        requestAds(mDefaultAdTagUrl);
    }

    public void setPlayerListener(KPlayerListener listener) {
        mPlayerListener = listener;
    }

    /**
     * Request video ads from the given VAST ad tag.
     * @param adTagUrl URL of the ad's VAST XML
     */
    private void requestAds(String adTagUrl) {
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setPlayer(imaAdapter.getVideoAdPlayer());
        mAdDisplayContainer.setAdContainer(imaAdapter.getAdUiContainer());

        // Create the ads request.
        AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdDisplayContainer(mAdDisplayContainer);
        request.setContentProgressProvider(imaAdapter.getContentProgressProvider());

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        mAdsLoader.requestAds(request);
    }

    /**
     * An event raised when ads are successfully loaded from the ad server via an AdsLoader.
     */
    @Override
    public void onAdsManagerLoaded(AdsManagerLoadedEvent adsManagerLoadedEvent) {
        // Ads were successfully loaded, so get the AdsManager instance. AdsManager has
        // events for ad playback and errors.
        mAdsManager = adsManagerLoadedEvent.getAdsManager();

        // Attach event and error event listeners.
        mAdsManager.addAdErrorListener(this);
        mAdsManager.addAdEventListener(this);
        mAdsManager.init();
    }

    /**
     * Responds to AdEvents.
     */
    @Override
    public void onAdEvent(AdEvent adEvent) {
        Log.i("ImaExample", "Event: " + adEvent.getType());

        // These are the suggested event types to handle. For full list of all ad event types,
        // see the documentation for AdEvent.AdEventType.
        switch (adEvent.getType()) {
            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or ad
                // rules playlists, as the SDK will automatically start executing the playlist.
                mAdsManager.start();
                break;
            case CONTENT_PAUSE_REQUESTED:
                // AdEventType.CONTENT_PAUSE_REQUESTED is fired immediately before a video ad is
                // played.
                imaAdapter.pauseContentForAdPlayback();
                break;
            case CONTENT_RESUME_REQUESTED:
                // AdEventType.CONTENT_RESUME_REQUESTED is fired when the ad is completed and you
                // should start playing your content.
                imaAdapter.resumeContentAfterAdPlayback();
                break;
            case ALL_ADS_COMPLETED:
                if (mAdsManager != null) {
                    mAdsManager.destroy();
                    mAdsManager = null;
                }
                break;
            default:
                break;
        }
    }

    /**
     * An event raised when there is an error loading or playing ads.
     */
    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        Log.e("ImaExample", "Ad Error: " + adErrorEvent.getError().getMessage());
        imaAdapter.resumeContentAfterAdPlayback();
    }

    /**
     * Event raised by VideoPlayerWithAdPlayback when the content video is complete.
     */
    @Override
    public void onContentComplete() {
        mAdsLoader.contentComplete();
    }

    /**
     * Set metadata about the content video. In more complex implementations, this might
     * more than just a URL and could trigger additional decisions regarding ad tag selection.
     */
    public void setContentVideo(String videoPath) {
        imaAdapter.setContentVideoPath(videoPath);
    }


    /**
     * Resumes ad playback
     */
    public void resume() {
        imaAdapter.restorePosition();
        if (mAdsManager != null) {
            mAdsManager.resume();
        }
    }

    /**
     * Pauses ad playback
     */
    public void pause() {
        imaAdapter.savePosition();
    }

}
