package com.kaltura.playersdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.google.android.gms.cast.CastDevice;
import com.google.gson.Gson;
import com.kaltura.playersdk.Helpers.KStringUtilities;
import com.kaltura.playersdk.actionHandlers.ShareManager;
import com.kaltura.playersdk.actionHandlers.ShareStrategyFactory;
import com.kaltura.playersdk.chromecast.ChromecastHandler;
import com.kaltura.playersdk.events.KPlayerEventListener;
import com.kaltura.playersdk.events.KPlayerJsCallbackReadyListener;
import com.kaltura.playersdk.events.OnCastDeviceChangeListener;
import com.kaltura.playersdk.events.OnCastRouteDetectedListener;
import com.kaltura.playersdk.events.OnShareListener;
import com.kaltura.playersdk.events.OnToggleFullScreenListener;
import com.kaltura.playersdk.players.BasePlayerView;
import com.kaltura.playersdk.players.KHLSPlayer;
import com.kaltura.playersdk.players.KPlayer;
import com.kaltura.playersdk.players.KPlayerController;
import com.kaltura.playersdk.players.KPlayerListener;
import com.kaltura.playersdk.types.PlayerStates;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by michalradwantzor on 9/24/13.
 */
public class PlayerViewController extends RelativeLayout implements KControlsView.KControlsViewClient, KPlayerListener {
    public static String TAG = "PlayerViewController";
    public static String DEFAULT_HOST = "http://kgit.html5video.org/";
    public static String DEFAULT_HTML5_URL = "/tags/v2.23.rc4/mwEmbedFrame.php";
    public static String DEFAULT_PLAYER_ID = "21384602";
    public static int CONTROL_BAR_HEIGHT = 38;

    //current active VideoPlayerInterface
    private BasePlayerView mVideoInterface;
    //Original VideoPlayerInterface that was created by "addComponents"
    private BasePlayerView mOriginalVideoInterface;

    private KPlayerController playerController;
    private KControlsView mWebView = null;
    private double mCurSec;
    private Activity mActivity;
    private double mDurationSec = 0;
    private OnToggleFullScreenListener mFSListener;
    private OnShareListener mShareListener;
    private HashMap<String, ArrayList<KPlayerEventListener>> mKplayerEventsMap = new HashMap<String, ArrayList<KPlayerEventListener>>();
    private HashMap<String, KPlayerEventListener> mKplayerEvaluatedMap = new HashMap<String, KPlayerEventListener>();
    private KPlayerJsCallbackReadyListener mJsReadyListener;
    private JSONObject nativeActionParams;

    public String host = DEFAULT_HOST;

    private String mVideoUrl;
    private String mVideoTitle = "";
    private String mThumbUrl = "";
    private String mIframeUrl = null;

    private PlayerStates mState = PlayerStates.START;
    private PowerManager mPowerManager;

    private boolean mWvMinimized = false;

    private int newWidth, newHeight;

    // trigger timeupdate events

    public PlayerViewController(Context context) {
        super(context);
        setupPlayerViewController( context );
    }

    public PlayerViewController(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupPlayerViewController( context );
    }

    public PlayerViewController(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
        setupPlayerViewController( context );
    }

    /**
     * Release player's instance and save its last position for resuming later on.
     * This method should be called when the main activity is paused.
     */
    public void releaseAndSavePosition() {
        savePlaybackPosition();
        if ( mVideoInterface != null )
        {
            mVideoInterface.release();
        }
    }

    /**
     * Recover from "releaseAndSavePosition", reload the player from previous position.
     * This method should be called when the main activity is resumed.
     */
    public void resumePlayer() {
        if ( mVideoInterface != null ) {
            mVideoInterface.recoverRelease();
        }
    }

    private void setupPlayerViewController( final Context context) {
        mPowerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());
        Runnable myRunnable = new Runnable() {
            @Override
            public void run() {
                if ( !ChromecastHandler.initialized )
                    ChromecastHandler.initialize(context, new OnCastDeviceChangeListener() {

                                @Override
                                public void onCastDeviceChange(CastDevice oldDevice, CastDevice newDevice) {
                                    if ( ChromecastHandler.selectedDevice != null ) {
                                        notifyKPlayer("trigger", new String[] { "chromecastDeviceConnected" });
                                    } else {
                                        notifyKPlayer("trigger", new String[] { "chromecastDeviceDisConnected" });
                                    }
//                                    createPlayerInstance();
                                }
                            },
                            new OnCastRouteDetectedListener(){
                                @Override
                                public void onCastRouteDetected() {
                                    setChromecastVisiblity();
                                }
                            });
            }
        };
        mainHandler.post(myRunnable);
    }

    public void setActivity( Activity activity ) {
        mActivity = activity;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    public void setOnFullScreenListener(OnToggleFullScreenListener listener) {
        mFSListener = listener;
    }

    public void setOnShareListener(OnShareListener listener) {
        mShareListener = listener;
    }

    private void setVolumeLevel(double percent) {//Itay
        AudioManager mgr = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        if (percent > 0.01) {
            while (percent < 1.0) {
                percent *= 10;
            }
        }
        mgr.setStreamVolume(AudioManager.STREAM_MUSIC, (int) percent, 0);
    }
    @SuppressLint("NewApi") private Point getRealScreenSize(){
        Display display = mActivity.getWindowManager().getDefaultDisplay();
        int realWidth = 0;
        int realHeight = 0;

        if (Build.VERSION.SDK_INT >= 17){
            //new pleasant way to get real metrics
            DisplayMetrics realMetrics = new DisplayMetrics();
            display.getRealMetrics(realMetrics);
            realWidth = realMetrics.widthPixels;
            realHeight = realMetrics.heightPixels;

        } else {
            try {
                Method mGetRawH = Display.class.getMethod("getRawHeight");
                Method mGetRawW = Display.class.getMethod("getRawWidth");
                realWidth = (Integer) mGetRawW.invoke(display);
                realHeight = (Integer) mGetRawH.invoke(display);
            } catch (Exception e) {
                realWidth = display.getWidth();
                realHeight = display.getHeight();
                Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
            }

        }
        return new Point(realWidth,realHeight);
    }


    /**
     * Sets the player's dimensions. Should be called for any player redraw
     * (for example, in screen rotation, if supported by the main activity)
     * @param width player's width
     * @param height player's height
     * @param xPadding player's X position
     * @param yPadding player's Y position
     */
    public void setPlayerViewDimensions(int width, int height, int xPadding, int yPadding) {
        setPadding(xPadding, yPadding, 0, 0);
        newWidth = width + xPadding;
        newHeight = height + yPadding;

        ViewGroup.LayoutParams lp = getLayoutParams();
        if ( lp == null ) {
            lp = new ViewGroup.LayoutParams( newWidth, newHeight );
        } else {
            lp.width = newWidth;
            lp.height = newHeight;
        }

        this.setLayoutParams(lp);
        for ( int i = 0; i < this.getChildCount(); i++ ) {

            View v = getChildAt(i);
            if( v == mVideoInterface )
            {
                continue;
            }
            ViewGroup.LayoutParams vlp = v.getLayoutParams();
            vlp.width = newWidth;
            if ( (!mWvMinimized || !v.equals( mWebView)) ) {//
                vlp.height = newHeight;
            }
            updateViewLayout(v, vlp);
        }


        if(mWebView != null) {
//            mWebView.loadUrl("javascript:android.onData(NativeBridge.videoPlayer.getControlBarHeight())");
            mWebView.fetchControlsBarHeight(new KControlsView.ControlsBarHeightFetcher() {
                @Override
                public void fetchHeight(int controlBarHeight) {
                    if ( mVideoInterface != null && mVideoInterface.getParent() == PlayerViewController.this ) {
                        LayoutParams wvLp = (LayoutParams) ((View) mVideoInterface).getLayoutParams();

                        if (getPaddingLeft() == 0 && getPaddingTop() == 0) {
                            wvLp.addRule(CENTER_IN_PARENT);
                        } else {
                            wvLp.addRule(CENTER_IN_PARENT, 0);
                        }
                        float scale = mActivity.getResources().getDisplayMetrics().density;
                        controlBarHeight = (int) (controlBarHeight * scale + 0.5f);
                        wvLp.height = newHeight - controlBarHeight;
                        wvLp.width = newWidth;
                        wvLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                        final LayoutParams lp = wvLp;
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateViewLayout(mVideoInterface, lp);
                                invalidate();
                            }
                        });

                    }
                }
            });
        }
        invalidate();
    }


    /**
     * Sets the player's dimensions. Should be called for any player redraw
     * (for example, in screen rotation, if supported by the main activity)
     * Player's X and Y position will be 0
     * @param width player's width
     * @param height player's height
     */
    public void setPlayerViewDimensions(int width, int height) {
        setPlayerViewDimensions(width, height, 0, 0);
    }

    private void setChromecastVisiblity() {
        if ( ChromecastHandler.routeInfos.size() > 0 ) {
            setKDPAttribute("chromecast", "visible", true);
        } else {
            setKDPAttribute("chromecast", "visible", false);
        }
    }

    /*
     * Build player URL and load it to player view
     * @param requestDataSource - RequestDataSource object
     */
    public void setComponents(RequestDataSource requestDataSource) {
        String iframeUrl = RequestHandler.videoRequestURL(requestDataSource);
//		String iframeUrl = host + html5Url + "?wid=_" + partnerId + "&uiconf_id=" + playerId + "&entry_id=" + entryId + "&flashvars[Kaltura.LeadHLSOnAndroid]=true";
        setComponents(iframeUrl);
    }

    /**
     * Build player URL and load it to player view
     * param iFrameUrl- String url
     */
    public void setComponents(String iframeUrl) {
        if(mWebView == null) {
            mWebView = new KControlsView(getContext());
            mWebView.setKControlsViewClient(this);

            mCurSec = 0;
            ViewGroup.LayoutParams currLP = getLayoutParams();
            LayoutParams wvLp = new LayoutParams(currLP.width, currLP.height);

            this.playerController = new KPlayerController(new KPlayer(mActivity), this);
            this.playerController.addPlayerToController(this);
            this.addView(mWebView, wvLp);
        }

        iframeUrl = RequestHandler.getIframeUrlWithNativeVersion(iframeUrl, this.getContext());
        if( mIframeUrl == null || !mIframeUrl.equals(iframeUrl) )
        {
            iframeUrl = iframeUrl + "&iframeembed=true";
            mIframeUrl = iframeUrl;
            mWebView.loadUrl(iframeUrl);
        }
    }

    public void triggerEvent(String event, String value) {
        mWebView.triggerEvent(event, value);
    }

    /**
     * create PlayerView / CastPlayer instance according to cast status
     */

    private void replacePlayerViewChild( View newChild, View oldChild ) {
        if ( oldChild.getParent().equals( this ) ) {
            this.removeView( oldChild );
        }

        if ( this.getChildCount() > 1 ) {
            //last child is the controls webview
            this.addView( newChild , this.getChildCount() -1, oldChild.getLayoutParams() );
        }
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

//    public void destroy() {
//        this.stop();
//    }

    public void savePlaybackPosition() {
        if ( mVideoInterface!= null ) {
            mVideoInterface.setStartingPoint( (int) (mCurSec * 1000) );
        }
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
    public double getDurationSec() {
        double duration = 0;
        if (mVideoInterface != null) {
            duration = mVideoInterface.getDuration();
        }
        duration /= 1000;
        return duration;
    }

    public String getVideoUrl() {
        String url = null;
        if (mVideoInterface != null)
            url = mVideoInterface.getVideoUrl();

        return url;
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
        notifyKPlayer("asyncEvaluate", new String[]{expression, callbackName});
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
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
                    for (int i = 0; i < eventValues.length; i++) {
                        if (eventValues[i] instanceof String) {
                            values += "'" + eventValues[i] + "'";
                        } else {
                            values += eventValues[i].toString();
                        }
                        if (i < eventValues.length - 1) {
                            values += ", ";
                        }
                    }
                    // values = TextUtils.join("', '", eventValues);
                }
                if (mWebView != null) {
                    Log.d(TAG, "NotifyKplayer: " + values);
                    mWebView.loadUrl("javascript:NativeBridge.videoPlayer."
                            + action + "(" + values + ");");
                }

            }
        });
    }


    @Override
    public void handleHtml5LibCall(String functionName, int callbackId, String args) {
        Method bridgeMethod = KStringUtilities.isMethodImplemented(this, functionName);
        Object object = this;
        if (bridgeMethod == null) {
            bridgeMethod = KStringUtilities.isMethodImplemented(this.playerController.getPlayer(), functionName);
            object = this.playerController.getPlayer();
        }
        if (bridgeMethod != null) {
            try {
                if (args == null) {
                    bridgeMethod.invoke(object);
                } else {
                    bridgeMethod.invoke(object, args);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void openURL(String url) {

    }

    //
    @Override
    public void eventWithValue(KPlayerController.KPlayer player, String eventName, String eventValue) {
        this.mWebView.triggerEvent(eventName, eventValue);
    }

    @Override
    public void eventWithJSON(KPlayerController.KPlayer player, String eventName, String jsonValue) {
        this.mWebView.triggerEventWithJSON(eventName, jsonValue);
    }

    @Override
    public void contentCompleted(KPlayerController.KPlayer currentPlayer) {
        this.mWebView.triggerEvent(KPlayer.EndedKey, null);
    }

    private static class ErrorBuilder {
        String errorMessage;
        int errorId;

        public ErrorBuilder()
        {

        }

        public ErrorBuilder setErrorMessage(String errorMessage){
            this.errorMessage = errorMessage;
            return this;
        }


        public ErrorBuilder setErrorId(int errorId){
            this.errorId = errorId;
            return this;
        }

        public ErrorObject build(){
            return new ErrorObject(this);
        }

        public static class ErrorObject{
            String errorMessage;
            int errorId;
            private ErrorObject(ErrorBuilder builder){
                errorMessage = builder.errorMessage;
                errorId = builder.errorId;
            }

            @Override
            public String toString() {
                return new Gson().toJson(this);
            }
        }
    }
    private class CustomWebViewClient extends WebViewClient {

//        @Override
//        public void onPageFinished(WebView view, String url) {
//            super.onPageFinished(view, url);
//            view.clearCache(true);
//        }

//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, String url) {
//            if (url != null) {
//                Log.d(TAG, "shouldOverrideUrlLoading::url to load: " + url);
//                KStringUtilities urlUtil = new KStringUtilities(url);
//                if ( urlUtil.isJSFrame() || url.contains("mwEmbedFrame.php")) {
//                    String[] arr = url.split(":");
//                    if (arr != null && arr.length > 1) {
//                        String action = arr[1];
//
////                        if (action.equals("notifyJsReady")) {
////                            if ( mJsReadyListener != null ) {
////                                mJsReadyListener.jsCallbackReady();
////                            }
////                        }
////                        else if (action.equals("notifyLayoutReady")) {
////                            setChromecastVisiblity();
////                            return true;
////                        }
//                        else if (action.equals("play")) {
//                            mVideoInterface.play();
//                            return true;
//                        }
//
//                        else if (action.equals("pause")) {
//                            if ( mVideoInterface.canPause() ) {
//                                mVideoInterface.pause();
//                                return true;
//                            }
//                        }
//
////                        else if (action.equals("toggleFullscreen")) {
////                            if (mFSListener != null) {
////                                mFSListener.onToggleFullScreen();
////                                return true;
////                            }
////                        }
////                        else if ( action.equals("showChromecastDeviceList") ) {
////                            if(!mActivity.isFinishing())
////                            {
////                                //workaround to fix weird exception sometimes
////                                try {
////                                    ChromecastHandler.showCCDialog(getContext());
////                                } catch (Exception e ) {
////                                    Log.d(TAG, "failed to open cc list");
////                                }
////
////                            }
////                        }
//                        // action with params
//                        else if (arr != null && arr.length > 3) {
//                            try {
//                                String value = URLDecoder.decode(arr[3], "UTF-8");
//                                if (value != null && value.length() > 2) {
//                                    if (action.equals("setAttribute")) {
//                                        JSONArray jsonArr = new JSONArray(value);
//
//                                        List<String> params = new ArrayList<String>();
//                                        for (int i = 0; i < jsonArr.length(); i++) {
//                                            params.add(jsonArr.getString(i));
//                                        }
//
//                                        if (params != null && params.size() > 1) {
//                                            if (params.get(0).equals("currentTime")) {
//                                                float seekTo = (Float.parseFloat(params.get(1)));
//                                                Log.d(TAG,"SEEK: from JS To :" + seekTo);
//                                                mVideoInterface.seek((int)(seekTo*1000));
//                                            } else if (params.get(0).equals("src")) {
//                                                // remove " from the edges
//                                                mVideoUrl = params.get(1);
//                                                //check for hls
//                                                int lastIndex = mVideoUrl.indexOf("?") != -1 ? mVideoUrl.indexOf("?") : mVideoUrl.length();
//                                                String videoUrl = mVideoUrl;
//                                                String extension = videoUrl.substring(videoUrl.lastIndexOf(".") + 1);
//
//                                                BasePlayerView tmpPlayer = null;
//                                                boolean shouldReplacePlayerView = false;
//                                                if (extension.equals("m3u8")) {
//                                                    if (!(mVideoInterface instanceof HLSPlayer)) {
//                                                        // ((HLSPlayer) mVideoInterface).switchQualityTrack
//                                                        tmpPlayer = new HLSPlayer(mActivity);
//                                                        shouldReplacePlayerView = true;
//                                                    }
//                                                } else {
//                                                    if (mVideoInterface instanceof HLSPlayer) {
//                                                        tmpPlayer = new KalturaPlayer(mActivity);
//                                                        shouldReplacePlayerView = true;
//                                                    }
//                                                }
//
//                                                if (shouldReplacePlayerView && tmpPlayer != null) {
//                                                    if (mVideoInterface instanceof View) {
//                                                        replacePlayerViewChild((View) tmpPlayer, (View) mVideoInterface);
//                                                    }
//                                                    removePlayerListeners();
//                                                    if (mVideoInterface instanceof HLSPlayer) {
//                                                        ((HLSPlayer) mVideoInterface).close();
//                                                    }
//
//                                                    mVideoInterface = tmpPlayer;
//                                                    setPlayerListeners();
//                                                }
//                                                mVideoInterface.setVideoUrl(mVideoUrl);
//                                                asyncEvaluate("{mediaProxy.entry.name}", new KPlayerEventListener() {
//                                                    @Override
//                                                    public void onKPlayerEvent(
//                                                            Object body) {
//                                                        mVideoTitle = (String) body;
//                                                    }
//
//                                                    @Override
//                                                    public String getCallbackName() {
//                                                        return "getEntryName";
//                                                    }
//                                                });
//                                                asyncEvaluate("{mediaProxy.entry.thumbnailUrl}", new KPlayerEventListener() {
//                                                    @Override
//                                                    public void onKPlayerEvent(
//                                                            Object body) {
//                                                        mThumbUrl = (String) body;
//                                                    }
//
//                                                    @Override
//                                                    public String getCallbackName() {
//                                                        return "getEntryThumb";
//                                                    }
//                                                });
//                                            } else if (params.get(0).equals("wvServerKey")) {
//                                                if (!(mVideoInterface instanceof PlayerView)) {
//
//                                                    ViewGroup.LayoutParams currLP = getLayoutParams();
//                                                    PlayerView playerView = new PlayerView(mActivity, true);
//                                                    LayoutParams lp = new LayoutParams(currLP.width, currLP.height);
//                                                    if (mVideoInterface instanceof View) {
//                                                        replacePlayerViewChild(playerView, (View) mVideoInterface);
//                                                    } else {
//                                                        addView(playerView, getChildCount() - 1, lp);
//                                                    }
//
//                                                    mOriginalVideoInterface = playerView;
//                                                    mOriginalVideoInterface.setVideoUrl(mVideoInterface.getVideoUrl());
//                                                    createPlayerInstance();
//                                                }
//                                                String licenseUrl = params.get(1);
//                                                WidevineHandler.acquireRights(
//                                                        mActivity,
//                                                        mVideoInterface.getVideoUrl(),
//                                                        licenseUrl);
//                                            } else if (params.get(0).equals("doubleClickRequestAds")) {
//                                                if (mVideoInterface instanceof KalturaPlayer) {
//                                                    mVideoInterface.release();
//                                                    IMAPlayer imaPlayer = new IMAPlayer(getContext());
//                                                    replacePlayerViewChild(imaPlayer, (KalturaPlayer) mVideoInterface);
//                                                    imaPlayer.setParams(mVideoInterface, params.get(1), mActivity, new KPlayerEventListener() {
//
//                                                        @Override
//                                                        public void onKPlayerEvent(
//                                                                Object body) {
//                                                            if (body instanceof Object[]) {
//                                                                notifyKPlayer("trigger", (Object[]) body);
//                                                            } else {
//                                                                notifyKPlayer("trigger", new String[]{body.toString()});
//                                                            }
//                                                        }
//
//                                                        @Override
//                                                        public String getCallbackName() {
//                                                            // TODO Auto-generated method stub
//                                                            return null;
//                                                        }
//
//                                                    });
//
//                                                    removePlayerListeners();
//                                                    mVideoInterface = imaPlayer;
//                                                    setPlayerListeners();
//
//                                                    imaPlayer.registerWebViewMinimize(new OnWebViewMinimizeListener() {
//
//                                                        @Override
//                                                        public void setMinimize(boolean minimize) {
//                                                            if (minimize != mWvMinimized) {
//                                                                mWvMinimized = minimize;
//                                                                LayoutParams wvLp = (LayoutParams) mWebView.getLayoutParams();
//
//                                                                if (minimize) {
//                                                                    float scale = mActivity.getResources().getDisplayMetrics().density;
//                                                                    wvLp.height = (int) (CONTROL_BAR_HEIGHT * scale + 0.5f);
//                                                                    ;
//                                                                    wvLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
//                                                                } else {
//                                                                    wvLp.height = getLayoutParams().height;
//                                                                }
//
//                                                                updateViewLayout(mWebView, wvLp);
//                                                            }
//
//                                                        }
//
//                                                    });
//                                                    mVideoInterface.play();
//                                                } else {
//                                                    Log.w(TAG, "DoubleClick is not supported by this player");
//                                                }
//                                            }else if(params.get(0).equals("goLive")){
//                                                Log.d(TAG, "SEEK: GOTOLIVE");
//                                                //temporary fix - waiting for an HLS api
//                                                if (params.get(1).equals("true") && mVideoInterface instanceof LiveStreamInterface){
//                                                    ((LiveStreamInterface)mVideoInterface).switchToLive();
//                                                }
//                                            } else if (params.get(0).equals("nativeAction")) {
//                                                doNativeAction(params.get(1));
//                                            }
//                                        }
//                                    } else if (action.equals("notifyKPlayerEvent")) {
//                                        return notifyKPlayerEvent(value, mKplayerEventsMap, false);
//                                    } else if (action.equals("notifyKPlayerEvaluated")) {
//                                        return notifyKPlayerEvent(value, mKplayerEvaluatedMap, true);
//                                    }
//                                }
//
//
//                            } catch (Exception e) {
//                                Log.w(TAG, "action failed: " + action);
//                            }
//                        }
//
//                    }
//                } else {
//                    if (mVideoInterface.canPause()) {
//                        mVideoInterface.pause();
//                        mVideoInterface.setStartingPoint((int) (mCurSec * 1000));
//                    }
//                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( url ));
//                    mActivity.startActivity(browserIntent);
//                    return true;
//                }
//
//            }
//
//            return false;
//        }

        /**
         *
         * @param input
         *            string
         * @return given string without its first and last characters
         */


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
//        private boolean notifyKPlayerEvent(String input,
//                                           HashMap hashMap,
//                                           boolean clearListeners) {
//            if (hashMap != null) {
//                String value = getStrippedString(input);
//                // //
//                // replace inner json "," delimiter so we can split with harming
//                // json objects
//                // value = value.replaceAll("([{][^}]+)(,)", "$1;");
//                // ///
//                value = value.replaceAll(("\\\\\""), "\"");
//                boolean isObject = true;
//                // can't split by "," since jsonString might have inner ","
//                String[] params = value.split("\\{");
//                // if parameter is not a json object, the delimiter is ","
//                if (params.length == 1) {
//                    isObject = false;
//                    params = value.split(",");
//                } else {
//                    params[0] = params[0].substring(0, params[0].indexOf(","));
//                }
//                String key = getStrippedString(params[0]);
//                // parse object, if sent
//                Object bodyObj = null;
//                if (params.length > 1 && params[1] != "null") {
//                    if (isObject) { // json string
//                        String body = "{" + params[1] + "}";
//                        try {
//                            bodyObj = new JSONObject(body);
//                        } catch (JSONException e) {
//                            Log.w(TAG, "failed to parse object");
//                        }
//                    } else { // simple string
//                        if ( params[1].startsWith("\"") )
//                            bodyObj = getStrippedString(params[1]);
//                        else
//                            bodyObj = params[1];
//                    }
//                }
//
//                Object mapValue = hashMap.get(key);
//                if ( mapValue instanceof KPlayerEventListener ) {
//                    ((KPlayerEventListener)mapValue).onKPlayerEvent(bodyObj);
//                }
//                else if ( mapValue instanceof ArrayList) {
//                    ArrayList<KPlayerEventListener> listeners = (ArrayList)mapValue;
//                    for (Iterator<KPlayerEventListener> i = listeners.iterator(); i
//                            .hasNext();) {
//                        i.next().onKPlayerEvent(bodyObj);
//                    }
//                }
//
//                if (clearListeners) {
//                    hashMap.remove(key);
//                }
//
//                return true;
//            }
//
//            return false;
//        }

    }


    /// Bridge methods
    private void setAttribute(String argsString) {
        String[] args = KStringUtilities.fetchArgs(argsString);
        if (args != null && args.length == 2) {
            String attributeName = args[0];
            String attributeValue = args[1];
            switch (KStringUtilities.attributeEnumFromString(attributeName)) {
                case src:
                    if (KStringUtilities.isHLSSource(attributeValue)) {
                        playerController.switchPlayer(new KHLSPlayer(mActivity));
                    }
                    this.playerController.setSrc(attributeValue);
                    break;
                case currentTime:
                    this.playerController.setCurrentPlaybackTime(Float.parseFloat(attributeValue));
                    break;
                case visible:
                    this.triggerEvent("visible", attributeValue);
                    break;
                case wvServerKey:
//                    this.playerController.switchPlayer(this.playerController.KWVPlayerClassName, attributeValue);
                    break;
                case nativeAction:
                    try {
                        this.nativeActionParams = new JSONObject(attributeValue);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case language:
                    this.playerController.setLocale(attributeValue);
                    break;
                case doubleClickRequestAds:
                    playerController.initIMA(attributeValue, mActivity);
                    break;
            }
        }
    }

    private void notifyJsReady() {
        if ( mJsReadyListener != null ) {
            mJsReadyListener.jsCallbackReady();
        }
    }

    private void notifyLayoutReady() {
        setChromecastVisiblity();
    }

    private void toggleFullscreen() {
        if (mFSListener != null) {
            mFSListener.onToggleFullScreen();
        }
    }

    private void showChromecastDeviceList() {
        if(!mActivity.isFinishing())
        {
            //workaround to fix weird exception sometimes
            try {
                ChromecastHandler.showCCDialog(getContext());
            } catch (Exception e ) {
                Log.d(TAG, "failed to open cc list");
            }
        }
    }

    private void bindPlayerEvents() {

    }

    private void doNativeAction(String params) {
        Log.d("NativeAction", params);
        try {
            JSONObject actionObj = new JSONObject(params);
            if (actionObj.get("actionType").equals("share")) {
                share(actionObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void notifyKPlayerEvent(String args) {

    }


    // Native actions
    private void share(JSONObject shareParams) {
        if(mShareListener != null){
            try {
                String videoUrl = (String)shareParams.get("sharedLink");
                String videoName = (String)shareParams.get("videoName");
                ShareManager.SharingType type = ShareStrategyFactory.getType(shareParams);
                if (!mShareListener.onShare(videoUrl, type, videoName)){
                    ShareManager.share(shareParams, mActivity);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }else {
            ShareManager.share(shareParams, mActivity);
        }
    }
}
