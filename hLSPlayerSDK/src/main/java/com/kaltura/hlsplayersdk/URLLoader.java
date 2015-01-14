package com.kaltura.hlsplayersdk;

import java.io.IOException;

import org.apache.http.Header;

import android.util.Log;

import com.kaltura.hlsplayersdk.cache.HLSSegmentCache;
import com.kaltura.hlsplayersdk.manifest.BaseManifestItem;
import com.loopj.android.http.*;

public class URLLoader extends AsyncHttpResponseHandler 
{	
	public BaseManifestItem manifestItem = null;
	public String uri;
	public int videoPlayId = 0; // Used for tracking which video play we're on. See HLSPlayerViewController.setVideoURL()
	private int reloadCount = 0;
	private final int MAX_RELOAD_TRIES = 3;
	
	public URLLoader(DownloadEventListener eventListener, BaseManifestItem item)
	{
		Log.i("URLLoader.URLLoader()", "Constructing" );
		setDownloadEventListener( eventListener );
		manifestItem = item;
	}
	
	public URLLoader(DownloadEventListener eventListener, BaseManifestItem item, int playId)
	{
		Log.i("URLLoader.URLLoader()", "Constructing" );
		setDownloadEventListener( eventListener );
		videoPlayId = playId;
		manifestItem = item;
	}
	
	public void get(String url)
	{
		uri = url;
		Log.i("URLLoader", "Getting: " + uri);
		AsyncHttpClient httpClient = HLSSegmentCache.httpClient();
		httpClient.setMaxRetriesAndTimeout(0,httpClient.getConnectTimeout());
		httpClient.setEnableRedirects(true);
		httpClient.get(url, this);
	}
	
	private boolean retrying()
	{
		++reloadCount;
		if (reloadCount <= MAX_RELOAD_TRIES)
		{
			Log.i("URLLoader", "Retrying [" + reloadCount + "]: " + uri);
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
			Log.e("URLLoader.setDownloadEventListener", "Tried to change the downloadEventListener for " + uri);
		}
		else
			mDownloadEventListener = listener;
	}

	//////////////////////////////////
	// Event Handlers
	//////////////////////////////////
	@Override
	public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
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
			HLSPlayerViewController.GetInterfaceThread().getHandler().post(new Runnable()
			{
				@Override
				public void run() {
					mDownloadEventListener.onDownloadFailed(thisLoader, "Failure: " + msg + "(" + sc + ")");				
				}
			});
		}
	}

	@Override
	public void onSuccess(int statusCode, Header[] headers, byte[] responseData) {
		
		Log.i("URLLoader.success", "Received: " + uri);
		final URLLoader thisLoader = this;

		for (int i = 0; i < headers.length; ++i)
		{
			Log.v("URLLoader.success", "Header: " + headers[i].getName() + ": " + headers[i].getValue());
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
			onFailure(statusCode, headers, responseData, null);
		}
		final String response = s;
		
				
		if (mDownloadEventListener != null)
		{
			// Post back to main thread to avoid re-entrancy problems
			HLSPlayerViewController.GetInterfaceThread().getHandler().post(new Runnable()
			{
				@Override
				public void run() {
					mDownloadEventListener.onDownloadComplete(thisLoader, response==null?"null" : response);
				}
			});
		}		
	}
}
