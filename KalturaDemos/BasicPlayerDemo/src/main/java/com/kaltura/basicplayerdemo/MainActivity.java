package com.kaltura.basicplayerdemo;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
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

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.casting.KCastRouterManagerListener;
import com.kaltura.playersdk.casting.KRouterInfo;
import com.kaltura.playersdk.events.KPErrorEventListener;
import com.kaltura.playersdk.events.KPPlayheadUpdateEventListener;
import com.kaltura.playersdk.events.KPStateChangedEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.tracks.KTrackActions;
import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.types.KPError;
import com.kaltura.playersdk.tracks.TrackType;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

//<<<<<<< HEAD
//public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, /*KPFullScreenToggeledEventListener,*/ KPPlayheadUpdateEventListener,KPErrorEventListener,KPStateChangedEventListener {
//=======
public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KTrackActions.EventListener, KPErrorEventListener, KPPlayheadUpdateEventListener, KPStateChangedEventListener {
//>>>>>>> FEM-385_Tracks
    private static final String TAG = "BasicPlayerDemo";

    private static final int MENU_GROUP_TRACKS = 1;
    private static final int TRACK_DISABLED = -1;
    private static final int ID_OFFSET = 2;

    private Button mPlayPauseButton;
//    private Button ccButton;
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
        mPlayPauseButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                getPlayer().getMediaControl().replay();
                return true;
            }
        });
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


            KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.43.rc11/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
            //KPPlayerConfig config = new KPPlayerConfig("http://192.168.1.10/html5.kaltura/mwEmbed/mwEmbedFrame.php", "12905712", "243342").setEntryId("0_uka1msg4");
            //KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/branches/master/mwEmbedFrame.php", "12905712", "243342").setEntryId("0_uka1msg4");
            config.addConfig("autoPlay", "true");
            
            config.addConfig("closedCaptions.plugin", "true");
            config.addConfig("sourceSelector.plugin", "true");
            config.addConfig("sourceSelector.displayMode", "bitrate");
            config.addConfig("audioSelector.plugin", "true");
            config.addConfig("closedCaptions.showEmbeddedCaptions", "true");

            
           // config.setAutoPlay(true);
            mPlayPauseButton.setText("Pause");

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
//<<<<<<< HEAD
            //mPlayer.addEventListener(this);
            mPlayer.setOnKPErrorEventListener(this);
            mPlayer.setOnKPPlayheadUpdateEventListener(this);
            //mPlayer.setOnKPFullScreenToggeledEventListener(this);
            mPlayer.setOnKPStateChangedEventListener(this);
//=======
//            mPlayer.addEventListener(this);

            //// Tracks on Web supported only from 2.44
            //// if TracksEventListener  is removed the tracks will be pushed to the web layer o/w app controled via
            ////onTracksUpdate and the mPlayer.getTrackManager() methodes

            //mPlayer.setTracksEventListener(this);

//>>>>>>> FEM-385_Tracks
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
            mPlayer.releaseAndSavePosition(true);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        if (onCreate) {
            onCreate = false;
        }
        if (mPlayer != null) {
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
        if (v.getId() != R.id.replay) {
            if (mPlayPauseButton.getText().equals("Play")) {
                mPlayPauseButton.setText("Pause");
                getPlayer().getMediaControl().start();
            } else {
                mPlayPauseButton.setText("Play");
                getPlayer().getMediaControl().pause();
            }
        } else {
            mPlayer.getMediaControl().replay();
            mPlayPauseButton.setText("Pause");
        }

    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            float progressInPercent = progress / 100f;
            float seekVal = (float) (progressInPercent * mPlayer.getDurationSec());
            getPlayer().getMediaControl().seek(seekVal);
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
        Log.d(TAG, "onKPlayerStateChanged state " + state.name());
        if (state == KPlayerState.PAUSED && playerViewController.getCurrentPlaybackTime() > 0) {
//            findViewById(R.id.replay).setVisibility(View.VISIBLE);
            mPlayPauseButton.setText("Play");
        } else if (state == KPlayerState.PLAYING) {
//            findViewById(R.id.replay).setVisibility(View.INVISIBLE);
            mPlayPauseButton.setText("Pause");
        }
        else if (state == KPlayerState.READY){

        }
    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, long currentTime) {
        mSeekBar.setProgress((int) (currentTime / playerViewController.getDurationSec() * 100));
        Log.d(TAG, "onKPlayerPlayheadUpdate currentTime " + currentTime);
    }

//    @Override
//    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
//        Log.d(TAG, "onKPlayerFullScreenToggeled isFullscreen " + isFullscreen);
//    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        Log.d(TAG, "onKPlayerError Error Received:" + error.getErrorMsg());
    }

    private void configurePopupWithTracks(PopupMenu popup,
                                          final PopupMenu.OnMenuItemClickListener customActionClickListener,
                                          final TrackType trackType) {
        int trackCount = 0;
        if (mPlayer == null || mPlayer.getTrackManager() == null) {
            return;
        }
        if (TrackType.AUDIO.equals(trackType)) {
            trackCount = mPlayer.getTrackManager().getAudioTrackList().size();
        }else if (TrackType.TEXT.equals(trackType)) {
            trackCount = mPlayer.getTrackManager().getTextTrackList().size();
        } else if (TrackType.VIDEO.equals(trackType)) {
             trackCount = mPlayer.getTrackManager().getVideoTrackList().size();
        }
        if (trackCount <= 0) {
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
        menu.add(MENU_GROUP_TRACKS, TRACK_DISABLED + ID_OFFSET, Menu.NONE, R.string.off);

        for (int i = 0; i < trackCount; i++) {

            if (TrackType.AUDIO.equals(trackType)) {
                menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE,
                        mPlayer.getTrackManager().getAudioTrackList().get(i).trackLabel);
            }else if (TrackType.TEXT.equals(trackType)) {
                menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE,
                        mPlayer.getTrackManager().getTextTrackList().get(i).trackLabel);
            } else if (TrackType.VIDEO.equals(trackType)) {
                menu.add(MENU_GROUP_TRACKS, i + ID_OFFSET, Menu.NONE,
                        mPlayer.getTrackManager().getVideoTrackList().get(i).trackLabel);
            }

        }
        menu.setGroupCheckable(MENU_GROUP_TRACKS, true, true);
        menu.findItem(mPlayer.getTrackManager().getCurrentTrack(trackType).index + ID_OFFSET).setChecked(true);
    }

    private boolean onTrackItemClick(MenuItem item, TrackType type) {
        if (mPlayer == null || item.getGroupId() != MENU_GROUP_TRACKS) {
            return false;
        }

        int switchTrackIndex = item.getItemId() - ID_OFFSET;
        Log.d(TAG, "onTrackItemClick switchTrackIndex: " + switchTrackIndex);
        mPlayer.getTrackManager().switchTrack(type, switchTrackIndex);

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



    @Override
    public void onTracksUpdate(KTrackActions tracksManager) {
        if (mPlayer != null) {
            updateButtonVisibilities();
            Log.e(TAG, "----------------");
            for (TrackFormat track : mPlayer.getTrackManager().getAudioTrackList()) {
                Log.d(TAG, track.toString());
            }
            Log.e(TAG, "----------------");
            for (TrackFormat track : mPlayer.getTrackManager().getVideoTrackList()) {
                Log.e(TAG, track.toString());
            }
            Log.e(TAG, "----------------");
            for (TrackFormat track : mPlayer.getTrackManager().getTextTrackList()) {
                Log.d(TAG, track.toString());
            }
            Log.e(TAG, "----------------");
        }
    }

    private void updateButtonVisibilities() {
        if (mPlayer != null) {
            if (mPlayer.getTrackManager() != null) {
                videoButton.setVisibility((mPlayer.getTrackManager().getVideoTrackList().size() > 0) ? View.VISIBLE : View.GONE);
                audioButton.setVisibility((mPlayer.getTrackManager().getAudioTrackList().size() > 0) ? View.VISIBLE : View.GONE);
                textButton.setVisibility((mPlayer.getTrackManager().getTextTrackList().size() > 0) ? View.VISIBLE : View.GONE);
            }
        }
    }

}
