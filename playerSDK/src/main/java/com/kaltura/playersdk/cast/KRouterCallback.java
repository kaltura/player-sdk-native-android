package com.kaltura.playersdk.cast;

import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;
import com.kaltura.playersdk.casting.KRouterInfo;

import java.util.ArrayList;

/**
 * Created by nissimpardo on 07/12/15.
 */
public class KRouterCallback extends MediaRouter.Callback {
    private KRouterCallbackListener mListener;
    private MediaRouter mRouter;
    private boolean didFindDevices = false;

    public interface KRouterCallbackListener {
        void onDeviceSelected(CastDevice castDeviceSelected);
        void onRouteAdded(boolean isAdded, KRouterInfo route);
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
        mListener.onDeviceSelected(CastDevice.getFromBundle(route.getExtras()));
    }

    @Override
    public void onRouteUnselected(MediaRouter router, MediaRouter.RouteInfo route) {
        mListener.onDeviceSelected(null);
    }

    @Override
    public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo route) {
        if (!didFindDevices) {
            mListener.onFoundDevices(true);
            didFindDevices = true;
        }
        KRouterInfo info = new KRouterInfo();
        info.setRouterName(route.getName());
        info.setRouterId(route.getId());
        mListener.onRouteAdded(true, info);
    }

    @Override
    public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
        if (router.getRoutes().size() == 0) {
            didFindDevices = false;
            mListener.onFoundDevices(false);
        }
        KRouterInfo info = new KRouterInfo();
        info.setRouterName(route.getName());
        info.setRouterId(route.getId());
        mListener.onRouteAdded(false, info);
    }
}
