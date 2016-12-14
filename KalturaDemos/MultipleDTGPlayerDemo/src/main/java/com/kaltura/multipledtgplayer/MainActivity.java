package com.kaltura.multipledtgplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPErrorEventListener;
import com.kaltura.playersdk.events.KPStateChangedEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;

public class MainActivity extends AppCompatActivity implements KPErrorEventListener, KPStateChangedEventListener {

    private static final String TAG = "MainActivity";

    private PlayerViewController mPlayer;
    private ViewGroup mPlayerContainer;
    private VideoItemsLoader mVideoItemsLoader;

    private PlayerViewController.SourceURLProvider mSourceURLProvider = new PlayerViewController.SourceURLProvider() {
        @Override
        public String getURL(String entryId, String currentURL) {

            String playbackURL = mVideoItemsLoader.getUrl(entryId);

            return playbackURL;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mPlayerContainer = (ViewGroup) findViewById(R.id.layout_player_container);
        LinearLayout mContainer = (LinearLayout) findViewById(R.id.items);

        mVideoItemsLoader = new VideoItemsLoader(this);
        mVideoItemsLoader.loadItems("content.json");
        mVideoItemsLoader.attachToParent(mContainer);

        findViewById(R.id.btn_play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VideoItem item = mVideoItemsLoader.getSelectedItem();
                if (item != null) {
                    getPlayer(item.config);
                }
            }
        });
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
    }


    private PlayerViewController getPlayer(KPPlayerConfig config) {
        config.addConfig("EmbedPlayer.PreloadNativeComponent", "true");
        if (mPlayer == null) {
            mPlayer = new PlayerViewController(this);
            mPlayerContainer.addView(mPlayer, new ViewGroup.LayoutParams(mPlayerContainer.getLayoutParams()));

            mPlayer.loadPlayerIntoActivity(this);

            mPlayer.initWithConfiguration(config);

            mPlayer.setCustomSourceURLProvider(mSourceURLProvider);

            mPlayer.setOnKPErrorEventListener(this);
            mPlayer.setOnKPStateChangedEventListener(this);
            mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
                @Override
                public void handler() {
                    LOGD(TAG, "Player ready after prefetch - will now destroy player");
                    //mPlayer.removePlayer();
                }
            });

        } else {
            mPlayer.changeConfiguration(config);
        }


        return mPlayer;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        Log.d("onKPlayerStateChanged:", " " + state);
    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        Log.d("onKPlayerError", error.getErrorMsg());
    }

}
