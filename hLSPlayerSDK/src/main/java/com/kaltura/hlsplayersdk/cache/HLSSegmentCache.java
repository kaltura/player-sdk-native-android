package com.kaltura.hlsplayersdk.cache;

import java.util.Collection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.loopj.android.http.*;

public class HLSSegmentCache 
{	
	protected static long targetSize = 16*1024*1024; // 16mb segment cache.
	protected static long minimumExpireAge = 5000; // Keep everything touched in last 5 seconds.
	
	protected static Map<String, SegmentCacheEntry> segmentCache = null;
	public static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
	public static AsyncHttpClient syncHttpClient = new SyncHttpClient();
	
	public static double lastDownloadDataRate = 0.0;
	public static int lastBufferPct = -1;
	
	public static void resetProgress() { lastBufferPct = -1; }
	
	public static Context context = null;
	
	public static AsyncHttpClient httpClient()
	{
		synchronized (asyncHttpClient)
		{
			if (Looper.myLooper() == null)
				return syncHttpClient;
			return asyncHttpClient;
		}
	}
	
	public static void resetHTTPLibrary()
	{
		synchronized(asyncHttpClient)
		{
			asyncHttpClient = new AsyncHttpClient();
			syncHttpClient = new SyncHttpClient();
		}
	}
	
	static public int getCryptoId(final String segmentUri)
	{
		initialize();
		synchronized (segmentCache)
		{
			SegmentCacheEntry sce = segmentCache.get(segmentUri);
			if (sce != null)
			{
				SegmentCacheItem sci = sce.getItem(segmentUri);
				Log.i("getCryptoId", "Found Id (" + sci.cryptoHandle + ") for URI: " + segmentUri );
				return sci.cryptoHandle;
			}
			else
			{
				Log.i("getCryptoId", "Found no existing sce for URI: " + segmentUri );
				return -1;
			}
		}
	}
	
	static public SegmentCacheEntry populateCache(String [] segmentUris)
	{
		if (segmentUris == null || segmentUris.length == 0)
		{
			Log.e("HLS Cache", "Tried to populate with a null or empty uri array");
			return null;
		}
		SegmentCacheEntry sce = null;
		synchronized (segmentCache)
		{
			SegmentCacheEntry existing = segmentCache.get(segmentUris[0]);
			if (existing != null)
			{
				if (existing.isRunning() || existing.dataSize() != 0)
				{
					existing.lastTouchedMillis = System.currentTimeMillis();
					return existing;
				}
			}
			
			// Populate a cache entry and initiate the requests
			Log.i("HLS Cache", "Miss on " + segmentUris[0] + ", populating..");
			sce = (existing != null) ? existing : new SegmentCacheEntry(segmentUris);
			
			// We're putting it in the map for every URI, so that it can be looked up by any of them
			for (int i = 0; i < segmentUris.length; ++i)
			{
				segmentCache.put(segmentUris[i], sce);
			}
		}
		
		if (sce != null)
		{
			sce.initiateDownload();
		}
		return sce;
	}
	
	static public void notifyStored(SegmentCacheEntry sce)
	{
		synchronized (segmentCache)
		{
			sce.lastTouchedMillis = System.currentTimeMillis();
		}
		
		
		sce.notifySegmentCached();
		
		if (sce.downloadCompletedTime != 0 && sce.downloadStartTime != 0 && sce.downloadCompletedTime != sce.downloadStartTime)
			lastDownloadDataRate = (double)sce.dataSize() / (sce.downloadCompletedTime - sce.downloadStartTime);
		
		expire();
	}
	
	static protected void initialize()
	{
		if(segmentCache == null)
		{
			Log.i("HLS Cache", "Initializing concurrent hash map.");
			segmentCache = new ConcurrentHashMap<String, SegmentCacheEntry>();
			context = HLSPlayerViewController.currentController.getContext();
			if (context == null) Log.e("HLS Cache", "Context is null!!!");
		}
	}
	
	/**
	 * Hint we will soon be wanting data from this segment and that we should 
	 * initiate the download.
	 * 
	 * @param segmentUri
	 * @param cryptoId
	 */
	static public void precache(String segmentUri, int cryptoId)
	{
		initialize();
		
		populateCache( new String [] { segmentUri } );

		SegmentCacheEntry sce = segmentCache.get( segmentUri );
		SegmentCacheItem sci = sce.getItem(segmentUri);
		sci.setCryptoHandle(cryptoId);
	}
	
	/**
	 * Hint we will soon be wanting data from this segment and that we should 
	 * initiate the download.
	 * 
	 * @param segmentUri
	 * @param cryptoId
	 * @param SegmentCachedListener
	 */
	static public void precache(final String segmentUri, int cryptoId, boolean forceWait, final SegmentCachedListener segmentCachedListener, Handler callbackHandler )
	{
		precache(segmentUri, cryptoId);
		synchronized (segmentCache)
		{
			SegmentCacheEntry sce = segmentCache.get(segmentUri);
			
			sce.registerSegmentCachedListener(segmentCachedListener, callbackHandler);
			sce.setWaiting(forceWait);
			if (!sce.isRunning())
			{
				HLSSegmentCache.postProgressUpdate(true);
				sce.notifySegmentCached();
			}
		}
	}
	
	static public void precache(String [] segmentUris, int [] cryptoIds, boolean forceWait, SegmentCachedListener segmentCachedListener, Handler callbackHandler)
	{
		initialize();
		
		SegmentCacheEntry sce = populateCache(segmentUris);
		synchronized (segmentCache)
		{
			sce.setCryptoIds(cryptoIds);
			sce.registerSegmentCachedListener(segmentCachedListener, callbackHandler);
			sce.setWaiting(true);
			if (!sce.isRunning())
			{
				HLSSegmentCache.postProgressUpdate(true);
				sce.notifySegmentCached();
			}
		}
	}
	
	/**
	 * Cancels all cache event notifications for a particular cache entry.
	 * 
	 * @param segmentUri
	 */
	static public void cancelCacheEvent(String segmentUri)
	{
		initialize();
		synchronized (segmentCache)
		{
			SegmentCacheEntry sce = segmentCache.get(segmentUri);
			if (sce != null) sce.registerSegmentCachedListener(null, null);
		}
	}
	
	static public void cancelAllCacheEvents()
	{
		initialize();
		synchronized (segmentCache)
		{
			// Get all the values in the set.
			Collection<SegmentCacheEntry> values = segmentCache.values();

			for(SegmentCacheEntry v : values)
				v.registerSegmentCachedListener(null, null);
		}
	}
	
	
	/**
	 * Return the size of a downloaded segment. Blocking.
	 */
	static public long getSize(String segmentUri)
	{
		initialize();

		Log.i("HLS Cache", "Querying size of " + segmentUri);
		SegmentCacheEntry sce = segmentCache.get(segmentUri);
		if (sce == null)
		{
			// Do we have a cache entry for the segment? Populate if it doesn't exist.
			sce = populateCache( new String [] { segmentUri });
		}
		waitForLoad(sce);
		SegmentCacheItem sci = sce.getItem(segmentUri);
		if(sci.forceSize != -1)
			return sci.forceSize;
		if (sci.data == null) return 0;
		return sci.data.length;
	}
	
	private static long lastTime = System.currentTimeMillis();
	public static void postProgressUpdate(boolean force)
	{
		if (System.currentTimeMillis() - 10 > lastTime || force)
		{
			lastTime = System.currentTimeMillis();

			int totalBytes = 0;
			int curBytes = 0;
			boolean segmentsWaiting = false;
			int segmentsWaitingCount = 0;
			synchronized (segmentCache)
			{
				Collection<SegmentCacheEntry> values = segmentCache.values();

				for(SegmentCacheEntry v : values)
				{
					if (v.isRunning())
					{
						Log.i("HLS Cache", "map value: " + v.toString());
						totalBytes += v.expectedSize();
						curBytes += v.bytesDownloaded();
						segmentsWaiting = true;
						++segmentsWaitingCount;
					}
				}
			}
			double pct = totalBytes != 0 ? ((double)curBytes / (double)totalBytes) * 100.0 : 0;
			if (lastBufferPct == (int)pct)
				return;

			lastBufferPct = (int)pct;
			
			if (segmentsWaiting)
			{
				Log.i("HLS Cache", "Progress=" + (int)pct + " (" + curBytes +"/" + totalBytes + ") seg count=" + segmentsWaitingCount);
				HLSPlayerViewController.currentController.postProgressUpdate((int)pct);
			}

		}
	}
	
	static public void cancelDownloads()
	{
		synchronized (segmentCache)
		{
			Log.i("HLS Cache", "Cancelling downloads");

			// Get all the values in the set.
			Collection<SegmentCacheEntry> values = segmentCache.values();

			for(SegmentCacheEntry v : values)
				v.cancel();
		}
	}
	
	static private void waitForLoad(SegmentCacheEntry sce)
	{
		// Wait for data, if required...
		if(!sce.isRunning())
			return;
		
		Log.i("HLS Cache", "Waiting on request: " + sce);
		long timerStart = System.currentTimeMillis();

		sce.setWaiting(true);
		while(sce.isRunning())
		{
			postProgressUpdate(false);
			try {
				Thread.sleep(30);
				Thread.yield();
			} catch (InterruptedException e) {
				// Don't care.
			}
		}
		sce.setWaiting(false);
		long timerElapsed = System.currentTimeMillis() - timerStart;
		if (sce.dataSize() > 0) Log.i("HLS Cache", "Request finished, " + (sce.dataSize()/1024) + "kb in " + timerElapsed + "ms");
		else Log.i("HLS Cache", "sce.data is 0 - request must have been canceled");
	}
	
	static public String readFileAsString(String segmentUri)
	{
		initialize();
		
		long size = getSize(segmentUri);
		ByteBuffer buffer = ByteBuffer.allocate((int)size);
		read(segmentUri, 0, size, buffer);
		return new String(buffer.array(), Charset.forName("UTF-8"));
	}

final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
public static String bytesToHex(ByteBuffer bytes) {
    char[] hexChars = new char[bytes.capacity() * 2];
    for ( int j = 0; j < bytes.capacity(); j++ ) {
        int v = bytes.get(j) & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
    }
    return new String(hexChars);
}	
	
	/**
	 * Read from segment and return bytes read + output.
	 * @param segmentUri URI identifying the segment.
	 * @param offset Offset into the segment.
	 * @param size Number of bytes to read.
	 * @param output Array pre-sized to at least size, to which data is written.
	 * @return Bytes read.
	 */
	static public long read(String segmentUri, long offset, long size, ByteBuffer output)
	{
		boolean adjusted = false;

		//Log.i("HLS Cache", "Reading " + segmentUri + " offset=" + offset + " size=" + size + " output.capacity()=" + output.capacity());
		
		initialize();
		
		// Do we have a cache entry for the segment? Populate if it doesn't exist.
		SegmentCacheEntry sce = populateCache( new String[] { segmentUri });
		
		// Sanity check.
		if(sce == null)
		{
			Log.e("HLS Cache", "Failed to populate cache! Aborting...");
			return 0;
		}
		
		waitForLoad(sce);
		
		if (sce.dataSize() == 0)
		{
			Log.e("HLS Cache", "Segment Data is nonexistant or empty");
			return 0;
		}
		
		synchronized(segmentCache)
		{
			SegmentCacheItem sci = sce.getItem(segmentUri);

			// How many bytes can we serve?
			if(offset + size > sci.data.length)
			{
				long newSize = sci.data.length - offset;
				Log.i("HLS Cache", "Adjusting size to " + newSize + " from " + size + " offset=" + offset + " data.length=" + sci.data.length + " for file:" + sci.uri);
				size = newSize;
				adjusted = true;
			}
			
			if(size < 0)
			{
				Log.i("HLS Cache", "Couldn't return any bytes.");
				return 0;
			}

			// Ensure decrypted.
			sci.ensureDecryptedTo(offset + size);

			// If we have decrypted to the end, look for padding and adjust length.
			if(sci.isFullyDecrypted() && sci.hasCrypto() && sci.forceSize == -1)
			{
				// Look for padding.
				byte padByte = sci.data[sci.data.length - 1];

				boolean isPadded = true;
				for(int i=sci.data.length-padByte; i<sci.data.length; i++)
				{
					if(sci.data[i] == padByte)
						continue;

					isPadded = false;
					break;
				}

				if(isPadded)
				{
					// Note new size.
					sci.forceSize = sci.data.length - padByte;
					Log.i("HLS Cache", "Forcing segment size to " + sci.forceSize);
				}
			} 
			
			// Truncate length based on forced size.
			if(sci.forceSize != -1)
			{
				if(offset + size >= sci.forceSize)
				{
					size = sci.forceSize - offset;
					Log.i("HLS Cache", "Truncating size due to padding to " + size);
				}
			}
			
			// Copy the available bytes.
			output.put(sci.data, (int)offset, (int)size);
			
//			if(adjusted)
//			{
//				try
//				{
//					Log.i("HLS Cache", "Saw bytes: " + bytesToHex(output));
//				}
//				catch(Exception e)
//				{
//					Log.i("HLS Cache", "Failed to dump hex bytes");
//				}
//			}

			// Return how much we read.
			return size;			
		}
	}
	
	/**
	 * We only have finite memory; evict segments when we exceed a maximum size.
	 */
	static public void expire()
	{
		synchronized (segmentCache)
		{
			// Get all the values in the set.
			Collection<SegmentCacheEntry> values = segmentCache.values();

			// First, determine total size.
			long totalSize = 0;
			for(SegmentCacheEntry v : values)
				totalSize += v.dataSize();
			
			Log.i("HLS Cache", "size=" + (totalSize/1024) + "kb  threshold=" + (targetSize/1024) + "kb");
			
			// If under threshold, we're done.
			if(totalSize <= targetSize)
				return;
			
			// Otherwise, find the oldest segment.
			long oldestTime = System.currentTimeMillis();
			SegmentCacheEntry oldestSce = null;
			for(SegmentCacheEntry v : values)
			{
				if(v.lastTouchedMillis >= oldestTime)
					continue;
				
				oldestSce = v;
				oldestTime = v.lastTouchedMillis;
			}
			
			long entryAge = System.currentTimeMillis() - oldestTime;
			if(entryAge < minimumExpireAge)
			{
				Log.i("HLS Cache", "Tried to purge segment that is less than " + minimumExpireAge/1000 + " seconds old. Ignoring... (" + oldestSce.toString() + ", " + entryAge/1000 + ")");
				return;
			}
			
			// We're over cache target, delete that one.
			Log.i("HLS Cache", "Purging " + oldestSce.toString() + ", freeing " + (oldestSce.dataSize()/1024) + "kb, age " + (entryAge/1000) + "sec");
			oldestSce.clear();
			oldestSce.removeMe(segmentCache);
		}
	}
}
