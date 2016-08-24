package com.kaltura.playersdk.casting;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;


/**
 * Created by nissimpardo on 29/05/16.
 */
public class KCastProviderImpl implements com.kaltura.playersdk.interfaces.KCastProvider,
                KRouterCallback.KRouterCallbackListener, GoogleApiClient.ConnectionCallbacks,
                GoogleApiClient.OnConnectionFailedListener, KCastKalturaChannel.KCastKalturaChannelListener {
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

    private boolean mWaitingForReconnect = false;
    private boolean mApplicationStarted = false;
    private boolean mCastButtonEnabled = false;
    private boolean mGuestModeEnabled = false; // do not enable paring the cc from guest network
    private KCastMediaRemoteControl mCastMediaRemoteControl;

    private String mSessionId;
    private String mRouterId;

    private InternalListener mInternalListener;
    protected AsyncTask<Void, Integer, Boolean> mReconnectionTask;

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
    public void startScan(Context context, String appID, boolean guestModeEnabled, boolean reconnectSessionIfPossible) {
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

        if (reconnectSessionIfPossible) {
            reconnectSessionIfPossible();
        }
    }


    @Override
    public void startScan(Context context, String appID) {
        startScan(context, appID, false, true);
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
        disconnectFromDevice(true);
        clearPersistedConnectionInfo();
    }

    /**
     * This method disconnects the receiver from the current sender.
     * The receiver will stop playing only when the last connected sender disconnects.
     * @param disableReconnectOption - if this argument is false - the sender will automatically
     * try to reconnect to the receiver the next time the app launches.
     * This is the recommended behaviour when the user explicitly chooses to disconnect.
     */
    @Override
    public void disconnectFromDevice(boolean disableReconnectOption) {
        //        if (mScanCastDeviceListener != null) {
//            mScanCastDeviceListener.onDisconnectCastDevice();
//        }
        if (mRouter != null) {
            mRouter.unselect(MediaRouter.UNSELECT_REASON_STOPPED);
            mSelectedDevice = null;
        }

        if (disableReconnectOption) {
            disableReconnectOption();
        }
    }


    /**
     * This method disables the reconnect option, meaning the app won't try to automatically reconnect
     * to the receiver when some time in the future the app will launch again.
     * The recommended behaviour is to call this method only when the user explicitly disconnects from
     * the receiver. If the user implicitly disconnects (closing the app, network connectivity lost etc...)
     * the recommended behaviour is not to invoke this method.
     */
    @Override
    public void disableReconnectOption() {
        // clear session id + rout id data
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

                        try {
                            String status = Cast.CastApi.getApplicationStatus(mApiClient);
                            LOGD(TAG, "onApplicationStatusChanged: " + status);


                            if ("Ready to play".equals(status) && mProviderListener != null) {
                                mProviderListener.onDeviceConnected(null,null);
                            }

                        } catch (IllegalStateException e) {
                            LOGE(TAG, e.getMessage());
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

    private void teardown() {
        LOGD(TAG, "teardown");
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
            mRouterId = castDeviceSelected.getDeviceId();
            Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions
                    .builder(castDeviceSelected, getCastClientListener());
            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
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
        if (mProviderListener != null) {
            if (isAdded) {
                mProviderListener.onDeviceCameOnline(route);
            } else {
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

    public boolean hasMediaSession() {
        return mCastMediaRemoteControl != null && mCastMediaRemoteControl.hasMediaSession();
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mWaitingForReconnect) {
            mWaitingForReconnect = false;
            reconnectChannels(bundle);
            // In case of kaltura receiver is loaded, open channel for sneding messages
        } else {
            try {
                loadSession();
                if (mSessionId != null && !mSessionId.isEmpty()) {
                    Cast.CastApi.joinApplication(mApiClient, mCastAppID, mSessionId)
                            .setResultCallback(new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(Cast.ApplicationConnectionResult result) {
                                    if (result.getStatus().isSuccess()) {
                                        onLauchedAppResult(result.getStatus().isSuccess(), result.getSessionId());
                                    } else {
                                        lauchCastApp();
                                    }
                                }
                            });
                } else {
                    lauchCastApp();
                }

            } catch (Exception e) {
                LOGD(TAG, "Failed to launch application", e);
            }

        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        mWaitingForReconnect = true;
        switch (i) {
            case CAUSE_NETWORK_LOST:
                break;
            case CAUSE_SERVICE_DISCONNECTED:
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        teardown();
    }

    private void reconnectChannels( Bundle hint ) {
        if( ( hint != null ) && hint.getBoolean( Cast.EXTRA_APP_NO_LONGER_RUNNING ) ) {
            //Log.e( TAG, "App is no longer running" );
            teardown();
        } else {
            onLauchedAppResult(true, mSessionId);
        }
    }

    private void lauchCastApp() {
        try {
            Cast.CastApi.launchApplication(mApiClient, mCastAppID, new LaunchOptions())
                    .setResultCallback(
                            new ResultCallback<Cast.ApplicationConnectionResult>() {
                                @Override
                                public void onResult(Cast.ApplicationConnectionResult result) {
                                    onLauchedAppResult(result.getStatus().isSuccess(), result.getSessionId());
                                }
                            });

        } catch (Exception e) {
            LOGD(TAG, "Failed to launch application", e);
        }
    }

    private void onLauchedAppResult(boolean isSuccess, String sessionId) {
        if (isSuccess && sessionId != null) {
            mSessionId = sessionId;
            saveSession();
            mChannel = new KCastKalturaChannel(nameSpace, this);
            mApplicationStarted = true;
            sendMessage("{\"type\":\"show\",\"target\":\"logo\"}");

            try {
                Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                        mChannel.getNamespace(),
                        mChannel);
            } catch (IOException e) {
                LOGE(TAG, "Exception while creating channel", e);
            }

            if (mProviderListener != null) {
                mProviderListener.onDeviceConnected(null,null);
            }
        } else {
            teardown();
        }
    }

    @Override
    public void readyForMedia(String[] castParams) {
        sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
        // Receiver send the new content
        String[] params = castParams;
        if (params == null) {
            params = KCastPrefs.loadArray(mContext, KCastPrefs.CHANNEL_PARAMS);
        }

        if (params != null) {
            KCastPrefs.saveArray(mContext, KCastPrefs.CHANNEL_PARAMS, params);
            mCastMediaRemoteControl = new KChromeCastPlayer(mApiClient);
            ((KChromeCastPlayer)mCastMediaRemoteControl).setMediaInfoParams(params);
            if (mInternalListener != null) {
                mInternalListener.onStartCasting((KChromeCastPlayer) mCastMediaRemoteControl);
            }
        }
    }

    public void prepareChannel() {
        if (mChannel == null) {
            return;
        }
        //if (mCastMediaRemoteControl == null) {
        //mChannel = new KCastKalturaChannel(nameSpace, KCastProviderImpl.this);
        sendMessage("{\"type\":\"show\",\"target\":\"logo\"}");
        try {
            Cast.CastApi.setMessageReceivedCallbacks(mApiClient,
                    mChannel.getNamespace(),
                    mChannel);
        } catch (IOException e) {
            LOGE(TAG, "Exception while creating channel", e);
        }
        //readyForMedia(null);
        //}
        /*if (mInternalListener != null) {
            mInternalListener.onStartCasting((KChromeCastPlayer) mCastMediaRemoteControl);
        }*/
    }

    private void saveSession() {
        KCastPrefs.save(mContext, KCastPrefs.ROUTER_ID,  mRouterId);
        KCastPrefs.save(mContext, KCastPrefs.SESSION_ID, mSessionId);
    }

    private void loadSession() {
        mRouterId  = KCastPrefs.loadString(mContext, KCastPrefs.ROUTER_ID);
        mSessionId = KCastPrefs.loadString(mContext, KCastPrefs.SESSION_ID);
    }


    @Override
    public void setCastMetaDataBundle(CastMetaDataBundle metaDataBundle) {

    }

    @Override
    public CastMetaDataBundle getCastMetaDataBundle() {
        return null;
    }


    public final boolean isConnected() {
        return (mApiClient != null) && mApiClient.isConnected();
    }

    private void reconnectSessionIfPossible() {
        reconnectSessionIfPossible(5);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void reconnectSessionIfPossible(final int timeoutInSeconds) { //, String ssidName) {
        LOGD(TAG, String.format("reconnectSessionIfPossible timeout = " + timeoutInSeconds));
        if (isConnected()) {
            return;
        }
        String routeId = KCastPrefs.loadString(mContext, KCastPrefs.ROUTER_ID);
        if (canConsiderSessionRecovery()) {//(ssidName)) {
            List<MediaRouter.RouteInfo> routes = mRouter.getRoutes();
            MediaRouter.RouteInfo theRoute = null;
            if (routes != null) {
                for (MediaRouter.RouteInfo route : routes) {
                    if (route.getId().equals(routeId)) {
                        theRoute = route;
                        break;
                    }
                }
            }
            if (theRoute != null) {
                // route has already been discovered, so lets just get the device
                reconnectSessionIfPossibleInternal(theRoute);
            } else {
                // we set a flag so if the route is discovered within a short period, we let
                // onRouteAdded callback of CastMediaRouterCallback take care of that
                //setReconnectionStatus(RECONNECTION_STATUS_STARTED);
            }

            // cancel any prior reconnection task
            if (mReconnectionTask != null && !mReconnectionTask.isCancelled()) {
                mReconnectionTask.cancel(true);
            }

            // we may need to reconnect to an existing session
            mReconnectionTask = new AsyncTask<Void, Integer, Boolean>() {

                @Override
                protected Boolean doInBackground(Void... params) {
                    for (int i = 0; i < timeoutInSeconds; i++) {
                        LOGD(TAG, "Reconnection: Attempt " + (i + 1));
                        if (isCancelled()) {
                            return true;
                        }
                        try {
                            if (isConnected()) {
                                cancel(true);
                            }
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    if (result == null || !result) {
                        LOGD(TAG, "Couldn't reconnect, dropping connection");
                        //setReconnectionStatus(RECONNECTION_STATUS_INACTIVE);
                        onDeviceSelected(null /* CastDevice */);
                    }
                }

            };
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mReconnectionTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                mReconnectionTask.execute();
            }
        }
    }

    private void reconnectSessionIfPossibleInternal(MediaRouter.RouteInfo theRoute) {
        if (isConnected()) {
            return;
        }
        String sessionId = KCastPrefs.loadString(mContext, KCastPrefs.SESSION_ID);
        String routeId = KCastPrefs.loadString(mContext, KCastPrefs.ROUTER_ID);
        LOGD(TAG, "reconnectSessionIfPossible() Retrieved from preferences: " + "sessionId="
                + sessionId + ", routeId=" + routeId);
        if (sessionId == null || routeId == null) {
            return;
        }
        //setReconnectionStatus(RECONNECTION_STATUS_IN_PROGRESS);
        CastDevice device = CastDevice.getFromBundle(theRoute.getExtras());

        if (device != null) {
            LOGD(TAG, "trying to acquire Cast Client for " + device);
            onDeviceSelected(device);
        }
    }

    public final boolean canConsiderSessionRecovery() { //(String ssidName) {
        String sessionId = KCastPrefs.loadString(mContext, KCastPrefs.SESSION_ID);
        String routeId = KCastPrefs.loadString(mContext, KCastPrefs.ROUTER_ID);
        if (sessionId == null || routeId == null) {
            return false;
        }

//        String ssid = mPreferenceAccessor.getStringFromPreference(PREFS_KEY_SSID);
//        if (ssidName != null && (ssid == null || (!ssid.equals(ssidName)))) {
//            return false;
//        }
        LOGD(TAG, "Found session info in the preferences, so proceed with an "
                + "attempt to reconnect if possible");
        return true;
    }

    public final void clearPersistedConnectionInfo() {
        KCastPrefs.save(mContext, KCastPrefs.ROUTER_ID, null);
        KCastPrefs.save(mContext, KCastPrefs.SESSION_ID, null);//        if (isFlagSet(what, CLEAR_SESSION)) {
    }
}
