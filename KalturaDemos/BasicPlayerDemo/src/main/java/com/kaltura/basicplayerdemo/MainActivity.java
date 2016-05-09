package com.kaltura.basicplayerdemo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.casting.KCastRouterManagerListener;
import com.kaltura.playersdk.casting.KRouterInfo;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {
    private static final String TAG = "BasicPlayerDemo";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;
    private ArrayList<KRouterInfo> mRouterInfos = new ArrayList<>();
    private boolean isCCActive = false;
    private Button ccButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mPlayPauseButton = (Button)findViewById(R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);


        ccButton = (Button)findViewById(R.id.ccButto);
        ccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentCCDevices();
            }
        });
        onCreate = true;
        getPlayer();
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);

            KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.43.rc11/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
            config.addConfig("autoPlay", "true");
            config.addConfig("chromecast.plugin", "true");
            config.addConfig("chromecast.applicationID", "5247861F");
            config.addConfig("chromecast.useKalturaPlayer", "true");
            config.addConfig("chromecast.receiverLogo", "true");
            mPlayer.getKCastRouterManager().enableKalturaCastButton(false);
            mPlayer.addKPlayerEventListener("onEnableKeyboardBinding", "someId", new PlayerViewController.EventListener() {
                @Override
                public void handler(String eventName, String params) {
                    Log.d(TAG, eventName);
                }
            });
            mPlayer.getKCastRouterManager().setCastRouterManagerListener(new KCastRouterManagerListener() {
                @Override
                public void onCastButtonClicked() {
                    if (!isCCActive) {
                        presentCCDevices();
                    } else {
                        mPlayer.getKCastRouterManager().disconnect();
                    }
                }

                @Override
                public void onApplicationStatusChanged(boolean isConnected) {
                    isCCActive = isConnected;
                }

                @Override
                public void shouldPresentCastIcon(boolean didDetect) {
                    ccButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAddedCastDevice(KRouterInfo info) {
                    mRouterInfos.add(info);
                }

                @Override
                public void onRemovedCastDevice(KRouterInfo info) {
                    mRouterInfos.remove(info);
                }
            });
            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(this);
        }
        return mPlayer;
    }

    private void presentCCDevices() {
        final String[] items = new String[mRouterInfos.size()];
        for (int i = 0; i < items.length; i++   ) {
            items[i] = mRouterInfos.get(i).getRouterName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make your selection");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
//                mDoneButton.setText(items[item]);
                mPlayer.getKCastRouterManager().connectDevice(mRouterInfos.get(item).getRouterId());
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }


    private RelativeLayout getPlayerContainer() {
        return (RelativeLayout)findViewById(R.id.playerContainer);
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
            if (powerManager.isScreenOn()) {
                mPlayer.releaseAndSavePosition(true);
            }

        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isScreenOn()) {
            mPlayer.saveState();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (onCreate) {
            onCreate = false;
        }
        mPlayer.resumePlayer();
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
    public void onConfigurationChanged(final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        Timer swapTimer = new Timer();
        swapTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) getPlayerContainer().getLayoutParams();
                        lp.weight = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 8;
                        lp.height = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT ? 7 : 3;
                        getPlayerContainer().setLayoutParams(lp);
                    }
                });
            }
        }, 100);


    }

    @Override
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        if (state == KPlayerState.PAUSED && playerViewController.getCurrentPlaybackTime() > 0) {
            //findViewById(R.id.replay).setVisibility(View.VISIBLE);
        }
        if (state == KPlayerState.PAUSED) {
            Log.d(TAG, "Stream PAUSED");
            mPlayPauseButton.setText("Play");
        }
        if (state == KPlayerState.PLAYING) {
            Log.d(TAG, "Stream PAUSED");
            mPlayPauseButton.setText("Pause");
            //findViewById(R.id.replay).setVisibility(View.INVISIBLE);
        }
        if (state == KPlayerState.ENDED) {
            Log.d(TAG,"Stream ENDED");
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() != R.id.replay) {

            if (mPlayPauseButton.getText().equals("Play")) {
                getPlayer().sendNotification("doPlay", null);
            } else {
                getPlayer().sendNotification("doPause", null);
            }
        } else {
            mPlayer.sendNotification("doSeek", "0.1");
            mPlayer.sendNotification("doPlay", null);
            mPlayPauseButton.setText("Pause");
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            float progressInPercent = progress / 100f;
            float seekVal = (float) (progressInPercent * mPlayer.getDurationSec());
            getPlayer().sendNotification("doSeek", Float.toString(seekVal));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
        mSeekBar.setProgress((int) (currentTime / playerViewController.getDurationSec() * 100));
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
        Log.d(TAG, "KPlayer onKPlayerFullScreenToggeled " +  Boolean.toString(isFullscreen));
        Log.e(TAG, "GILAD onKPlayerFullScreenToggeled " + isFullscreen);
        toggleFullscreen(this, isFullscreen);
    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        Log.e(TAG, "Error Received:" + error.getErrorMsg());
    }

    private void toggleFullscreen(Activity activity, boolean fullscreen) {

        int uiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

        if (fullscreen) {
            Log.d(TAG,"Set to onOpenFullScreen");
            mPlayer.sendNotification("onOpenFullScreen", null);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

            }else{
                activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
            getSupportActionBar().hide();
        } else {
            Log.d(TAG,"Set to onCloseFullScreen");
            mPlayer.sendNotification("onCloseFullScreen", null);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }else{
                activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            }
            getSupportActionBar().show();
        }
        // set landscape
        // if(fullscreen)  activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        // else activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }
}
