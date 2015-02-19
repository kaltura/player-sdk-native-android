package com.kaltura.hlsplayersdk.cache;

import android.os.Handler;
import android.util.Log;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.hlsplayersdk.events.OnErrorListener;
import com.loopj.android.http.*;

public class SegmentCacheEntry {
	public String uri;
	public byte[] data;
	public boolean running;
	public boolean waiting;
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
	
	private Handler mCallbackHandler = null;
	
	public int bytesDownloaded = 0;
	public int totalSize = 0;
	
	SegmentCacheEntry selfRef = this;
	
	public void cancel()
	{
		if (running)
		{
			Log.i("HLS Cache", "Cancelling " + uri);
			registerSegmentCachedListener(null, null);
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
	
	private SegmentCachedListener mSegmentCachedListener = null;
	public void registerSegmentCachedListener(SegmentCachedListener listener, Handler callbackHandler)
	{
		synchronized (selfRef)
		{
			if (mSegmentCachedListener != listener)
			{
				Log.i("SegmentCacheEntry", "Setting the SegmentCachedListener to a new value: " + listener);
				mSegmentCachedListener = listener;
				mCallbackHandler = callbackHandler;
			}
			
			if (mCallbackHandler == null)
			{
				Log.i("SegmentCacheEntry.registerSegmentCachedListener", "CallbackHandler == null");
			}
		}
	}
	
	public void notifySegmentCached()
	{
		if (waiting) HLSSegmentCache.postProgressUpdate(true);
		waiting = false;
		if (mSegmentCachedListener != null && mCallbackHandler != null)
		{
			mCallbackHandler.post(new Runnable() {
				public void run()
				{
					synchronized (selfRef)
					{
						if (mSegmentCachedListener != null) mSegmentCachedListener.onSegmentCompleted(uri);
					}
				}
			});
		}
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
			Log.i("SegmentCacheEntry.postOnSegmentFailed", "Segment download failed. Retrying: " + uri + " : " + statusCode);
			HLSSegmentCache.retry(this);
		}
		else
		{
			Log.i("SegmentCacheEntry.postOnSegmentFailed", "Segment download failed. No More Retries Left: " + uri + " : " + statusCode);
			running = false;
			if (mSegmentCachedListener != null)
				mSegmentCachedListener.onSegmentFailed(uri, statusCode);
			HLSPlayerViewController.currentController.postError(OnErrorListener.MEDIA_ERROR_IO, uri + "(" + statusCode + ")");
		}
	}
	
	public void postSegmentSucceeded(int statusCode, byte[] responseData)
	{
		if (statusCode == 200)
		{
			downloadCompletedTime = System.currentTimeMillis();
			Log.i("SegmentCacheEntry.postSegmentSucceeded", "Got " + uri);
			HLSSegmentCache.store(uri, responseData);
		}
		else
		{
			Log.i("SegmentCacheEntry.postSegmentSucceeded", "statusCode " + "[" + statusCode + "]" + uri);
			if (mSegmentCachedListener != null)
				mSegmentCachedListener.onSegmentFailed(uri, statusCode);
			HLSPlayerViewController.currentController.postError(OnErrorListener.MEDIA_ERROR_IO, uri + "(" + statusCode + ")");
		}
	}
	
	public void updateProgress(int bytesWritten, int totalBytesExpected)
	{
		
		bytesDownloaded = bytesWritten;
		totalSize = totalBytesExpected;
		// If we have a callback handler, it pretty much means that we're not going to be
		// in a wait state in the SegmentCache
		if (mCallbackHandler != null && waiting && bytesWritten != totalBytesExpected)
		{
			HLSSegmentCache.postProgressUpdate(false);
		}
	}
	
	@Override
	public String toString()
	{
		return "SegmentCacheEntry(" + ((Object)this).hashCode() + ")[" + waiting + "]";
	}

}
