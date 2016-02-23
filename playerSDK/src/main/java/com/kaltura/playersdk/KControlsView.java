package com.kaltura.playersdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kaltura.playersdk.helpers.CacheManager;
import com.kaltura.playersdk.helpers.KStringUtilities;


/**
 * Created by nissopa on 6/7/15.
 */
public class KControlsView extends WebView implements View.OnTouchListener {

    private static final String TAG = "KControlsView";

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    public interface KControlsViewClient {
        public void handleHtml5LibCall(String functionName, int callbackId, String args);
        public void openURL(String url);
    }

    public interface ControlsBarHeightFetcher {
        public void fetchHeight(int height);
    }

    private KControlsViewClient controlsViewClient;
    private String entryId;
    private ControlsBarHeightFetcher fetcher;
    private Context mContext;
    private CacheManager mCacheManager;

    private static String AddJSListener = "addJsListener";
    private static String RemoveJSListener = "removeJsListener";

    @SuppressLint("JavascriptInterface")
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

    public void triggerEvent(String event, String value) {
        this.loadUrl(KStringUtilities.triggerEvent(event, value));
    }

    public void triggerEventWithJSON(String event, String jsonString) {
        this.loadUrl(KStringUtilities.triggerEventWithJSON(event, jsonString));
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
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Log.d(TAG, "shouldOverrideUrlLoading: " + url);
            if (url == null) {
                return false;
            }
            KStringUtilities urlUtil = new KStringUtilities(url);
            if (urlUtil.isJSFrame()) {
                KControlsView.this.controlsViewClient.handleHtml5LibCall(urlUtil.getAction(), 1, urlUtil.getArgsString());
                return true;
            } else if (!urlUtil.isEmbedFrame()) {
                KControlsView.this.controlsViewClient.openURL(url);
                return true;
            }
            return false;
        }


        @Override
        public void onPageFinished(WebView view, String url) {
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            Log.d(TAG, "Webview Error: " + description);
        }

        @Override
        public void onLoadResource(WebView view, String url) {
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
//            Log.d(TAG, "WebResponse: " + request.getUrl());
            if (mCacheManager != null) {
                WebResourceResponse response = null;
                try {
                    mCacheManager.setContext(mContext);
                    response = mCacheManager.getResponse(request);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return response;
            }
            return null;
        }
    }

}
