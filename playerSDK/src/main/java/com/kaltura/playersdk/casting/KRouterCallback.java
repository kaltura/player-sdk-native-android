package com.kaltura.playersdk.casting;

import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;

/**
 * Created by nissimpardo on 07/12/15.
 */
public class KRouterCallback extends MediaRouter.Callback {
    private static final String TAG = "KRouterCallback";

    private KRouterCallbackListener mListener;
    private MediaRouter mRouter;
    private boolean didFindDevices = false;
    private boolean mGuestModeEnabled;

    public KRouterCallback(boolean guestModeEnabled) {
        mGuestModeEnabled = guestModeEnabled;
    }

    public interface KRouterCallbackListener {
        void onDeviceSelected(CastDevice castDeviceSelected);
        void onRouteUpdate(boolean isAdded, KCastDevice route);
        void onFoundDevices(boolean didFound);
    }

    public void setListener(KRouterCallbackListener listener) {
        mListener = listener;
    }
    public void setRouter(MediaRouter router) {
        if (mRouter == null) {
            mRouter = router;
        }
    }

    public MediaRouter.RouteInfo routeById(String routeId) {
        for (MediaRouter.RouteInfo info: mRouter.getRoutes()) {
            if (info.getId().equals(routeId)) {
                return info;
            }
        }
        return null;
    }

    @Override
    public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
        if (mListener != null) {
            CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
            if (castDevice != null) {
                mListener.onDeviceSelected(castDevice);
            }
        }
    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
        if (mListener != null) {
            mListener.onDeviceSelected(null);
        }
    }

    @Override
    public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo route) {
        foudRoute(router, route);
    }

    @Override
    public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
        LOGD(TAG, "start onRouteAdded: " + route.getName());
        foudRoute(router, route);
    }

    private void foudRoute(MediaRouter router, MediaRouter.RouteInfo route) {
        if (mListener == null) {
            return;
        }
        if (!didFindDevices) {
            mListener.onFoundDevices(true);
            didFindDevices = true;
        }
        KCastDevice kCastDevice = new KCastDevice(route);
        CastDevice castDevice = CastDevice.getFromBundle(route.getExtras());
        boolean sendAddEvent =  (mGuestModeEnabled || (castDevice != null && castDevice.isOnLocalNetwork())) && !route.isDefaultOrBluetooth();
        LOGD(TAG, "foudRoute: isSendAddEvent = " + sendAddEvent);
        if (sendAddEvent) {
            LOGD(TAG, "foudRoute fire onRouteAdded: " + route.getName());
            mListener.onRouteUpdate(true, kCastDevice);
        }
    }

    @Override
    public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
        LOGD(TAG, "start onRouteRemoved: " + route.getName());
        if (mListener == null) {
            return;
        }
        if (router.getRoutes().size() == 0) {
            didFindDevices = false;
            LOGD(TAG, "fire onRouteRemoved->onFoundDevices: flase");
            mListener.onFoundDevices(false);
        }
        KCastDevice info = new KCastDevice(route);
        LOGD(TAG, "onRouteRemoved->fire onRouteUpdate :" + route.getName());
        mListener.onRouteUpdate(false, info);
    }
}
