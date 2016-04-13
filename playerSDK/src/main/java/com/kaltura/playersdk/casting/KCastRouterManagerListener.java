package com.kaltura.playersdk.casting;

/**
 * Created by nissimpardo on 18/11/15.
 */
public interface KCastRouterManagerListener {
    void castButtonClicked();
    void castDeviceConnectionState(boolean isConnected);
    void didDetectCastDevices(boolean didDetect);
    void addedCastDevice(KRouterInfo info);
    void removedCastDevice(KRouterInfo info);
}
