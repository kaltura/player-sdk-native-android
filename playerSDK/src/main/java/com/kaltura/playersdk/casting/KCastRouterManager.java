package com.kaltura.playersdk.casting;

/**
 * Created by nissimpardo on 18/11/15.
 */
public interface KCastRouterManager {
    void disconnect();
    void connectDevice(String deviceId);
    void setCastRouterManagerListener(KCastRouterManagerListener listener);
    void enableKalturaCastButton(boolean enabled);
}
