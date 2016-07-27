package com.kaltura.basicplayertest;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.kaltura.playersdk.utils.LogUtils.LOGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {
    private static final String TAG = "BasicPlayerTest";
    private Button mPlayPauseButton;
    //private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;
    private boolean shouldResume = false;

    Map <String,String> paramsMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);
        TextView durationTV = (TextView) findViewById(R.id.durationText);
        durationTV.setVisibility(View.INVISIBLE);

        TextView durationTV1 = (TextView) findViewById(R.id.durationText1);
        durationTV1.setVisibility(View.INVISIBLE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }


        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (Intent.ACTION_VIEW.equals( intent.getAction())) {
            paramsMap = new HashMap<>();
            Uri uri = intent.getData();
            LOGE(TAG,uri.toString());
            String [] input = (uri.toString()).replace("view://", "").split("/"); //view://testId=1/entryId=1_gpzg0ebw/partnerId=1788671/uiConfId=33291342/mwEmbed=v2.41.rc9
            for(String param : input){
                String [] paramKeyValue = param.split("=");
                paramsMap.put(paramKeyValue[0],paramKeyValue[1]);
                LOGE(TAG,param);
            }
        }

        mPlayPauseButton = (Button)findViewById(R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        //mSeekBar = (SeekBar)findViewById(R.id.seekBar);
       // mSeekBar.setOnSeekBarChangeListener(this);
        onCreate = true;
        if (paramsMap != null) {
            getPlayer();
        }else{
            Toast.makeText(this, "Error, intent input params are missing", Toast.LENGTH_LONG).show();
            LOGE(TAG,"Error, intent input params are missing");
            mPlayPauseButton.setClickable(false);
        }
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);
            //KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.41.rc9/mwEmbedFrame.php", "26698911", "1831271").setEntryId("1_o426d3i4");
            KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/" + paramsMap.get("mwEmbed") + "/mwEmbedFrame.php", paramsMap.get("uiConfId"), paramsMap.get("partnerId")).setEntryId(paramsMap.get("entryId"));

            config.addConfig("controlBarContainer.plugin", "true");
            config.addConfig("topBarContainer.plugin", "true");
            config.addConfig("largePlayBtn.plugin", "true");
            config.addConfig("autoPlay", "true");
            config.addConfig("chromecast.plugin", Boolean.TRUE.toString());
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
        LOGE(TAG, "state " + state.name());
        if (state == KPlayerState.PLAYING) {
            TextView durationTV = (TextView) findViewById(R.id.durationText);

            mPlayer.sendNotification("doSeek", String.valueOf(Math.round(getPlayer().getDurationSec()) - 20));
            mPlayer.sendNotification("doPlay", null);


            durationTV.setText("STARTED");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            durationTV.setVisibility(View.VISIBLE);
        }
        if (state == KPlayerState.ENDED) {
                TextView durationTV1 = (TextView) findViewById(R.id.durationText1);
                durationTV1.setText("ENDED");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                durationTV1.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
       // mSeekBar.setProgress((int) (currentTime / playerViewController.getDurationSec() * 100));
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        LOGE(TAG, "Error Received:" + error.getErrorMsg());
    }
}
