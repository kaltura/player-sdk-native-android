package com.kaltura.playersdk.casting;

import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;

/**
 * Created by nissimpardo on 18/11/15.
 */
public class KCastDevice {
    private String routerName;
    private String routerId;

    public KCastDevice(MediaRouter.RouteInfo info) {
        routerName = info.getName();
        routerId = info.getId();
    }

    public KCastDevice(CastDevice castDevice) {
        routerName = castDevice.getFriendlyName();
        routerId = castDevice.getDeviceId();
    }

    public String getRouterName() {
        return routerName;
    }

    public void setRouterName(String routerName) {
        this.routerName = routerName;
    }

    public String getRouterId() {
        return routerId;
    }

    public void setRouterId(String routerId) {
        this.routerId = routerId;
    }

    @Override
    public String toString() {
        return routerName;
    }
}
