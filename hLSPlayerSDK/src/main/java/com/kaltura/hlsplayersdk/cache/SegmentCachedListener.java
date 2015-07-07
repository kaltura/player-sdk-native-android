package com.kaltura.hlsplayersdk.cache;

import java.io.IOException;

/**
 * 
 * @author Mark
 *
 *	Used for notification of individual cache entry download results.
 *
 */
public interface SegmentCachedListener {
	public void onSegmentCompleted(String [] uri);
	public void onSegmentFailed(String uri, int errorCode);
}
