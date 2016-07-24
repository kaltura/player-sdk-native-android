package com.kaltura.ccplayerdemo;

import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.casting.KCastDevice;
import com.kaltura.playersdk.casting.KCastFactory;
import com.kaltura.playersdk.events.KPErrorEventListener;
import com.kaltura.playersdk.events.KPPlayheadUpdateEventListener;
import com.kaltura.playersdk.events.KPStateChangedEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.interfaces.KCastProvider;
import com.kaltura.playersdk.interfaces.KMediaControl;
import com.kaltura.playersdk.tracks.KTrackActions;
import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;
import com.kaltura.playersdk.types.KPError;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KTrackActions.EventListener, KPErrorEventListener, KPPlayheadUpdateEventListener, KPStateChangedEventListener /*--deprecated, KPEventListener*/ {
    private static final String TAG = "CCPlayerDemo";

    private static final int MENU_GROUP_TRACKS = 1;
    private static final int TRACK_DISABLED = -1;
    private static final int ID_OFFSET = 2;

    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;
    private ArrayList<KCastDevice> mRouterInfos = new ArrayList<>();
    private boolean isCCActive = false;
    private Button ccButton;
    private ImageButton mMediaRouteButtonDiscon;
    private ImageButton mMediaRouteButtonCon;
    private ImageButton mStreamButton;
    private Button mLoadPlayer;
    private Button mStopCasting;
    private
    KCastProvider mCastProvider;

    private boolean enableBackgroundAudio;
    private Button videoButton;
    private Button audioButton;
    private Button textButton;

    private RelativeLayout.LayoutParams defaultVideoViewParams;
    private int defaultScreenOrientationMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        android.support.v7.app.ActionBar mActionBar = getSupportActionBar();
        LayoutInflater mInflater = LayoutInflater.from(this);
        View mCustomView = mInflater.inflate(R.layout.action_bar, null);
        if (mActionBar != null) {
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(true);
            mActionBar.setCustomView(mCustomView);
            mActionBar.setDisplayShowCustomEnabled(true);
        }




        mMediaRouteButtonDiscon = (ImageButton) mCustomView.findViewById(R.id.route_button_discon);
        mMediaRouteButtonDiscon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presentCCDevices();
            }
        });

        mMediaRouteButtonCon = (ImageButton)mCustomView.findViewById(R.id.route_button_con);
        mMediaRouteButtonCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCastProvider.disconnectFromDevcie();
            }
        });

        mStreamButton = (ImageButton) mCustomView.findViewById(R.id.stream);
        mStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setId(v.getId() != 0 ? 0 : 1);
                mStreamButton.setBackgroundResource((v.getId() != 0) ? R.drawable.stream_icon_normal : R.drawable.stream_icon);
                mPlayer.setCastProvider(mCastProvider);
            }
        });

        mLoadPlayer = (Button) findViewById(R.id.loadPlayer);
        mLoadPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPlayer();
            }
        });

        mStopCasting = (Button) findViewById(R.id.stopCasting);
        mStopCasting.setVisibility(View.INVISIBLE);
        mStopCasting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCastProvider.disconnectFromDevcie();
                mStopCasting.setVisibility(View.INVISIBLE);
            }
        });

        videoButton = (Button) findViewById(R.id.video_controls);
        audioButton = (Button) findViewById(R.id.audio_controls);
        textButton = (Button) findViewById(R.id.text_controls);
        mPlayPauseButton = (Button)findViewById(R.id.button);
        if (mPlayPauseButton != null) {
            mPlayPauseButton.setOnClickListener(this);
            mPlayPauseButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    getPlayer().getMediaControl().replay();
                    return true;
                }
            });
        }

        mSeekBar = (SeekBar)findViewById(R.id.seekBar);
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(this);
        }
        ccButton = (Button)findViewById(R.id.ccButto);
        ccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                presentCCDevices();
                startCC();
            }
        });
        onCreate = true;


    }


    private void startCC() {

        mCastProvider = KCastFactory.createCastProvider();
        mCastProvider.setKCastProviderListener(new KCastProvider.KCastProviderListener() {
            @Override
            public void onCastMediaRemoteControlReady(KCastMediaRemoteControl castMediaRemoteControl) {
                mCastProvider.getCastMediaRemoteControl().addListener(new KCastMediaRemoteControl.KCastMediaRemoteControlListener() {
                    @Override
                    public void onCastMediaProgressUpdate(long currentPosition) {

                    }

                    @Override
                    public void onCastMediaStateChanged(KCastMediaRemoteControl.State state) {

                    }
                });
            }

            @Override
            public void onDeviceCameOnline(KCastDevice device) {
                Log.d(TAG, "onDeviceCameOnline deviceName = " + device.getRouterName());
                mRouterInfos.add(device);
                if (mMediaRouteButtonDiscon.getVisibility() == View.INVISIBLE && mRouterInfos.size() > 0) {
                    mMediaRouteButtonDiscon.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onDeviceWentOffline(KCastDevice device) {
                mRouterInfos.remove(device);
                if (mRouterInfos.size() == 0) {
                    mMediaRouteButtonDiscon.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onDeviceConnected() {
                mMediaRouteButtonDiscon.setVisibility(View.INVISIBLE);
                mMediaRouteButtonCon.setVisibility(View.VISIBLE);
                mStopCasting.setVisibility(View.VISIBLE);
            }

            @Override
            public void onDeviceDisconnected() {
                mMediaRouteButtonDiscon.setVisibility(View.VISIBLE);
                mMediaRouteButtonCon.setVisibility(View.INVISIBLE);
                mStopCasting.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onDeviceFailedToConnect(KPError error) {

            }

            @Override
            public void onDeviceFailedToDisconnect(KPError error) {

            }
        });
        mCastProvider.startScan(getApplicationContext(), "C43947A1");
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            if (mPlayer != null) {
                mPlayer.loadPlayerIntoActivity(this);
                //KPPlayerConfig config = new KPPlayerConfig("http://10.0.0.11/html5.kaltura/mwEmbed/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
                //KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.43.rc11/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
                KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.46.rc4/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
                config.addConfig("closedCaptions.plugin", "true");
                config.addConfig("sourceSelector.plugin", "true");
                config.addConfig("sourceSelector.displayMode", "bitrate");
                config.addConfig("audioSelector.plugin", "true");
                config.addConfig("closedCaptions.showEmbeddedCaptions", "true");


                config.addConfig("chromecast.plugin", "true");
                config.addConfig("chromecast.applicationID", "C43947A1");
                config.addConfig("chromecast.useKalturaPlayer", "true");
                config.addConfig("chromecast.receiverLogo", "true");

                mPlayer.initWithConfiguration(config);

                mPlayPauseButton.setText("Pause");
                mPlayer.getMediaControl().seek(100, new KMediaControl.SeekCallback() {
                    @Override
                    public void seeked(long milliSeconds) {

                    }
                });

                if (mCastProvider != null) {
                    mPlayer.setCastProvider(mCastProvider);
                }

                mPlayer.setOnKPErrorEventListener(this);
                mPlayer.setOnKPPlayheadUpdateEventListener(this);
                //mPlayer.setOnKPFullScreenToggeledEventListener(this);
                mPlayer.setOnKPStateChangedEventListener(this);
                mPlayer.addKPlayerEventListener("onEnableKeyboardBinding", "eventID", new PlayerViewController.EventListener() {
                    @Override
                    public void handler(String eventName, String params) {

                    }
                });

                /****FOR TRACKS****/
                //// Tracks on Web supported only from 2.44
                //// if TracksEventListener  is removed the tracks will be pushed to the web layer o/w app controled via
                ////onTracksUpdate and the mPlayer.getTrackManager() methodes
                //mPlayer.setTracksEventListener(this);
            }


        }
        return mPlayer;
    }

    private void presentCCDevices() {
        final ArrayList<KCastDevice> devices = mCastProvider.getDevices();
        final String[] items = new String[devices.size()];
        for (int i = 0; i < items.length; i++   ) {
            items[i] = devices.get(i).getRouterName();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Make your selection");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                // Do something with the selection
                mCastProvider.connectToDevice(devices.get(item));
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
        if (state == KPlayerState.PAUSED && playerViewController.getCurrentPlaybackTime() > 0) {
//            findViewById(R.id.replay).setVisibility(View.VISIBLE);

        } else if (state == KPlayerState.PLAYING) {
//            findViewById(R.id.replay).setVisibility(View.INVISIBLE);

        }
        switch (state) {
            case PAUSED:
                mPlayPauseButton.setText("Play");
                break;
            case PLAYING:
                mPlayPauseButton.setText("Pause");
                break;
            case READY:
                mStreamButton.setVisibility(View.VISIBLE);
                break;
        }
    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        Log.d(TAG, "onKPlayerError Error Received:" + error.getErrorMsg());
    }


//    @Override
//    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
//        Log.d(TAG, "onKPlayerFullScreenToggeled isFullscreen " + isFullscreen);
//    }


    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, long currentTime) {
        mSeekBar.setProgress((int) (currentTime / playerViewController.getDurationSec() * 100));
        Log.d(TAG, "onKPlayerPlayheadUpdate currentTime " + currentTime);
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
