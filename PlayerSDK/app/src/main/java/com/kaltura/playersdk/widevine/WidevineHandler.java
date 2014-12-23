package com.kaltura.playersdk.widevine;

import android.app.Activity;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.drm.DrmInfoEvent;
import android.drm.DrmInfoRequest;
import android.drm.DrmManagerClient;
import android.widget.Toast;

public class WidevineHandler {
	
	public static String WIDEVINE_MIME_TYPE = "video/wvm";
	
	public static void acquireRights (final Activity activity, String url, String wvDRMServerKey) {
		DrmManagerClient mDrmManager = new DrmManagerClient(activity);
		DrmInfoRequest drmInfoRequest = new DrmInfoRequest(DrmInfoRequest.TYPE_RIGHTS_ACQUISITION_INFO, WIDEVINE_MIME_TYPE); 
		drmInfoRequest.put("WVAssetURIKey", url);
		drmInfoRequest.put("WVDRMServerKey", wvDRMServerKey);
		drmInfoRequest.put("WVDeviceIDKey", "device1234");
		drmInfoRequest.put("WVPortalKey", "kaltura");

		mDrmManager.setOnEventListener(new DrmManagerClient.OnEventListener() {
			public void onEvent(DrmManagerClient client, DrmEvent event) {
		                switch (event.getType()) {
		                case DrmEvent.TYPE_DRM_INFO_PROCESSED:
						  //INFO PROCESSED
						break;
		                }      }      });

		mDrmManager.setOnErrorListener(new DrmManagerClient.OnErrorListener() {
            public void onError(DrmManagerClient client, DrmErrorEvent event) {
                switch (event.getType()) {
				 case DrmErrorEvent.TYPE_RIGHTS_NOT_INSTALLED:
					 Toast.makeText(activity , "We're sorry, you don't have a valid license for this video.", Toast.LENGTH_SHORT).show();
			 	//RIGHTA NOT INSTALLED
				break;
                }      }      });

		mDrmManager.setOnInfoListener(new DrmManagerClient.OnInfoListener() {
			public void onInfo(DrmManagerClient client, DrmInfoEvent event) {
               if (event.getType() == DrmInfoEvent.TYPE_RIGHTS_INSTALLED) {
                   //RIGHTS INSTALLED
               }     }      });
		
		//get license
		mDrmManager.acquireRights(drmInfoRequest);
	}
}
