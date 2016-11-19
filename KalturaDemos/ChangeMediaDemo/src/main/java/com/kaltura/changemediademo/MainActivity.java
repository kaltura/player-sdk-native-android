package com.kaltura.changemediademo;


import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import java.util.Timer;
import java.util.TimerTask;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener, SelectMediaFragment.MediaIdPostman {
    public static final String MEDIA_ID_KEY = "MEDIA_ID";

    private static final String TAG = "ChangeMediaDemo";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;

    private Button chooseMediaButton;
    private String currentMediaId;

    private KPPlayerConfig config;
    //SelectMediaFragment selectMediaFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        chooseMediaButton = (Button)this.findViewById(R.id.open_select_media_fragment_button);
        chooseMediaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectMediaFragment fragment = new SelectMediaFragment();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction transaction = fm.beginTransaction();
                Bundle bundle = new Bundle();
                bundle.putString(MEDIA_ID_KEY, "Alo Kaltura!");

                fragment.setArguments(bundle);
                transaction.add(R.id.contentFragment, fragment);
                transaction.addToBackStack(null);
                transaction.commit();
                chooseMediaButton.setVisibility(View.INVISIBLE);
                mPlayPauseButton.setVisibility(View.INVISIBLE);
                mSeekBar.setVisibility(View.INVISIBLE);
                if (mPlayer != null)
                 mPlayer.setVisibility(View.INVISIBLE);
            }
        });

        mPlayPauseButton = (Button)findViewById(R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        mPlayPauseButton.setClickable(false);
        mSeekBar.setEnabled(false);
        onCreate = true;
    }

    @Override
    public void postMediaId(String mediaId) {
        getSupportFragmentManager().popBackStack();
        chooseMediaButton.setVisibility(View.INVISIBLE);
        mPlayPauseButton.setVisibility(View.VISIBLE);
        mSeekBar.setVisibility(View.VISIBLE);
        if (mPlayer != null)
            mPlayer.setVisibility(View.VISIBLE);
        mPlayPauseButton.setClickable(true);
        mSeekBar.setEnabled(true);

        currentMediaId = mediaId;
        Toast.makeText(getApplicationContext(), "Selected Media Id = " + mediaId, Toast.LENGTH_SHORT).show();
        if (mPlayer == null) {
            LOGD(TAG, "first time with entry id = " +  currentMediaId);
            getPlayer();
        }
        else {
            LOGD(TAG, "changeMedia with entry id = " + currentMediaId);
            mPlayer.changeMedia(currentMediaId);

            //Change configuration
//            config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.42.rc8/mwEmbedFrame.php", "33189171", "2068231").setEntryId(currentMediaId);
//            config.addConfig("controlBarContainer.plugin", "false");
//            config.addConfig("topBarContainer.plugin", "false");
//            config.addConfig("largePlayBtn.plugin", "false");
//            mPlayer.changeConfiguration(config);
        }
    }
    
    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);
            if (currentMediaId == null || "".equals(currentMediaId)){
                return null;
            }

            config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.49/mwEmbedFrame.php", "33189171", "2068231").setEntryId(currentMediaId);
            config.addConfig("controlBarContainer.plugin", "false");
            config.addConfig("topBarContainer.plugin", "false");
            config.addConfig("largePlayBtn.plugin", "false");
            config.addConfig("autoPlay", "true");
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
            mPlayer.releaseAndSavePosition();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (onCreate) {
            onCreate = false;
        } else {
            if (mPlayer != null)
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

            if (mPlayPauseButton.getText().equals("Play")) {
                mPlayPauseButton.setText("Pause");
                getPlayer().sendNotification("doPlay", null);
            } else {
                mPlayPauseButton.setText("Play");
                getPlayer().sendNotification("doPause", null);
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
        if (state == KPlayerState.ENDED) {
            LOGD(TAG,"Stream ENDED");
            chooseMediaButton.setVisibility(View.VISIBLE);
            mPlayPauseButton.setVisibility(View.INVISIBLE);
            mSeekBar.setVisibility(View.INVISIBLE);
            mPlayer.setVisibility(View.INVISIBLE);
        }
        if (state == KPlayerState.PAUSED) {
            LOGD(TAG, "Stream PAUSED");
            chooseMediaButton.setVisibility(View.VISIBLE);
        }
        if (state == KPlayerState.PLAYING) {
            LOGD(TAG, "Stream PAUSED");
            chooseMediaButton.setVisibility(View.INVISIBLE);
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
        LOGE(TAG, "Error Received:" + error.getErrorMsg());
    }
}
