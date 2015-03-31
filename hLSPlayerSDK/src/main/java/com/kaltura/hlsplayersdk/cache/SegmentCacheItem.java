package com.kaltura.hlsplayersdk.cache;

import android.os.Handler;
import android.util.Log;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.hlsplayersdk.events.OnErrorListener;
import com.loopj.android.http.*;

public class SegmentCacheItem {
	public String uri;
	public byte[] data;
	public boolean running = false;
	public boolean waiting = false;
	public long lastTouchedMillis;
	public long downloadStartTime = 0;
	public long downloadCompletedTime = 0;
	public long forceSize = -1;

	// If >= 0, ID of a crypto context on the native side.
	protected int cryptoHandle = -1;

	// All bytes < decryptHighWaterMark are descrypted; all >=  are still 
	// encrypted. This allows us to avoid duplicating every segment.
	protected long decryptHighWaterMark = 0;
	private boolean fullyDecrypted = false;
	
	// We will retry 3 times before giving up
	private static final int maxRetries = 3;
	private int curRetries = 0;
	
	
	public static native int allocAESCryptoState(byte[] key, byte[] iv);
	public static native void freeCryptoState(int id);
	public static native long decrypt(int cryptoHandle, byte[] data, long start, long length);
	
	public RequestHandle request = null;
	
	SegmentCacheEntry cacheEntry = null;
	
	public SegmentCacheItem(SegmentCacheEntry entry)
	{
		cacheEntry = entry;
	}
	
	public int bytesDownloaded = 0;
	public int expectedSize = 0;
	
	public void cancel()
	{
		if (running)
		{
			Log.i("HLS Cache", "Cancelling " + uri);
			running = false;
			waiting = false;
		}
		
	}
	
	public boolean hasCrypto()
	{
		return (cryptoHandle != -1);
	}

	public void setCryptoHandle(int handle)
	{
		// If we already have a crypto handle, I don't think we want to reset it
		if (cryptoHandle == -1)
			cryptoHandle = handle;
		else if (handle != cryptoHandle)
			Log.i("setCryptoHandle", "Tried to change an existing cryptoHandle (" + cryptoHandle + ") to (" + handle + ")");
	}

	public void ensureDecryptedTo(long offset)
	{
		if(cryptoHandle == -1)
			return;
//		if (offset == 188)
//		{
//			Log.i("HLS Cache", "Decrypting " + uri);
//			Log.i("HLS Cache", "  to " + offset);
//			Log.i("HLS Cache", "  first bytes = " + data[0] + data[1] + data[2] + data[3] + data[4]);
//		}
		long delta = offset - decryptHighWaterMark;
//		if (offset == 188)
//			Log.i("HLS Cache", "  delta = " + delta + " | HighWaterMark = " + decryptHighWaterMark);
		
		if (delta > 0)
			decryptHighWaterMark = decrypt(cryptoHandle, data, decryptHighWaterMark, delta);
//		if (offset == 188)
//		{
//			Log.i("HLS Cache", "Decrypted to " + decryptHighWaterMark);
//			Log.i("HLS Cache", "  first bytes = " + data[0] + data[1] + data[2] + data[3] + data[4]);
//		}
	}

	public boolean isFullyDecrypted()
	{
		return (decryptHighWaterMark == data.length);
	}
	
	private boolean retry()
	{
		++curRetries;
		if (curRetries >= maxRetries) return false;
		return true;
	}
	
	public void postOnSegmentFailed(int statusCode)
	{
		if (retry())
		{
			if (statusCode == 0) HLSSegmentCache.expire();
			Log.i("SegmentCacheItem.postOnSegmentFailed", "Segment download failed. Retrying: " + uri + " : " + statusCode);
			cacheEntry.retry(this);
		}
		else
		{
			Log.i("SegmentCacheItem.postOnSegmentFailed", "Segment download failed. No More Retries Left: " + uri + " : " + statusCode);
			running = false;
			cacheEntry.postItemFailed(this, statusCode);
		}
	}
	
	public void postSegmentSucceeded(int statusCode, byte[] responseData)
	{
		if (statusCode == 200)
		{
			data = responseData;
			
			downloadCompletedTime = System.currentTimeMillis();
			Log.i("SegmentCacheItem.postSegmentSucceeded", "Got " + (responseData != null ? responseData.length + " bytes for " : " null document for " )  + uri);
			if (waiting) updateProgress(responseData != null ? responseData.length : 0, expectedSize);
			if (waiting) cacheEntry.updateProgress(true);
			running = false; // We are still running until we've posted the success!!!
			cacheEntry.postItemSucceeded(this, statusCode);
			

		}
		else
		{
			Log.i("SegmentCacheItem.postSegmentSucceeded", "statusCode " + "[" + statusCode + "]" + uri);
			postOnSegmentFailed(statusCode);
		}
	}
	
	public void updateProgress(int bytesWritten, int totalBytesExpected)
	{
		
		bytesDownloaded = bytesWritten;
		expectedSize = totalBytesExpected;
		if (waiting) cacheEntry.updateProgress(false);

	}
	
	@Override
	public String toString()
	{
		return "SegmentCacheItem(" + ((Object)this).hashCode() + ")[" + waiting + "]" + uri.substring(uri.lastIndexOf('/'));
	}

}
