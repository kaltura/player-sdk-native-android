package com.kaltura.playersdk;

import android.app.ActionBar;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class BrowserActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        Bundle extras = getIntent().getExtras();
        String shareLink = extras.getString("ShareLink");
        if (shareLink != null) {
            WebView webView = (WebView)findViewById(R.id.browserWebView);
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl(shareLink);
        }
        ActionBar actionBar = getActionBar();
        if(actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(extras.getString("VideoName"));
            String barColor = extras.getString("BarColor");
            if(barColor != null && !barColor.isEmpty()){
                actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(barColor)));
            }
        }

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_browser, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        overridePendingTransition(R.animator.trans_right_in, R.animator.trans_right_out);
        return super.onOptionsItemSelected(item);
    }


}
