package com.kaltura.kalturaplayertoolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;

import com.kaltura.playersdk.KPPlayerConfig;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;


public class MainActivity extends Activity implements LoginFragment.OnFragmentInteractionListener, PlayerFragment.OnFragmentInteractionListener, FullscreenFragment.OnFragmentInteractionListener{
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
        
        KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "29802711", "1831271").setEntryId("1_1fncksnw");


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
