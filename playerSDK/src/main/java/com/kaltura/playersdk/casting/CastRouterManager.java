package com.kaltura.playersdk.casting;

import android.content.Context;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.util.Log;


import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;

import java.util.ArrayList;

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

    @Override
    public void disconnect() {
        router.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED);
    }

    @Override
    public void connectDevice(String deviceId) {
        for (MediaRouter.RouteInfo info: getRouteInfos()) {
            if (info.getId().equals(deviceId)) {
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
    }

    public CastRouterManager(Context context, String castAppID) {
        router = MediaRouter.getInstance(context);
        callback = new KMediaRouterCallback();
        selector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(castAppID)).build();
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
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
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            CastDevice oldDevice = selectedDevice;
            selectedDevice = CastDevice.getFromBundle(info.getExtras());
            if (oldDevice == null || !selectedDevice.isSameDevice(oldDevice)) {
                mRouterListener.castDeviceChanged(oldDevice, selectedDevice);
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            CastDevice oldDevice = selectedDevice;
            selectedDevice = null;
            if (oldDevice != null) {
                mRouterListener.castDeviceChanged(oldDevice, selectedDevice);
            }
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
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
    }
}
