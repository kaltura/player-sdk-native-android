package com.kaltura.playersdk.casting;

/**
 * Created by nissimpardo on 18/11/15.
 */
public interface KCastRouterManagerListener {
    void onCastButtonClicked();
    void onApplicationStatusChanged(boolean isConnected);
    void shouldPresentCastIcon(boolean shouldPresentOrDismiss);
    void onAddedCastDevice(KCastDevice info);
    void onRemovedCastDevice(KCastDevice info);
}
