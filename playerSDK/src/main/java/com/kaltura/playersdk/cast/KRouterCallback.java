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
    private ArrayList<MediaRouter.RouteInfo> mRoutes;

    public interface KRouterCallbackListener {
        void onDeviceSelected(CastDevice castDeviceSelected);
        void onRouteAdded(boolean isAdded, KRouterInfo route);
        void onFoundDevices(boolean didFound);
    }

    public void setListener(KRouterCallbackListener listener) {
        mListener = listener;
    }

    private ArrayList<MediaRouter.RouteInfo> getRoutes() {
        if (mRoutes == null) {
            mRoutes = new ArrayList<>();
        }
        return mRoutes;
    }

    public MediaRouter.RouteInfo routeById(String routeId) {
        for (MediaRouter.RouteInfo info: getRoutes()) {
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
        if (getRoutes().size() == 0) {
            mListener.onFoundDevices(true);
        }
        getRoutes().add(route);
        KRouterInfo info = new KRouterInfo();
        info.setRouterName(route.getName());
        info.setRouterId(route.getId());
        mListener.onRouteAdded(true, info);
    }

    @Override
    public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo route) {
        getRoutes().remove(route);
        if (getRoutes().size() == 0) {
            mListener.onFoundDevices(false);
        }
        KRouterInfo info = new KRouterInfo();
        info.setRouterName(route.getName());
        info.setRouterId(route.getId());
        mListener.onRouteAdded(false, info);
    }
}
