package com.kaltura.playersdk.interfaces;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.common.api.GoogleApiClient;

/**
 * Created by nissimpardo on 29/05/16.
 */
public interface ScanCastDeviceListener {
    void onDisconnectCastDevice();
    void onConnecting();
    void onStartCasting(GoogleApiClient apiClient, CastDevice selectedDevice);
    void onDevicesInRange(boolean foundDevices);
}
