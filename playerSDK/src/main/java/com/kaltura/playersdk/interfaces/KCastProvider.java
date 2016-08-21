package com.kaltura.playersdk.interfaces;

import android.content.Context;

import com.kaltura.playersdk.casting.CastMetaDataBundle;
import com.kaltura.playersdk.casting.KCastDevice;

import java.util.ArrayList;

/**
 * Created by nissimpardo on 29/05/16.
 */
public interface KCastProvider {
    void setKCastButton(boolean enable);
    void startScan(Context context, String appID, boolean guestModeEnabled, boolean reconnectSessionIfPossible);
    void startScan(Context context, String appID);
    void stopScan();
    void setPassiveScan(boolean passiveScan);
    void connectToDevice(KCastDevice device);
    void disconnectFromDevice(boolean disableReconnectOption);
    void disconnectFromDevice();
    void disableReconnectOption();
    KCastDevice getSelectedCastDevice();
    void setKCastProviderListener(KCastProviderListener listener);
    ArrayList<KCastDevice> getDevices();
    KCastMediaRemoteControl getCastMediaRemoteControl();
    void setCastMetaDataBundle(CastMetaDataBundle metaDataBundle);
    CastMetaDataBundle getCastMetaDataBundle();

    interface KCastProviderListener {
        void onCastMediaRemoteControlReady(KCastMediaRemoteControl castMediaRemoteControl);
        void onDeviceCameOnline(KCastDevice device);
        void onDeviceWentOffline(KCastDevice device);
        void onDeviceConnected(ConnectionEvent connectionType, CastMetaDataBundle metaDataBundle);
        void onDeviceDisconnected();
        void onDeviceFailedToConnect(ConnectionEvent connectionEvent);
        void onDeviceFailedToDisconnect(ConnectionEvent connectionEvent);
        void onStartToReconnectIfPossible();
    }


    enum ConnectionEvent {
        CONNECTED, // succeeded to connect to receiver - simple connection
        RECONNECTED_SAME_SESSION, // succeeded to reconnect to receiver
        CONNECTION_FAILED,
        RECONNECTION_FAILED_ROUTE_NOT_DISCOVERED,
        RECONNECTION_FAILED_SESSION_KILLED
    }
}
