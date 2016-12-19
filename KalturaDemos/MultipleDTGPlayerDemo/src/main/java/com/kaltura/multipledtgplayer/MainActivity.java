package com.kaltura.multipledtgplayer;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
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
import com.kaltura.playersdk.interfaces.KPrefetchListener;
import com.kaltura.playersdk.types.KPError;
import com.kaltura.playersdk.utils.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements KPErrorEventListener, KPStateChangedEventListener {

    private static final String TAG = "MainActivity";

    private PlayerViewController mPlayer;
    private ViewGroup mPlayerContainer;
    private VideoItemsLoader mVideoItemsLoader;

    private PlayerViewController.SourceURLProvider mSourceURLProvider = new PlayerViewController.SourceURLProvider() {
        @Override
        public String getURL(String entryId, String currentURL) {

            String playbackURL = mVideoItemsLoader.getUrl(entryId);
            LogUtils.LOGD(TAG, "Playback URL => " + playbackURL);
            return playbackURL;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prepareCache();
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

    public String getPrefetchjJson() {
        String json = "{\n" +
                "  \"base\": {\n" +
                //    "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.48.6_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.7/mwEmbed/mwEmbedFrame.php\",\n" +

                // "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.48.7_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.11/mwEmbed/mwEmbedFrame.php\",\n" +

                "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.48.9_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.12/mwEmbed/mwEmbedFrame.php\",\n" +
                // "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.47_viacom_v0.30_v0.4.1_viacom_proxy_v0.4.4/mwEmbed/mwEmbedFrame.php\",\n" +
                //                   "    \"server\": \"http://192.168.160.69/html5.kaltura/mwEmbed/mwEmbedFrame.php\",\n" +
                // "    \"server\": \"http://192.168.150.160/html5.kaltura/mwEmbed/mwEmbedFrame.php\",\n" +
                "    \"partnerId\": \"\",\n" +
                "    \"uiConfId\": \"32626752\"\n" +
                //"    \"entryId\": \"374130\"\n" +
                //                 "    \"entryId\": \"384080\"\n" +



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
                // "    \"tvpapiGetLicensedLinks.plugin\": true,\n" +
                "    \"TVPAPIBaseUrl\": \"http://tvpapi-as.ott.kaltura.com/v3_4/gateways/jsonpostgw.aspx?m=\"\n" +
                "  }\n" +
                "}\n";
        return json;
    }

    private void prepareCache() {
        KPPlayerConfig prefetchConfig = null;
        try {
            prefetchConfig = KPPlayerConfig.fromJSONObject(new JSONObject(getPrefetchjJson()));
            prefetchConfig.getCacheConfig().addIncludePattern(Pattern.compile("https://voot-kaltura.s3.amazonaws.com/voot-watermark.png", Pattern.LITERAL));
            prefetchConfig.getCacheConfig().addIncludePattern(".*kwidget-ps/ps/modules/viacom/resources/.+/images.*");

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String vootDeploy = prefetchConfig.getServerURL().replaceAll("/mwEmbed/mwEmbedFrame.php","");
        //String vootDeploy = "http://player-as.ott.kaltura.com/225/v2.48.1_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.4";
//?2016-11-07T12:03:20Z
        String ext = "?2016-11-07T12:03:20Z";//?2016-10-06T11:13:20Z";//?2016-10-13T15:43:20Z
        //String ext = "?2016-10-13T15:43:20Z";
        //String ext = "?2016-11-07T12:03:20Z";//"?2016-10-13T15:43:20Z";
        final String nextPng = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Next.png" + ext;
        final String prvPng  = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Previous.png"+ ext;
        final String kidsPlay  = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Play.png" + ext;
        final String kidsPause = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Pause.png" + ext;
        final String adultPlay  = vootDeploy +  "/kwidget-ps/ps/modules/viacom/resources/Player_Adult/images/Play.png"+ ext;
        final String adultPause = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Adult/images/Pause.png"+ ext;
        final String watermark  = "https://voot-kaltura.s3.amazonaws.com/voot-watermark.png";

        List<Uri> itemsToCache = new ArrayList<>();
        itemsToCache.add(Uri.parse(nextPng));
        itemsToCache.add(Uri.parse(prvPng));
        itemsToCache.add(Uri.parse(kidsPlay));
        itemsToCache.add(Uri.parse(kidsPause));
        itemsToCache.add(Uri.parse(adultPlay));
        itemsToCache.add(Uri.parse(adultPause));
        itemsToCache.add(Uri.parse(watermark));
        KPrefetchListener prefetchListener = new KPrefetchListener() {
            @Override
            public void onPrefetchFinished() {
                Log.d(TAG, "XXXXXXXX onPrefetchFinished");
            }
        };
        PlayerViewController.prefetchPlayerResources(prefetchConfig, itemsToCache, prefetchListener, this);
    }


    private PlayerViewController getPlayer(KPPlayerConfig config) {

        if (mPlayer == null) {
            mPlayer = new PlayerViewController(this);
            mPlayerContainer.addView(mPlayer, new ViewGroup.LayoutParams(mPlayerContainer.getLayoutParams()));

            mPlayer.loadPlayerIntoActivity(this);

            mPlayer.initWithConfiguration(config);

            mPlayer.setCustomSourceURLProvider(mSourceURLProvider);

            mPlayer.setOnKPErrorEventListener(this);
            mPlayer.setOnKPStateChangedEventListener(this);

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
