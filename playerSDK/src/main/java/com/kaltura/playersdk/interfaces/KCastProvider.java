package com.kaltura.playersdk.interfaces;

import android.content.Context;

import com.kaltura.playersdk.casting.KCastDevice;
import com.kaltura.playersdk.types.KPError;

/**
 * Created by nissimpardo on 29/05/16.
 */
public interface KCastProvider {
    void setKCastButton(boolean enable);
    void startScan(Context context, String appID);
    void stopScan();
    void setPassiveScan(boolean passiveScan);
    void connectToDevice(KCastDevice device);
    void disconnectFromDevcie();
    void setKCastProviderListener(KCastProviderListener listener);
    KCastMediaRemoteControl getCastMediaRemoteControl();

    interface KCastProviderListener {
        void onDeviceCameOnline(KCastDevice device);
        void onDeviceWentOffline(KCastDevice device);
        void onDeviceConnected();
        void onDeviceDisconnected();
        void onDeviceFailedToConnect(KPError error);
        void onDeviceFailedToDisconnect(KPError error);
    }
}
