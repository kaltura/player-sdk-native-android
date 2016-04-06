package com.kaltura.basicplaydemo;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {
    private static final String TAG = "BasicPlayerDemo";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;
    private boolean shouldResume = false;

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
        onCreate = true;
        getPlayer();
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);
            KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");
            config.addConfig("controlBarContainer.plugin", "false");
            config.addConfig("topBarContainer.plugin", "false");
            config.addConfig("largePlayBtn.plugin", "false");
            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(this);
        }
        return mPlayer;
    }

    private RelativeLayout getPlayerContainer() {
        return (RelativeLayout)findViewById(R.id.playerContainer);
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
            if (powerManager.isScreenOn()) {
                mPlayer.releaseAndSavePosition(false);
                shouldResume = true;
            }
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isScreenOn()) {
            mPlayer.getMediaControl().pause();
            shouldResume = false;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (onCreate) {
            onCreate = false;
        } else if (shouldResume) {
            mPlayer.resumePlayer();
            shouldResume = false;
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
    public void onClick(View v) {
        if (v.getId() != R.id.replay) {
            if (mPlayPauseButton.getText().equals("Play")) {
                mPlayPauseButton.setText("Pause");
                getPlayer().sendNotification("doPlay", null);
            } else {
                mPlayPauseButton.setText("Play");
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
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        if (state == KPlayerState.PAUSED && playerViewController.getCurrentPlaybackTime() > 0) {
//            findViewById(R.id.replay).setVisibility(View.VISIBLE);
        } else if (state == KPlayerState.PLAYING) {
//            findViewById(R.id.replay).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
        mSeekBar.setProgress((int) (currentTime / playerViewController.getDurationSec() * 100));
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        Log.e(TAG, "Error Received:" + error.getErrorMsg());
    }
}
