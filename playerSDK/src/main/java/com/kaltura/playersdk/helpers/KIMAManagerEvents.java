package com.kaltura.playersdk.helpers;

import com.google.ads.interactivemedia.v3.api.AdEvent;

/**
 * Created by nissimpardo on 23/05/16.
 */
public class KIMAManagerEvents {
    public static String eventName(AdEvent.AdEventType adEventType) {
        switch (adEventType) {
            case ALL_ADS_COMPLETED:
                return "allAdsCompleted";
            case CLICKED:
                return "adClicked";
            case COMPLETED:
                return "adCompleted";
            case CONTENT_PAUSE_REQUESTED:
                return "contentPauseRequested";
            case CONTENT_RESUME_REQUESTED:
                return "contentResumeRequested";
            case FIRST_QUARTILE:
                return "firstQuartile";
            case LOG:
                break;
            case AD_BREAK_READY:
                break;
            case MIDPOINT:
                return "midpoint";
            case PAUSED:
                break;
            case RESUMED:
                break;
            case SKIPPED:
                return "adSkipped";
            case STARTED:
                return "adStart";
            case TAPPED:
                break;
            case THIRD_QUARTILE:
                return "thirdQuartile";
            case LOADED:
                return "adLoaded";
            case AD_BREAK_STARTED:
                break;
            case AD_BREAK_ENDED:
                break;
        }
        return null;
    }
}
