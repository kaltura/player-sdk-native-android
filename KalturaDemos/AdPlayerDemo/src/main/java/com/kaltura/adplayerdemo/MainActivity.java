package com.kaltura.adplayerdemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
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
import com.kaltura.playersdk.config.KProxyData;
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

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener, Observer {

    private final String adUrl1 = "http://html5demos.com/assets/dizzy.mp4";
    //private final String adUrl = "http://dpndczlul8yjf.cloudfront.net/creatives/assets/9d266094-8d1e-49e3-b13c-249515529bfc/c01b6747-0a3b-4480-9286-811d469b977d.mp4";

    private final String adUrl2 = "http://dpndczlul8yjf.cloudfront.net/creatives/assets/79dba610-b5ee-448b-8e6b-531b3d3ebd54/5fe7eb54-0296-4688-af06-9526007054a4.mp4";
    private final String adUrl = "http://dpndczlul8yjf.cloudfront.net/creatives/assets/c00cfcf0-985c-4d83-b32a-af8824025e9b/fa69a864-0e37-4597-b2f0-bdaceb16b56b.mp4";
    private final String playerUrl = "http://player-as.ott.kaltura.com/225/v2.48.9_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.12/mwEmbed//mwEmbedFrame.php"; // "http://player-as.ott.kaltura.com/225/v2.48.6_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.7/mwEmbed/mwEmbedFrame.php"

    private static final String TAG = "KalturaMultiPlayer";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private Button skipAd;
    private Button nextContent;

    private PlayerViewController mPlayer;
    private boolean onCreate = false;

    private FrameLayout adPlayerContainer;
    private SimpleVideoPlayer mAdPlayer;
    private boolean adPlayerIsPlaying;
    private boolean adIsDone;
    private boolean kPlayerReady;
    private boolean isDRMContent = true;
    private List<String> adList;
    private int currentAdIndex = 0;
    int randomNum = 0;
    private KPPlayerConfig config = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mPlayPauseButton = (Button) findViewById(R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);

        skipAd = (Button) findViewById(R.id.skip_button);
        skipAd.setClickable(false);
        skipAd.setVisibility(View.INVISIBLE);
        skipAd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGD(TAG, "Skip selected");
                mAdPlayer.seek(mAdPlayer.getDuration(), true);
            }
        });

        nextContent = (Button) findViewById(R.id.next_button);
        nextContent.setClickable(false);
        nextContent.setVisibility(View.INVISIBLE);
        nextContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LOGD(TAG, "Next selected");
                if (config != null && !adPlayerIsPlaying) {

                    //  mPlayer.changeMedia("384080");
                    mPlayer.getMediaControl().pause();
                    mPlayer.detachView();

                    try {
                        config = getConfig("388409");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (adList.size() > 0) {
                        mPlayer.setPrepareWithConfigurationMode(true);
                    }
                    mPlayer.changeConfiguration(config);
                    addMultiAdPlayer();
                }
            }
        });

        onCreate = true;
        adList = new ArrayList<>();
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        randomNum = rand.nextInt((2));
        LOGD(TAG, "randomNum " + randomNum);
        if (randomNum == 1) {
            adList.add(adUrl);
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
            mPlayer = (PlayerViewController) findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);

//            KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");
//            config.addConfig("autoPlay", "true");
//            config.addConfig("debugKalturaPlayer", Boolean.TRUE.toString());
//            config.addConfig("topBarContainer.hover", "true");
//            config.addConfig("controlBarContainer.hover", "true");
//            config.addConfig("controlBarContainer.plugin", "true");
//            config.addConfig("topBarContainer.plugin", "true");
//            config.addConfig("largePlayBtn.plugin", "true");

//            String adTagUrl = "http://pubads.g.doubleclick.net/gampad/ads?sz=640x480&iu=%2F3510761%2FadRulesSampleTags&ciu_szs=160x600%2C300x250%2C728x90&cust_params=adrule%3Dpremidpostwithpod&impl=s&gdfp_req=1&env=vp&ad_rule=1&vid=12345&cmsid=3601&output=xml_vast2&unviewed_position_start=1&url=[referrer_url]&correlator=[timestamp]";
//            config.addConfig("doubleClick.adTagUrl",adTagUrl);
//            config.addConfig("doubleClick.plugin","true");
//
//            String json = getJson("384080"/*"388409"*/); //456237


            try {
                if (config == null) {
                    config = getConfig("384080");
                }
                mPlayer.setKDPAttribute("nextBtnComponent", "visible", "false");
                mPlayer.setKDPAttribute("prevBtnComponent", "visible", "false");

            } catch (Exception e) {
                e.printStackTrace();
            }

            if (adList.size() > 0) {
                boolean prepareWithConfigurationMode = true; // false to load surface automatically
                mPlayer.setPrepareWithConfigurationMode(prepareWithConfigurationMode);
            }
            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(this);

        }
        return mPlayer;
    }

    public KPPlayerConfig getConfig(String mediaID) {
        KPPlayerConfig config = new KPPlayerConfig(playerUrl, "32626752", "");
        config.setEntryId(mediaID);
        config.addConfig("topBarContainer.hover", "true");
        //config.addConfig("autoPlay", "true");
        config.addConfig("controlBarContainer.plugin", "true");
        config.addConfig("durationLabel.prefix", " ");
        config.addConfig("largePlayBtn.plugin", "true");
        //config.addConfig("mediaProxy.mediaPlayFrom", String.valueOf("100"));
        config.addConfig("scrubber.sliderPreview", "false");
        //config.addConfig("largePlayBtn","false");
        //config.addConfig("debugKalturaPlayer", "true");
        config.addConfig("EmbedPlayer.HidePosterOnStart", "true");
        config.addConfig("watermark.plugin", "true");
        config.addConfig("watermark.img", "https://voot-kaltura.s3.amazonaws.com/voot-watermark.png");
        config.addConfig("watermark.title", "Viacom18");
        config.addConfig("watermark.cssClass", "topRight");
        config.addConfig("controlBarContainer.hover", "true");
        config.addConfig("controlBarContainer.plugin", "true");
        config.addConfig("kidsPlayer.plugin", "true,");
        config.addConfig("nextBtnComponent.plugin", "true");
        config.addConfig("prevBtnComponent.plugin", "true");
        config.addConfig("liveCore.disableLiveCheck", "true");
//        config.addConfig("tvpapiGetLicensedLinks.plugin", "true");
        config.addConfig("TVPAPIBaseUrl", "http://tvpapi-as.ott.kaltura.com/v3_4/gateways/jsonpostgw.aspx?m=");
        config.addProxyData(KProxyData.newBuilder().setMediaId(mediaID)
                .setIMediaId(mediaID)
                .setMediaType("0")
                .setPicSize(640, 360)
                .setWithDynamic(false)
                .setDomainId(0)
                .setUserProtection("tvpapi_225", "11111", "aa5e1b6c96988d68")
                .setSiteGuid("")
                .setPlatform("Cellular")
                .setLocale("", "", "Unknown", "")
                .addProxyConfigFilter("dash Main")
                .build());
        return config;
    }

    private RelativeLayout getPlayerContainer() {
        return (RelativeLayout) findViewById(R.id.playerContainer);
    }

    @Override
    protected void onPause() {
        // If the screen is off then the device has been locked
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean isScreenOn = powerManager.isScreenOn();


        if (mPlayer != null) {
            if (!isScreenOn) {
                LOGD(TAG, "Screen OFF");
                // The screen has been locked
                // do stuff...
                mPlayer.saveState();
                mPlayer.getMediaControl().pause();
            } else {
                mPlayer.releaseAndSavePosition();
            }

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
                mPlayer.getMediaControl().start();
            mPlayer.resumePlayer();
            LOGD(TAG, "on Resume called for player");
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

    @Override
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        LOGD(TAG, "onKPlayerStateChanged state = " + state.name());
        if (state == KPlayerState.READY) {
            LOGD(TAG, "onKPlayerStateChanged PLAYER STATE_READY");
            kPlayerReady = true;
            mPlayer.getMediaControl().pause();
            if (randomNum != 1) { //ad failed
                mPlayer.getMediaControl().start();
            }
        }
        if (state == KPlayerState.ENDED && adIsDone) {
            LOGD(TAG, "onKPlayerStateChanged PLAYER STATE_ENDED");
            if (!wvClassicRequired(isDRMContent)) {
                mPlayer.detachView();
            }
            kPlayerReady = false;
            addMultiAdPlayer();
        }
    }


    private void removeAdPlayer() {
        LOGD(TAG, "removeAdPlayer");
        if (wvClassicRequired(isDRMContent)) {
            LOGD(TAG, "WV Classic mode");
            if (adPlayerContainer != null) {
                switchLayers(true);
            }
        } else {
            LOGD(TAG, "WV Modular mode/ ExoPlayer");
            switchLayers(false);
        }
        mAdPlayer = null;
    }

    public void switchLayers(boolean removeContainer) {
        if (!removeContainer) {
            mAdPlayer.release();
            mAdPlayer.moveSurfaceToBackground();
        }
        adPlayerContainer.setVisibility(View.GONE);
        mPlayer.setVisibility(View.VISIBLE);
        ViewGroup myViewGroup = ((ViewGroup) adPlayerContainer.getParent());

        View player = findViewById(R.id.player);
        myViewGroup.bringChildToFront(player);
        myViewGroup.bringChildToFront(findViewById(R.id.webView_1));

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
        } else {
            mAdPlayer.changedMedia(adPlayerContainer, source, true);
        }
        mAdPlayer.addPlaybackListener(new ExoplayerWrapper.PlaybackListener() {
            @Override
            public void onStateChanged(boolean playWhenReady, int playbackState) {
                switch (playbackState) {
                    case ExoPlayer.STATE_READY:
                        if (!playWhenReady && adPlayerIsPlaying) {
                            LOGD(TAG, "SimpleVideoPlayer STATE_READY playWhenReady pause " + playWhenReady);
                            adPlayerIsPlaying = false;
                            mAdPlayer.pause();
                            break;
                        }

                        LOGD(TAG, "SimpleVideoPlayer STATE_READY playWhenReady play " + playWhenReady);
                        //if (playWhenReady) {
                        if (!adPlayerIsPlaying && adPlayerContainer != null && mAdPlayer != null) {
                            LOGD(TAG, "START PLAY AD ");
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
                        LOGD(TAG, "changeAdMedia AD ENDED prev index = " + currentAdIndex);
                        skipAd.setClickable(true);
                        skipAd.setVisibility(View.INVISIBLE);
                        currentAdIndex++;
                        if (currentAdIndex < adList.size()) {
                            changeAdMedia(adList.get(currentAdIndex), currentAdIndex);
                        }

                        adPlayerIsPlaying = false;
                        adIsDone = true;
                        LOGD(TAG, "isLast " + index + "/" + adList.size());
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
            LOGD(TAG, "KPLAY FROM NORMAL PATH");
            if (!wvClassicRequired(isDRMContent)) {
                mPlayer.attachView();
            }

            nextContent.setClickable(true);
            nextContent.setVisibility(View.VISIBLE);
            mPlayer.getMediaControl().start();
            LOGD(TAG, "ENDED KPLAY FROM NORMAL PATH");


        } else {
            mPlayer.registerReadyEvent(new PlayerViewController.ReadyEventListener() {
                @Override
                public void handler() {
                    LOGD(TAG, "KPLAY FROM HANDLER");
                    if (!wvClassicRequired(isDRMContent)) {
                        mPlayer.attachView();
                    }

                    LOGD(TAG, "BEFORE ENDED - KPLAY FROM HANDLER");
                    nextContent.setClickable(true);
                    nextContent.setVisibility(View.VISIBLE);
                    mPlayer.getMediaControl().start();

                    LOGD(TAG, "ENDED - KPLAY FROM HANDLER");
                    kPlayerReady = false;

                }
            });
        }
    }

    @Override
    public void update(Observable observable, Object objectStatus) {
        Boolean isConnected = (Boolean) objectStatus;
        if (isConnected) {
            onNetworkConnected();
        } else {
            onNetworkDisConnected();
        }

    }

    protected void onNetworkConnected() {
        if (null != mPlayer) {
            mPlayer.resumePlayer();
        }
    }

    protected void onNetworkDisConnected() {
        if (null != mPlayer) {
            //mPlayer.getMediaControl().pause();
            mPlayer.saveState();
            mPlayer.getMediaControl().pause();
        }
    }

    public String getJsonString(String mediaID) {
        String json = "{\n" +
                "  \"base\": {\n" +
                "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.48.7_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.11/mwEmbed/mwEmbedFrame.php\",\n" +
                //                 "    \"server\": \"http://192.168.160.160/html5.kaltura/mwEmbed/mwEmbedFrame.php\",\n" +

                "    \"partnerId\": \"\",\n" +
                "    \"uiConfId\": \"32626752\",\n" +
                //"    \"entryId\": \"374130\"\n" +
                //                 "    \"entryId\": \"384080\"\n" +
                "    \"entryId\": \"" + mediaID + "\"\n" +


                "  },\n" +
                "  \"extra\": {\n" +
                "    \"watermark.plugin\": \"true\",\n" +
                "    \"watermark.img\": \"https://voot-kaltura.s3.amazonaws.com/voot-watermark.png\",\n" +
                "    \"watermark.title\": \"Viacom18\",\n" +
                "    \"watermark.cssClass\": \"topRight\",\n" +
                "    \n" +
                "    \"controlBarContainer.hover\": true,\n" +
                "    \"controlBarContainer.plugin\": true,\n" +
//                    "    \"adultPlayer.plugin\": false,\n" +
                "    \"kidsPlayer.plugin\": true,\n" +
                "    \"nextBtnComponent.plugin\": true,\n" +
                "    \"prevBtnComponent.plugin\": true,\n" +
                "    \n" +
                "    \"liveCore.disableLiveCheck\": true,\n" +
                //"    \"tvpapiGetLicensedLinks.plugin\": true,\n" +
                "    \"TVPAPIBaseUrl\": \"http://tvpapi-as.ott.kaltura.com/v3_4/gateways/jsonpostgw.aspx?m=\",\n" +
                "    \"proxyData\": {\n";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 /*4.3*/) {
            json = json + "\"config\": {\n" +
                    "                                    \"flavorassets\": {\n" +
                    "                                        \"filters\": {\n" +
                    "                                            \"include\": {\n" +
                    "                                                \"Format\": [\n" +
                    "                                                    \"Tablet Main\"\n" + // dash Main/dash Mobile
                    "                                                ]\n" +
                    "                                            }\n" +
                    "                                        }\n" +
                    "                                    }\n" +
                    "                                },";
        }
        json = json + "      \"MediaID\": \"" + mediaID + "\",\n" +
                "      \"iMediaID\": \"" + mediaID + "\",\n" +
                "      \"mediaType\": \"0\",\n" +
                "      \"picSize\": \"640x360\",\n" +
                "      \"withDynamic\": \"false\",\n" +
                "      \"initObj\": {\n" +
                "        \"ApiPass\": \"11111\",\n" +
                "        \"ApiUser\": \"tvpapi_225\",\n" +
                "        \"DomainID\": 0,\n" +
                "        \"Locale\": {\n" +
                "            \"LocaleCountry\": \"null\",\n" +
                "            \"LocaleDevice\": \"null\",\n" +
                "            \"LocaleLanguage\": \"null\",\n" +
                "            \"LocaleUserState\": \"Unknown\"\n" +
                "        },\n" +
                "        \"Platform\": \"Cellular\",\n" +
                "        \"SiteGuid\": \"\",\n" +
                "        \"UDID\": \"aa5e1b6c96988d68\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
        return json;
    }
}