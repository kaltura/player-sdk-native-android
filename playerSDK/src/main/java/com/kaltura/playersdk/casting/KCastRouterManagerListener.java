package com.kaltura.playersdk.casting;

/**
 * Created by nissimpardo on 18/11/15.
 */
public interface KCastRouterManagerListener {
    public void castButtonClicked();
    public void castDeviceConnectionState(boolean isConnected);
    public void didDetectCastDevices(boolean didDetect);
    public void addedCastDevice(KRouterInfo info);
    public void removedCastDevice(KRouterInfo info);
}
