package com.kaltura.playersdk.casting;

import android.app.Activity;
import android.content.Context;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;

import java.util.ArrayList;

/**
 * Created by nissimpardo on 10/11/15.
 */
public class CastManager {
    private static VideoCastManager mCastManager;
    private static CastManagerListener mListener;
    private static ArrayList<MediaRouter.RouteInfo> routeInfos;
    private static CastDevice selectedDevice;
    private static boolean isConnected = false;
    private static MediaRouter.Callback callback;
    private static MediaRouteSelector selector;
    private static MediaRouter router;
    private static boolean detectedDevicesIsPresented= false;

    public interface CastManagerListener {
        public void didFoundDevices(boolean didFound);
        public void updateDetectedDeviceList(boolean shouldAdd, String routeDescription);
        public void castDeviceChanged(CastDevice oldDevice, CastDevice newDevice);
    }

    public static void initialize(Activity context, CastManagerListener listetner) {
        mListener = listetner;
//        getCastManager(context);
        router = MediaRouter.getInstance(context);
        callback = new KMediaRouterCallback();
        selector = new MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast("FFCC6D19")).build();
        router.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
        MediaRouter.RouteInfo route = router.updateSelectedRoute(selector);
    }

    private static VideoCastManager getCastManager(Context context) {
        if (mCastManager == null) {
            mCastManager = VideoCastManager.initialize(context, "DB6462E9",null, null);
            mCastManager.enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
                    VideoCastManager.FEATURE_DEBUGGING);
        }
        return mCastManager;
    }

    public static ArrayList<MediaRouter.RouteInfo> getRouteInfos() {
        if (routeInfos == null) {
            routeInfos = new ArrayList<>();
        }
        return routeInfos;
    }

    public static ArrayList<String> getCastDevicesList() {
        if (getRouteInfos().size() == 0) {
            return null;
        }
        ArrayList<String> deviceList = new ArrayList<>();
        if (mCastManager.isConnected()) {
            deviceList.add("Disconnect");
        } else {
            for (MediaRouter.RouteInfo info : getRouteInfos()) {
                deviceList.add(info.getName());
            }
        }
        return deviceList;
    }

    public static void connectToCastDeviceAtIndex(int index) {
        router.selectRoute(getRouteInfos().get(index));
    }

    public static void disconnect() {
        router.unselect(MediaRouter.UNSELECT_REASON_DISCONNECTED);
    }


    public static void setDetectedDevicesIsPresented(boolean isPresented) {
        detectedDevicesIsPresented = isPresented;
    }

    private static class KMediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo info) {
            CastDevice oldDevice = selectedDevice;
            selectedDevice = CastDevice.getFromBundle(info.getExtras());
            if (oldDevice == null || !selectedDevice.isSameDevice(oldDevice)) {
                CastManager.mListener.castDeviceChanged(oldDevice, selectedDevice);
            }
        }

        @Override
        public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
            CastDevice oldDevice = selectedDevice;
            selectedDevice = null;
            if (oldDevice != null) {
                CastManager.mListener.castDeviceChanged(oldDevice, selectedDevice);
            }
        }

        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
            // Add route to list of discovered routes
            synchronized (this) {
                if (CastManager.getRouteInfos().size() == 0) {
                    CastManager.mListener.didFoundDevices(true);
                }
                CastManager.getRouteInfos().add(route);
                if (detectedDevicesIsPresented) {
                    CastManager.mListener.updateDetectedDeviceList(true, route.getName());
                }
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
                    CastManager.mListener.didFoundDevices(false);
                } else if (detectedDevicesIsPresented) {
                    CastManager.mListener.updateDetectedDeviceList(false, route.getName());
                }
            }
        }
    }
}
