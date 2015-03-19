package com.kaltura.hlsplayersdk.events;

/*
 * This event is fired once when there is a line of text available.
 * 
 * The start time and length is in seconds, and the buffer contains
 * the text to be displayed.
 * 
 */

public interface OnTextTrackTextListener {
	void onSubtitleText(double startTime, double length, String align, String buffer);
}
