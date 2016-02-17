package com.kaltura.playersdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.text.TextUtils;
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
import com.google.android.gms.common.api.GoogleApiClient;
import com.kaltura.playersdk.cast.KRouterManager;
import com.kaltura.playersdk.casting.KCastRouterManager;
import com.kaltura.playersdk.casting.KRouterInfo;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.helpers.CacheManager;
import com.kaltura.playersdk.helpers.KStringUtilities;
import com.kaltura.playersdk.players.KPlayer;
import com.kaltura.playersdk.players.KPlayerController;
import com.kaltura.playersdk.players.KPlayerListener;
import com.kaltura.playersdk.players.MediaFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by michalradwantzor on 9/24/13.
 */
public class PlayerViewController extends RelativeLayout implements KControlsView.KControlsViewClient, KRouterManager.KRouterManagerListener, KPlayerListener {
    public static String TAG = "PlayerViewController";



    private KPlayerController playerController;
    public KControlsView mWebView = null;
    private double mCurSec;
    private Activity mActivity;
//    private OnShareListener mShareListener;
    private JSONObject nativeActionParams;
    private KPPlayerConfig mConfig;


    private String mIframeUrl = null;

    private boolean mWvMinimized = false;

    private int newWidth, newHeight;

    private boolean mIsJsCallReadyRegistration = false;
    private Set<ReadyEventListener> mCallBackReadyRegistrations;
    private HashMap<String, ArrayList<HashMap<String, EventListener>>> mPlayerEventsHash;
    private HashMap<String, EvaluateListener> mPlayerEvaluatedHash;
    private Set<KPEventListener> eventListeners;
    private SourceURLProvider mCustomSourceURLProvider;
    private boolean isFullScreen = false;

    private KRouterManager routerManager;



    /// KCastKalturaChannel Listener
    @Override
    public void onDeviceSelected(CastDevice castDeviceSelected) {
        if (castDeviceSelected == null) {
            mWebView.triggerEvent("chromecastDeviceDisConnected", null);
            playerController.removeCastPlayer();
        } else {
//            playerController.startCasting(mActivity);
            mWebView.triggerEvent("onNativeRequestSessionSuccess", null);
        }

    }

    @Override
    public void onRouteAdded(boolean isAdded, KRouterInfo route) {
        if (getRouterManager().getAppListener() != null) {
            if (isAdded) {
                getRouterManager().getAppListener().addedCastDevice(route);
            } else {
                getRouterManager().getAppListener().removedCastDevice(route);
            }
        }
    }

    @Override
    public void onFoundDevices(final boolean didFound) {
        if (getRouterManager().shouldEnableKalturaCastButton()) {
            registerReadyEvent(new ReadyEventListener() {
                @Override
                public void handler() {
                    mWebView.setKDPAttribute("chromecast", "visible", didFound ? "true" : "false");
                }
            });
        }
        if (getRouterManager().getAppListener() != null) {
            getRouterManager().getAppListener().didDetectCastDevices(didFound);
        }
    }

    @Override
    public void onShouldDisconnectCastDevice() {
        playerController.stopCasting();
        mWebView.triggerEvent("chromecastDeviceDisConnected", null);
        if (getRouterManager().getAppListener() != null) {
            getRouterManager().getAppListener().castDeviceConnectionState(false);
        }
    }

    @Override
    public void onConnecting() {
        mWebView.triggerEvent("showConnectingMessage", null);
    }
    
    @Override
    public void onStartCasting(GoogleApiClient apiClient, CastDevice selectedDevice) {
        if (getRouterManager().getAppListener() != null) {
            getRouterManager().getAppListener().castDeviceConnectionState(true);
        }
        playerController.startCasting(apiClient);
    }

    @Override
    public void setOnTouchListener(OnTouchListener l) {
        mWebView.setOnTouchListener(l);
    }

    // trigger timeupdate events

    public interface EventListener {
        void handler(String eventName, String params);
    }

    public interface ReadyEventListener {
        void handler();
    }

    public interface EvaluateListener {
        void handler(String evaluateResponse);
    }

    public interface SourceURLProvider {
        String getURL(String entryId, String currentURL);
    }

    public PlayerViewController(Context context) {
        super(context);
    }

    public KCastRouterManager getKCastRouterManager() {
        if (routerManager == null) {
            routerManager = new KRouterManager(mActivity, this);
        }
        return routerManager;
    }

    private KRouterManager getRouterManager() {
        return (KRouterManager)getKCastRouterManager();
    }

    public PlayerViewController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerViewController(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
    }

    public void initWithConfiguration(KPPlayerConfig configuration) {
        mConfig = configuration;

        setComponents(mConfig.getVideoURL());
        
//        setComponents("http://player-stg-eu.ott.kaltura.com/viacomIN/v2.37.2/mwEmbed/mwEmbedFrame.php/uiconf_id/8413353?wid=_&entry_id=295868&flashvars[proxyData]={%22initObj%22:{%22Locale%22:{%22LocaleLanguage%22:%22%22,%22LocaleCountry%22:%22%22,%22LocaleDevice%22:%22%22,%22LocaleUserState%22:%22Unknown%22},%22Platform%22:%22Cellular%22,%22SiteGuid%22:%22613999%22,%22DomainID%22:%22282563%22,%22UDID%22:%22123456%22,%22ApiUser%22:%22tvpapi_225%22,%22ApiPass%22:%2211111%22},%22MediaID%22:%22295868%22,%22iMediaID%22:%22295868%22,%22picSize%22:%22640x360%22,%22mediaType%22:%220%22,%22withDynamic%22:%22false%22}&flashvars[tvpapiGetLicensedLinks.plugin]=true&flashvars[TVPAPIBaseUrl]=http://stg.eu.tvinci.com/tvpapi_v3_3/gateways/jsonpostgw.aspx?m=&flashvars[liveCore.disableLiveCheck]=true&iframeembed=true&flashvars[chromecast.plugin]=true");
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

    public KPPlayerConfig getConfig() {
        return mConfig;
    }

    public void changeMedia(String entryId) {
        if (entryId != null && entryId.length() > 0) {
            JSONObject entryJson = new JSONObject();
            try {
                entryJson.put("entryId", entryId);
                sendNotification("changeMedia", entryJson.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void changeConfiguration(KPPlayerConfig config) {
        if (config != null) {
            mWebView.loadUrl(config.getVideoURL() + "#" + buildSupportedMediaFormats());
        }
    }

    public void removeEventListener(KPEventListener listener) {
        if (listener != null && eventListeners != null && eventListeners.contains(listener)) {
            eventListeners.remove(listener);
        }
    }

    public void setCustomSourceURLProvider(SourceURLProvider provider) {
        mCustomSourceURLProvider = provider;
    }
    
    private String getOverrideURL(String entryId, String currentURL) {
        if (mCustomSourceURLProvider != null) {
            String overrideURL = mCustomSourceURLProvider.getURL(entryId, currentURL);
            if (overrideURL != null) {
                return overrideURL;
            }
        }
        return currentURL;
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
        if (mWebView != null) {
            mWebView.loadUrl("about:blank");
            removeView(mWebView);
            mWebView.destroy();
        }
        if (routerManager != null) {
            routerManager.release();
        }
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

            this.playerController = new KPlayerController(this);
            this.addView(mWebView, wvLp);
            
        }

        iframeUrl += "#" + buildSupportedMediaFormats();

        if( mIframeUrl == null || !mIframeUrl.equals(iframeUrl) ) {
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

//    public void sendNotification(String noteName, JSONObject noteBody) {
//        notifyKPlayer("sendNotification", new String[]{noteName, noteBody.toString()});
//    }



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
            KPlayer player = this.playerController.getPlayer();
            bridgeMethod = KStringUtilities.isMethodImplemented(player, functionName);
            object = player;
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
    public void eventWithValue(KPlayer player, String eventName, String eventValue) {
        Log.d("EventWithValue", "Name: " + eventName + " Value: " + eventValue);
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
    public void eventWithJSON(KPlayer player, String eventName, String jsonValue) {
        Log.d("EventWithJSON", "Name: " + eventName + " Value: " + jsonValue);
        this.mWebView.triggerEventWithJSON(eventName, jsonValue);
    }

    @Override
    public void contentCompleted(KPlayer currentPlayer) {
        if (eventListeners != null) {
            for (KPEventListener listener: eventListeners) {
                listener.onKPlayerStateChanged(this, KPlayerState.ENDED);
            }
        }
    }

    private void play() {
        playerController.play();
    }

    private void pause() {
        playerController.pause();
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
                    mPlayerEventsHash = new HashMap();
                }
                ArrayList<HashMap<String, EventListener>> listenerArr = (ArrayList)mPlayerEventsHash.get(event);
                if (listenerArr == null) {
                    listenerArr = new ArrayList();
                }
                HashMap<String, EventListener> addedEvent = new HashMap();
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

    public void sendNotification(String notificationName,@Nullable String params) {
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

            KStringUtilities.Attribute attribute = KStringUtilities.attributeEnumFromString(attributeName);
            if (attribute == null) {
                return;
            }
            switch (attribute) {
                case src:
                    // attributeValue is the selected source -- allow override.
                    attributeValue = getOverrideURL(mConfig.getEntryId(), attributeValue);
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
                case licenseUri:
                    this.playerController.setLicenseUri(attributeValue);
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
                case chromecastAppId:
//                    getRouterManager().initialize(attributeValue, mActivity);
                    getRouterManager().initialize(attributeValue);
                    Log.d("chromecast.initialize", attributeValue);
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

        KPlayer player = playerController.getPlayer();
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
        if(!mActivity.isFinishing() && getRouterManager().getAppListener() != null) {
            getRouterManager().getAppListener().castButtonClicked();
        }
    }

    private void sendCCRecieverMessage(String args) {
        String decodeArgs = null;
        JSONArray jsonArgs = null;
        try {
            decodeArgs = URLDecoder.decode(args, "UTF-8");
            Log.d(getClass().getSimpleName(), "sendCCRecieverMessage : " + decodeArgs);
            jsonArgs = new JSONArray(decodeArgs);
            getRouterManager().sendMessage(jsonArgs.getString(0), jsonArgs.getString(1));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCCMedia() {
//        getRouterManager().sendMessage(jsonArgs.getString(0), jsonArgs.getString(1));
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


    private String buildSupportedMediaFormats() {
        Set<MediaFormat> supportedFormats = KPlayerController.supportedFormats(getContext());

        Set<String> drmTypes = new HashSet<>();
        Set<String> allTypes = new HashSet<>();

        for (MediaFormat format : supportedFormats) {
            if (format.drm != null) {
                drmTypes.add(format.shortName);
            }
            allTypes.add(format.shortName);
        }
        
        return new Uri.Builder()
                .appendQueryParameter("nativeSdkDrmFormats", TextUtils.join(",", drmTypes))
                .appendQueryParameter("nativeSdkAllFormats", TextUtils.join(",", allTypes))
                .build().getQuery();
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
