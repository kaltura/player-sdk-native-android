package com.kaltura.playersdk.interfaces;

import android.content.Context;

import com.kaltura.playersdk.casting.KCastDevice;

/**
 * Created by nissimpardo on 29/05/16.
 */
public interface KCastProvider {
    void startReceiver(Context context, boolean guestModeEnabled);
    void startReceiver(Context context);
    void disconnectFromCastDevice();
    KCastDevice getSelectedCastDevice();
    void setKCastProviderListener(KCastProviderListener listener);
    KCastMediaRemoteControl getCastMediaRemoteControl();
    boolean isRecconected();
    boolean isConnected();
    boolean isCasting();

    interface KCastProviderListener {
        void onCastMediaRemoteControlReady(KCastMediaRemoteControl castMediaRemoteControl);
        void onCastReceiverError(String errorMsg, int errorCode);
        //void onDeviceCameOnline(KCastDevice device);
        //void onDeviceWentOffline(KCastDevice device);
        //void onDeviceConnected();
        //void onDeviceDisconnected();

    }
}
