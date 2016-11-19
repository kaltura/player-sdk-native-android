package com.kaltura.playersdk.casting;

import android.support.v7.media.MediaRouter;

import com.google.android.gms.cast.CastDevice;

/**
 * Created by nissimpardo on 18/11/15.
 */
public class KCastDevice {
    private String routerName;
    private String modelName;
    private String deviceVersion;
    private String routerId;

    public KCastDevice(MediaRouter.RouteInfo info) {
        routerName = info.getName();
        routerId = info.getId();
    }

    public KCastDevice(CastDevice castDevice) {
        routerName = castDevice.getFriendlyName();
        modelName = castDevice.getModelName();
        deviceVersion = castDevice.getDeviceVersion();
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

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getDeviceVersion() {
        return deviceVersion;
    }

    public void setDeviceVersion(String deviceVersion) {
        this.deviceVersion = deviceVersion;
    }

    @Override
    public String toString() {
        return routerName;
    }
}
