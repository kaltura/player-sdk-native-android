package com.kaltura.basicplayerdemo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.casting.KCastRouterManagerListener;
import com.kaltura.playersdk.casting.KRouterInfo;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;
import com.kaltura.playersdk.types.TrackType;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {
    private static final String TAG = "BasicPlayerDemo";

    private static final int MENU_GROUP_TRACKS = 1;
    private static final int ID_OFFSET = 2;

    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;
    private ArrayList<KRouterInfo> mRouterInfos = new ArrayList<>();
    private boolean isCCActive = false;
    private Button ccButton;
    private boolean enableBackgroundAudio;
    private Button videoButton;
    private Button audioButton;
    private Button textButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        videoButton = (Button) findViewById(R.id.video_controls);
        audioButton = (Button) findViewById(R.id.audio_controls);
        textButton = (Button) findViewById(R.id.text_controls);
        mPlayPauseButton = (Button)findViewById(R.id.button);
        mPlayPauseButton.setOnClickListener(this);
        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        mSeekBar.setOnSeekBarChangeListener(this);
        ccButton = (Button)findViewById(R.id.ccButto);
        ccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentCCDevices();
            }
        });
        onCreate = true;
        getPlayer();
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            mPlayer.loadPlayerIntoActivity(this);

            KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/branches/master/mwEmbedFrame.php", "12905712", "243342").setEntryId("0_uka1msg4");
            config.addConfig("autoPlay", "true");
            
            config.addConfig("closedCaptions.plugin", "true");
            config.addConfig("sourceSelector.plugin", "true");
            config.addConfig("sourceSelector.displayMode", "bitrate");
            config.addConfig("audioSelector.plugin", "true");
            config.addConfig("closedCaptions.showEmbeddedCaptions", "true");

            config.addConfig("chromecast.plugin", "true");
            config.addConfig("chromecast.applicationID", "5247861F");
            config.addConfig("chromecast.useKalturaPlayer", "true");
            config.addConfig("chromecast.receiverLogo", "true");
            mPlayer.getKCastRouterManager().enableKalturaCastButton(false);
            mPlayer.addKPlayerEventListener("onEnableKeyboardBinding", "someId", new PlayerViewController.EventListener() {
                @Override
                public void handler(String eventName, String params) {
                    Log.d(TAG, eventName);
                }
            });
            mPlayer.getKCastRouterManager().setCastRouterManagerListener(new KCastRouterManagerListener() {
                @Override
                public void onCastButtonClicked() {
                    if (!isCCActive) {
                        presentCCDevices();
                    } else {
                        mPlayer.getKCastRouterManager().disconnect();
                    }
                }

                @Override
                public void onApplicationStatusChanged(boolean isConnected) {
                    isCCActive = isConnected;
                }

                @Override
                public void shouldPresentCastIcon(boolean didDetect) {
                    ccButton.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAddedCastDevice(KRouterInfo info) {
                    mRouterInfos.add(info);
                }

                @Override
                public void onRemovedCastDevice(KRouterInfo info) {
                    mRouterInfos.remove(info);
                }
            });
            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(this);

        }
        return mPlayer;
    }

    private void presentCCDevices() {
        final String[] items = new String[mRouterInfos.size()];
        for (int i = 0; i < items.length; i++   ) {
            items[i] = mRouterInfos.get(i).getRouterName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make your selection");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
//                mDoneButton.setText(items[item]);
                mPlayer.getKCastRouterManager().connectDevice(mRouterInfos.get(item).getRouterId());
            }
        });
        AlertDialog alert = builder.create();
        alert.show();

    }


    private RelativeLayout getPlayerContainer() {
        return (RelativeLayout)findViewById(R.id.playerContainer);
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
            if (powerManager.isScreenOn()) {
                mPlayer.releaseAndSavePosition(true);
            }

        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        if (!powerManager.isScreenOn()) {
            mPlayer.saveState();
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (onCreate) {
            onCreate = false;
        }
        mPlayer.resumePlayer();
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
        else if (state == KPlayerState.READY){
            if (mPlayer != null) {
                updateButtonVisibilities();
                Log.d(TAG, "aud tracks num = " + mPlayer.getTracks().getTracksList(TrackType.AUDIO).size());
                Log.d(TAG, "vid tracks num = " + mPlayer.getTracks().getTracksList(TrackType.VIDEO).size());
                Log.d(TAG, "text tracks num = " + mPlayer.getTracks().getTracksList(TrackType.TEXT).size());
                for (String track : mPlayer.getTracks().getTracksList(TrackType.AUDIO)) {
                    Log.d(TAG, track);
                }
                Log.d(TAG, "----------------");
                for (String track : mPlayer.getTracks().getTracksList(TrackType.VIDEO)) {
                    Log.d(TAG, track);
                }
                Log.d(TAG, "----------------");
                for (String track : mPlayer.getTracks().getTracksList(TrackType.TEXT)) {
                    Log.d(TAG, track);
                }
                Log.d(TAG, "----------------");
                Log.d(TAG, "curr audindex = " + mPlayer.getTracks().getCurrentTrackIndex(TrackType.AUDIO));
                //mPlayer.getTracks().switchTrack(TrackType.AUDIO,2);
                Log.d(TAG, "curr aud index = " + mPlayer.getTracks().getCurrentTrackIndex(TrackType.AUDIO));
                Log.d(TAG, "curr vid index = " + mPlayer.getTracks().getCurrentTrackIndex(TrackType.VIDEO));
                //mPlayer.getTracks().switchTrack(TrackType.VIDEO,2);
                Log.d(TAG, "curr vid index = " + mPlayer.getTracks().getCurrentTrackIndex(TrackType.VIDEO));
                Log.d(TAG, "curr text index = " + mPlayer.getTracks().getCurrentTrackIndex(TrackType.TEXT));
                //mPlayer.getTracks().switchTrack(TrackType.TEXT,1);
                Log.d(TAG, "curr text index = " + mPlayer.getTracks().getCurrentTrackIndex(TrackType.TEXT));
            }
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

    private void configurePopupWithTracks(PopupMenu popup,
                                          final PopupMenu.OnMenuItemClickListener customActionClickListener,
                                          final TrackType trackType) {
        if (mPlayer == null || mPlayer.getTracks() == null) {
            return;
        }
        int trackCount = mPlayer.getTracks().getTrackCount(trackType);
        if (trackCount == 0) {
            return;
        }
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                return (customActionClickListener != null
                        && customActionClickListener.onMenuItemClick(item))
                        || onTrackItemClick(item, trackType);
            }
        });
        Menu menu = popup.getMenu();
        // ID_OFFSET ensures we avoid clashing with Menu.NONE (which equals 0).
        menu.add(MENU_GROUP_TRACKS, ExoplayerWrapper.TRACK_DISABLED + ID_OFFSET, Menu.NONE, R.string.off);
        for (int i = 0; i < trackCount; i++) {
            menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE,
                    mPlayer.getTracks().getTrackName(mPlayer.getTracks().getTrackFormat(trackType, i)));
        }
        menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true);
        menu.findItem(mPlayer.getTracks().getCurrentTrackIndex(trackType) + ID_OFFSET).setChecked(true);
    }

    private boolean onTrackItemClick(MenuItem item, TrackType type) {
        if (mPlayer == null || item.getGroupId() != MENU_GROUP_TRACKS) {
            return false;
        }
        mPlayer.getTracks().switchTrack(type, item.getItemId() - ID_OFFSET);
        return true;
    }

    public void showVideoPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        configurePopupWithTracks(popup, null,TrackType.VIDEO);
        popup.show();
    }

    public void showAudioPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, Menu.NONE, Menu.NONE, R.string.enable_background_audio);
        final MenuItem backgroundAudioItem = menu.findItem(0);
        backgroundAudioItem.setCheckable(true);
        backgroundAudioItem.setChecked(enableBackgroundAudio);
        PopupMenu.OnMenuItemClickListener clickListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item == backgroundAudioItem) {
                    enableBackgroundAudio = !item.isChecked();
                    return true;
                }
                return false;
            }
        };
        configurePopupWithTracks(popup, clickListener, TrackType.AUDIO);
        popup.show();
    }

    public void showTextPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        configurePopupWithTracks(popup, null, TrackType.TEXT);
        popup.show();
    }

    private void updateButtonVisibilities() {
        videoButton.setVisibility((mPlayer.getTracks().getTrackCount(TrackType.VIDEO) > 0) ? View.VISIBLE : View.GONE);
        audioButton.setVisibility((mPlayer.getTracks().getTrackCount(TrackType.AUDIO) > 0) ? View.VISIBLE : View.GONE);
        textButton.setVisibility((mPlayer.getTracks().getTrackCount(TrackType.TEXT) > 0) ? View.VISIBLE : View.GONE);
    }
}
