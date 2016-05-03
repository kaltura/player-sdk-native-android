package com.kaltura.basicottplayerdemo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {

    public static final String OTT_TOKEN_URL = "http://tvpapi-preprod.ott.kaltura.com/v3_7/gateways/jsonpostgw.aspx?m=SignIn";
    public static final String OTT_REST_JSON = "{\"initObj\":{\"Locale\":{\"LocaleLanguage\":\"en\",\"LocaleCountry\":null,\"LocaleDevice\":null,\"LocaleUserState\":\"Unknown\"},\"SiteGuid664\":\"0\",\"DomainID\":\"0\",\"Platform\":\"Cellular\",\"ApiUser\":\"tvpapi_198\",\"ApiPass\":\"11111\",\"UDID\":\"885bc1e13552233f\",\"Token\":\"\"},\"userName\":\"vladi@yahoo.com\",\"password\":\"123456\"}";
    public static final String OTT_FRAME_URL = "http://player-227562931.eu-west-1.elb.amazonaws.com/v2.42.rc12/mwEmbed/mwEmbedFrame.php";
    
    private static final String TAG = "BasicOTTPlayerDemo";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;
    private boolean shouldResume = false;
    private boolean isOtt = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        mPlayPauseButton = (Button) findViewById(R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        mSeekBar = (SeekBar) findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        onCreate = true;
        String androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        if (!isOtt) {
            getOvpPlayer();
        } else {
            new MyAsyncTask().execute(OTT_REST_JSON);
        }
    }

    private PlayerViewController getOvpPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);
            KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");
            config.addConfig("controlBarContainer.plugin", "false");
            config.addConfig("topBarContainer.plugin", "false");
            config.addConfig("largePlayBtn.plugin", "false");
            config.addConfig("autoPlay", "true");
            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(this);
        }
        return mPlayer;
    }

    private PlayerViewController getOttPlayer(String json) {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);
            //KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271").setEntryId("1_o426d3i4");

            KPPlayerConfig config = new KPPlayerConfig(OTT_FRAME_URL, "8413352", ""/*"1774581"*/).setEntryId("1_mphei4ku"); //ITAN1
            config.setEntryId("258464");//("258656");//assetId
            config.addConfig("UIConfig", "8413352");//assetId
            config.addConfig("mediaProxy.mediaPlayFrom", "" + 0);
            config.addConfig("autoPlay", "true");
            config.addConfig("proxyData", json);

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
                if (isOtt) {
                    mPlayer.sendNotification("doPlay", null);
                }else {
                    getOvpPlayer().sendNotification("doPlay", null);
                }
            } else {
                mPlayPauseButton.setText("Play");
                if (isOtt) {
                    mPlayer.sendNotification("doPause", null);
                } else {
                    getOvpPlayer().sendNotification("doPause", null);
                }

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
            mPlayer.sendNotification("doSeek", Float.toString(seekVal));

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

    @Override
    public void onBackPressed(){
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public static HttpResponse makeRequest(String uri, String json) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(json));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            return new DefaultHttpClient().execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private class MyAsyncTask extends AsyncTask<String, Integer, String> {
        String token;
        @Override
        protected String doInBackground(String... params) {
            // TODO Auto-generated method stub
            return postData(params[0]);

        }

        @Override
        protected void onPostExecute(String result) {
            if ("".equals(result)) {
                Toast.makeText(getApplicationContext(), "Error, Could not get OTT token..." , Toast.LENGTH_LONG).show();
                return;
            }
            getOttPlayer(result);
        }
        @Override
        protected void onProgressUpdate(Integer... progress) {

        }

        public String postData(String valueIWantToSend) {
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(OTT_TOKEN_URL);

            try {
                // Add your data
                httppost.setEntity(new StringEntity(valueIWantToSend));
                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);

                Header[] headers = response.getAllHeaders();//response.getEntity().getContent()));

                for (Header header : headers) {
                    String headerStr = header.getValue();
                    if ("access_token".equals(header.getName())){
                        String tokenVal = header.getValue();
                        String token =  tokenVal.substring(0, tokenVal.indexOf("|"));
                        UUID uuid = null;
                        try {
                            String androidId = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                                    Settings.Secure.ANDROID_ID);
                            uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        String json = "{\"initObj\":{\"Locale\":{\"LocaleLanguage\":\"en\",\"LocaleCountry\":null,\"LocaleDevice\":null,\"LocaleUserState\":\"Unknown\"},\"SiteGuid\":\"716158\",\"DomainID\":\"354531\",\"Platform\":\"Cellular\",\"ApiUser\":\"tvpapi_198\",\"ApiPass\":\"11111\",\"UDID\":\"" + uuid + "\",\"Token\":\"" + token +  "\"},\"mediaType\":420,\"withDynamic\":false,\"MediaID\":\"258464\",\"iMediaID\":\"258464\"}";
                        return json;
                    }
                    continue;
                }
            } catch (ClientProtocolException e) {
                // TODO Auto-generated catch block
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

    }
}
