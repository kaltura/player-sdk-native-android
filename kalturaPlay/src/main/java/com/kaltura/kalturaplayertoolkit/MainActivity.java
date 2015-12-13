package com.kaltura.kalturaplayertoolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import com.kaltura.playersdk.KPPlayerConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class MainActivity extends FragmentActivity implements LoginFragment.OnFragmentInteractionListener, PlayerFragment.OnFragmentInteractionListener, FullscreenFragment.OnFragmentInteractionListener{
	public static String TAG = MainActivity.class.getSimpleName();

    @SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        loadFragment();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadFragment();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Intent intent = getIntent();
        Fragment fragment = new FullscreenFragment();
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = new Bundle();
        }
        //https://cdnapisec.kaltura.com
        //http://172.20.10.9/html5.kaltura/mwEmbed/mwEmbedFrame.php
        KPPlayerConfig config = new KPPlayerConfig("http://169.254.113.96/html5.kaltura/mwEmbed/mwEmbedFrame.php", "26698911", "1831271").setEntryId("1_1fncksnw");
        config.addConfig("chromecast.receiverLogo", "true");
        config.addConfig("chromecast.applicationID", "FFCC6D19");
        config.addConfig("chromecast.useKalturaPlayer", "true");

        config.setCacheSize(0.8f);

//        KPPlayerConfig config = new KPPlayerConfig("http://player-stg-eu.ott.kaltura.com/viacomIN/v2.37.2/mwEmbed/mwEmbedFrame.php", "8413353", "");
//        config.setEntryId("295868");
//        config.addConfig("liveCore.disableLiveCheck", "true");
//        config.addConfig("tvpapiGetLicensedLinks.plugin", "true");
//        config.addConfig("proxyData","{\"initObj\":{\"Locale\":{\"LocaleLanguage\":\"\",\"LocaleCountry\":\"\",\"LocaleDevice\":\"\",\"LocaleUserState\":\"Unknown\"},\"Platform\":\"Cellular\",\"SiteGuid\":\"613999\",\"DomainID\":\"282563\",\"UDID\":\"123456\",\"ApiUser\":\"tvpapi_225\",\"ApiPass\":\"11111\"},\"MediaID\":\"295868\",\"iMediaID\":\"295868\",\"picSize\":\"640x360\",\"mediaType\":\"0\",\"withDynamic\":\"false\"}");
//        config.addConfig("TVPAPIBaseUrl", "http://stg.eu.tvinci.com/tvpapi_v3_3/gateways/jsonpostgw.aspx?m=");
//        config.setCacheSize(0.8f);
        extras.putSerializable("config", config);

        FragmentUtilities.loadFragment(false, fragment, extras, getFragmentManager());
    }

    private void loadFragment(){
        Intent intent = getIntent();
        Fragment fragment = new LoginFragment();
        Bundle extras = intent.getExtras();

        if (Intent.ACTION_VIEW.equals( intent.getAction())) {
            Uri uri = intent.getData();
            String url = null;
            try {
                url = URLDecoder.decode(uri.toString(), "UTF-8").replace("https://kalturaplay.appspot.com/play?", "");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "couldn't decode/split intent url");
                e.printStackTrace();
            }
            if (url != null) {
                extras.putSerializable("config", KPPlayerConfig.valueOf(url));
                fragment = new FullscreenFragment();

            } else {
                Log.w(TAG, "didn't load iframe, invalid iframeUrl parameter was passed");
            }

        }

        FragmentUtilities.loadFragment(false, fragment, extras, getFragmentManager());
    }
}
