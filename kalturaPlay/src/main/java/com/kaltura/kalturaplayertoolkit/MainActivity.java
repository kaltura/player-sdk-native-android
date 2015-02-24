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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import Fragments.FullscreenFragment;
import Fragments.LoginFragment;
import Fragments.PlayerFragment;
import Utilities.FragmentUtilities;


public class MainActivity extends Activity implements LoginFragment.OnFragmentInteractionListener, PlayerFragment.OnFragmentInteractionListener, FullscreenFragment.OnFragmentInteractionListener{
	public static String TAG = MainActivity.class.getSimpleName();

    @SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        setContentView(R.layout.activity_main);
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        Intent intent = getIntent();
        Fragment firstFragment = new LoginFragment();
        Bundle extras = intent.getExtras();

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        FragmentUtilities.loadFragment(false, firstFragment, extras, getFragmentManager());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_VIEW.equals( intent.getAction())) {
            Uri uri = intent.getData();
            String[] params = null;
            try {
                params = URLDecoder.decode(uri.toString(), "UTF-8").split(":=");
            } catch (UnsupportedEncodingException e) {
                Log.w(TAG, "couldn't decode/split intent url");
                e.printStackTrace();
            }
            if (params != null && params.length > 1) {
                String iframeUrl = params[1];
                Bundle extras = intent.getExtras();
                extras.putString(getString(R.string.prop_iframe_url), iframeUrl);
                Fragment fullscreenFragment = new FullscreenFragment();
                FragmentUtilities.loadFragment(false, fullscreenFragment, extras, getFragmentManager());
            } else {
                Log.w(TAG, "didn't load iframe, invalid iframeUrl parameter was passed");
            }

        }
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

    }
}
