package com.kaltura.hlsplayersdk;

import java.io.IOException;

import org.apache.http.Header;

import android.util.Log;

import com.kaltura.hlsplayersdk.cache.HLSSegmentCache;
import com.kaltura.hlsplayersdk.events.OnErrorListener;
import com.kaltura.hlsplayersdk.manifest.BaseManifestItem;
import com.loopj.android.http.*;

public class URLLoader extends AsyncHttpResponseHandler 
{	
	private static int urlHandleSource = 0;
	
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
		Log.i("URLLoader [" + myUrlHandle + "].URLLoader[" + tag + "]", "Constructing [handle=" + myUrlHandle + "]" );
		setDownloadEventListener( eventListener );
		manifestItem = item;
		mTag = tag;
	}
	
	public URLLoader(String tag, DownloadEventListener eventListener, BaseManifestItem item, int playId)
	{
		myUrlHandle = getNextHandle();
		Log.i("URLLoader [" + myUrlHandle + "].URLLoader[" + tag + "]", "Constructing [handle=" + myUrlHandle + "]" );
		setDownloadEventListener( eventListener );
		videoPlayId = playId;
		manifestItem = item;
	}
	
	public void get(String url)
	{
		uri = url;
		Log.i("URLLoader [" + myUrlHandle + "].get[" + mTag + "]", "Getting: " + uri);
		AsyncHttpClient httpClient = HLSSegmentCache.httpClient();
		httpClient.setMaxRetriesAndTimeout(0,httpClient.getConnectTimeout());
		httpClient.setEnableRedirects(true);
		try
		{
			httpClient.get(url, this);
		}
		catch (Exception e)
		{
			HLSPlayerViewController.currentController.postError(OnErrorListener.ERROR_UNKNOWN, "URL Get Failed: " + e.getMessage());
		}
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
			mDownloadEventListener = listener;
	}

	//////////////////////////////////
	// Event Handlers
	//////////////////////////////////
	@Override
	public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
		Log.i("URLLoader [" + myUrlHandle + "].failure[" + mTag + "]", uri + "StatusCode (" + statusCode + ")");
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
			HLSPlayerViewController.postToInterfaceThread(new Runnable()
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
		final URLLoader thisLoader = this;

		for (int i = 0; i < headers.length; ++i)
		{
			Log.v("URLLoader [" + myUrlHandle + "].success", "Header: " + headers[i].getName() + ": " + headers[i].getValue());
		}
	
		if (mDownloadEventListener == null) return;

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
			// Post back to main thread to avoid re-entrancy problems
			HLSPlayerViewController.postToInterfaceThread(new Runnable()
			{
				@Override
				public void run() {
					mDownloadEventListener.onDownloadComplete(thisLoader, response==null?"null" : response);
				}
			});
		}		
	}
}
