package com.kaltura.hlsplayersdk.events;

public interface HLSPlayerEventListener {
	public void onHLSPlayerEvent(Object body);
	public String getCallbackName();
}
