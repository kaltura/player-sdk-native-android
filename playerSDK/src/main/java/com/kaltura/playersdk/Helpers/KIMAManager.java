package com.kaltura.playersdk.helpers;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.kaltura.playersdk.players.KPlayerListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nissopa on 6/30/15.
 */
public class KIMAManager implements AdErrorEvent.AdErrorListener,
        AdsLoader.AdsLoadedListener, AdEvent.AdEventListener {
    // Container with references to video player and ad UI ViewGroup.
    private AdDisplayContainer mAdDisplayContainer;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader mAdsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager mAdsManager;

    // Factory class for creating SDK objects.
    private ImaSdkFactory mSdkFactory;

    // Ad-enabled video player.
    private VideoAdPlayer mIMAPlayer;

    // Default VAST ad tag; more complex apps might select ad tag based on content video criteria.
    private String mDefaultAdTagUrl;

    private KPlayerListener mPlayerListener;

    private boolean mContentCompleted;

    private JSONObject jsonValue = new JSONObject();

    private String DurationKey = "duration";
    private String TimeKey = "time";
    private String RemainKey = "remain";
    private String IsLinearKey = "isLinear";
    private String AdIDKey = "adID";
    private String AdSystemKey = "adSystem";
    private String AdPositionKey = "adPosition";
    private String ContextKey = "context";
    private ViewGroup mAdUIContainer;

    private String AdRemainingTimeChangeKey = "adRemainingTimeChange";
    private String AdLoadedEventKey = "adLoadedEvent";
    private String AdLoadedKey = "adLoaded";
    private String AdStartKey = "adStart";
    private String AdCompletedKey = "adCompleted";
    static public String AllAdsCompletedKey = "allAdsCompleted";
    static public String ContentPauseRequestedKey = "contentPauseRequested";
    static public String ContentResumeRequestedKey = "contentResumeRequested";
    private String FirstQuartileKey = "firstQuartile";
    private String MidPointKey = "midpoint";
    private String ThirdQuartileKey = "thirdQuartile";
    private String AdClickedKey = "adClicked";
    private String AdsLoadErrorKey = "adsLoadError";


    public KIMAManager(Activity context, VideoAdPlayer adPlayer, ViewGroup adUiContainer, String adTagURL) {
        mIMAPlayer = adPlayer;
        mAdUIContainer = adUiContainer;
//        mIMAPlayer.setKIMAAdEventListener(this);
        mDefaultAdTagUrl = adTagURL;
        mContentCompleted = false;

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
    public void requestAds(ContentProgressProvider contentProgressProvider) {
        requestAds(mDefaultAdTagUrl, contentProgressProvider);
    }

    public void setPlayerListener(KPlayerListener listener) {
        mPlayerListener = listener;
    }


    /**
     * Request video ads from the given VAST ad tag.
     * @param adTagUrl URL of the ad's VAST XML
     */
    private void requestAds(String adTagUrl, ContentProgressProvider contentProgressProvider) {
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setPlayer(mIMAPlayer);
        mAdDisplayContainer.setAdContainer(mAdUIContainer);

        // Create the ads request.
        AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdTagUrl(adTagUrl);
        request.setAdDisplayContainer(mAdDisplayContainer);
        request.setContentProgressProvider(contentProgressProvider);

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
        Ad ad = adEvent.getAd();
        try {
        switch (adEvent.getType()) {
            case LOADED:
                // AdEventType.LOADED will be fired when ads are ready to be played.
                // AdsManager.start() begins ad playback. This method is ignored for VMAP or ad
                // rules playlists, as the SDK will automatically start executing the playlist.
                mAdsManager.start();
                jsonValue.put(IsLinearKey, ad.isLinear());
                jsonValue.put(AdIDKey, ad.getAdId());
                jsonValue.put(AdSystemKey, "null");
                jsonValue.put(AdPositionKey, ad.getAdPodInfo().getAdPosition());
                fireIMAEvent(AdLoadedKey);
                break;
            case STARTED:
                jsonValue.put(DurationKey, ad.getDuration());
                fireIMAEvent(AdStartKey);
                break;
            case COMPLETED:
                jsonValue.put(AdIDKey, ad.getAdId());
                fireIMAEvent(AdCompletedKey);
                break;
            case FIRST_QUARTILE:
                fireIMAEvent(FirstQuartileKey);
                break;
            case MIDPOINT:
                fireIMAEvent(MidPointKey);
                break;
            case THIRD_QUARTILE:
                fireIMAEvent(ThirdQuartileKey);
                break;
            case CLICKED:
                jsonValue.put(IsLinearKey, ad.isLinear());
                fireIMAEvent(AdClickedKey);
                break;
            case CONTENT_PAUSE_REQUESTED:
                fireIMAEvent(ContentPauseRequestedKey);
//                mPLayerCallback.playerStateChanged(KPlayerController.SHOULD_PAUSE);
                break;
            case CONTENT_RESUME_REQUESTED:
                fireIMAEvent(ContentResumeRequestedKey);
                if (!mContentCompleted) {
//                    mPLayerCallback.playerStateChanged(KPlayerController.SHOULD_PLAY);
                }
//                mIMAPlayer.removeAd();
                break;
            case ALL_ADS_COMPLETED:
                if (mContentCompleted) {
                    mPlayerListener.contentCompleted(null);
                }
                fireIMAEvent(AllAdsCompletedKey);
                if (mAdsManager != null) {
                    mAdsManager.destroy();
                    mAdsManager = null;
//                    mIMAPlayer.release();
                    mIMAPlayer = null;
                    mPlayerListener = null;
                }
                break;
            case SKIPPED:
//                mIMAPlayer.removeAd();
                break;
            default:
                break;
        }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * An event raised when there is an error loading or playing ads.
     */
    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        Log.e("ImaExample", "Ad Error: " + adErrorEvent.getError().getMessage());
//        imaAdapter.resumeContentAfterAdPlayback();
    }

    /**
     * Event raised by VideoPlayerWithAdPlayback when the content video is complete.
     */
    public void contentComplete() {
        mContentCompleted = true;
        mAdsLoader.contentComplete();
    }

//    @Override
//    public void adDidProgress(float toTime, float totalTime) {
//        try {
//            jsonValue.put(TimeKey, toTime);
//            jsonValue.put(DurationKey, totalTime);
//            jsonValue.put(RemainKey, (totalTime - toTime));
//            fireIMAEvent(AdRemainingTimeChangeKey);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }

    private void fireIMAEvent(String eventName) {
        if (mPlayerListener != null && jsonValue.length() == 0) {
            mPlayerListener.eventWithJSON(null, eventName, "(null)");
            return;
        }
        if (mPlayerListener != null) {
            mPlayerListener.eventWithJSON(null, eventName, jsonValue.toString());
        }
        jsonValue = new JSONObject();
    }

    public void destroy() {
        if (mIMAPlayer != null) {
//            mIMAPlayer.release();
            mPlayerListener = null;
            mAdsManager.destroy();
        }
    }
}
