package com.kaltura.playersdk.chromecast;
import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.example.kplayersdk.R;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.CastMediaControlIntent;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.kaltura.playersdk.events.OnCastDeviceChangeListener;
import com.kaltura.playersdk.events.OnCastRouteDetectedListener;

public class ChromecastHandler {
	 public static String TAG = "ChromecastHandler";
	
	public static MediaRouter.Callback callback;
	public static MediaRouteSelector selector;
	public static MediaRouter router;
	public static ArrayList<MediaRouter.RouteInfo> routeInfos = new ArrayList<MediaRouter.RouteInfo>();
	public static ArrayList<String> routeNames;// = new ArrayList<String>();
	public static ArrayAdapter<String> adapter;
	public static CastDevice selectedDevice;
	private static Dialog mDialog;
	private static OnCastDeviceChangeListener mDeviceChangeLister;
	private static OnCastRouteDetectedListener mRouteDetectedListener;
	public static boolean initialized = false;
	
	private static VideoCastManager mCastMgr;
	    
	public static VideoCastManager getVideoCastManager(Context ctx) { 
		if (null == mCastMgr) {
        mCastMgr = VideoCastManager.initialize(ctx, "DB6462E9",null, null);
        mCastMgr.enableFeatures(VideoCastManager.FEATURE_NOTIFICATION |
                                VideoCastManager.FEATURE_DEBUGGING);
		}
		mCastMgr.setContext(ctx);
		return mCastMgr;
	}
	
	public static void initialize( Context context, OnCastDeviceChangeListener deviceChangeListener, OnCastRouteDetectedListener routeDetectedListener ) {
		getVideoCastManager( context );
		initialized = true;
		mDeviceChangeLister = deviceChangeListener;
		mRouteDetectedListener = routeDetectedListener;
		router = MediaRouter.getInstance(context);
    	
    	mDialog = new Dialog(context);
    	mDialog.setContentView(R.layout.media_routers);
    	mDialog.setTitle( Html.fromHtml("<font color='#0db6d1'>Connect to Device</font>"));
    	Button disconnect = (Button) mDialog.findViewById(R.id.disconnect);
        if ( disconnect != null) {
        	disconnect.setOnClickListener( new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					mCastMgr.disconnect();
					mDialog.hide();
			
				}
        		
        	});
        }
		
		final ListView listview = (ListView) mDialog.findViewById(R.id.listview);
		routeNames = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_list_item_1, routeNames);
		listview.setAdapter(adapter);
		listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				Log.d(TAG, "onItemClick: position=" + position);

				MediaRouter.RouteInfo info = routeInfos.get(position);
				router.selectRoute(info);
				
				mDialog.dismiss();
			}
		});
		
		
    	callback = new MyMediaRouterCallback();
    	
        selector = new MediaRouteSelector.Builder()
	        .addControlCategory(CastMediaControlIntent.categoryForCast("DB6462E9"))
	        .build();
        
        router.addCallback(selector, callback,
                MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);

        MediaRouter.RouteInfo route = router.updateSelectedRoute(selector);	
    }
	
	public static void showCCDialog( Context context ) {
		if ( mDialog != null ) {
			Button disconnect = (Button) mDialog.findViewById(R.id.disconnect);
			if ( mCastMgr.isConnected() ) {
				disconnect.setVisibility(View.VISIBLE);
			} else {
				disconnect.setVisibility(View.INVISIBLE);
			}
			mDialog.show();
		}
	}
	
	
	private static class MyMediaRouterCallback extends MediaRouter.Callback {
		@Override
		public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
			Log.d(TAG, "onRouteAdded: info=" + info);

			// Add route to list of discovered routes
			synchronized (this) {
				ChromecastHandler.routeInfos.add(info);
				ChromecastHandler.routeNames.add(info.getName() + " (" + info.getDescription()
						+ ")");
				if ( ChromecastHandler.adapter != null )
					ChromecastHandler.adapter.notifyDataSetChanged();
				
				mRouteDetectedListener.onCastRouteDetected();
			}
		}

		@Override
		public void onRouteRemoved(MediaRouter router,
				MediaRouter.RouteInfo info) {
			Log.d(TAG, "onRouteRemoved: info=" + info);

			// Remove route from list of routes
			synchronized (this) {
				for (int i = 0; i < ChromecastHandler.routeInfos.size(); i++) {
					MediaRouter.RouteInfo routeInfo = ChromecastHandler.routeInfos.get(i);
					if (routeInfo.equals(info)) {
						ChromecastHandler.routeInfos.remove(i);
						if ( i < ChromecastHandler.routeNames.size())
							ChromecastHandler.routeNames.remove(i);
						if ( ChromecastHandler.adapter != null )
							ChromecastHandler.adapter.notifyDataSetChanged();
						return;
					}
				}
				mRouteDetectedListener.onCastRouteDetected();
			}
		}

		@Override
		public void onRouteSelected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteSelected: info=" + info);
			CastDevice oldDevice = selectedDevice;
	        selectedDevice = CastDevice.getFromBundle(info.getExtras());
	        if ( oldDevice==null || !oldDevice.equals(selectedDevice)) {
	        	 mDeviceChangeLister.onCastDeviceChange(oldDevice, selectedDevice);
	        }
	        
		}

		@Override
		public void onRouteUnselected(MediaRouter router, RouteInfo info) {
			Log.d(TAG, "onRouteUnselected: info=" + info);
			CastDevice oldDevice = selectedDevice;
			selectedDevice = null;
			
			if ( oldDevice!=null ) {
				mDeviceChangeLister.onCastDeviceChange(oldDevice, selectedDevice);
			}
		}

	}
}
