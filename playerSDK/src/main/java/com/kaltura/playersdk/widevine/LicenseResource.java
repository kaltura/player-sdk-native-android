package com.kaltura.playersdk.widevine;

import android.drm.DrmInfoRequest;

/**
 * Created by carloluismb on 9/10/16.
 */

public interface LicenseResource {
    DrmInfoRequest createDrmInfoRequest(String assetUri, String licenseServerUri);

    String getPortalName();
}
