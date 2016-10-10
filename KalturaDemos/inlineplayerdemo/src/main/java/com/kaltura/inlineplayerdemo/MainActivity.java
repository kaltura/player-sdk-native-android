package com.kaltura.inlineplayerdemo;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPFullScreenToggledEventListener;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PlayerFragment.OnFragmentInteractionListener, KPFullScreenToggledEventListener {
    private PlayerViewController mPlayer;
    private static final String TAG = "MainActivity";
    private PlayerFragment mPlayerFragment;
    private Button inlineViewButton;
    private boolean onCreate = false;
    private Button inlineFragmentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        inlineViewButton = (Button) findViewById(R.id.inlineView);
        if (inlineViewButton != null) {
            inlineViewButton.setOnClickListener(this);
        }
        inlineFragmentButton = (Button) findViewById(R.id.inlineFragment);
        if (inlineFragmentButton != null) {
            inlineFragmentButton.setOnClickListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.videoContainer);

            if (mPlayer != null) {
                mPlayer.loadPlayerIntoActivity(this);
                KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.43.rc11/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
                config.setAutoPlay(true);
                mPlayer.setOnKPFullScreenToggledEventListener(this);
                mPlayer.initWithConfiguration(config);
            }
        }
        return mPlayer;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        onCreate = true;
        switch (v.getId()) {
            case R.id.inlineView:
                getPlayer().setVisibility(View.VISIBLE);
                break;
            case R.id.inlineFragment:
                findViewById(R.id.videoContainer).setVisibility(View.GONE);
                mPlayerFragment = PlayerFragment.newInstance(null, null);
                android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.fragmentContainer, mPlayerFragment);
                transaction.commit();
                break;
        }
        inlineViewButton.setVisibility(View.GONE);
        inlineFragmentButton.setVisibility(View.GONE);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.releaseAndSavePosition(true);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (onCreate) {
            onCreate = false;
        }
        if (mPlayer != null) {
            mPlayer.resumePlayer();
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.removePlayer();
        }
        super.onDestroy();
    }

    @Override
    public void onKPlayerFullScreenToggled(PlayerViewController playerViewController, boolean isFullscreen) {
        fullscreenToggle(isFullscreen);
    }

    private void fullscreenToggle(boolean isFullScreen) {

        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (isFullScreen) {
            LOGD(TAG,"Set to onOpenFullScreen");
            mPlayer.sendNotification("onOpenFullScreen", null);
            if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                ViewGroup.LayoutParams params = mPlayer.getLayoutParams();
                params.width = metrics.widthPixels;
                params.height = metrics.heightPixels;
                mPlayer.setLayoutParams(params);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            }else{
                getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
            getSupportActionBar().hide();
        } else {
            LOGD(TAG,"Set to onCloseFullScreen");
            mPlayer.sendNotification("onCloseFullScreen", null);
            if(getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                ViewGroup.LayoutParams params = mPlayer.getLayoutParams();
                params.width = (int) (400 * metrics.density);
                params.height = (int) (300 * metrics.density);
                mPlayer.setLayoutParams(params);
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }else{
                getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
            getSupportActionBar().show();
        }
        // set landscape
        // if(fullscreen)  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        // else activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }
}
