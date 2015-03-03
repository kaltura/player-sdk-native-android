package com.kaltura.hlsplayersdk.cache;

import org.apache.http.Header;

import android.util.Log;

import com.loopj.android.http.*;

public class SegmentBinaryResponseHandler extends AsyncHttpResponseHandler {

	public SegmentCacheItem entry = null;
	
	private boolean succeeded = false;
	
	public SegmentBinaryResponseHandler(SegmentCacheItem sci)
	{
		entry = sci;
	}
	
	@Override
	public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
		if (succeeded)
		{
			if (entry != null && entry.request != null && !entry.request.cancel(true))
			{
				Log.e("SegmentBinaryResponseHandler.onFailure", "Request has succeded, but then called onFailure. request.cancel attempt failed. Status Code = " + statusCode);
			}
			else
			{
				Log.e("SegmentBinaryResponseHandler.onFailure", "Request has succeded, but then called onFailure. Status Code = " + statusCode);
				HLSSegmentCache.resetHTTPLibrary();
			}
			return; // This is a hack because sometimes, loopj likes to call onFailure after it's called onSuccess
		}
		Log.e("SegmentBinaryResponseHandler.onFailure", "Failed to download '" + entry.uri + "'! " + statusCode);
		entry.postOnSegmentFailed(statusCode);
	}

	@Override
	public void onSuccess(int statusCode, Header[] headers, byte[] responseData) {
		Log.i("SegmentBinaryResponseHandler.onSuccess", "Download Succeeded: " + entry.uri);
		succeeded = true;
		entry.postSegmentSucceeded(statusCode, responseData);
	}
	
    @Override
    public void onRetry(int retryNo) {
        Log.i("SegmentBinaryResponseHandler.onRetry", "Automatic Retry: " + retryNo);
    }

    @Override
    public void onProgress(int bytesWritten, int totalSize) {
    	//Log.i("SegmentBinaryResponseHandler.onProgress", "Bytes Written:" + bytesWritten + " Total Size:" + totalSize + " : " + entry.uri);
        entry.updateProgress(bytesWritten, totalSize);
    }

    @Override
    public void onFinish() {
        // Completed the request (either success or failure)
    	Log.i("SegmentBinaryResponseHandler.onFinish", entry.uri);
    }
}
