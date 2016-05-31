package com.kaltura.playersdk.interfaces;

import com.google.ads.interactivemedia.v3.api.AdEvent;

/**
 * Created by nissimpardo on 23/05/16.
 */
public interface KIMAManagerListener {
    void onAdEvent(AdEvent.AdEventType eventType, String jsonValue);
    void onAdUpdateProgress(String jsonValue);
    void onAdError(String errorMsg);
}
