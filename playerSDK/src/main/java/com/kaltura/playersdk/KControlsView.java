package com.kaltura.playersdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.kaltura.playersdk.Helpers.KStringUtilities;

import java.util.ArrayList;


/**
 * Created by nissopa on 6/7/15.
 */
public class KControlsView extends WebView {

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

    private static String AddJSListener = "addJsListener";
    private static String RemoveJSListener = "removeJsListener";

    @SuppressLint("JavascriptInterface")
    public KControlsView(Context context) {
        super(context);
        this.getSettings().setJavaScriptEnabled(true);
        this.addJavascriptInterface(this, "android");
        this.setWebViewClient(new CustomWebViewClient());
        this.setWebChromeClient(new WebChromeClient());
        this.getSettings().setUserAgentString( this.getSettings().getUserAgentString() + " kalturaNativeCordovaPlayer" );
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
    private void onData(String value) {
        if (this.fetcher != null) {
            int height = Integer.parseInt(value) + 5;
            this.fetcher.fetchHeight(height);
            this.fetcher = null;
        }
    }


    private class CustomWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url == null) {
                return false;
            }
            KStringUtilities urlUtil = new KStringUtilities(url);
            if (urlUtil.isJSFrame()) {
                KControlsView.this.controlsViewClient.handleHtml5LibCall(urlUtil.getAction(), 1, urlUtil.getArgsString());
                return false;
            } else if (!urlUtil.isEmbedFrame()) {
                KControlsView.this.controlsViewClient.openURL(url);
                return false;
            }
            return true;
        }


        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.clearCache(true);
        }
    }
}
