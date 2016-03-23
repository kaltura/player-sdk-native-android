package com.kaltura.playersdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.helpers.CacheManager;
import com.kaltura.playersdk.helpers.KStringUtilities;
import com.kaltura.playersdk.interfaces.KMediaControl;
import com.kaltura.playersdk.players.KPlayerListener;
import com.kaltura.playersdk.types.KPError;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;


/**
 * Created by nissopa on 6/7/15.
 */
public class KControlsView extends WebView implements View.OnTouchListener, KMediaControl {

    private static final String TAG = "KControlsView";
    private boolean mCanPause = false;
    private int mCurrentPosition = 0;
    private int mDuration = 0;
    private SeekCallback mSeekCallback;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    @Override
    public void start() {
        sendNotification("doPlay", null);
    }

    @Override
    public void pause() {
        sendNotification("doPause", null);
    }

    @Override
    public void seek(double seconds) {
        sendNotification("doSeek", Double.toString(seconds));
    }

    @Override
    public void replay() {
        sendNotification("doReplay", null);
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    @Override
    public int getDuration() {
        return mDuration;
    }

    @Override
    public boolean isPlaying() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCurrentPosition > 0;
    }

    @Override
    public boolean canSeekForward() {
        return mCurrentPosition < mDuration;
    }

    @Override
    public void seek(long seconds, SeekCallback callback) {
        mSeekCallback = callback;
        seek((double)seconds / 1000f);
    }

    public interface KControlsViewClient {
        void handleHtml5LibCall(String functionName, int callbackId, String args);
        void openURL(String url);
        void handleKControlsError(KPError error);
    }

    public interface ControlsBarHeightFetcher {
        void fetchHeight(int height);
    }

    private KControlsViewClient controlsViewClient;
    private String entryId;
    private ControlsBarHeightFetcher fetcher;
    private Context mContext;
    private CacheManager mCacheManager;

    private static String AddJSListener = "addJsListener";
    private static String RemoveJSListener = "removeJsListener";

    @SuppressLint("SetJavaScriptEnabled")
    public KControlsView(Context context) {
        super(context);
        mContext = context;
        getSettings().setJavaScriptEnabled(true);
        init();
    }

    public void setCacheManager(CacheManager cacheManager) {
        mCacheManager = cacheManager;
    }

    private void init() {
        getSettings().setAllowFileAccessFromFileURLs(true);
        getSettings().setAllowUniversalAccessFromFileURLs(true);
        getSettings().setAllowFileAccess(true);
        getSettings().setDomStorageEnabled(true);
        this.addJavascriptInterface(this, "android");
        this.setWebViewClient(new CustomWebViewClient());
        this.setWebChromeClient(new WebChromeClient());
        this.getSettings().setUserAgentString(this.getSettings().getUserAgentString() + " kalturaNativeCordovaPlayer");
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        this.setBackgroundColor(0);
    }
    

    public void setKControlsViewClient(KControlsViewClient client) {
        this.controlsViewClient = client;
    }

    public void setEntryId(String entryId) {
        if (!this.entryId.equals(entryId)) {
            this.entryId = entryId;
            String entry = "'{\"entryId\":\"" + entryId + "}'";
            sendNotification("changeMedia", entry);
        }
    }

    public void fetchControlsBarHeight(ControlsBarHeightFetcher fetcher) {
        this.fetcher = fetcher;
        this.loadUrl("javascript:android.onData(NativeBridge.videoPlayer.getControlBarHeight())");
    }

    public void addEventListener(String event) {
        this.loadUrl(KStringUtilities.addEventListener(event));
    }

    public void removeEventListener(String event) {
        this.loadUrl(KStringUtilities.removeEventListener(event));
    }

    public void evaluate(String expression, String evaluateID) {
        this.loadUrl(KStringUtilities.asyncEvaluate(expression, evaluateID));
    }

    public void sendNotification(String notification, String params) {
        Log.d("JavaSCRIPT", KStringUtilities.sendNotification(notification, params));

        this.loadUrl(KStringUtilities.sendNotification(notification, params));
    }


    public void setKDPAttribute(String pluginName, String propertyName, String value) {
        this.loadUrl(KStringUtilities.setKDPAttribute(pluginName, propertyName, value));
    }

    public void triggerEvent(final String event, final String value) {
        KPlayerState kState = KPlayerState.getStateForEventName(event);
        switch (kState) {
            case PLAYING:
                mCanPause = true;
                break;
            case PAUSED:
                mCanPause = false;
                break;
            case SEEKED:
                if (mSeekCallback != null) {
                    mSeekCallback.seeked(mCurrentPosition);
                    mSeekCallback = null;
                }
                break;
            case UNKNOWN:
                //Log.w("TAG", ", unsupported event name : " + event);
                break;
        }
        if (event.equals(KPlayerListener.TimeUpdateKey)) {
            mCurrentPosition = (int) (Double.parseDouble(value) * 1000);
        }
        if (event.equals(KPlayerListener.DurationChangedKey)) {
            mDuration = (int) (Double.parseDouble(value) * 1000);
        }

        loadUrl(KStringUtilities.triggerEvent(event, value));
    }

    public void triggerEventWithJSON(String event, String jsonString) {
        this.loadUrl(KStringUtilities.triggerEventWithJSON(event, jsonString));
    }

    private WebResourceResponse getResponse(Uri requestUrl, Map<String, String> headers, String method) {
        // Only handle http(s)
        if (!requestUrl.getScheme().startsWith("http")) {
            Log.d(TAG, "Will not handle " + requestUrl);
            return null;
        }
        mCacheManager.setContext(mContext);
        try {
            return mCacheManager.getResponse(requestUrl, headers, method);
        } catch (IOException e) {
            return null;
        }
    }

    @JavascriptInterface
    public void onData(String value) {
        if (this.fetcher != null && value != null) {
            int height = Integer.parseInt(value) + 5;
            this.fetcher.fetchHeight(height);
            this.fetcher = null;
        }
    }


    private class CustomWebViewClient extends WebViewClient {

        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "shouldOverrideUrlLoading: " + url);
            if (url == null) {
                return false;
            }
            final KStringUtilities urlUtil = new KStringUtilities(url);
            if (urlUtil.isJSFrame()) {
                final String action = urlUtil.getAction();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        KControlsView.this.controlsViewClient.handleHtml5LibCall(action, 1, urlUtil.getArgsString());
                    }
                };
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    runnable.run();
                } else {
                    post(runnable);
                }
                return true;
            }
            return false;
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            controlsViewClient.handleKControlsError(new KPError(error.toString()));

        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            controlsViewClient.handleKControlsError(new KPError(errorResponse.toString()));
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            controlsViewClient.handleKControlsError(new KPError(error.toString()));
        }

        private WebResourceResponse textResponse(String text) {
            return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(text.getBytes()));
        }
        
        private WebResourceResponse handleWebRequest(WebView view, String url, Map<String, String> headers, String method) {
            // On some devices, shouldOverrideUrlLoading() misses the js-frame call.
            if (shouldOverrideUrlLoading(view, url)) {
                return textResponse("JS-FRAME");
            }

            return getResponse(Uri.parse(url), headers, method);
        }

        @SuppressWarnings("deprecation")
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            return handleWebRequest(view, url, Collections.<String, String>emptyMap(), "GET");
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            return handleWebRequest(view, request.getUrl().toString(), request.getRequestHeaders(), request.getMethod());
        }
    }
}
