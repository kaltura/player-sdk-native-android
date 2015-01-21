package com.kaltura.playersdk.events;

public interface KPlayerEventListener {
	public void onKPlayerEvent(Object body);
	public String getCallbackName();
}
