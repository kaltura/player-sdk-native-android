package com.kaltura.playersdk.cast;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.kaltura.playersdk.casting.KCastKalturaChannel;
import com.kaltura.playersdk.casting.KCastRouterManager;
import com.kaltura.playersdk.casting.KCastRouterManagerListener;
import com.kaltura.playersdk.casting.KRouterInfo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by nissimpardo on 07/12/15.
 */
public class KRouterManager implements KRouterCallback.KRouterCallbackListener, KCastRouterManager {
    private static final String TAG = "KRouterManager";
    private Context mContext;
    private MediaRouter mRouter;
    private KRouterCallback mCallback;
    private MediaRouteSelector mSelector;
    private KRouterManagerListener mListener;   
    private GoogleApiClient mApiClient;
    private String mSessionId;
    private KCastKalturaChannel mChannel;
    private CastDevice mSelectedDevice;
    private String mCastAppID;
    private KCastRouterManagerListener mAppListener;
    private ConnectionCallbacks mConnectionCallbacks;
    private ConnectionFailedListener mConnectionFailedListener;
    private Cast.Listener mCastClientListener;
    private boolean mWaitingForReconnect = false;
    private boolean mApplicationStarted = false;
    private boolean mEnableKalturaCastButton = true;
    private String nameSpace = "urn:x-cast:com.kaltura.cast.player";


    public interface KRouterManagerListener extends KRouterCallback.KRouterCallbackListener{
        void onShouldDisconnectCastDevice();
        void onConnecting();
        void onStartCasting(GoogleApiClient apiClient, CastDevice connectedDevice);
    }

    public KRouterManager(Context context, KRouterManagerListener listener) {
        mContext = context;
        mListener = listener;
    }

    public void initialize(String castAppId) {
        mCastAppID = castAppId;
        mListener.onConnecting();
            mChannel = new KCastKalturaChannel(nameSpace, new KCastKalturaChannel.KCastKalturaChannelListener() {

                @Override
                public void readyForMedia() {
                    sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
                    mListener.onStartCasting(mApiClient, mSelectedDevice);
                }
            });
        mRouter = MediaRouter.getInstance(mContext);
        mCallback = new KRouterCallback();
        mCallback.setListener(this);
        mSelector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(mCastAppID)).build();
        mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
    }

    public MediaRouteSelector getSelector() {
        return mSelector;
    }

    public void sendMessage(final String message) {
        if (mApiClient != null && mChannel != null) {
            try {
                Log.d("chromecast.sendMessage", "namespace: " + nameSpace + " message: " + message);
                Cast.CastApi.sendMessage(mApiClient, nameSpace, message)
                        .setResultCallback(
                                new ResultCallback<Status>() {
                                    @Override
                                    public void onResult(Status result) {
                                        if (result.isSuccess()) {
                                            Log.d(TAG, "namespace:" + nameSpace + " message:" + message);
                                        } else {
                                            Log.e(TAG, "Sending message failed");
                                        }
                                    }
                                });
            } catch (Exception e) {
                Log.e(TAG, "Exception while sending message", e);
            }
        }
    }

    public boolean shouldEnableKalturaCastButton() {
        return mEnableKalturaCastButton;
    }

    public KCastRouterManagerListener getAppListener() {
        return mAppListener;
    }

    public void release() {
        teardown();
        if (mRouter != null) {
            mRouter.removeCallback(mCallback);
        }
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

    private Cast.Listener getCastClientListener() {
        if (mCastClientListener == null) {
            mCastClientListener = new Cast.Listener() {
                @Override
                public void onApplicationStatusChanged() {
                    if (mApiClient != null) {
                        Log.d(TAG, "onApplicationStatusChanged: "
                                + Cast.CastApi.getApplicationStatus(mApiClient));
                    }
                }

                @Override
                public void onApplicationMetadataChanged(ApplicationMetadata applicationMetadata) {
                }

                @Override
                public void onApplicationDisconnected(int statusCode) {
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

    private void teardown() {
        Log.d(TAG, "teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected() || mApiClient.isConnecting()) {
                    try {
                        Cast.CastApi.stopApplication(mApiClient, mSessionId);
                        if (mChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    mApiClient,
                                    mChannel.getNamespace());
                            mChannel = null;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Exception while removing channel", e);
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
        } else {
            teardown();
        }
    }

    @Override
    public void onRouteAdded(boolean isAdded, KRouterInfo route) {
        mListener.onRouteAdded(isAdded, route);
    }

    @Override
    public void onFoundDevices(boolean didFound) {
        mListener.onFoundDevices(didFound);
    }

    // KCastRouterManager
    @Override
    public void disconnect() {
        mListener.onShouldDisconnectCastDevice();
        mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED);
        mSelectedDevice = null;
    }

    @Override
    public void connectDevice(String deviceId) {
        mCallback.setRouter(mRouter);
        MediaRouter.RouteInfo selectedRoute = mCallback.routeById(deviceId);
        mRouter.selectRoute(selectedRoute);
        mListener.onConnecting();
    }

    @Override
    public void setCastRouterManagerListener(KCastRouterManagerListener listener) {
        mAppListener = listener;
    }

    @Override
    public void enableKalturaCastButton(boolean enabled) {
        mEnableKalturaCastButton = enabled;
    }

    private class ConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            if (mWaitingForReconnect) {
                mWaitingForReconnect = false;

                // In case of kaltura receiver is loaded, open channel for sneding messages
            } else if (mChannel != null){
                try {
                    Cast.CastApi.launchApplication(mApiClient, mCastAppID, new LaunchOptions())
                            .setResultCallback(
                                    new ResultCallback<Cast.ApplicationConnectionResult>() {
                                        @Override
                                        public void onResult(Cast.ApplicationConnectionResult result) {
                                            Status status = result.getStatus();
                                            if (status.isSuccess()) {
//                                                ApplicationMetadata applicationMetadata =
//                                                        result.getApplicationMetadata();
                                                mSessionId = result.getSessionId();
//                                                String applicationStatus = result.getApplicationStatus();
//                                                boolean wasLaunched = result.getWasLaunched();
                                                mApplicationStarted = true;
                                                try {
                                                    Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                                                            mChannel.getNamespace(),
                                                            mChannel);
                                                } catch (IOException e) {
                                                    Log.e(TAG, "Exception while creating channel", e);
                                                }
                                                mListener.onDeviceSelected(mSelectedDevice);
                                            } else {
                                                teardown();
                                            }
                                        }
                                    });

                } catch (Exception e) {
                    Log.d(TAG, "Failed to launch application", e);
                }
            } else {
//                mListener.onStartCasting(mApiClient, mSelectedDevice);
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
}
