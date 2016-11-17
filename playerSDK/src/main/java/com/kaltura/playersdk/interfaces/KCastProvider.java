package com.kaltura.playersdk.interfaces;

import android.content.Context;

import com.kaltura.playersdk.casting.KCastDevice;

/**
 * Created by nissimpardo on 29/05/16.
 */
public interface KCastProvider {
    void init(Context context);
    void startReceiver(Context context, boolean guestModeEnabled);
    void startReceiver(Context context);
    void showLogo();
    void hideLogo();
    void disconnectFromCastDevice();
    KCastDevice getSelectedCastDevice();
    void setKCastProviderListener(KCastProviderListener listener);
    KCastMediaRemoteControl getCastMediaRemoteControl();
    void setCastProviderContext(Context newContext);
    boolean isReconnected();
    boolean isConnected();
    boolean isCasting();
    long getStreamDuration();
    String getSessionEntryID();
    void setAppBackgroundState(boolean appBgState);
    boolean getAppBackgroundState();
    int getNumOfConnectedSenders();

    interface KCastProviderListener {
        void onCastMediaRemoteControlReady(KCastMediaRemoteControl castMediaRemoteControl);
        void onCastReceiverError(String errorMsg, int errorCode);
        void onCastReceiverAdOpen();
        void onCastReceiverAdComplete();
        //void onDeviceCameOnline(KCastDevice device);
        //void onDeviceWentOffline(KCastDevice device);
        //void onDeviceConnected();
        //void onDeviceDisconnected();

    }
}
