package com.kaltura.playersdk.events;

public interface KPlayerEvalListener extends KPlayerEventListener  {
	/**
	 * Should return unique callback name
	 */
	public String getEvaluatedCallbackName();
}
