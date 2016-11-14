package com.kaltura.playersdk;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Looper;
import android.util.Base64;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kaltura.playersdk.helpers.CacheManager;
import com.kaltura.playersdk.helpers.KStringUtilities;
import com.kaltura.playersdk.types.KPError;
import com.kaltura.playersdk.utils.LogUtils;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;
import static com.kaltura.playersdk.utils.LogUtils.LOGW;

/**
 * Created by nissopa on 6/7/15.
 */
public class KControlsView extends WebView implements View.OnTouchListener {

    private static String TAG = KControlsView.class.getSimpleName();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return false;
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
    private CacheManager mCacheManager;

    private static String AddJSListener = "addJsListener";
    private static String RemoveJSListener = "removeJsListener";

    @SuppressLint("SetJavaScriptEnabled")
    public KControlsView(Context context) {
        super(context);
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
        getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        getSettings().setAppCacheEnabled(false);
        this.addJavascriptInterface(this, "android");
        this.setWebViewClient(new CustomWebViewClient());
        //this.setWebChromeClient(new WebChromeClient());
        this.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                String message = cm.message();
                String sourceId = cm.sourceId();
                int lineNumber = cm.lineNumber();
                ConsoleMessage.MessageLevel messageLevel = cm.messageLevel();

                if (LogUtils.isWebViewDebugModeOn()) {
                    LOGD(TAG, "WebView console " + messageLevel.name() + ": " + message + " at " + sourceId + " : " + lineNumber);
                }
                
                // Special case: clear cache if this error is given.
                if (messageLevel == ConsoleMessage.MessageLevel.ERROR) {
                    if (message.contains("Uncaught SyntaxError")) { // for cases like "Uncaught SyntaxError: Unexpected end of input" or "Uncaught SyntaxError: Invalid or unexpected token"
                        LOGW(TAG, "Removing faulty cached resource: " + sourceId);
                        mCacheManager.removeCachedResponse(Uri.parse(sourceId));
                    }
                }
                
                return true;
            }
        });
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
        LOGD(TAG, "JavaSCRIPT " + KStringUtilities.sendNotification(notification, params));

        this.loadUrl(KStringUtilities.sendNotification(notification, params));
    }


    public void setKDPAttribute(String pluginName, String propertyName, String value) {
        this.loadUrl(KStringUtilities.setKDPAttribute(pluginName, propertyName, value));
    }

    public void setStringKDPAttribute(String pluginName, String propertyName, String value) {
        this.loadUrl(KStringUtilities.setStringKDPAttribute(pluginName, propertyName, value));
    }

    public void setKDPAttribute(String pluginName, String propertyName, JSONObject value) {
        this.loadUrl(KStringUtilities.setKDPAttribute(pluginName, propertyName, value));
    }

    public void triggerEvent(final String event, final String value) {
        try {
            loadUrl(KStringUtilities.triggerEvent(event, value));
        }
        catch(NullPointerException e) { //for old android there is bug in WebView internal that they through NPE
            LOGE(TAG, "WebView NullPointerException caught: " + e.getMessage());
        }
    }

    public void triggerEventWithJSON(String event, String jsonString) {
        this.loadUrl(KStringUtilities.triggerEventWithJSON(event, jsonString));
    }
    
    private WebResourceResponse getWhiteFaviconResponse() {
        // 16x16 white favicon
        byte[] data = Base64.decode("AAABAAEAEBAQAAEABAAoAQAAFgAAACgAAAAQAAAAIAAAAAEABAAAAAAAgAAAAAAAAAAAAAAAEAAAAAAAAAD///8AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 0);
        return new WebResourceResponse("image/x-icon", null, new ByteArrayInputStream(data));
    }

    private WebResourceResponse getResponse(Uri requestUrl, Map<String, String> headers, String method) {
        // Only handle http(s)
        if (!requestUrl.getScheme().startsWith("http")) {
            LOGD(TAG, "Will not handle " + requestUrl);
            return null;
        }
        WebResourceResponse response = null;
        if (mCacheManager != null) {
            try {
                LOGD(TAG, "getResponse: CacheManager - requestUrl=" + requestUrl);
                response = mCacheManager.getResponse(requestUrl, headers, method);
                
            } catch (IOException e) {
                if (requestUrl.getPath().endsWith("favicon.ico")) {
                    response = getWhiteFaviconResponse();
                } else {
                    LOGE(TAG, "getResponse From CacheManager error:: " + e.getMessage(), e);
                }
            }
        }
        return response;
    }

    @JavascriptInterface
    public void onData(String value) {
        if (this.fetcher != null && value != null) {
            int height = Integer.parseInt(value) + 5;
            this.fetcher.fetchHeight(height);
            this.fetcher = null;
        }
    }

    @Override
    public void destroy() {
        LOGD(TAG, "destroy()");
        super.destroy();
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            LOGD(TAG, "onPageStarted:" + url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            LOGD(TAG, "onPageFinished:" + url);
            super.onPageFinished(view, url);
        }

        @SuppressWarnings("deprecation")    // deprecated on lollipop, required on earlier versions.
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LOGD(TAG, "shouldOverrideUrlLoading: " + url);
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

        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError webResourceError) {
            String errMsg = "WebViewError:";


            if (webResourceError != null) {
                if (webResourceError.getErrorCode() == -2) {
                    //view.loadData("<div></div>", "text/html", "UTF-8");
                }
                errMsg += webResourceError.getErrorCode() + "-" ;
                errMsg += webResourceError.getDescription() + "-";
                if (request != null && request.getUrl() != null) {
                    errMsg += request.getUrl().toString();
                }

            }
            if (errMsg.contains("favicon.ico")) {
                return;
            }
            controlsViewClient.handleKControlsError(new KPError(errMsg));
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (errorCode == -2)    {
                //view.loadData("<div></div>", "text/html", "UTF-8");
            }

            String errMsg = "WebViewError:";
            errMsg += errorCode + "-" ;

            if (description != null) {
                errMsg += description + "-";
            }

            if (failingUrl != null) {
                errMsg += failingUrl;
            }

            if (errMsg.contains("favicon.ico")) {
                return;
            }

            controlsViewClient.handleKControlsError(new KPError(errMsg));
        }


        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse webResourceResponse) {
            String errMsg = "WebViewError:";
            errMsg += webResourceResponse.getStatusCode() + "-" ;
            if (request != null && request.getUrl() != null) {
                errMsg += request.getUrl().toString() + "-";
            }
            if (webResourceResponse != null) {
                errMsg += webResourceResponse.getReasonPhrase();
            }

            if (errMsg.contains("favicon.ico")) {
                return;
            }
           controlsViewClient.handleKControlsError(new KPError(errMsg));
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
            Map<String, String> headers = request.getRequestHeaders();
            String method = request.getMethod();
            return handleWebRequest(view, request.getUrl().toString(), headers, method);
        }
    }
}
