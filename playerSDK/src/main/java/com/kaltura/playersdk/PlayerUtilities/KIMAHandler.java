package com.kaltura.playersdk.PlayerUtilities;

import android.content.Context;
import android.view.ViewGroup;

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
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by nissopa on 6/24/15.
 */
public class KIMAHandler implements AdErrorEvent.AdErrorListener,
        AdsLoader.AdsLoadedListener, AdEvent.AdEventListener{
    public static String AdErrorKey = "AdErrorKey";
    private static String AdLoadedEventKey = "adLoadedEvent";
    private static String AdLoadedKey = "adLoaded";
    private static String IsLinearKey = "isLinear";
    private static String AdIDKey = "adID";
    private static String AdSystemKey = "adSystem";
    private static String AdPositionKey = "adPosition";
    private static String ContextKey = "context";
    private static String DurationKey = "duration";
    private static String TimeKey = "time";
    private static String RemainTimeKey = "remain";
    private static String AdStartKey = "adStart";
    private static String AllAdsCompletedKey = "allAdsCompleted";
    private static String AdCompletedKey = "adCompleted";
    private static String FirstQuartileKey = "firstQuartile";
    private static String MidPointKey = "midpoint";
    private static String ThirdQuartileKey = "thirdQuartile";
    private static String AdClickedKey = "adClicked";

    private KIMEventsHandler mAdEventsHandler;
    // Container with references to video player and ad UI ViewGroup.
    private AdDisplayContainer mAdDisplayContainer;

    // The AdsLoader instance exposes the requestAds method.
    private AdsLoader mAdsLoader;

    // AdsManager exposes methods to control ad playback and listen to ad events.
    private AdsManager mAdsManager;

    // Factory class for creating SDK objects.
    private ImaSdkFactory mSdkFactory;

    private JSONObject mEventParams;


    public interface KIMEventsHandler {
        public void eventParams(HashMap<String, String> eventParams);
        public void updtePlayerEvents(AdEvent.AdEventType adPlayerEventType);
    }

    public void setHandlerEvents(KIMEventsHandler handlerEvents) {
        mAdEventsHandler = handlerEvents;
    }

    public KIMAHandler(Context context) {
        mSdkFactory = ImaSdkFactory.getInstance();
        mAdsLoader = mSdkFactory.createAdsLoader(context);
        mAdsLoader.addAdErrorListener(this);
        mAdsLoader.addAdsLoadedListener(this);
    }

    public void requestAds(String adsTagURL, VideoAdPlayer adPlayer, ViewGroup adUIContainer, ContentProgressProvider contentProgressProvider) {
        mAdDisplayContainer = mSdkFactory.createAdDisplayContainer();
        mAdDisplayContainer.setPlayer(adPlayer);
        mAdDisplayContainer.setAdContainer(adUIContainer);

        // Create the ads request.
        AdsRequest request = mSdkFactory.createAdsRequest();
        request.setAdTagUrl(adsTagURL);
        request.setAdDisplayContainer(mAdDisplayContainer);
        request.setContentProgressProvider(contentProgressProvider);

        // Request the ad. After the ad is loaded, onAdsManagerLoaded() will be called.
        mAdsLoader.requestAds(request);
    }

    public void contentCompleted() {
        mAdsLoader.contentComplete();
    }


    @Override
    public void onAdError(AdErrorEvent adErrorEvent) {
        HashMap<String, String> errorEventParams = new HashMap<String, String>();
        errorEventParams.put(AdErrorKey, adErrorEvent.toString());
        mAdEventsHandler.eventParams(errorEventParams);
    }

    @Override
    public void onAdEvent(AdEvent adEvent) {
        try {
            switch (adEvent.getType()) {
                case LOADED:
                    mAdsManager.start();
                    getEventParams().put(IsLinearKey, Boolean.toString(adEvent.getAd().isLinear()));
                    getEventParams().put(AdIDKey, adEvent.getAd().getAdId());
                    getEventParams().put(AdSystemKey, "null");
                    getEventParams().put(AdPositionKey, Integer.toString(adEvent.getAd().getAdPodInfo().getAdPosition()));
                    mAdEventsHandler.eventParams(eventParams(AdLoadedKey, getEventParams()));
                    break;
                case CONTENT_PAUSE_REQUESTED:
                case CONTENT_RESUME_REQUESTED:
                    mAdEventsHandler.updtePlayerEvents(adEvent.getType());
                    break;
                case ALL_ADS_COMPLETED:
                    mAdEventsHandler.updtePlayerEvents(adEvent.getType());
                    mAdEventsHandler.eventParams(eventParams(AllAdsCompletedKey, null));
                    if (mAdsManager != null) {
                        mAdsManager.destroy();
                        mAdsManager = null;
                    }
                    break;
                case STARTED:
                    mAdEventsHandler.updtePlayerEvents(adEvent.getType());
                    getEventParams().put(DurationKey, Double.toString(adEvent.getAd().getDuration()));
                    mAdEventsHandler.eventParams(eventParams(AdStartKey, getEventParams()));
                    break;
                case COMPLETED:
                    getEventParams().put(AdIDKey, adEvent.getAd().getAdId());
                    mAdEventsHandler.eventParams(eventParams(AdCompletedKey, getEventParams()));
                    break;
                case FIRST_QUARTILE:
                    mAdEventsHandler.eventParams(eventParams(FirstQuartileKey, null));
                    break;
                case MIDPOINT:
                    mAdEventsHandler.eventParams(eventParams(MidPointKey, null));
                    break;
                case THIRD_QUARTILE:
                    mAdEventsHandler.eventParams(eventParams(ThirdQuartileKey, null));
                    break;
                case CLICKED:
                    getEventParams().put(IsLinearKey, Boolean.toString(adEvent.getAd().isLinear()));
                    mAdEventsHandler.eventParams(eventParams(AdClickedKey, getEventParams()));
                    break;
            }
        } catch (JSONException e) {

        }
    }

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


    private JSONObject getEventParams() {
        if (mEventParams == null) {
            mEventParams = new JSONObject();
        }
        return mEventParams;
    }

    private HashMap<String, String> eventParams(String key, JSONObject params) {
        HashMap<String, String> prepareEventParams = new HashMap<String, String>();
        if (params == null) {
            prepareEventParams.put(key, "(null)");
        } else {
            prepareEventParams.put(key, params.toString());
        }
        mEventParams = null;
        return prepareEventParams;
    }
}
