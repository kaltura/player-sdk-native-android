package com.kaltura.multiplayerdemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.SimpleVideoPlayer;
import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import java.util.Timer;
import java.util.TimerTask;

import static com.kaltura.playersdk.utils.LogUtils.LOGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {

    private final String adUrl = "http://html5demos.com/assets/dizzy.mp4";
    private static final String TAG = "KalturaMultiPlayer";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;

    private FrameLayout adPlayerContainer;
    SimpleVideoPlayer mAdPlayer;
    boolean adPlayerIsPlaying;
    boolean adIsDone;
    boolean kPlayerReady;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(com.kaltura.multiplayerdemo.R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }




        mPlayPauseButton = (Button)findViewById(com.kaltura.multiplayerdemo.R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        mSeekBar = (SeekBar)findViewById(com.kaltura.multiplayerdemo.R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        onCreate = true;

        getPlayer();
        addAdPlayer();
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(com.kaltura.multiplayerdemo.R.id.player);
            mPlayer.loadPlayerIntoActivity(this);

            KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");
            config.addConfig("topBarContainer.hover", "true");
            config.addConfig("controlBarContainer.plugin", "true");
            config.addConfig("durationLabel.prefix", " ");
            config.addConfig("largePlayBtn.plugin", "true");
            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(this);

        }
        return mPlayer;
    }

    private RelativeLayout getPlayerContainer() {
        return (RelativeLayout)findViewById(com.kaltura.multiplayerdemo.R.id.playerContainer);
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.releaseAndSavePosition();
        }
        if (mAdPlayer != null) {
            mAdPlayer.pause();
            mAdPlayer.moveSurfaceToBackground();
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
            if (mAdPlayer != null ){
                mAdPlayer.moveSurfaceToForeground();
                mAdPlayer.play();
            }
        }
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.removePlayer();
        }
        if (mAdPlayer != null ){
            removeAdPlayer();
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
        if (v.getId() != com.kaltura.multiplayerdemo.R.id.replay) {
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
        if (state == KPlayerState.READY){
            LOGE(TAG, "onKPlayerStateChanged PLAYER STATE_READY");
            kPlayerReady = true;
        }
        if (state == KPlayerState.ENDED && adIsDone){
            LOGE(TAG, "onKPlayerStateChanged PLAYER STATE_ENDED");
            addAdPlayer();
        }
    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
        mSeekBar.setProgress((int)(currentTime / playerViewController.getDurationSec() * 100));
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        LOGE(TAG, "Error Received:" + error.getErrorMsg());
    }

    ///AD PLAYER METHODS
    private void addAdPlayer() {

        // Add adPlayer view
        adPlayerContainer = new FrameLayout(mPlayer.getContext());
        ViewGroup.LayoutParams lp = mPlayer.getLayoutParams();
        lp = new ViewGroup.LayoutParams(lp.width, lp.height);
        mPlayer.addView(adPlayerContainer, lp);

        Video source = new Video(adUrl, Video.VideoType.MP4);
        mAdPlayer = new SimpleVideoPlayer(this, adPlayerContainer, source, "", true);
        mAdPlayer.addPlaybackListener(new ExoplayerWrapper.PlaybackListener() {
            @Override
            public void onStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case ExoPlayer.STATE_READY:
                        LOGE(TAG, "SimpleVideoPlayer STATE_READY");
                        //if (playWhenReady) {
                        if (!adPlayerIsPlaying && adPlayerContainer != null && mAdPlayer != null) {
                            LOGE(TAG, "START PLAY AD ");
                            adPlayerIsPlaying = true;
                            mAdPlayer.play();
                        }
                        break;
                    case ExoPlayer.STATE_ENDED:
                        LOGE(TAG, "SimpleVideoPlayer AD ENDED");
                        adPlayerIsPlaying = false;
                        adIsDone = true;
                        removeAdPlayer();
                        if (kPlayerReady){
                            LOGE(TAG, "KPLAY FROM NORMAL PATH");
                            mPlayer.getMediaControl().start();
                        }else {
                            mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
                                @Override
                                public void handler() {
                                    LOGE(TAG, "KPLAY FROM HANDLER");
                                    mPlayer.getMediaControl().start();
                                }
                            });
                        }

                        break;
                }
            }

            @Override
            public void onError(Exception e) {

            }

            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {

            }
        });
        mAdPlayer.moveSurfaceToForeground();
    }

    private void removeAdPlayer() {
        if (mPlayer != null) {
            mPlayer.removeView(adPlayerContainer);
        }
        if (adPlayerContainer != null) {
            adPlayerContainer.setVisibility(View.GONE);
            adPlayerContainer = null;
        }
        mAdPlayer = null;

    }
}
