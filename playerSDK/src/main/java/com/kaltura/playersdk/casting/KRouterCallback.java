package com.kaltura.playersdk.casting;

import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;

/**
 * Created by nissimpardo on 07/12/15.
 */
public class KRouterCallback extends MediaRouter.Callback {
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
    public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
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

        if (sendAddEvent) {
            mListener.onRouteUpdate(true, kCastDevice);
        }
    }

    @Override
    public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
        if (mListener == null) {
            return;
        }
        if (router.getRoutes().size() == 0) {
            didFindDevices = false;
            mListener.onFoundDevices(false);
        }
        KCastDevice info = new KCastDevice(route);
        mListener.onRouteUpdate(false, info);
    }
}
