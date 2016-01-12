package com.kaltura.playersdk.casting;

/**
 * Created by nissimpardo on 18/11/15.
 */
public interface KCastRouterManager {
    public void disconnect();
    public void connectDevice(String deviceId);
    public void setCastRouterManagerListener(KCastRouterManagerListener listener);
    public void enableKalturaCastButton(boolean enabled);
}
