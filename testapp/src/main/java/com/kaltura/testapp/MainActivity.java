package com.kaltura.testapp;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

public class MainActivity extends AppCompatActivity implements PlayerFragment.OnFragmentInteractionListener, View.OnClickListener {
    private PlayerFragment mPlayerFragment;
    private PlayerViewController mPlayer;
    private Button mPreloadButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(this);
        button = (Button)findViewById(R.id.button2);
        button.setOnClickListener(this);
        mPreloadButton = (Button)findViewById(R.id.button3);
        mPreloadButton.setOnClickListener(this);
    }

    private void updateButtons(int visibility) {
        findViewById(R.id.button2).setVisibility(visibility);
        findViewById(R.id.button).setVisibility(visibility);
        mPreloadButton.setVisibility(visibility);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
    protected void onPause() {
        super.onPause();
        if (mPlayerFragment != null && !isFinishing()) {
            mPlayerFragment.killPlayer();
            mPlayerFragment = null;
            findViewById(R.id.button).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d("URI", uri.toString());
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            mPlayerFragment.pausePlayer();
            getFragmentManager().popBackStack();
            findViewById(R.id.button).setVisibility(View.VISIBLE);
        } else if (mPlayer != null) {
            mPlayer.setVisibility(View.INVISIBLE);
            mPlayer.sendNotification("doPause", null);
            updateButtons(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            v.setVisibility(View.INVISIBLE);
            findViewById(R.id.button2).setVisibility(View.INVISIBLE);
            boolean isPlayer = false;
            if (mPlayerFragment == null) {
                mPlayerFragment = new PlayerFragment();
                isPlayer = true;
            }
            getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.animator.slide_up, R.animator.slide_down)
                    .add(R.id.fragment_container, mPlayerFragment)
                    .addToBackStack(mPlayerFragment.getClass().getName())
                    .commit();
            if (!isPlayer) {
                mPlayerFragment.resumePlayer();
            }
            // if Preload clicked
        } else if (v.getId() == R.id.button3) {
            // when the vidoe is ready to play show the player and start playing
            if (!mPreloadButton.getText().equals("Preload Player")) {
//                updateButtons(View.INVISIBLE);
                mPlayer.setVisibility(View.VISIBLE);
                mPlayer.sendNotification("doPlay", null);
                // start loading the player while is hidden
            } else if (mPreloadButton.getText().equals("Preload Player")) {

                mPreloadButton.setText("Loading Player..");
                if (mPlayer == null) {
                    mPlayer = (PlayerViewController) findViewById(R.id.player);
                    final KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.44.rc5/mwEmbedFrame.php", "32855491", "1424501");
                    config.setEntryId("1_32865911");
                    mPlayer.loadPlayerIntoActivity(this);
                    mPlayer.initWithConfiguration(config);
                    mPlayer.addEventListener(new KPEventListener() {
                        @Override
                        public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
                            if (state == KPlayerState.READY) {
                                mPreloadButton.setText("Ready To Play");
                            }
                        }

                        @Override
                        public void onKPlayerError(PlayerViewController playerViewController, KPError error) {

                        }

                        @Override
                        public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {

                        }

                        @Override
                        public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

                        }
                    });
                }
            }
        } else {
            KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.40.rc8", "26698911", "1831271");
            config.setEntryId("1_1fncksnw");
            if (mPlayer != null) {
                mPlayer.changeMedia("0_vs3e2h32");
//            Intent intent = new Intent(this, OfflineActivity.class);
//            startActivity(intent);
            }
        }
    }
}
