package com.kaltura.playersdk;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.kaltura.playersdk.events.KPlayerEventListener;
import com.kaltura.playersdk.events.KPlayerJsCallbackReadyListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.events.OnToggleFullScreenListener;
import com.kaltura.playersdk.types.PlayerStates;
import com.kaltura.playersdk.widevine.WidevineHandler;

/**
 * Created by michalradwantzor on 9/24/13.
 */
public class PlayerViewController extends RelativeLayout {
    public static String TAG = "PlayerViewController";
    public static String DEFAULT_HOST = "http://cdnbakmi.kaltura.com";
    public static String DEFAULT_HTML5_URL = "/html5/html5lib/v2.1.1/mwEmbedFrame.php";
    public static String DEFAULT_PLAYER_ID = "21384602";

    private VideoPlayerInterface mVideoInterface;
    private PlayerView mPlayerView;
    private WebView mWebView;
    private double mCurSec;
    private Activity mActivity;
    private double mDuration = 0;
    private OnToggleFullScreenListener mFSListener;
    private HashMap<String, ArrayList<KPlayerEventListener>> mKplayerEventsMap = new HashMap<String, ArrayList<KPlayerEventListener>>();
    private HashMap<String, KPlayerEventListener> mKplayerEvaluatedMap = new HashMap<String, KPlayerEventListener>();
    private KPlayerJsCallbackReadyListener mJsReadyListener;
    
    public String host = DEFAULT_HOST;
    public String html5Url = DEFAULT_HTML5_URL;
    public String playerId = DEFAULT_PLAYER_ID;
    
    private String mVideoUrl;
    private String mVideoTitle = "";
    private String mThumbUrl ="";
    
    private PlayerStates mState = PlayerStates.START;

    public PlayerViewController(final Context context) {
        super(context); 
    }

    public PlayerViewController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerViewController(Context context, AttributeSet attrs,
            int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setOnFullScreenListener(OnToggleFullScreenListener listener) {
        mFSListener = listener;
    }
    
    public void setPlayerViewDimensions(int width, int height, int xPadding, int yPadding) {
    	setPadding(xPadding, yPadding, 0, 0);
    	setPlayerViewDimensions( width+xPadding, height+yPadding);
    }

    public void setPlayerViewDimensions(int width, int height) {
        ViewGroup.LayoutParams lp = getLayoutParams();
        if ( lp == null ) {
        	lp = new ViewGroup.LayoutParams( width, height );
        } else {
            lp.width = width;
            lp.height = height;
        }

        this.setLayoutParams(lp);
        if (mWebView != null) {
            ViewGroup.LayoutParams wvlp = mWebView.getLayoutParams();
            wvlp.width = width;
            wvlp.height = height;
            updateViewLayout(mWebView, wvlp);
        }
        if ( mPlayerView != null ) {
        	LayoutParams plp = (LayoutParams) mPlayerView.getLayoutParams();
        	plp.width = width;
        	plp.height = height;
            updateViewLayout(mPlayerView, plp);
        }

        invalidate();
    }
    
    /**
     * Build player URL and load it to player view
     * @param partnerId partner ID
     * @param entryId entry ID
     * @param activity bounding activity
     */
    public void addComponents(String partnerId, String entryId, Activity activity) {
        String iframeUrl = host + html5Url + "?wid=_" + partnerId + "&uiconf_id=" + playerId + "&entry_id=" + entryId;
        addComponents( iframeUrl, activity );
    }

    /**
     * load given url to the player view
     * 
     * @param iframeUrl
     *            url to payer
     * @param activity
     *            bounding activity
     */
    public void addComponents(String iframeUrl, Activity activity) {	
        mActivity = activity;
        mCurSec = 0;
        ViewGroup.LayoutParams currLP = getLayoutParams();
        mPlayerView = new PlayerView(mActivity);
        super.addView(mPlayerView, currLP);
        mVideoInterface = mPlayerView;
        setPlayerListeners();
        
        
        LayoutParams wvLp = new LayoutParams(currLP.width, currLP.height);
        mWebView = new WebView(mActivity);
        this.addView(mWebView, wvLp);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new CustomWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setUserAgentString(
                mWebView.getSettings().getUserAgentString()
                        + " kalturaNativeCordovaPlayer");
        if (Build.VERSION.SDK_INT >= 11) {
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        mWebView.loadUrl(iframeUrl);
        mWebView.setBackgroundColor(0);
    }
    

    /**
     * slides with animation according the given values
     * 
     * @param x
     *            x offset to slide
     * @param duration
     *            animation time in milliseconds
     */
    public void slideView(int x, int duration) {
        this.animate().xBy(x).setDuration(duration)
                .setInterpolator(new BounceInterpolator());
    }
    
    public void destroy() {
       this.stop();
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // VideoPlayerInterface methods
    // /////////////////////////////////////////////////////////////////////////////////////////////
    public boolean isPlaying() {
        return (mVideoInterface != null && mVideoInterface.isPlaying());
    }

    /**
     * 
     * @return duration in seconds
     */
    public double getDuration() {
        double duration = 0;
        if (mVideoInterface != null)
            duration = mVideoInterface.getDuration() / 1000;

        return duration;
    }

    public String getVideoUrl() {
        String url = null;
        if (mVideoInterface != null)
            url = mVideoInterface.getVideoUrl();

        return url;
    }

    public void play() {
        if (mVideoInterface != null) {
            mVideoInterface.play();
        }
    }

    public void pause() {
        if (mVideoInterface != null) {
            mVideoInterface.pause();
        }
    }

    public void stop() {
        if (mVideoInterface != null) {
            mVideoInterface.stop();
        }
    }

    public void seek(int msec) {
        if (mVideoInterface != null) {
            mVideoInterface.seek(msec);
        }
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Kaltura Player external API
    // /////////////////////////////////////////////////////////////////////////////////////////////
    public void registerJsCallbackReady( KPlayerJsCallbackReadyListener listener ) {
    	mJsReadyListener = listener;
    }
    
    public void sendNotification(String noteName, JSONObject noteBody) {
        notifyKPlayer("sendNotification",  new String[] { noteName, noteBody.toString() });      
    }

    public void addKPlayerEventListener(String eventName,
            KPlayerEventListener listener) {
        ArrayList<KPlayerEventListener> listeners = mKplayerEventsMap
                .get(eventName);
        boolean isNewEvent = false;
        if ( listeners == null ) {
            listeners = new ArrayList<KPlayerEventListener>();
        }
        if ( listeners.size() == 0 ) {
            isNewEvent = true;
        }
        listeners.add(listener);
        mKplayerEventsMap.put(eventName, listeners);
        if ( isNewEvent )
            notifyKPlayer("addJsListener", new String[] { eventName });
    }

    public void removeKPlayerEventListener(String eventName,String callbackName) {
        ArrayList<KPlayerEventListener> listeners = mKplayerEventsMap.get(eventName);
        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                if ( listeners.get(i).getCallbackName().equals( callbackName )) {
                    listeners.remove(i);
                    break;
                }
            }
            if ( listeners.size() == 0 )
                notifyKPlayer( "removeJsListener", new String[] { eventName });
        }
    }

    public void setKDPAttribute(String hostName, String propName, Object value) {
        notifyKPlayer("setKDPAttribute", new Object[] { hostName, propName, value });
    }

    public void asyncEvaluate(String expression, KPlayerEventListener listener) {
        String callbackName = listener.getCallbackName();
        mKplayerEvaluatedMap.put(callbackName, listener);
        notifyKPlayer("asyncEvaluate", new String[] { expression, callbackName });
    }

    // /////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * call js function on NativeBridge.videoPlayer
     * 
     * @param action
     *            function name
     * @param eventValues
     *            function arguments
     */
    private void notifyKPlayer(final String action, final Object[] eventValues) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String values = "";
                
                if (eventValues != null) {
                	for ( int i=0; i< eventValues.length; i++ ) {
                		if ( eventValues[i] instanceof String ) {
                			values += "'" + eventValues[i] + "'";
                		} else {
                			values += eventValues[i].toString();
                		}
            			if ( i < eventValues.length - 1 ) {
            				values += ", ";
            			}
                	}
                   // values = TextUtils.join("', '", eventValues);
                }
                
                mWebView.loadUrl("javascript:NativeBridge.videoPlayer."
                        + action + "(" + values + ");");
            }
        });
    }

    private void setPlayerListeners() {
        // notify player state change events
        mVideoInterface
                .registerPlayerStateChange(new OnPlayerStateChangeListener() {
                    @Override
                    public boolean onStateChanged(PlayerStates state) {
                    	if ( state == PlayerStates.START ) {
                    		mDuration = getDuration();
                    		notifyKPlayer("trigger", new Object[]{ "durationchange", mDuration });
                    		notifyKPlayer("trigger", new Object[]{ "loadedmetadata" });                  		
                    	}
                    	if ( state != mState ) {
                    		mState = state;
                    		String stateName = "";
                            switch (state) {
                            case PLAY:
                                stateName = "play";
                                break;
                            case PAUSE:
                                stateName = "pause";
                                break;
                            case END:
                                stateName = "ended";
                                break;
                            case SEEKING:
                            	stateName = "seeking";
                            	break;
                            case SEEKED:
                            	stateName = "seeked";
                            	break;
                            default:
                                break;
                            }
                            if (stateName != "") {
                                final String eventName = stateName;
                                notifyKPlayer("trigger", new String[] { eventName });
                            }
                    	}
                        
                        return false;
                    }
                });

        // trigger timeupdate events
        final Runnable runUpdatePlayehead = new Runnable() {
            @Override
            public void run() {               
                notifyKPlayer( "trigger", new Object[]{ "timeupdate", mCurSec});
            }
        };

        // listens for playhead update
        mVideoInterface.registerPlayheadUpdate(new OnPlayheadUpdateListener() {
            @Override
            public void onPlayheadUpdated(int msec) {
            	double curSec = msec / 1000.0;
            	if ( curSec <= mDuration ) {
            		mCurSec = curSec;
            		 mActivity.runOnUiThread(runUpdatePlayehead);
            	}
            }
        });

        // listens for progress events and notify javascript
        mVideoInterface.registerProgressUpdate(new OnProgressListener() {
            @Override
            public void onProgressUpdate(int progress) {
                double percent = progress / 100.0;
                notifyKPlayer( "trigger", new Object[]{ "progress", percent});
 
            }
        });
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url != null) {
             	Log.d(TAG, "shouldOverrideUrlLoading::url to load: " + url);
             	
            	if ( url.startsWith("js-frame:") ) {
                    String[] arr = url.split(":");
                    if (arr != null && arr.length > 1) {
                        String action = arr[1];

                        if (action.equals("notifyJsReady")) {
                        	if ( mJsReadyListener != null ) {
                        		mJsReadyListener.jsCallbackReady();
                        	}
                        }
                        else if (action.equals("notifyLayoutReady")) {   
                        	return true;
                        }
                        else if (action.equals("play")) {
                            mVideoInterface.play();
                            return true;
                        }

                        else if (action.equals("pause")) {
                            if (mVideoInterface.canPause()) {
                                mVideoInterface.pause();
                                return true;
                            }
                        }

                        else if (action.equals("toggleFullscreen")) {  
                            if (mFSListener != null) {
                                mFSListener.onToggleFullScreen();
                                return true;
                            }
                        }
                        // action with params
                        else if (arr.length > 3) {
                            try {
                                String value = URLDecoder.decode(arr[3], "UTF-8");
                                if (value != null && value.length() > 2) {
                                    if (action.equals("setAttribute")) {
                                    	JSONArray jsonArr = new JSONArray( value );
                                    	
                                    	List<String> params = new ArrayList<String>();
                                    	for (int i=0; i<jsonArr.length(); i++) {
                                    		params.add( jsonArr.getString(i) );
                                    	}
                                    
                                        if (params != null && params.size() > 1) {
                                            if (params.get(0).equals("currentTime")) {
                                                int seekTo = Math.round(Float.parseFloat(params.get(1)) * 1000);
                                                mVideoInterface.seek(seekTo);
                                            } else if (params.get(0).equals("src")) {
                                                // remove " from the edges
                                                mVideoUrl = params.get(1);
                                                mVideoInterface.setVideoUrl(mVideoUrl);
                                                asyncEvaluate("{mediaProxy.entry.name}", new KPlayerEventListener() {
    												@Override
    												public void onKPlayerEvent(
    														Object body) {
    													mVideoTitle = (String) body;												
    												}

    												@Override
    												public String getCallbackName() {
    													return "getEntryName";
    												}
                                                });
                                                asyncEvaluate("{mediaProxy.entry.thumbnailUrl}", new KPlayerEventListener() {
                                                	@Override
                                                	public void onKPlayerEvent(
                                                			Object body) {
                                                		mThumbUrl = (String) body;												
                                                	}
                                                	
                                                	@Override
                                                	public String getCallbackName() {
                                                		return "getEntryThumb";
                                                	}
                                                });
                                            } else if (params.get(0).equals("wvServerKey")) {
                                                String licenseUrl = params.get(1);
                                                WidevineHandler.acquireRights(
                                                        mActivity,
                                                        mVideoInterface.getVideoUrl(),
                                                        licenseUrl);
                                            }
                                        }
                                    } else if (action.equals("notifyKPlayerEvent")) {
                                        return notifyKPlayerEvent(value, mKplayerEventsMap, false);
                                    } else if (action.equals("notifyKPlayerEvaluated")) {
                                        return notifyKPlayerEvent(value, mKplayerEvaluatedMap, true);
                                    }
                                }

                            } catch (Exception e) {
                                Log.w(TAG, "action failed: " + action);
                            }
                        }

                    }
            	} else {
            		if (mVideoInterface.canPause()) {
                        mVideoInterface.pause();
                        mVideoInterface.setStartingPoint((int) (mCurSec * 1000));
                    }
            		Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( url ));
            		mActivity.startActivity(browserIntent);
            		return true;
            	}

            }

            return false;
        }

        /**
         * 
         * @param input
         *            string
         * @return given string without its first and last characters
         */
        private String getStrippedString(String input) {
            return input.substring(1, input.length() - 1);
        }

        /**
         * Notify the matching listener that event has occured
         * 
         * @param input
         *            String with event params
         * @param hashMap
         *            data provider to look the listener in
         * @param clearListeners
         *            whether to remove listeners after notifying them
         * @return true if listener was noticed, else false
         */
        private boolean notifyKPlayerEvent(String input,
                HashMap hashMap,
                boolean clearListeners) {
            if (hashMap != null) {
                String value = getStrippedString(input);
                // //
                // replace inner json "," delimiter so we can split with harming
                // json objects
                // value = value.replaceAll("([{][^}]+)(,)", "$1;");
                // ///
                value = value.replaceAll(("\\\\\""), "\"");
                boolean isObject = true;
                // can't split by "," since jsonString might have inner ","
                String[] params = value.split("\\{");
                // if parameter is not a json object, the delimiter is ","
                if (params.length == 1) {
                    isObject = false;
                    params = value.split(",");
                } else {
                    params[0] = params[0].substring(0, params[0].indexOf(","));
                }
                String key = getStrippedString(params[0]);
                // parse object, if sent
                Object bodyObj = null;
                if (params.length > 1 && params[1] != "null") {
                    if (isObject) { // json string
                        String body = "{" + params[1] + "}";
                        try {
                            bodyObj = new JSONObject(body);
                        } catch (JSONException e) {
                            Log.w(TAG, "failed to parse object");
                        }
                    } else { // simple string
                    	if ( params[1].startsWith("\"") )
                    		bodyObj = getStrippedString(params[1]);
                    	else
                    		bodyObj = params[1];
                    }
                }
                
                Object mapValue = hashMap.get(key);
                if ( mapValue instanceof KPlayerEventListener ) {
                    ((KPlayerEventListener)mapValue).onKPlayerEvent(bodyObj);
                } 
                else if ( mapValue instanceof ArrayList) {
                    ArrayList<KPlayerEventListener> listeners = (ArrayList)mapValue;
                            for (Iterator<KPlayerEventListener> i = listeners.iterator(); i
                                    .hasNext();) {
                                i.next().onKPlayerEvent(bodyObj);
                            }
                }

                if (clearListeners) {
                    hashMap.remove(key);
                }

                return true;
            }

            return false;
        }

    }
}