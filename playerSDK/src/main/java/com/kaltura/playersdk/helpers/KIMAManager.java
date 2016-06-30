package com.kaltura.playersdk.helpers;

import android.app.Activity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.ads.interactivemedia.v3.api.Ad;
import com.google.ads.interactivemedia.v3.api.AdDisplayContainer;
import com.google.ads.interactivemedia.v3.api.AdErrorEvent;
import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.AdsLoader;
import com.google.ads.interactivemedia.v3.api.AdsManager;
import com.google.ads.interactivemedia.v3.api.AdsManagerLoadedEvent;
import com.google.ads.interactivemedia.v3.api.AdsRenderingSettings;
import com.google.ads.interactivemedia.v3.api.AdsRequest;
import com.google.ads.interactivemedia.v3.api.ImaSdkFactory;
import com.google.ads.interactivemedia.v3.api.UiElement;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.kaltura.playersdk.interfaces.KIMAManagerListener;
import com.kaltura.playersdk.players.KIMAAdPlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by nissopa on 6/30/15.
 */
public class KIMAManager implements AdErrorEvent.AdErrorListener,
        AdsLoader.AdsLoadedListener, AdEvent.AdEventListener,
        KIMAAdPlayer.KIMAAdPlayerEvents {
    // Container with references to video player and ad UI ViewGroup.
    private AdDisplayContainer mAdDisplayContainer;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader mAdsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager mAdsManager;

    // Factory class for creating SDK objects.
    private ImaSdkFactory mSdkFactory;

    // Ad-enabled video player.
    private KIMAAdPlayer mIMAPlayer;

    // Default VAST ad tag; more complex apps might select ad tag based on content video criteria.
    private String mDefaultAdTagUrl;
    private KIMAManagerListener mListener;

    private String DurationKey = "duration";
    private String TimeKey = "time";
    private String RemainKey = "remain";
    private String IsLinearKey = "isLinear";
    private String AdIDKey = "adID";
    private String AdSystemKey = "adSystem";
    private String AdPositionKey = "adPosition";


    public KIMAManager(Activity context, FrameLayout adPlayerContainer, ViewGroup adUiContainer, String adTagURL) {
        mIMAPlayer = new KIMAAdPlayer(context, adPlayerContainer, adUiContainer);

        mIMAPlayer.setKIMAAdEventListener(this);
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
    public void requestAds(ContentProgressProvider contentProgressProvider) {
        requestAds(mDefaultAdTagUrl, contentProgressProvider);
    }

    public void setListener(KIMAManagerListener listener) {
        mListener = listener;
    }



    public void pause() {
//        if (mIMAPlayer != null) {
//            mIMAPlayer.pause();
//        }
        if (mAdsManager != null) {
            mAdsManager.pause();
        }
    }

    public void resume() {
//        if (mIMAPlayer != null) {
//            mIMAPlayer.resume();
//        }
        if (mAdsManager != null) {
            mAdsManager.resume();
        }
    }

    /**
     * Request video ads from the given VAST ad tag.
     * @param adTagUrl URL of the ad's VAST XML
     */
    private void requestAds(String adTagUrl, ContentProgressProvider contentProgressProvider) {
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setPlayer(mIMAPlayer);
        mAdDisplayContainer.setAdContainer(mIMAPlayer.getAdUIContainer());

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
        AdsRenderingSettings renderingSettings = ImaSdkFactory.getInstance().createAdsRenderingSettings();
        List<String> mimeTypes = new ArrayList<>();
        mimeTypes.add("application/x-mpegURL");
        //mimeTypes.add("video/mp4");
        //mimeTypes.add("video/3gpp");
        renderingSettings.setMimeTypes(mimeTypes);
        renderingSettings.setUiElements(Collections.<UiElement>emptySet());
        mAdsManager.init(renderingSettings);
    }

    /**
     * Responds to AdEvents.
     */

    @Override
    public void onAdEvent(AdEvent adEvent) {
        if (mListener != null) {
            mListener.onAdEvent(adEvent.getType(), adJSONValue(adEvent));
        }
    }


    /**
     * An event raised when there is an error loading or playing ads.
     */
    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        String errMsg = "UNKNOWN ERROR";
        if (adErrorEvent != null) {
            errMsg = "Ad Error: " + adErrorEvent.getError().getErrorCode().name() + " - " + adErrorEvent.getError().getMessage();
        }
        Log.e("IMA onAdError", errMsg);
        if (mListener != null) {
            mListener.onAdError(errMsg);
        }
    }

    private String adJSONValue(AdEvent adEvent) {
        if (adEvent == null) {
            return "(null)";
        }
        Ad ad = adEvent.getAd();
        JSONObject jsonValue = null;
        try {
            jsonValue = new JSONObject();
            switch (adEvent.getType()) {
                case LOADED:
                    mAdsManager.start();
                    jsonValue.put(IsLinearKey, ad.isLinear());
                    jsonValue.put(AdIDKey, ad.getAdId());
                    jsonValue.put(AdSystemKey, "null");
                    jsonValue.put(AdPositionKey, ad.getAdPodInfo().getAdPosition());
                    break;
                case COMPLETED:
                    jsonValue.put(AdIDKey, ad.getAdId());
                    break;
                case CLICKED:
                    jsonValue.put(IsLinearKey, ad.isLinear());
                    break;
                case SKIPPED:
                    jsonValue.put(IsLinearKey, ad.isLinear());
                    break;
            }
        } catch (Exception e) {

        }
        return jsonValue.toString();
    }

    /**
     * Event raised by VideoPlayerWithAdPlayback when the content video is complete.
     */
    public void contentComplete() {
        mAdsLoader.contentComplete();
    }

    @Override
    public void adDidProgress(float toTime, float totalTime) {
        if (mListener != null) {
            JSONObject jsonValue = null;
            try {
                jsonValue = new JSONObject();
                jsonValue.put(TimeKey, toTime);
                jsonValue.put(DurationKey, totalTime);
                jsonValue.put(RemainKey, (totalTime - toTime));
                mListener.onAdUpdateProgress(jsonValue.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void adDurationUpdate(float totalTime) {
        JSONObject jsonValue = new JSONObject();
        try {
            jsonValue.put(DurationKey, totalTime);
            mListener.onAdEvent(AdEvent.AdEventType.STARTED, jsonValue.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        if (mIMAPlayer != null) {
            mIMAPlayer.release();
            mIMAPlayer = null;
            if (mAdsManager != null) {
                mAdsManager.removeAdEventListener(this);
                mAdsManager.destroy();
            }
            mListener = null;
        }
    }
}
