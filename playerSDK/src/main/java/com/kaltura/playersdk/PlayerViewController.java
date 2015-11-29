package com.kaltura.playersdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
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
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.cast.CastDevice;
import com.google.gson.Gson;
import com.kaltura.playersdk.casting.CastRouterManager;
import com.kaltura.playersdk.casting.KCastRouterManager;
import com.kaltura.playersdk.casting.KCastRouterManagerListener;
import com.kaltura.playersdk.casting.KRouterInfo;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.helpers.CacheManager;
import com.kaltura.playersdk.helpers.KStringUtilities;
import com.kaltura.playersdk.players.KCCPlayer;
import com.kaltura.playersdk.players.KHLSPlayer;
import com.kaltura.playersdk.players.KPlayer;
import com.kaltura.playersdk.players.KPlayerController;
import com.kaltura.playersdk.players.KPlayerListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by michalradwantzor on 9/24/13.
 */
public class PlayerViewController extends RelativeLayout implements KControlsView.KControlsViewClient, KPlayerListener, CastRouterManager.CastRouterManagerListener {
    public static String TAG = "PlayerViewController";



    private KPlayerController playerController;
    private KControlsView mWebView = null;
    private double mCurSec;
    private Activity mActivity;
//    private OnShareListener mShareListener;
    private JSONObject nativeActionParams;
    private KPPlayerConfig mConfig;


    private String mIframeUrl = null;

    private PowerManager mPowerManager;

    private boolean mWvMinimized = false;

    private int newWidth, newHeight;

    private boolean mIsJsCallReadyRegistration = false;
    private Set<ReadyEventListener> mCallBackReadyRegistrations;
    private HashMap<String, ArrayList<HashMap<String, EventListener>>> mPlayerEventsHash;
    private HashMap<String, EvaluateListener> mPlayerEvaluatedHash;
    private Set<KPEventListener> eventListeners;
    private boolean isFullScreen = false;
    private CastRouterManager mRouterManager;

    @Override
    public void didFoundDevices(final boolean didFound) {
        if (mRouterManager.shouldEnableKalturaCastButton()) {
            registerReadyEvent(new ReadyEventListener() {
                @Override
                public void handler() {
                    mWebView.setKDPAttribute("chromecast", "visible", didFound ? "true" : "false");
                }
            });
        }
        if (mRouterManager.getAppListener() != null) {
            mRouterManager.getAppListener().didDetectCastDevices(didFound);
        }
    }

    @Override
    public void updateDetectedDeviceList(boolean shouldAdd, KRouterInfo info) {
        if (mRouterManager.getAppListener() != null) {
            if (shouldAdd) {
                mRouterManager.getAppListener().addedCastDevice(info);
            } else {
                mRouterManager.getAppListener().removedCastDevice(info);
            }
        }
    }

    @Override
    public void castDeviceChanged(CastDevice oldDevice, CastDevice newDevice) {
        if (mRouterManager.shouldEnableKalturaCastButton()) {
            if (newDevice == null) {
                mWebView.triggerEvent("chromecastDeviceDisConnected", null);
            } else {
                mWebView.triggerEvent("hideConnectingMessage", null);
                mWebView.triggerEvent("chromecastDeviceConnected", null);
                playerController.switchPlayer(new KCCPlayer(mActivity, "FFCC6D19"));
                playerController.setSrc(playerController.getSrc());
            }
        }
        if (mRouterManager.getAppListener() != null) {
            mRouterManager.getAppListener().castDeviceConnectionState(newDevice != null);
        }
    }

    @Override
    public void connecting() {
        mWebView.triggerEvent("showConnectingMessage", null);
    }

    // trigger timeupdate events

    public interface EventListener {
        public void handler(String eventName, String params);
    }

    public interface ReadyEventListener {
        public void handler();
    }

    public interface EvaluateListener {
        public void handler(String evaluateResponse);
    }

    public PlayerViewController(Context context) {
        super(context);
        setupPlayerViewController( context );
    }

    public KCastRouterManager getKCastRouterManager() {
        if (mRouterManager == null) {
            mRouterManager = new CastRouterManager(mActivity.getApplicationContext(), "FFCC6D19");
            mRouterManager.setCastRouterListener(this);
        }
        return mRouterManager;
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

    public void initWithConfiguration(KPPlayerConfig configuration) {
        mConfig = configuration;
        setComponents(mConfig.getVideoURL());
    }

    public void loadPlayerIntoActivity(Activity activity) {
        registerReadyEvent(new ReadyEventListener() {
            @Override
            public void handler() {
                if (eventListeners != null) {
                    for (KPEventListener listener: eventListeners) {
                        listener.onKPlayerStateChanged(PlayerViewController.this, KPlayerState.LOADED);
                    }
                }
            }
        });
        mActivity = activity;
    }

    public void addEventListener(KPEventListener listener) {
        if (listener != null) {
            if (eventListeners == null) {
                eventListeners = new HashSet<>();
            }
            eventListeners.add(listener);
        }
    }

    public void removeEventListener(KPEventListener listener) {
        if (listener != null && eventListeners != null && eventListeners.contains(listener)) {
            eventListeners.remove(listener);
        }
    }

    /**
     * Release player's instance and save its last position for resuming later on.
     * This method should be called when the main activity is paused.
     */
    public void releaseAndSavePosition() {
        playerController.removePlayer();
    }

    /**
     * Recover from "releaseAndSavePosition", reload the player from previous position.
     * This method should be called when the main activity is resumed.
     */
    public void resumePlayer() {
        if (playerController != null) {
            playerController.recoverPlayer();
        }
    }

    public void removePlayer() {
        if (playerController != null) {
            playerController.destroy();
        }
        if (eventListeners != null) {
            eventListeners = null;
        }
    }

    private void setupPlayerViewController( final Context context) {
        mPowerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(context.getMainLooper());
//        Runnable myRunnable = new Runnable() {
//            @Override
//            public void run() {
//                if ( !ChromecastHandler.initialized )
//                    ChromecastHandler.initialize(context, new OnCastDeviceChangeListener() {
//
//                                @Override
//                                public void onCastDeviceChange(CastDevice oldDevice, CastDevice newDevice) {
//                                    if ( ChromecastHandler.selectedDevice != null ) {
//                                        notifyKPlayer("trigger", new String[] { "chromecastDeviceConnected" });
//                                    } else {
//                                        notifyKPlayer("trigger", new String[] { "chromecastDeviceDisConnected" });
//                                    }
////                                    createPlayerInstance();
//                                }
//                            },
//                            new OnCastRouteDetectedListener(){
//                                @Override
//                                public void onCastRouteDetected() {
//                                    setChromecastVisiblity();
//                                }
//                            });
//            }
//        };
//        mainHandler.post(myRunnable);
    }

    public void setActivity( Activity activity ) {
        mActivity = activity;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }


//    public void setOnShareListener(OnShareListener listener) {
//        mShareListener = listener;
//    }

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
            if( v == playerController.getPlayer() )
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
                    if ( playerController.getPlayer() != null && playerController.getPlayer() instanceof FrameLayout && ((FrameLayout)playerController.getPlayer()).getParent() == PlayerViewController.this ) {
                        LayoutParams wvLp = (LayoutParams) ((View) playerController.getPlayer()).getLayoutParams();

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
                                updateViewLayout((View) playerController.getPlayer(), lp);
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
//        mWebView.setKDPAttribute("chromecast", "visible", ChromecastHandler.routeInfos.size() > 0 ? "true" : "false");
    }

    /*
     * Build player URL and load it to player view
     * @param requestDataSource - RequestDataSource object
     */

    /**
     * Build player URL and load it to player view
     * param iFrameUrl- String url
     */
    public void setComponents(String iframeUrl) {
        if(mWebView == null) {
            mWebView = new KControlsView(getContext().getApplicationContext());
            mWebView.setKControlsViewClient(this);

            mCurSec = 0;
            ViewGroup.LayoutParams currLP = getLayoutParams();
            LayoutParams wvLp = new LayoutParams(currLP.width, currLP.height);

            this.playerController = new KPlayerController(new KPlayer(mActivity), this);
            this.playerController.addPlayerToController(this);
            this.addView(mWebView, wvLp);
        }
        if( mIframeUrl == null || !mIframeUrl.equals(iframeUrl) )
        {
//            iframeUrl = iframeUrl + "&iframeembed=true";
            mIframeUrl = iframeUrl;
            Uri uri = Uri.parse(iframeUrl);
            if (mConfig.getCacheSize() > 0) {
                CacheManager.getInstance().setHost(uri.getHost());
                CacheManager.getInstance().setCacheSize(mConfig.getCacheSize());
                mWebView.setCacheManager(CacheManager.getInstance());
            }

            mWebView.loadUrl(iframeUrl);
        }
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



    // /////////////////////////////////////////////////////////////////////////////////////////////
    // VideoPlayerInterface methods
    // /////////////////////////////////////////////////////////////////////////////////////////////
//    public boolean isPlaying() {
//        return (playerController.getPlayer() != null && playerController.getPlayer().isPlaying());
//    }

    /**
     *
     * @return duration in seconds
     */
    public double getDurationSec() {
        double duration = 0;
        if (playerController != null) {
            duration = playerController.getDuration();
        }
        return duration;
    }

    public String getVideoUrl() {
        String url = null;
        if (playerController != null)
            url = playerController.getSrc();

        return url;
    }

    public double getCurrentPlaybackTime() {
        if (playerController != null) {
            return playerController.getCurrentPlaybackTime();
        }
        return 0.0;
    }


    // /////////////////////////////////////////////////////////////////////////////////////////////
    // Kaltura Player external API
    // /////////////////////////////////////////////////////////////////////////////////////////////

    public void sendNotification(String noteName, JSONObject noteBody) {
        notifyKPlayer("sendNotification", new String[]{noteName, noteBody.toString()});
    }



    public void setKDPAttribute(String hostName, String propName, Object value) {
        notifyKPlayer("setKDPAttribute", new Object[]{hostName, propName, value});
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
        KStringUtilities event = new KStringUtilities(eventName);
        if (eventListeners != null) {
            for (KPEventListener listener : eventListeners) {
                if (KPlayerState.getStateForEventName(eventName) != null) {
                    listener.onKPlayerStateChanged(this, KPlayerState.getStateForEventName(eventName));
                } else if (event.isTimeUpdate()) {
                    listener.onKPlayerPlayheadUpdate(this, Float.parseFloat(eventValue));
                }
            }
        }
        this.mWebView.triggerEvent(eventName, eventValue);
    }

    @Override
    public void eventWithJSON(KPlayerController.KPlayer player, String eventName, String jsonValue) {
        this.mWebView.triggerEventWithJSON(eventName, jsonValue);
    }

    @Override
    public void contentCompleted(KPlayerController.KPlayer currentPlayer) {
        if (eventListeners != null) {
            for (KPEventListener listener: eventListeners) {
                listener.onKPlayerStateChanged(this, KPlayerState.ENDED);
            }
        }
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

    public void registerReadyEvent(ReadyEventListener listener) {
        if (mIsJsCallReadyRegistration) {
            listener.handler();
        } else {
            if (mCallBackReadyRegistrations == null && listener != null) {
                mCallBackReadyRegistrations = new HashSet<>();
            }
            mCallBackReadyRegistrations.add(listener);
        }
    }

    public void addKPlayerEventListener(final String event, final String eventID, final EventListener listener) {
        this.registerReadyEvent(new ReadyEventListener() {
            @Override
            public void handler() {
                if (mPlayerEventsHash == null) {
                    mPlayerEventsHash = new HashMap<String, ArrayList<HashMap<String,EventListener>>>();
                }
                ArrayList<HashMap<String, EventListener>> listenerArr = (ArrayList<HashMap<String, EventListener>>)mPlayerEventsHash.get(event);
                if (listenerArr == null) {
                    listenerArr = new ArrayList<HashMap<String, EventListener>>();
                }
                HashMap<String, EventListener> addedEvent = new HashMap<String, EventListener>();
                addedEvent.put(eventID, listener);
                listenerArr.add(addedEvent);
                mPlayerEventsHash.put(event, listenerArr);
                if (listenerArr.size() == 1 && !KStringUtilities.isToggleFullScreen(event)) {
                    mWebView.addEventListener(event);
                }
            }
        });
    }

    public void removeKPlayerEventListener(String event,String eventID) {
        ArrayList<HashMap<String, EventListener>> listenerArr = mPlayerEventsHash.get(event);
        if (listenerArr == null || listenerArr.size() == 0) {
            return;
        }
        ArrayList<HashMap<String, EventListener>> temp = new ArrayList<HashMap<String, EventListener>>(listenerArr);
        for (HashMap<String, EventListener> hash: temp) {
            if (hash.keySet().toArray()[hash.keySet().size() - 1].equals(eventID)) {
                listenerArr.remove(hash);
            }
        }
        if (listenerArr.size() == 0) {
            listenerArr = null;
            if (!KStringUtilities.isToggleFullScreen(event)) {
                mWebView.removeEventListener(event);
            }
        }
    }

    public void asyncEvaluate(String expression, String expressionID, EvaluateListener evaluateListener) {
        if (mPlayerEvaluatedHash == null) {
            mPlayerEvaluatedHash = new HashMap<String, EvaluateListener>();
        }
        mPlayerEvaluatedHash.put(expressionID, evaluateListener);
        mWebView.evaluate(expression, expressionID);
    }

    public void sendNotification(String notificationName, String params) {
        if (notificationName == null) {
            notificationName = "null";
        }
        mWebView.sendNotification(notificationName, params);
    }

    public void setKDPAttribute(String pluginName, String propertyName, String value) {
        mWebView.setKDPAttribute(pluginName, propertyName, value);
    }

    public void triggerEvent(String event, String value) {
        mWebView.triggerEvent(event, value);
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
                    if (eventListeners != null) {
                        for (KPEventListener listener: eventListeners) {
                            listener.onKPlayerStateChanged(this, KPlayerState.SEEKING);
                        }
                    }
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
                case goLive:
                    ((LiveStreamInterface)playerController.getPlayer()).switchToLive();
                    break;
            }
        }
    }

    
    private void switchFlavor(String index) {
        int flavorIndex = -1;
        try {
            flavorIndex = Integer.parseInt(index);
        } catch (NumberFormatException e) {
            Log.e(TAG, "switchFlavor failed parsing index, ignoring request" + index);
            return;
        }

        KPlayerController.KPlayer player = playerController.getPlayer();
        if (player instanceof QualityTracksInterface) {
            QualityTracksInterface adaptivePlayer = (QualityTracksInterface) player;
            adaptivePlayer.switchQualityTrack(flavorIndex);
        }
    }
    
    private void notifyJsReady() {
        mIsJsCallReadyRegistration = true;
        if (mCallBackReadyRegistrations != null) {
            ArrayList<ReadyEventListener> temp = new ArrayList<ReadyEventListener>(mCallBackReadyRegistrations);
            for (ReadyEventListener listener: temp) {
                listener.handler();
                mCallBackReadyRegistrations.remove(listener);
            }
            temp = null;
            mCallBackReadyRegistrations = null;
        }
    }

    private void notifyKPlayerEvent(String args) {
        String[] arguments = KStringUtilities.fetchArgs(args);
        if (arguments.length == 2) {
            String eventName = arguments[0];
            String params = arguments[1];
            ArrayList<HashMap<String, EventListener>> listenerArr = mPlayerEventsHash.get(eventName);
            if (listenerArr != null) {
                for (HashMap<String, EventListener> hash: listenerArr) {
                    ((EventListener)hash.values().toArray()[hash.values().size() - 1]).handler(eventName, params);
                }
            }
        }
    }

    private void notifyKPlayerEvaluated(String args) {
        String[] arguments = KStringUtilities.fetchArgs(args);
        if (arguments.length == 2) {
            EvaluateListener listener = mPlayerEvaluatedHash.get(arguments[0]);
            if (listener != null) {
                listener.handler(arguments[1]);
            }
        } else {
            Log.d("AsyncEvaluate Error", "Missing evaluate params");
        }
    }

    private void notifyLayoutReady() {
        setChromecastVisiblity();
    }

    private void toggleFullscreen() {
        isFullScreen = !isFullScreen;
        if (eventListeners != null) {
            for (KPEventListener listener : eventListeners) {
                listener.onKPlayerFullScreenToggeled(this, isFullScreen);
            }
        }
    }

    private void showChromecastDeviceList() {
        if(!mActivity.isFinishing() && mRouterManager.getAppListener() != null) {
            mRouterManager.getAppListener().castButtonClicked();
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




    // Native actions
    private void share(JSONObject shareParams) {
//        if(mShareListener != null){
//            try {
//                String videoUrl = (String)shareParams.get("sharedLink");
//                String videoName = (String)shareParams.get("videoName");
//                ShareManager.SharingType type = ShareStrategyFactory.getType(shareParams);
//                if (!mShareListener.onShare(videoUrl, type, videoName)){
//                    ShareManager.share(shareParams, mActivity);
//                }
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        }else {
//            ShareManager.share(shareParams, mActivity);
//        }
    }
}
