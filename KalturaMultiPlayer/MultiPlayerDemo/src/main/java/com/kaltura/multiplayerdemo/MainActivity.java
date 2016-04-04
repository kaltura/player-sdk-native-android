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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {

    private final String adUrl = "http://dpndczlul8yjf.cloudfront.net/creatives/assets/9d266094-8d1e-49e3-b13c-249515529bfc/c01b6747-0a3b-4480-9286-811d469b977d.mp4";
    private static final String TAG = "KalturaMultiPlayer";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;

    private FrameLayout adPlayerContainer;
    SimpleVideoPlayer mAdPlayer;
    boolean adPlayerIsPlaying;
    boolean adIsDone;

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

            String json = "{\n" +
                    "  \"base\": {\n" +
//                    "    \"server\": \"http://player-as.ott.kaltura.com/viacom18/v2.41.1.1_viacom_v0.17_v0.3.rc9_viacom_proxy_v0.2.2/mwEmbed/mwEmbedFrame.php\",\n" +
                    "    \"server\": \"http://192.168.160.160/html5.kaltura/mwEmbed/mwEmbedFrame.php\",\n" +
//                    "    \"server\": \"http://player-as.ott.kaltura.com/viacom18/v2.39.7.2_viacom_v0.12/mwEmbed/mwEmbedFrame.php\",\n" +
//                    "    \"server\": \"http://player-stg-eu.ott.kaltura.com/viacomIN/v2.39.7.2_v0.13/mwEmbed/mwEmbedFrame.php\",\n" +
                    "    \"partnerId\": \"\",\n" +
                    "    \"uiConfId\": \"32626752\",\n" +
                    //"    \"entryId\": \"374130\"\n" +
                    "    \"entryId\": \"384080\"\n" +

                    //"    \"entryId\": \"361705\"\n" +
                    //"    \"entryId\": \"364365\"\n" +
                    //"    \"entryId\": \"375391\"\n" +
                    //"    \"entryId\": \"365444\"\n" +
                    //"    \"entryId\": \"371348\"\n" +

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
                    "    \"tvpapiGetLicensedLinks.plugin\": true,\n" +
                    "    \"TVPAPIBaseUrl\": \"http://tvpapi-as.ott.kaltura.com/v3_4/gateways/jsonpostgw.aspx?m=\",\n" +
                    "    \"proxyData\": {\n" +
                    "\"config\": {\n" +
                    "                                    \"flavorassets\": {\n" +
                    "                                        \"filters\": {\n" +
                    "                                            \"include\": {\n" +
                    "                                                \"Format\": [\n" +
                    "                                                    \"dash Main\"\n" +
                    "                                                ]\n" +
                    "                                            }\n" +
                    "                                        }\n" +
                    "                                    }\n" +
                    "                                }," +
                    "      \"MediaID\": \"384080\",\n" +
                    "      \"iMediaID\": \"384080\",\n" +
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
            KPPlayerConfig config = null;
            try {
                config = KPPlayerConfig.fromJSONObject(new JSONObject(json));

                config.addConfig("topBarContainer.hover", "true");
                config.addConfig("controlBarContainer.plugin", "true");
                config.addConfig("durationLabel.prefix", " ");
                config.addConfig("largePlayBtn.plugin", "true");
                //config.addConfig("","");
                //config.addConfig("autoPlay", "true");
                config.addConfig("scrubber.sliderPreview","false");
                //config.addConfig("largePlayBtn","false");
                //config.addConfig("debugKalturaPlayer", Boolean.TRUE.toString());
                String KALTURA_SESSION_SHORT_NAME = "ks";
                String KALTURA_MEDIA_PLAY_FROM = "mediaProxy.mediaPlayFrom";
                String KALTURA_CONTROL_BAR = "controlBarContainer.plugin";
                String KALTURA_TOP_BAR_PROPERTY = "topBarContainer.plugin";
                String KALTURA_LARGE_PLAY_PROPERTY = "largePlayBtn.plugin";
                String KALTURA_AUTOPLAY = "autoPlay";

                //config.addConfig(KALTURA_CONTROL_BAR, Boolean.FALSE.toString());
                //config.addConfig(KALTURA_TOP_BAR_PROPERTY, Boolean.FALSE.toString());
                //config.addConfig(KALTURA_LARGE_PLAY_PROPERTY, Boolean.FALSE.toString());
                // config.addConfig(KALTURA_AUTOPLAY, Boolean.TRUE.toString());
                //
                //

                //config.addConfig("kidsPlayer.plugin", "true");
                //config.addConfig("nextBtnComponent.plugin", "true");
                //config.addConfig("prevBtnComponent.plugin", "true");
                config.addConfig("debugKalturaPlayer", "true");
                config.addConfig("EmbedPlayer.HidePosterOnStart", "true");
                config.addConfig("EmbedPlayer.ShowPosterOnStop", "true");
                //config.addConfig("chromecast.plugin", "true");

                mPlayer.setKDPAttribute("nextBtnComponent", "visible", "false");
                mPlayer.setKDPAttribute("prevBtnComponent", "visible", "false");

            } catch (JSONException e) {
                e.printStackTrace();
            }

            //KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");
            //KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "33189171", "2068231").setEntryId("1_dzu1m8qt");

            //config.addConfig("autoPlay", "true");
            config.addConfig("debugKalturaPlayer", Boolean.TRUE.toString());
//            config.addConfig("topBarContainer.hover", "true");
//            config.addConfig("controlBarContainer.hover", "true");
//            config.addConfig("controlBarContainer.plugin", "true");
//            config.addConfig("topBarContainer.plugin", "true");
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

        }
        if (state == KPlayerState.ENDED && adIsDone){
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
        Log.e(TAG, "Error Received:" + error.getErrorMsg());
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
                        Log.e("GILAD", "STATE_READY");
                        //if (playWhenReady) {
                            if (!adPlayerIsPlaying && adPlayerContainer != null && mAdPlayer != null) {
                                Log.e(TAG, "START PLAY AD ");
                                adPlayerIsPlaying = true;
                                mAdPlayer.play();
                            }
                        break;
                    case ExoPlayer.STATE_ENDED:
                        Log.e(TAG, "AD ENDED");
                        adPlayerIsPlaying = false;
                        adIsDone = true;
                        removeAdPlayer();
                        mPlayer.getMediaControl().start();
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
