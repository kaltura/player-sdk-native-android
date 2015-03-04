package com.kaltura.playersdk;

import android.app.ActionBar;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.example.kplayersdk.R;

public class BrowserActivity extends ActionBarActivity {

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
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(extras.getString("VideoName"));
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor(extras.getString("BarColor"))));

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
        overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
        return super.onOptionsItemSelected(item);
    }


}
