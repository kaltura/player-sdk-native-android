package com.kaltura.playersdk.casting;

import android.content.Context;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;


import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.libraries.cast.companionlibrary.cast.BaseCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.android.libraries.cast.companionlibrary.cast.player.VideoCastController;

import java.util.ArrayList;
import java.util.Locale;

import static com.google.android.libraries.cast.companionlibrary.utils.LogUtils.LOGD;

/**
 * Created by nissimpardo on 18/11/15.
 */
public class CastRouterManager implements KCastRouterManager {
    private static CastRouterManagerListener mRouterListener;
    private static CastDevice selectedDevice;
    private static MediaRouter.Callback callback;
    private static MediaRouteSelector selector;
    private static MediaRouter router;
    private static ArrayList<MediaRouter.RouteInfo> routeInfos;
    private static boolean shouldEnableKalturaButton =true;
    private KCastRouterManagerListener appListener;
    private static BaseCastManager mCastManager;

    public static final double VOLUME_INCREMENT = 0.05;
    public static final int PRELOAD_TIME_S = 20;

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

    public interface CastRouterManagerListener {
        public void didFoundDevices(boolean didFound);
        public void updateDetectedDeviceList(boolean shouldAdd, KRouterInfo info);
        public void castDeviceChanged(CastDevice oldDevice, CastDevice newDevice);
        public void shouldDisconnectCastDevice();
        public void connecting();
    }

    public CastRouterManager(Context context, String castAppID) {
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
            if (mCastManager.getReconnectionStatus() == BaseCastManager.RECONNECTION_STATUS_FINALIZED) {
                mCastManager.setReconnectionStatus(BaseCastManager.RECONNECTION_STATUS_INACTIVE);
                mCastManager.cancelReconnectionTask();
            }
            mCastManager.getPreferenceAccessor().saveStringToPreference(
                    BaseCastManager.PREFS_KEY_ROUTE_ID, info.getId());
            CastDevice oldDevice = selectedDevice;
            selectedDevice = CastDevice.getFromBundle(info.getExtras());
            mCastManager.onDeviceSelected(selectedDevice);
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
