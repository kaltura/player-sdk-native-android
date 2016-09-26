package com.kaltura.playersdk.casting;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.players.KChromeCastPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;


/**
 * Created by nissimpardo on 29/05/16.
 */
public class KCastProviderImpl implements com.kaltura.playersdk.interfaces.KCastProvider, KRouterCallback.KRouterCallbackListener {
    private static final String TAG = "KCastProviderImpl";
    private String nameSpace = "urn:x-cast:com.kaltura.cast.player";
    private String mCastAppID;
    private KCastProviderListener mProviderListener;
    private Context mContext;

    private KCastKalturaChannel mChannel;
    private GoogleApiClient mApiClient;
    private CastDevice mSelectedDevice;
    private MediaRouter mRouter;
    private KRouterCallback mCallback;
    private MediaRouteSelector mSelector;

    private Cast.Listener mCastClientListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;

    private boolean mWaitingForReconnect = false;
    private boolean mApplicationStarted = false;
    private boolean mCastButtonEnabled = false;
    private boolean mGuestModeEnabled = false; // do not enable paring the cc from guest network
    private KCastMediaRemoteControl mCastMediaRemoteControl;

    private String mSessionId;

    private InternalListener mInternalListener;
    public GoogleApiClient getApiClient() {
        return mApiClient;
    }

    public interface InternalListener extends KCastMediaRemoteControl.KCastMediaRemoteControlListener {
        void onStartCasting(KChromeCastPlayer remoteMediaPlayer);
        void onCastStateChanged(String state);
        void onStopCasting();
    }

    public void setInternalListener(InternalListener internalListener) {
        mInternalListener = internalListener;
    }

    public KCastProviderListener getProviderListener() {
        return mProviderListener;
    }



    public KCastKalturaChannel getChannel() {
        return mChannel;
    }

    @Override
    public void setKCastButton(boolean enable) {
        mCastButtonEnabled = enable;
    }

    @Override
    public void startScan(Context context, String appID, boolean guestModeEnabled) {
        mContext = context;
        mCastAppID = appID;
        mGuestModeEnabled = guestModeEnabled;
        mRouter = MediaRouter.getInstance(mContext.getApplicationContext());
        mCallback = new KRouterCallback(guestModeEnabled);
        mCallback.setListener(this);
        mCallback.setRouter(mRouter);
        mSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(mCastAppID)).build();
        if (mRouter != null) {
            mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        }
    }

    @Override
    public void startScan(Context context, String appID) {
        startScan(context, appID, false);
    }

    @Override
    public void stopScan() {
        mRouter.removeCallback(mCallback);
    }

    @Override
    public void setPassiveScan(boolean passiveScan) {

    }

    @Override
    public void connectToDevice(KCastDevice device) {
        MediaRouter.RouteInfo selectedRoute = mCallback.routeById(device.getRouterId());
        if (mRouter != null) {
            mRouter.selectRoute(selectedRoute);
        }
//        if (mScanCastDeviceListener != null) {
//            mScanCastDeviceListener.onConnecting();
//        }
    }

    @Override
    public void disconnectFromDevice() {
//        if (mScanCastDeviceListener != null) {
//            mScanCastDeviceListener.onDisconnectCastDevice();
//        }
        if (mRouter != null) {
            mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED);
            mSelectedDevice = null;
        }
        if (mInternalListener != null) {
            mInternalListener.onCastStateChanged("hideConnectingMessage");
        }
    }

    @Override
    public KCastDevice getSelectedCastDevice() {
        if (mSelectedDevice != null) {
            return new KCastDevice(mSelectedDevice);
        }
        return null;
    }

    @Override
    public void setKCastProviderListener(KCastProviderListener listener) {
        mProviderListener = listener;
    }

    @Override
    public ArrayList<KCastDevice> getDevices() {
        if (mRouter != null && mRouter.getRoutes() != null && mRouter.getRoutes().size() > 0) {
            ArrayList<KCastDevice> devices = new ArrayList<>();
            for (MediaRouter.RouteInfo route: mRouter.getRoutes()) {
                if (route.isDefaultOrBluetooth()) {
                    continue;
                }
                KCastDevice castDevice = new KCastDevice(route);
                CastDevice device = CastDevice.getFromBundle(route.getExtras());
                if (!mGuestModeEnabled && device != null && !device.isOnLocalNetwork()) {
                    continue;
                }
                devices.add(castDevice);
            }
            return devices;
        }
        return null;
    }

    @Override
    public KCastMediaRemoteControl getCastMediaRemoteControl() {
        return mCastMediaRemoteControl;
    }

    private Cast.Listener getCastClientListener() {
        if (mCastClientListener == null) {
            mCastClientListener = new Cast.Listener() {
                @Override
                public void onApplicationStatusChanged() {
                    if (mApiClient != null) {
                        if (hasMediaSession()) {
                            LOGD(TAG, "onApplicationStatusChanged: " + Cast.CastApi.getApplicationStatus(mApiClient));
                            if (mProviderListener != null && "Ready to play".equals(Cast.CastApi.getApplicationStatus(mApiClient))) {
                                mProviderListener.onDeviceConnected();
                            }
                        }
                    }
                }

                @Override
                public void onApplicationMetadataChanged(ApplicationMetadata applicationMetadata) {
                }

                @Override
                public void onApplicationDisconnected(int statusCode) {
                    if (mProviderListener != null) {
                        mProviderListener.onDeviceDisconnected();
                    }
                    teardown();
                }

                @Override
                public void onActiveInputStateChanged(int activeInputState) {
                }

                @Override
                public void onStandbyStateChanged(int standbyState) {
                }

                @Override
                public void onVolumeChanged() {

                }
            };
        }
        return mCastClientListener;
    }

    private ConnectionCallbacks getConnectionCallbacks() {
        if (mConnectionCallbacks == null) {
            mConnectionCallbacks = new ConnectionCallbacks();
        }
        return mConnectionCallbacks;
    }

    private ConnectionFailedListener getConnectionFailedListener() {
        if (mConnectionFailedListener == null) {
            mConnectionFailedListener = new ConnectionFailedListener();
        }
        return mConnectionFailedListener;
    }

    private void teardown() {
        if (mApiClient != null) {
            LOGD(TAG, "START TEARDOWN mApiClient.isConnected() = " + mApiClient.isConnected() + " mApiClient.isConnecting() = " + mApiClient.isConnecting()) ;
            if (mApplicationStarted) {
                boolean isConnected = mApiClient.isConnected();
                boolean isConnecting = mApiClient.isConnecting();
                if (isConnected || isConnecting) {
                    try {
                        if (isConnected) {
                            Cast.CastApi.stopApplication(mApiClient, mSessionId);
                        }
                        if (mChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    mApiClient,
                                    mChannel.getNamespace());
                            mChannel = null;
                        }
                    } catch (IOException e) {
                        LOGE(TAG, "Exception while removing channel", e);
                    }
                    mApiClient.disconnect();
                }
                mApplicationStarted = false;
            }
            mApiClient = null;
        }
        mSelectedDevice = null;
        mWaitingForReconnect = false;
        mSessionId = null;
    }


    public void sendMessage(final String message) {
        if (mApiClient != null && mChannel != null) {
            try {
                LOGD(TAG, "chromecast.sendMessage  namespace: " + nameSpace + " message: " + message);
                Cast.CastApi.sendMessage(mApiClient, nameSpace, message)
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(Status result) {
                                        if (result.isSuccess()) {
                                            LOGD(TAG, "namespace:" + nameSpace + " message:" + message);
                                        } else {
                                            LOGE(TAG, "Sending message failed");
                                        }
                                    }
                                });
            } catch (Exception e) {
                LOGE(TAG, "Exception while sending message", e);
            }
        }
    }


    @Override
    public void onDeviceSelected(CastDevice castDeviceSelected) {
        if (castDeviceSelected != null) {
            mSelectedDevice = castDeviceSelected;
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(castDeviceSelected, getCastClientListener());
            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(getConnectionCallbacks())
                    .addOnConnectionFailedListener(getConnectionFailedListener())
                    .build();
            mApiClient.connect();
        } else if (mProviderListener != null){
            if (mInternalListener != null) {
                mInternalListener.onCastStateChanged("chromecastDeviceDisConnected");
                mInternalListener.onStopCasting();
                mInternalListener = null;
            }
            mProviderListener.onDeviceDisconnected();
            teardown();
        }
    }

    @Override
    public void onRouteUpdate(boolean isAdded, KCastDevice route) {
        LOGD(TAG, "start onRouteUpdate: " + route.getRouterName() + " isAdded: " + isAdded);
        if (mProviderListener != null) {
            if (isAdded) {
                LOGD(TAG, "fire onDeviceCameOnline: " + route.getRouterName());
                mProviderListener.onDeviceCameOnline(route);
            } else {
                LOGD(TAG, "fire onDeviceWentOffline: " + route.getRouterName());
                mProviderListener.onDeviceWentOffline(route);
            }
        }
    }

    @Override
    public void onFoundDevices(boolean didFound) {
//        if (mCastButtonEnabled) {
//            mScanCastDeviceListener.onDevicesInRange(didFound);
//        }
    }


    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;

                // In case of kaltura receiver is loaded, open channel for sneding messages
            } else {

                try {
                    Cast.CastApi.launchApplication(mApiClient, mCastAppID, new LaunchOptions())
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
                                                mSessionId = result.getSessionId();
//                                                mRemoteMediaPlayer = new RemoteMediaPlayer();

                                                // Prepare the custom channel (listens to Kaltura's receiver messages)
                                                mChannel = new KCastKalturaChannel(nameSpace, new KCastKalturaChannel.KCastKalturaChannelListener() {

                                                    @Override
                                                    public void readyForMedia(final String[] params) {
                                                        sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
                                                        // Receiver send the new content
                                                        if (params != null) {
                                                            mCastMediaRemoteControl = new KChromeCastPlayer(mApiClient);
                                                            ((KChromeCastPlayer)mCastMediaRemoteControl).setMediaInfoParams(params);
                                                            if (mInternalListener != null) {
                                                                mInternalListener.onStartCasting((KChromeCastPlayer) mCastMediaRemoteControl);
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void textTeacksRecived(HashMap<String,Integer> textTrackHash) {
                                                        getCastMediaRemoteControl().setTextTracks(textTrackHash);
                                                    }

                                                    @Override
                                                    public void videoTracksReceived(List<Integer> videoTracksList) {
                                                        getCastMediaRemoteControl().setVideoTracks(videoTracksList);
                                                    }
                                                });
                                                sendMessage("{\"type\":\"show\",\"target\":\"logo\"}");
                                                mApplicationStarted = true;
                                                try {
                                                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                                            mChannel.getNamespace(),
                                                            mChannel);
                                                } catch (IOException e) {
                                                    LOGE(TAG, "Exception while creating channel", e);
                                                }
                                                if (mProviderListener != null) {
                                                    mProviderListener.onDeviceConnected();
                                                }
                                            } else {
                                                teardown();
                                            }
                                        }
                                    });

                } catch (Exception e) {
                    LOGD(TAG, "Failed to launch application", e);
                }
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            mWaitingForReconnect = true;
        }
    }

    private class ConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult result) {
            teardown();
        }
    }

    public boolean hasMediaSession() {
        return mCastMediaRemoteControl != null && mCastMediaRemoteControl.hasMediaSession();
    }
}
