package com.kaltura.hlsplayersdk;

import java.io.IOException;
import java.util.Vector;

import org.apache.http.Header;

import android.os.Looper;
import android.util.Log;

import com.kaltura.hlsplayersdk.cache.HLSSegmentCache;
import com.kaltura.hlsplayersdk.events.OnErrorListener;
import com.kaltura.hlsplayersdk.manifest.BaseManifestItem;
import com.loopj.android.http.*;

public class URLLoader extends AsyncHttpResponseHandler 
{	
	private static int urlHandleSource = 0;
	
	
	/// LOADER STATE DEBUG INFO
	
	// Note that this MUST be false for release builds. This is only for debug purposes. 
	// The array of loaders will have invalid entries if you try to play another video
	private static final boolean mlogLoaderStates = false; 

	private static Vector<URLLoader> loaders = new Vector<URLLoader>();
	public static void logLoaderStates()
	{
		if (!mlogLoaderStates) return;
		for (int i = 0; i < loaders.size(); ++i)
		{
			Log.i("URLLoader.logLoaderStates[" + i + "]", loaders.get(i).toString());
		}
	}
	
	/// END LOADER STATE DEBUG INFO
	
	
	private static int getNextHandle()
	{
		++urlHandleSource;
		return urlHandleSource;
	}
	
	private int myUrlHandle = 0;
	
	public BaseManifestItem manifestItem = null;
	public String uri;
	public int videoPlayId = 0; // Used for tracking which video play we're on. See HLSPlayerViewController.setVideoURL()
	private int reloadCount = 0;
	private final int MAX_RELOAD_TRIES = 3;
	private String mTag;
	
	public URLLoader(String tag, DownloadEventListener eventListener, BaseManifestItem item)
	{
		myUrlHandle = getNextHandle();
		if (mlogLoaderStates) loaders.add(this);
		Log.i("URLLoader [" + myUrlHandle + "].URLLoader[" + tag + "] ThreadId["+ Thread.currentThread().getId() +"]", "Constructing [handle=" + myUrlHandle + "] eventListener=" + eventListener.hashCode() );
		setDownloadEventListener( eventListener );
		manifestItem = item;
		mTag = tag;
	}
	
	public URLLoader(String tag, DownloadEventListener eventListener, BaseManifestItem item, int playId)
	{
		myUrlHandle = getNextHandle();
		if (mlogLoaderStates) loaders.add(this);
		Log.i("URLLoader [" + myUrlHandle + "].URLLoader[" + tag + "] ThreadId["+ Thread.currentThread().getId() +"]", "Constructing [handle=" + myUrlHandle + "] eventListener=" + eventListener.hashCode() );
		setDownloadEventListener( eventListener );
		videoPlayId = playId;
		manifestItem = item;
	}
	
	RequestHandle reqHandle = null;
	
	public void get(String url)
	{
		uri = url;
		AsyncHttpClient httpClient = HLSSegmentCache.httpClient();
		if (httpClient == HLSSegmentCache.syncHttpClient) Log.i("HLS Cache", "Using Synchronous HTTP CLient");
		else Log.i("HLS Cache", "Using Asynchronous HTTP Client");

		Log.i("URLLoader [" + myUrlHandle + "].get[" + mTag + "]", "Client:" + httpClient.hashCode() + " Looper:" + Looper.myLooper() + " Getting: " + uri);
		httpClient.setMaxRetriesAndTimeout(0,httpClient.getConnectTimeout());
		httpClient.setEnableRedirects(true);
		try
		{
			reqHandle = httpClient.get(HLSSegmentCache.context, url, this);
		}
		catch (Exception e)
		{
			HLSPlayerViewController.currentController.postError(OnErrorListener.ERROR_UNKNOWN, "URL Get Failed: " + e.getMessage());
		}
		Log.i("URLLoader [" + myUrlHandle + "].get[" + mTag + "]", "reqHandle.isCancelled: " + reqHandle.isCancelled());
	}
	
	private boolean retrying()
	{
		++reloadCount;
		if (mDownloadEventListener == null) return false;
		if (reloadCount <= MAX_RELOAD_TRIES)
		{
			Log.i("URLLoader [" + myUrlHandle + "].retrying[" + mTag + "]", "Retrying [" + reloadCount + "]: " + uri);
			get(uri);
			return true;
		}
		return false;
	}
	
	
	/////////////////////////////////////////////
	// Listener interface
	////////////////////////////////////////////
	
	public interface DownloadEventListener
	{
		public void onDownloadComplete(URLLoader loader, String response);
		public void onDownloadFailed(URLLoader loader, String response);
	}
	
	private DownloadEventListener mDownloadEventListener = null;
	
	public void setDownloadEventListener(DownloadEventListener listener)
	{
		if (mDownloadEventListener != null && listener != null)
		{
			Log.e("URLLoader [" + myUrlHandle + "].setDownloadEventListener[" + mTag + "]", "Tried to change the downloadEventListener for " + uri);
		}
		else
		{
			if (listener == null) Log.w("URLLoader [" + myUrlHandle + "].setDownloadEventListener[" + mTag + "]", "Setting downloadEventListener to null");
			mDownloadEventListener = listener;
		}
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("Tag: " + mTag );
		sb.append(" urlHandle: " + myUrlHandle);
		sb.append(" reloadCount: " + reloadCount);
		sb.append(" videoPlayId: " + videoPlayId);
		sb.append(" reqHandle: " + reqHandle);
		if (reqHandle != null)
		{
			sb.append(" reqHandle.isFinsihed: " + reqHandle.isFinished());
			sb.append(" reqHandle.isCancelled: " + reqHandle.isCancelled());
		}
		sb.append(" mDownloadEventListener = " + mDownloadEventListener.hashCode());
		
		
		return sb.toString();
	}

	//////////////////////////////////
	// Event Handlers
	//////////////////////////////////
	
	@Override
	public void onProgress(int bytes, int totalSize)
	{
		
	}
	
	@Override
	public void onCancel()
	{
		Log.i("URLLoader [" + myUrlHandle + "].cancelled[" + mTag + "]", "Request Cancelled - " + toString());
	}
	
	@Override
	public void onStart()
	{
		Log.i("URLLoader [" + myUrlHandle + "].started[" + mTag + "]", "Request Started");
	}
	
	@Override
	public void onFinish()
	{
		Log.i("URLLoader [" + myUrlHandle + "].finished[" + mTag + "]", "Request Finished");
	}
	
	@Override
	public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
		Log.i("URLLoader [" + myUrlHandle + "].failure[" + mTag + "]", uri + "StatusCode (" + statusCode + ")");
		logLoaderStates();
		if (retrying()) return;
		final URLLoader thisLoader = this;
		final int sc = statusCode;
		String m = "";
		if (error != null)
		{
			m = error.getMessage();
			if (m == null && error.getCause() != null)
				m = error.getCause().getMessage();
		}
		final String msg = m;
		
		if (mDownloadEventListener != null)
		{
			// Post back to main thread to avoid re-entrancy that breaks OkHTTP.
			HLSPlayerViewController.postToHTTPResponseThread(new Runnable()
			{
				@Override
				public void run() {
					mDownloadEventListener.onDownloadFailed(thisLoader, "URLLoader [" + myUrlHandle + "] Failure: " + msg + "(" + sc + ")");				
				}
			});
		}
	}

	@Override
	public void onSuccess(int statusCode, Header[] headers, byte[] responseData) {
		
		Log.i("URLLoader [" + myUrlHandle + "].success[" + mTag + "]", "Received: " + uri);
		logLoaderStates();
		final URLLoader thisLoader = this;

		for (int i = 0; i < headers.length; ++i)
		{
			Log.v("URLLoader [" + myUrlHandle + "].success", "Header: " + headers[i].getName() + ": " + headers[i].getValue());
		}
	
		if (mDownloadEventListener == null)
		{
			Log.e("URLLoader [" + myUrlHandle + "].success", "No event listener set - returning!");
			return;
		}

		if (uri.lastIndexOf(".m3u8") == uri.length() - 5)
		{
			for (int i = 0; i < headers.length; ++i)
			{
				if (headers[i].getName().equals("Content-Type") && headers[i].getValue().toLowerCase().contains("mpegurl") == false)
				{
					onFailure(statusCode, headers, responseData, null);
					return;
				}
			}
		}
		
		String s = null;
		try
		{
			s = new String(responseData);
		}
		catch (Exception e)
		{
			Log.i("URLLoader [" + myUrlHandle + "]", "Failed to convert response to string for uri: " + uri);
			onFailure(statusCode, headers, responseData, null);
		}
		final String response = s;
		
				
		if (mDownloadEventListener != null)
		{
			Log.i("URLLoader [" + myUrlHandle + "].success[" + mTag + "]", "Posting To Interface Thread: " + uri);
			
			// Post back to main thread to avoid re-entrancy problems
			HLSPlayerViewController.postToHTTPResponseThread(new Runnable()
			{
				@Override
				public void run() {
					Log.i("URLLoader [" + myUrlHandle + "].success[" + mTag + "]", "Calling onDownloadComplete: " + uri);
					mDownloadEventListener.onDownloadComplete(thisLoader, response==null?"null" : response);
				}
			});
		}		
	}
}
