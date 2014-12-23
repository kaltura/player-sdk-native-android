package com.kaltura.hlsplayersdk.cache;

import org.apache.http.Header;

import android.util.Log;

import com.loopj.android.http.*;

public class SegmentBinaryResponseHandler extends AsyncHttpResponseHandler {

	public SegmentCacheEntry entry = null;
	
	public SegmentBinaryResponseHandler(SegmentCacheEntry sce)
	{
		entry = sce;
	}
	
	@Override
	public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
		Log.e("HLS Cache", "Failed to download '" + entry.uri + "'! " + statusCode);
		entry.postOnSegmentFailed(statusCode);
	}

	@Override
	public void onSuccess(int statusCode, Header[] headers, byte[] responseData) {
		entry.postSegmentSucceeded(statusCode, responseData);
	}
	
    @Override
    public void onRetry(int retryNo) {
        Log.i("SegmentBinaryResponseHandler", "Automatic Retry: " + retryNo);
    }

    @Override
    public void onProgress(int bytesWritten, int totalSize) {
    	Log.i("SegmentBinaryResponseHandler.onProgress", "Bytes Written:" + bytesWritten + " Total Size:" + totalSize);
        entry.updateProgress(bytesWritten, totalSize);
    }

    @Override
    public void onFinish() {
        // Completed the request (either success or failure)
    }
}
