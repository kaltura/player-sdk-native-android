package com.kaltura.multiplayerdemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {

    private final String adUrl = "http://html5demos.com/assets/dizzy.mp4";

    private static final String TAG = "KalturaMultiPlayer";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;

    private FrameLayout adPlayerContainer;
    private SimpleVideoPlayer mAdPlayer;
    private boolean adPlayerIsPlaying;
    private boolean adIsDone;
    private boolean kPlayerReady;
    private int lastGroupIndex = 0;
    private boolean isDRMContent = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
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
            mPlayer = (PlayerViewController) findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);

            KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");
            //config.addConfig("autoPlay", "true");
            //config.addConfig("debugKalturaPlayer", Boolean.TRUE.toString());
//            config.addConfig("topBarContainer.hover", "true");
//            config.addConfig("controlBarContainer.hover", "true");
            config.addConfig("controlBarContainer.plugin", "true");
//            config.addConfig("topBarContainer.plugin", "true");
            config.addConfig("largePlayBtn.plugin", "true");
//            String adTagUrl = "http://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=%2F3510761%2FadRulesSampleTags&ciu_szs=160x600%2C300x250%2C728x90&cust_params=adrule%3Dpremidpostwithpod&impl=s&gdfp_req=1&env=vp&ad_rule=1&vid=12345&cmsid=3601&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&correlator=[timestamp]";

//            config.addConfig("doubleClick.adTagUrl",adTagUrl);
//            config.addConfig("doubleClick.plugin","true");



            boolean setPrepareWithConfigurationMode = false;
            mPlayer.initWithConfiguration(config,setPrepareWithConfigurationMode);
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
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
        mSeekBar.setProgress((int)(currentTime / playerViewController.getDurationSec() * 100));
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        Log.e(TAG, "Error Received:" + error.getErrorMsg());
    }

    @Override
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        if (state == KPlayerState.READY){
            Log.e(TAG, "onKPlayerStateChanged PLAYER STATE_READY");
            kPlayerReady = true;
        }
        if (state == KPlayerState.ENDED && adIsDone){
            Log.e(TAG, "onKPlayerStateChanged PLAYER STATE_ENDED");
            if (!wvClassicRequired(isDRMContent)) {
                mPlayer.detachView();
            }
            addAdPlayer();
        }
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
        mAdPlayer.disableSeeking();
        mAdPlayer.addPlaybackListener(new ExoplayerWrapper.PlaybackListener() {
            @Override
            public void onStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case ExoPlayer.STATE_READY:
                        Log.e(TAG, "SimpleVideoPlayer STATE_READY");
                        //if (playWhenReady) {
                        if (!adPlayerIsPlaying && adPlayerContainer != null && mAdPlayer != null) {
                            Log.e(TAG, "START PLAY AD ");
                            adPlayerIsPlaying = true;
                            mAdPlayer.play();
                        }
                        break;
                    case ExoPlayer.STATE_ENDED:
                        Log.e(TAG, "SimpleVideoPlayer AD ENDED");
                        adPlayerIsPlaying = false;
                        adIsDone = true;
                        removeAdPlayer();

                        if (kPlayerReady){
                            Log.e(TAG, "KPLAY FROM NORMAL PATH");
                            if (!wvClassicRequired(isDRMContent)) {
                                mPlayer.attachView();
                            }
                            mPlayer.getMediaControl().start();
                        }else {
                            mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
                                @Override
                                public void handler() {
                                    Log.e(TAG, "KPLAY FROM HANDLER");
                                    if (!wvClassicRequired(isDRMContent)) {
                                        mPlayer.attachView();
                                    }
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

//    private void removeAdPlayerOrig() {
//        Log.e(TAG, "removeAdPlayer");
//        if (wvClassicRequired(isDRMContent)) {
//            Log.e(TAG, "WV Classic mode");
//            if (adPlayerContainer != null) {
//
//                adPlayerContainer.setVisibility(View.GONE);
//                mPlayer.setVisibility(View.VISIBLE);
//                ViewGroup myViewGroup = ((ViewGroup) adPlayerContainer.getParent());
//                int index = myViewGroup.indexOfChild(adPlayerContainer);
//                Log.d(TAG, "myViewGroup index =" + index);
//                for(int i = lastGroupIndex; i<index; i++)
//                {
//                    Log.d(TAG, "myViewGroup i = "  + i + " / lastGroupIndex" + lastGroupIndex);
//                    myViewGroup.bringChildToFront(myViewGroup.getChildAt(i));
//
//                }
//                myViewGroup.removeView(adPlayerContainer);
//                lastGroupIndex += 2;
//                //adPlayerContainer = null;
//            }
//            mAdPlayer = null;
//
//        } else {
//            Log.e(TAG, "WV Modular mode/ ExoPlayer");
//            mAdPlayer.release();
//            mAdPlayer.moveSurfaceToBackground();
//           // mPlayer.removeView(adPlayerContainer);
//            adPlayerContainer.setVisibility(View.GONE);
//            mPlayer.setVisibility(View.VISIBLE);
//            ViewGroup myViewGroup = ((ViewGroup) adPlayerContainer.getParent());
//            int index = myViewGroup.indexOfChild(adPlayerContainer);
//            Log.d(TAG, "myViewGroup index =" + index);
//            for(int i = lastGroupIndex; i<index; i++)
//            {
//                Log.d(TAG, "myViewGroup i = "  + i + " / lastGroupIndex" + lastGroupIndex);
//                myViewGroup.bringChildToFront(myViewGroup.getChildAt(i));
//            }
//            //myViewGroup.removeView(adPlayerContainer);
//            lastGroupIndex++;
//            mAdPlayer = null;
//        }
//    }

    private void removeAdPlayer() {
        Log.e(TAG, "removeAdPlayer");
        if (wvClassicRequired(isDRMContent)) {
            Log.e(TAG, "WV Classic mode");
            if (adPlayerContainer != null) {
                adPlayerContainer.setVisibility(View.GONE);
                mPlayer.setVisibility(View.VISIBLE);
                switchLayers(2, true);
            }
        } else {
            Log.e(TAG, "WV Modular mode/ ExoPlayer");
            mAdPlayer.release();
            mAdPlayer.moveSurfaceToBackground();
            switchLayers(1, false);
        }
        mAdPlayer = null;
    }

    public void switchLayers(int groupIndexIncrementBy, boolean removeContainer){
        adPlayerContainer.setVisibility(View.GONE);
        mPlayer.setVisibility(View.VISIBLE);
        ViewGroup myViewGroup = ((ViewGroup) adPlayerContainer.getParent());
        int index = myViewGroup.indexOfChild(adPlayerContainer);
        Log.d(TAG, "myViewGroup index =" + index);
        for(int i = lastGroupIndex; i<index; i++)
        {
            Log.d(TAG, "myViewGroup i = "  + i + " / lastGroupIndex" + lastGroupIndex);
            myViewGroup.bringChildToFront(myViewGroup.getChildAt(i));

        }
        lastGroupIndex += groupIndexIncrementBy;
        if (removeContainer){
            myViewGroup.removeView(adPlayerContainer);
        }
    }
    public boolean wvClassicRequired(boolean isDRMContent){
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 && isDRMContent){
            return true;
        }
        return false;
    }
}
