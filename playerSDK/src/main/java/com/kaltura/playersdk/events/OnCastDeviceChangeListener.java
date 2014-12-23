package com.kaltura.playersdk.events;

import com.google.android.gms.cast.CastDevice;

public interface OnCastDeviceChangeListener {
	public void onCastDeviceChange(CastDevice oldDevice, CastDevice newDevice);
}
