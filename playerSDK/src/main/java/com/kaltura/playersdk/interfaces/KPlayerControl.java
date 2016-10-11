package com.kaltura.playersdk.interfaces;

import android.app.Activity;

import com.kaltura.playersdk.widevine.LicenseResource;

/**
 * Created by carloluismb on 11/10/16.
 */

public interface KPlayerControl extends KMediaControl {
    void destroy();

    void savePlayerState();

    void recoverPlayerState();

    void initIMA(String adsURL, String adMimeType, int adPreferredBitrate, Activity activity);

    void changeMedia();

    void reset();

    void setWidevineClassicLicenseDataSource(LicenseResource widevineClassicDataSource);

    void setCurrentPlaybackTime(float mStartPosition);

    void setSrc(String videoLicenseUrl);

    void setLicenseUri(String license);
}
