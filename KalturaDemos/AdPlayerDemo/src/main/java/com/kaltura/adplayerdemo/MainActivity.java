package com.kaltura.adplayerdemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.Toast;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.SimpleVideoPlayer;
import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener, Observer {

    private final String adUrl1 = "http://html5demos.com/assets/dizzy.mp4";
    private final String adUrl2 = "http://html5demos.com/assets/remy-and-ellis2.mp4";

    private static final String TAG = "KalturaMultiPlayer";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private Button skipAd;
    private Button nextContent;

    private PlayerViewController mPlayer;
    private boolean onCreate = false;

    private FrameLayout adPlayerContainer;
    private SimpleVideoPlayer mAdPlayer;
    private boolean adPlayerIsPlaying = false;
    private boolean adIsDone;
    private boolean kPlayerReady;
    private int lastGroupIndex = 0;
    private boolean isDRMContent = true;
    private List<String> adList;
    private int currentAdIndex = 0;
    private KPPlayerConfig config = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(com.kaltura.adplayerdemo.R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mPlayPauseButton = (Button) findViewById(com.kaltura.adplayerdemo.R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        mSeekBar = (SeekBar) findViewById(com.kaltura.adplayerdemo.R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);

        skipAd = (Button) findViewById(R.id.skip_button);
        skipAd.setClickable(false);
        skipAd.setVisibility(View.INVISIBLE);
        skipAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdPlayer != null) {
                    Log.e(TAG, "Skip selected");
                    mAdPlayer.seek(mAdPlayer.getDuration(), true);
                }
            }
        });

        nextContent = (Button) findViewById(R.id.next_button);
        nextContent.setClickable(false);
        nextContent.setVisibility(View.INVISIBLE);
        nextContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "Next selected");
                if (config != null && !adPlayerIsPlaying) {
                    //  mPlayer.changeMedia("384080");
                    mPlayer.getMediaControl().pause();
                    mPlayer.detachView();
                    config = new KPPlayerConfig("http://kgit.html5video.org/branches/master/mwEmbedFrame.php", "20540612", "243342").setEntryId("1_sf5ovm7u");
                    config.addConfig("autoPlay", "true");
                    if (adList.size() > 0) {
                        mPlayer.setPrepareWithConfigurationMode(true);
                    }
                    mPlayer.changeConfiguration(config);
                    addMultiAdPlayer();
                } else {
                    Log.e(TAG, "Next selected not executed");
                }

            }
        });

        onCreate = true;
        adList = new ArrayList<>();
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((2));
        Log.e(TAG, "randomNum " + randomNum);
        if (randomNum == 1) {
            adList.add(adUrl1);
            //adList.add(adUrl1);
            adList.add(adUrl2);
        } else {
            Toast.makeText(this, "Error with getting AD", Toast.LENGTH_LONG).show();
        }


        getPlayer();
        //addAdPlayer();
        addMultiAdPlayer();
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController) findViewById(com.kaltura.adplayerdemo.R.id.player);
            mPlayer.loadPlayerIntoActivity(this);

            config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");
            config.addConfig("autoPlay", "true");
//            //config.addConfig("debugKalturaPlayer", Boolean.TRUE.toString());
////            config.addConfig("topBarContainer.hover", "true");
////            config.addConfig("controlBarContainer.hover", "true");
//            config.addConfig("controlBarContainer.plugin", "true");
////            config.addConfig("topBarContainer.plugin", "true");
//            config.addConfig("largePlayBtn.plugin", "true");
////            String adTagUrl = "http://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=%2F3510761%2FadRulesSampleTags&ciu_szs=160x600%2C300x250%2C728x90&cust_params=adrule%3Dpremidpostwithpod&impl=s&gdfp_req=1&env=vp&ad_rule=1&vid=12345&cmsid=3601&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&correlator=[timestamp]";
//
////            config.addConfig("doubleClick.adTagUrl",adTagUrl);
////            config.addConfig("doubleClick.plugin","true");

            if (adList.size() > 0) {
                boolean prepareWithConfigurationMode = true; // false to load surface automatically
                mPlayer.setPrepareWithConfigurationMode(prepareWithConfigurationMode);
            }
            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(this);

        }
        return mPlayer;
    }


    private RelativeLayout getPlayerContainer() {
        return (RelativeLayout) findViewById(com.kaltura.adplayerdemo.R.id.playerContainer);
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
        NetworkChangeReceiver.getObservable().deleteObserver(this);

    }

    @Override
    protected void onResume() {
        if (onCreate) {
            onCreate = false;
        } else {
            if (mPlayer != null)
                mPlayer.resumePlayer();
            if (mAdPlayer != null) {
                mAdPlayer.moveSurfaceToForeground();
                mAdPlayer.play();
            }
        }
        super.onResume();
        NetworkChangeReceiver.getObservable().addObserver(this);
    }

    @Override
    protected void onDestroy() {
        if (mPlayer != null) {
            mPlayer.removePlayer();
        }
        if (mAdPlayer != null) {
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
        if (v.getId() != com.kaltura.adplayerdemo.R.id.replay) {
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
        mSeekBar.setProgress((int) (currentTime / playerViewController.getDurationSec() * 100));
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
        if (state == KPlayerState.READY) {
            Log.e(TAG, "onKPlayerStateChanged PLAYER STATE_READY");
            kPlayerReady = true;
        }
        if (state == KPlayerState.ENDED && adIsDone) {
            Log.e(TAG, "onKPlayerStateChanged PLAYER STATE_ENDED");
            if (!wvClassicRequired(isDRMContent)) {
                mPlayer.detachView();
            }
            kPlayerReady = false;
            addMultiAdPlayer();
        }
    }


    private void removeAdPlayer() {
        Log.e(TAG, "removeAdPlayer");
        if (wvClassicRequired(isDRMContent)) {
            Log.e(TAG, "WV Classic mode");
            if (adPlayerContainer != null) {
                switchLayers(2, true);
            }
        } else {
            Log.e(TAG, "WV Modular mode/ ExoPlayer");
            switchLayers(1, false);
        }
        mAdPlayer = null;
    }

    public void switchLayers(int groupIndexIncrementBy, boolean removeContainer) {
        if (!removeContainer) {
            mAdPlayer.release();
            mAdPlayer.moveSurfaceToBackground();
        }
        adPlayerContainer.setVisibility(View.GONE);
        mPlayer.setVisibility(View.VISIBLE);
        ViewGroup myViewGroup = ((ViewGroup) adPlayerContainer.getParent());
        int index = myViewGroup.indexOfChild(adPlayerContainer);

        Log.d(TAG, "myViewGroup index =" + index);
        for (int i = lastGroupIndex; i < index; i++) {
            Log.d(TAG, "myViewGroup i = " + i + " / lastGroupIndex" + lastGroupIndex);
            myViewGroup.bringChildToFront(myViewGroup.getChildAt(i));

        }
        lastGroupIndex += groupIndexIncrementBy;
        if (removeContainer) {
            myViewGroup.removeView(adPlayerContainer);
        }
    }

    public boolean wvClassicRequired(boolean isDRMContent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 && isDRMContent) {
            return true;
        }
        return false;
    }

    ///AD PLAYER METHODS
    private void addMultiAdPlayer() {
        if (adList.size() == 0) {
            //problem with ads
            mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
                @Override
                public void handler() {
                    nextContent.setClickable(true);
                    nextContent.setVisibility(View.VISIBLE);
                    mPlayer.getMediaControl().start();
                }
            });
            return;
        }
        nextContent.setClickable(false);
        nextContent.setVisibility(View.INVISIBLE);
        // Add adPlayer view
        adPlayerContainer = new FrameLayout(mPlayer.getContext());
        ViewGroup.LayoutParams lp = mPlayer.getLayoutParams();
        lp = new ViewGroup.LayoutParams(lp.width, lp.height);
        mPlayer.addView(adPlayerContainer, lp);

        changeAdMedia(adList.get(0), 0);
    }

    public void changeAdMedia(String adUrl, final int index) {

        Video source = new Video(adUrl, Video.VideoType.MP4);
        if (mAdPlayer == null) {
            mAdPlayer = new SimpleVideoPlayer(this, adPlayerContainer, source, "", true);
            mAdPlayer.disableSeeking();
        }
        mAdPlayer.changeAdMedia(adPlayerContainer, source, true);
        mAdPlayer.addPlaybackListener(new ExoplayerWrapper.PlaybackListener() {
            @Override
            public void onStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case ExoPlayer.STATE_READY:
                        if (!playWhenReady && adPlayerIsPlaying) {
                            Log.e(TAG, "SimpleVideoPlayer STATE_READY playWhenReady pause " + playWhenReady);
                            adPlayerIsPlaying = false;
                            mAdPlayer.pause();
                            break;
                        }

                        Log.e(TAG, "SimpleVideoPlayer STATE_READY playWhenReady play " + playWhenReady);
                        //if (playWhenReady) {
                        if (!adPlayerIsPlaying && adPlayerContainer != null && mAdPlayer != null) {
                            Log.e(TAG, "START PLAY AD ");
                            adPlayerIsPlaying = true;
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    skipAd.setClickable(true);
                                    skipAd.setVisibility(View.VISIBLE);
                                }
                            }, 4000);
                            mAdPlayer.play();
                            //mAdPlayer.pause();
                            //kPlayerReady = false;
                            //mPlayer.attachView();
//                        mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
//                            @Override
//                            public void handler() {
//                                kPlayerReady = true;
//                                switchPlayers();
//                            }
//                        });

                        }
                        break;
                    case ExoPlayer.STATE_ENDED:
                        Log.e(TAG, "changeAdMedia AD ENDED prev index = " + currentAdIndex);
                        skipAd.setClickable(true);
                        skipAd.setVisibility(View.INVISIBLE);
                        currentAdIndex++;
                        if (currentAdIndex < adList.size()) {
                            changeAdMedia(adList.get(currentAdIndex), currentAdIndex);
                        }

                        adPlayerIsPlaying = false;
                        Log.e(TAG, "adPlayerIsPlaying = " + adPlayerIsPlaying);
                        adIsDone = true;
                        Log.e(TAG, "isLast " + index + "/" + adList.size());
                        if (index == adList.size() - 1) {
                            currentAdIndex = 0;
                            //changeAdMedia(adUrl1,true);
                            switchPlayers();
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
    }

    public void switchPlayers() {
        removeAdPlayer();

        if (kPlayerReady) {
            Log.e(TAG, "KPLAY FROM NORMAL PATH");
            if (!wvClassicRequired(isDRMContent)) {
                mPlayer.attachView();
            }

            nextContent.setClickable(true);
            nextContent.setVisibility(View.VISIBLE);
            mPlayer.getMediaControl().start();
            Log.e(TAG, "ENDED KPLAY FROM NORMAL PATH");


        } else {
            mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
                @Override
                public void handler() {
                    Log.e(TAG, "KPLAY FROM HANDLER");
                    if (!wvClassicRequired(isDRMContent)) {
                        mPlayer.attachView();
                    }

                    Log.e(TAG, "BEFORE ENDED KPLAY FROM HANDLER");
                    nextContent.setClickable(true);
                    nextContent.setVisibility(View.VISIBLE);
                    mPlayer.getMediaControl().start();

                    Log.e(TAG, "ENDED KPLAY FROM HANDLER");
                    kPlayerReady = false;

                }
            });

        }
    }


    @Override
    public void update (Observable observable, Object objectStatus){
        Log.e(TAG, "Update Observer");

        Boolean isConnected = (Boolean) objectStatus;
        if (isConnected) {
            onNetworkConnected();
        } else {
            onNetworkDisConnected();
        }

    }

    protected void onNetworkConnected () {
        Log.d(TAG, "onNetworkConnected");
        if (null != mPlayer) {
            mPlayer.resumePlayer();
            mPlayer.getMediaControl().start();
        }
    }

    protected void onNetworkDisConnected () {
        Log.d(TAG, "onNetworkDisConnected");
        if (null != mPlayer) {
            mPlayer.getMediaControl().pause();
        }
    }

}
    //    ///AD PLAYER METHODS
//    private void addAdPlayer() {
//
//        // Add adPlayer view
//        adPlayerContainer = new FrameLayout(mPlayer.getContext());
//        ViewGroup.LayoutParams lp = mPlayer.getLayoutParams();
//        lp = new ViewGroup.LayoutParams(lp.width, lp.height);
//        mPlayer.addView(adPlayerContainer, lp);
//
//        Video source = new Video(adUrl, Video.VideoType.MP4);
//        mAdPlayer = new SimpleVideoPlayer(this, adPlayerContainer, source, "", true);
//        mAdPlayer.disableSeeking();
//        mAdPlayer.addPlaybackListener(new ExoplayerWrapper.PlaybackListener() {
//            @Override
//            public void onStateChanged(boolean playWhenReady, int playbackState) {
//                switch (playbackState) {
//                    case ExoPlayer.STATE_READY:
//                        Log.e(TAG, "SimpleVideoPlayer STATE_READY");
//                        //if (playWhenReady) {
//                        if (!adPlayerIsPlaying && adPlayerContainer != null && mAdPlayer != null) {
//                            Log.e(TAG, "START PLAY AD ");
//                            adPlayerIsPlaying = true;
//                            mAdPlayer.play();
//                        }
//                        break;
//                    case ExoPlayer.STATE_ENDED:
//                        Log.e(TAG, "SimpleVideoPlayer AD ENDED");
//                        adPlayerIsPlaying = false;
//                        adIsDone = true;
//                        removeAdPlayer();
//
//                        if (kPlayerReady){
//                            Log.e(TAG, "KPLAY FROM NORMAL PATH");
//                            if (!wvClassicRequired(isDRMContent)) {
//                                mPlayer.attachView();
//                            }
//                            mPlayer.getMediaControl().start();
//                        }else {
//                            mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
//                                @Override
//                                public void handler() {
//                                        Log.e(TAG, "KPLAY FROM HANDLER");
//                                        if (!wvClassicRequired(isDRMContent)) {
//                                            mPlayer.attachView();
//                                        }
//                                        mPlayer.getMediaControl().start();
//                                }
//                            });
//                        }
//
//                        break;
//                }
//            }
//
//            @Override
//            public void onError(Exception e) {
//
//            }
//
//            @Override
//            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
//
//            }
//        });
//        mAdPlayer.moveSurfaceToForeground();
//    }

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


