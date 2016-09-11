package com.kaltura.playersdk.interfaces;

import android.content.Context;

import com.google.android.gms.cast.framework.CastSession;
import com.kaltura.playersdk.casting.KCastDevice;

/**
 * Created by nissimpardo on 29/05/16.
 */
public interface KCastProvider {
    void startReceiver(Context context, String appID, boolean guestModeEnabled, CastSession castSession);
    void startReceiver(Context context, String appID, CastSession castSession);
    void disconnectFromCastDevice();
    KCastDevice getSelectedCastDevice();
    void setKCastProviderListener(KCastProviderListener listener);
    KCastMediaRemoteControl getCastMediaRemoteControl();

    interface KCastProviderListener {
        void onCastMediaRemoteControlReady(KCastMediaRemoteControl castMediaRemoteControl);
        //void onDeviceCameOnline(KCastDevice device);
        //void onDeviceWentOffline(KCastDevice device);
        //void onDeviceConnected();
        //void onDeviceDisconnected();

    }
}
