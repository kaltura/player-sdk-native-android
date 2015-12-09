package com.kaltura.playersdk.casting;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;


import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;

/**
 * Created by nissimpardo on 18/11/15.
 */
public class CastRouterManager implements KCastRouterManager, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static CastRouterManagerListener mRouterListener;
    private static CastDevice selectedDevice;
    GoogleApiClient mApiClient;
    private static MediaRouter.Callback callback;
    private static MediaRouteSelector selector;
    private static MediaRouter router;
    private static ArrayList<MediaRouter.RouteInfo> routeInfos;
    private static boolean shouldEnableKalturaButton =true;
    private KCastRouterManagerListener appListener;
    private static BaseCastManager mCastManager;
    private Context mContext;

    private boolean mApplicationStarted = false;
    private boolean mWaitingForReconnect = false;
    private String mSessionId;
    private CastDevice mSelectedDevice;
    private KCastKalturaChannel mKalturaChannel;

    public static final double VOLUME_INCREMENT = 0.05;
    public static final int PRELOAD_TIME_S = 20;



    public interface CastRouterManagerListener {
        public void didFoundDevices(boolean didFound);
        public void updateDetectedDeviceList(boolean shouldAdd, KRouterInfo info);
        public void castDeviceChanged(CastDevice oldDevice, CastDevice newDevice);
        public void shouldDisconnectCastDevice();
        public void connecting();
    }

    @Override
    public void disconnect() {
//        router.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED);
//        mCastManager.disconnectDevice(true, true, true);
        mRouterListener.shouldDisconnectCastDevice();
    }

    @Override
    public void connectDevice(String deviceId) {
        for (MediaRouter.RouteInfo info: getRouteInfos()) {
            if (info.getId().equals(deviceId)) {
                mRouterListener.connecting();
                router.selectRoute(info);
                return;
            }
        }
        Log.d("CastRouterManager", "No such Cast Device ID");
    }

    @Override
    public void setCastRouterManagerListener(KCastRouterManagerListener listener) {
        appListener = listener;
    }

    @Override
    public void enableKalturaCastButton(boolean enabled) {
        shouldEnableKalturaButton = enabled;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mWaitingForReconnect) {
            mWaitingForReconnect = false;
//            reconnectChannels();
        } else {
            try {
                Cast.CastApi.launchApplication(mApiClient, "YOUR_APPLICATION_ID", false)
                        .setResultCallback(
                                new ResultCallback<Cast.ApplicationConnectionResult>() {
                                    @Override
                                    public void onResult(Cast.ApplicationConnectionResult result) {
                                        Status status = result.getStatus();
                                        if (status.isSuccess()) {
                                            ApplicationMetadata applicationMetadata =
                                                    result.getApplicationMetadata();
                                            String sessionId = result.getSessionId();
                                            String applicationStatus = result.getApplicationStatus();
                                            boolean wasLaunched = result.getWasLaunched();

                                        } else {
                                            teardown();
                                        }
                                    }
                                });

            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), "Failed to launch application", e);
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }



    public CastRouterManager(Context context) {
        mContext = context;
    }

    public void initialize(String castAppID, Context context) {
        // initialize VideoCastManager
        VideoCastManager.
                initialize(context, castAppID, null, null).
                setVolumeStep(VOLUME_INCREMENT).
                enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
                        VideoCastManager.FEATURE_LOCKSCREEN |
                        VideoCastManager.FEATURE_WIFI_RECONNECT |
                        VideoCastManager.FEATURE_AUTO_RECONNECT |
                        VideoCastManager.FEATURE_CAPTIONS_PREFERENCE |
                        VideoCastManager.FEATURE_DEBUGGING);

        // this is the default behavior but is mentioned to make it clear that it is configurable.
        VideoCastManager.getInstance().setNextPreviousVisibilityPolicy(
                VideoCastController.NEXT_PREV_VISIBILITY_POLICY_DISABLED);

        // this is to set the launch options, the following values are the default values
        VideoCastManager.getInstance().setLaunchOptions(false, Locale.getDefault());

        // this is the default behavior but is mentioned to make it clear that it is configurable.
        VideoCastManager.getInstance().setCastControllerImmersive(true);
        mCastManager = VideoCastManager.getInstance();
        router = MediaRouter.getInstance(context);
        callback = new KMediaRouterCallback();
        selector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(castAppID)).build();
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        mCastManager = VideoCastManager.getInstance();
        VideoCastConsumerImpl castConsumer = new VideoCastConsumerImpl() {

            @Override
            public void onFailed(int resourceId, int statusCode) {
                String reason = "Not Available";
                if (resourceId > 0) {
//                    reason = getString(resourceId);
                }
                Log.e(getClass().getSimpleName(), "Action failed, reason:  " + reason + ", status code: " + statusCode);
            }

            @Override
            public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId,
                                               boolean wasLaunched) {
//                invalidateOptionsMenu();
                Log.d(getClass().getSimpleName(), appMetadata.getName());
                mRouterListener.castDeviceChanged(null, selectedDevice);
            }



            @Override
            public void onDisconnected() {
//                invalidateOptionsMenu();
                mRouterListener.castDeviceChanged(null, null);
            }

            @Override
            public void onConnectionSuspended(int cause) {
                Log.d(getClass().getSimpleName(), "onConnectionSuspended() was called with cause: " + cause);
//                com.google.sample.cast.refplayer.utils.Utils.
//                        showToast(VideoBrowserActivity.this, R.string.connection_temp_lost);
            }

            @Override
            public void onConnectivityRecovered() {
//                com.google.sample.cast.refplayer.utils.Utils.
//                        showToast(VideoBrowserActivity.this, R.string.connection_recovered);
            }
        };
        VideoCastManager.getInstance().addVideoCastConsumer(castConsumer);
    }

    public void setCastRouterListener(CastRouterManagerListener listener) {
        mRouterListener = listener;
    }

    public static ArrayList<MediaRouter.RouteInfo> getRouteInfos() {
        if (routeInfos == null) {
            routeInfos = new ArrayList<>();
        }
        return routeInfos;
    }

    private void teardown() {
        Log.d(getClass().getSimpleName(), "teardown");
        if (mApiClient != null) {
            if (mApplicationStarted) {
                if (mApiClient.isConnected() || mApiClient.isConnecting()) {
                    try {
                        Cast.CastApi.stopApplication(mApiClient, mSessionId);
                        if (mKalturaChannel != null) {
                            Cast.CastApi.removeMessageReceivedCallbacks(
                                    mApiClient,
                                    mKalturaChannel.getNamespace());
                            mKalturaChannel = null;
                        }
                    } catch (IOException e) {
                        Log.e(getClass().getSimpleName(), "Exception while removing channel", e);
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

    private Cast.CastOptions.Builder getCastOptionBuilder(CastDevice castDevice) {
        return Cast.CastOptions
                .builder(castDevice, new Cast.Listener(){
                    @Override
                    public void onApplicationStatusChanged() {
                        if (mApiClient != null) {
                            Log.d(getClass().getSimpleName(), "onApplicationStatusChanged: "
                                    + Cast.CastApi.getApplicationStatus(mApiClient));
                            switch (Cast.CastApi.getApplicationStatus(mApiClient)) {

                            }
                        }
                    }

                    @Override
                    public void onApplicationMetadataChanged(ApplicationMetadata applicationMetadata) {
                        super.onApplicationMetadataChanged(applicationMetadata);
                    }

                    @Override
                    public void onApplicationDisconnected(int statusCode) {
                        super.onApplicationDisconnected(statusCode);
                    }

                    @Override
                    public void onActiveInputStateChanged(int activeInputState) {
                        super.onActiveInputStateChanged(activeInputState);
                    }

                    @Override
                    public void onStandbyStateChanged(int standbyState) {
                        super.onStandbyStateChanged(standbyState);
                    }

                    @Override
                    public void onVolumeChanged() {
                        super.onVolumeChanged();
                    }
                });
    }

    private void onDeviceSelected(CastDevice castDevice) {
        mSelectedDevice = castDevice;
        if (mApiClient == null) {
            LOGD(getClass().getSimpleName(), "acquiring a connection to Google Play services for " + castDevice);
            Cast.CastOptions.Builder apiOptionsBuilder = getCastOptionBuilder(castDevice);
            mApiClient = new GoogleApiClient.Builder(mContext)
                    .addApi(Cast.API, apiOptionsBuilder.build())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mApiClient.connect();
        } else if (!mApiClient.isConnected() && !mApiClient.isConnecting()) {
            mApiClient.connect();
        }
    }


    public KCastRouterManagerListener getAppListener() {
        return appListener;
    }

    public boolean shouldEnableKalturaCastButton() {
        return shouldEnableKalturaButton;
    }


    private static class KMediaRouterCallback extends MediaRouter.Callback {

        private boolean mRouteAvailable = false;
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
//            if (mCastManager.getReconnectionStatus() == BaseCastManager.RECONNECTION_STATUS_FINALIZED) {
//                mCastManager.setReconnectionStatus(BaseCastManager.RECONNECTION_STATUS_INACTIVE);
//                mCastManager.cancelReconnectionTask();
//            }
//            mCastManager.getPreferenceAccessor().saveStringToPreference(
//                    BaseCastManager.PREFS_KEY_ROUTE_ID, info.getId());
//            CastDevice oldDevice = selectedDevice;
//            selectedDevice = CastDevice.getFromBundle(info.getExtras());
//            mCastManager.onDeviceSelected(selectedDevice);

//            if (oldDevice == null || !selectedDevice.isSameDevice(oldDevice)) {
//                mRouterListener.castDeviceChanged(oldDevice, selectedDevice);
//            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
//            CastDevice oldDevice = selectedDevice;
            mCastManager.onDeviceSelected(null);
//            selectedDevice = null;
//            if (oldDevice != null) {
//                mRouterListener.castDeviceChanged(oldDevice, selectedDevice);
//            }
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            if (!router.getDefaultRoute().equals(route)) {
                notifyRouteAvailabilityChangedIfNeeded(router);
                mCastManager.onCastDeviceDetected(route);
            }

            if (mCastManager.getReconnectionStatus()
                    == BaseCastManager.RECONNECTION_STATUS_STARTED) {
                String routeId = mCastManager.getPreferenceAccessor().getStringFromPreference(
                        BaseCastManager.PREFS_KEY_ROUTE_ID);
                if (route.getId().equals(routeId)) {
                    // we found the route, so lets go with that
                    LOGD(getClass().getSimpleName(), "onRouteAdded: Attempting to recover a session with info=" + route);
                    mCastManager.setReconnectionStatus(BaseCastManager.RECONNECTION_STATUS_IN_PROGRESS);

                    CastDevice device = CastDevice.getFromBundle(route.getExtras());
                    LOGD(getClass().getSimpleName(), "onRouteAdded: Attempting to recover a session with device: "
                            + device.getFriendlyName());
                    mCastManager.onDeviceSelected(device);
                }
            }
            // Add route to list of discovered routes
            synchronized (this) {
                if (getRouteInfos().size() == 0) {
                    mRouterListener.didFoundDevices(true);
                }
                getRouteInfos().add(route);
                KRouterInfo info = new KRouterInfo();
                info.setRouterId(route.getId());
                info.setRouterName(route.getName());
                mRouterListener.updateDetectedDeviceList(true, info);
            }
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
            notifyRouteAvailabilityChangedIfNeeded(router);
            synchronized (this) {
                ArrayList<MediaRouter.RouteInfo> tempInfos = new ArrayList<>(getRouteInfos());
                for (MediaRouter.RouteInfo currentInfo: tempInfos) {
                    if (route.equals(currentInfo)) {
                        getRouteInfos().remove(currentInfo);
                    }
                }
                if (getRouteInfos().size() == 0) {
                    mRouterListener.didFoundDevices(false);
                }
                KRouterInfo info = new KRouterInfo();
                info.setRouterId(route.getId());
                info.setRouterName(route.getName());
                mRouterListener.updateDetectedDeviceList(false, info);
            }
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
            notifyRouteAvailabilityChangedIfNeeded(router);
        }

        private void notifyRouteAvailabilityChangedIfNeeded(MediaRouter router) {
            boolean routeAvailable = isRouteAvailable(router);
            if (routeAvailable != mRouteAvailable) {
                // availability of routes have changed
                mRouteAvailable = routeAvailable;
                mCastManager.onCastAvailabilityChanged(mRouteAvailable);
            }
        }

        private boolean isRouteAvailable(MediaRouter router) {
            return router.isRouteAvailable(mCastManager.getMediaRouteSelector(),
                    MediaRouter.AVAILABILITY_FLAG_IGNORE_DEFAULT_ROUTE
                            | MediaRouter.AVAILABILITY_FLAG_REQUIRE_MATCH);
        }
    }
}
