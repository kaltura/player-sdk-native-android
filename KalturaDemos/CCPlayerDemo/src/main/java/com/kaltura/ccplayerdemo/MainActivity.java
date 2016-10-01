package com.kaltura.ccplayerdemo;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastState;
import com.google.android.gms.cast.framework.CastStateListener;
import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.casting.KCastFactory;
import com.kaltura.playersdk.casting.KCastProviderV3Impl;
import com.kaltura.playersdk.events.KPErrorEventListener;
import com.kaltura.playersdk.events.KPPlayheadUpdateEventListener;
import com.kaltura.playersdk.events.KPStateChangedEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.interfaces.KCastProvider;
import com.kaltura.playersdk.tracks.KTrackActions;
import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;
import com.kaltura.playersdk.types.KPError;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KTrackActions.EventListener, KPErrorEventListener, KPPlayheadUpdateEventListener, KPStateChangedEventListener /*--deprecated, KPEventListener*/ {
    private static final String TAG = "CCPlayerDemo";

    private static final int MENU_GROUP_TRACKS = 1;
    private static final int TRACK_DISABLED = -1;
    private static final int ID_OFFSET = 2;
//  private static final String CCApplicationID = "48A28189"; //"276999A7"; //Old Id C43947A1

    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;

    private MediaRouteButton mMediaRouteButton;

    private CastStateListener mCastStateListener;

    private Button mStreamButton;
    private Button mLoadPlayer;
    private Button mAddCaptionsBtn;
    private Button mChangeMediaBtn;
    KCastProviderV3Impl mCastProvider;

    private boolean enableBackgroundAudio;
    private Button videoButton;
    private Button audioButton;
    private Button textButton;
    private static int changeLangIdx = 0;
    private static int changeMediaIdx = 0;
    int firstCastDeviceState = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        mCastStateListener = new CastStateListener() {
            @Override
            public void onCastStateChanged(int newState) {
                LOGD(TAG, "onCastStateChanged newState:" + newState);
                if (newState == CastState.NO_DEVICES_AVAILABLE) {
                    LOGD(TAG, "NO_DEVICES_AVAILABLE");
                    mAddCaptionsBtn.setVisibility(View.INVISIBLE);
                    mStreamButton.setVisibility(View.INVISIBLE);
//                    if (mCastProvider != null) {
//                        mCastProvider.disconnectFromCastDevice();
//                    } else {
//                        //getPlayer().sendNotification("hideConnectingMessage", "");
                    if (mPlayer != null) {
                        getPlayer().sendNotification("chromecastDeviceDisConnected", "");
                    }
//                    }
                    //showIntroductoryOverlay();
                } else if (newState == CastState.CONNECTING) {
                    LOGD(TAG, "CONNECTING");
                } else if (newState == CastState.CONNECTED) {
                    LOGD(TAG, "CONNECTED");
                    if (firstCastDeviceState == -1) {
                        firstCastDeviceState = CastState.NOT_CONNECTED;
                    }
                    mStreamButton.setVisibility(View.VISIBLE);
                } else if (newState == CastState.NOT_CONNECTED) {
                    LOGD(TAG, "NOT_CONNECTED");
                    if (firstCastDeviceState == -1) {
                        firstCastDeviceState = CastState.NOT_CONNECTED;
                    }
                    mAddCaptionsBtn.setVisibility(View.INVISIBLE);
                    mStreamButton.setVisibility(View.INVISIBLE);
//                    if (mCastProvider != null) {
//                        mCastProvider.disconnectFromCastDevice();
//                    } else {
//                        //getPlayer().sendNotification("hideConnectingMessage", "");
                    if (mPlayer != null) {
                        getPlayer().sendNotification("chromecastDeviceDisConnected", "");
                    }
//                    }
                }
            }
        };
        mMediaRouteButton = (MediaRouteButton) findViewById(R.id.media_route_button);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), mMediaRouteButton);

        mCastProvider = (KCastProviderV3Impl) KCastFactory.createCastProvider(MainActivity.this, getString(R.string.app_id), getString(R.string.cast_logo_url));
        mCastProvider.addCastStateListener(mCastStateListener);


        mStreamButton = (Button) findViewById(R.id.stream_to_cc);
        mStreamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer == null) {
                    return;
                }
                mPlayer.setCastProvider(mCastProvider);
                mCastProvider.setKCastProviderListener(new KCastProvider.KCastProviderListener() {
                    @Override
                    public void onCastMediaRemoteControlReady(KCastMediaRemoteControl castMediaRemoteControl) {
                        LOGD(TAG, "onCastMediaRemoteControlReady hasMediaSession = " + castMediaRemoteControl.hasMediaSession(false));
                        mAddCaptionsBtn.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCastReceiverError(String errorMsg , int errorCode) {
                        LOGE(TAG, "onCastReceiverError errorMsg = " + errorMsg + " errorCode = "  + errorCode);
                    }
                });
            }

        });

        mLoadPlayer = (Button) findViewById(R.id.loadPlayer);
        mLoadPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPlayer();
            }
        });

        mAddCaptionsBtn = (Button) findViewById(R.id.add_captions);
        mAddCaptionsBtn.setVisibility(View.INVISIBLE);
        mAddCaptionsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCastProvider != null && mCastProvider.getCastMediaRemoteControl() != null) {
                    HashMap<String, Integer> tracksHash = mCastProvider.getCastMediaRemoteControl().getTextTracks();
                    if (tracksHash == null || tracksHash.keySet() == null) {
                        return;
                    }
                    if (tracksHash.keySet().size() > 0) {
                        if (changeLangIdx % 2 == 0) {
                            if (tracksHash.containsKey("eng")) {
                                mCastProvider.getCastMediaRemoteControl().switchTextTrack(tracksHash.get("eng"));
                                for (TrackFormat tf : mPlayer.getTrackManager().getTextTrackList()) {
                                    LOGD(TAG, "getTrackFullLanguageName " + tf.getTrackFullName());
                                    LOGD(TAG, "getTrackLanguage " + tf.getTrackLanguage());
                                    LOGD(TAG, "getTrackName " + tf.getTrackName());
                                }

                                int castTextTrackIndex = mCastProvider.getCastMediaRemoteControl().getSelectedTextTrackIndex();

                                for (String castLang : mCastProvider.getCastMediaRemoteControl().getTextTracks().keySet()) {
                                    LOGD(TAG, "loop castLang  = " + castLang);
                                    if (castTextTrackIndex == mCastProvider.getCastMediaRemoteControl().getTextTracks().get(castLang)) {

                                        for (TrackFormat textTrack : mPlayer.getTrackManager().getTextTrackList()) {
                                            if ((textTrack.language).equals(castLang)) {
                                                mPlayer.getTrackManager().switchTrack(TrackType.TEXT, textTrack.index);
                                                break;
                                            }
                                        }
                                        break;
                                    }
                                }

                            } else {
                                LOGE(TAG, "lang <eng> does not exist");
                            }

                        } else {
                            mCastProvider.getCastMediaRemoteControl().switchTextTrack(0);
                            mPlayer.getTrackManager().switchTrack(TrackType.TEXT, -1);
                        }
                    }
                    changeLangIdx++;
                }
            }
        });

        mChangeMediaBtn = (Button) findViewById(R.id.change_media);
        mChangeMediaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCastProvider != null && mCastProvider.getCastMediaRemoteControl() != null) {
                    if (mPlayer != null) {
                        boolean hasSession = mCastProvider.getCastMediaRemoteControl().hasMediaSession(true);
                        LOGD(TAG,"hasMediaSession" + hasSession);
                        if (hasSession) {
                            if (changeMediaIdx % 3 == 0) {
                                mPlayer.changeMedia("1_8t7qo08r");
                            } else if (changeMediaIdx % 3 == 1) {
                                mPlayer.changeMedia("1_ng282arr");
                            } else if (changeMediaIdx % 3 == 2) {
                                mPlayer.changeMedia("1_uvmb65k7");
                            }
                            changeMediaIdx++;
                        }
                    }
                }
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
        onCreate = true;
    }

    private PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.player);
            if (mPlayer != null) {
                mPlayer.loadPlayerIntoActivity(this);
                //LOCAL - KPPlayerConfig config = new KPPlayerConfig("http://10.0.0.11/html5.kaltura/mwEmbed/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
                KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.48.rc3/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
                //KPPlayerConfig config = new KPPlayerConfig("http://192.168.160.149/html5.kaltura/mwEmbed/mwEmbedFrame.php", "15190232", "4171").setEntryId("0_nq4v8mc2");//0_nq4v8mc2

                 //KPPlayerConfig config = new KPPlayerConfig("http://qa-apache-testing-ubu-01.dev.kaltura.com", "15190232", "4171").setEntryId("0_nq4v8mc2");//0_nq4v8mc2
                //HAROLD - KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.47.1/mwEmbedFrame.php", "12905712", "243342").setEntryId("0_uka1msg4");

                //config.addConfig("debugKalturaPlayer", "true");
                config.addConfig("autoPlay", "true");
                config.addConfig("closedCaptions.plugin", "true");
                config.addConfig("sourceSelector.plugin", "true");
                config.addConfig("sourceSelector.displayMode", "bitrate");
                config.addConfig("audioSelector.plugin", "true");
                config.addConfig("closedCaptions.showEmbeddedCaptions", "true");


                config.addConfig("chromecast.plugin", "true");
                config.addConfig("chromecast.applicationID", getString(R.string.app_id));
                config.addConfig("chromecast.useKalturaPlayer", "true");
                config.addConfig("chromecast.receiverLogo", "true");

                //config.addConfig("chromecast.defaultThumbnail", "the thumbnail you want to use");
                //config.addConfig("chromecast", "{\"proxyData\":" + proxyDataReceiver + "}");  // change media Format in order to stream it to TV in higher resolution
                //config.addConfig("strings.mwe-chromecast-loading", "Loading to CC");  // Set Loading message
                //config.addConfig("chromecast.logoUrl", "Your Logo")


                //config.addConfig("topBarContainer.hover", "true");
                config.addConfig("topBarContainer.plugin", "true");

                mPlayer.initWithConfiguration(config);

                mPlayPauseButton.setText("Pause");


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


    private RelativeLayout getPlayerContainer() {
        return (RelativeLayout)findViewById(R.id.playerContainer);
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.releaseAndSavePosition(true,false);
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mCastProvider != null) {
            mCastProvider.removeCastStateListener(mCastStateListener);
        }
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
            if (mPlayer != null) {
                float progressInPercent = progress / 100f;
                float seekVal = (float) (progressInPercent * mPlayer.getDurationSec());
                getPlayer().getMediaControl().seek(seekVal);
            }
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
        LOGD(TAG, "onKPlayerError Error Received:" + error.getErrorMsg());
    }


//    @Override
//    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
//        LOGD(TAG, "onKPlayerFullScreenToggeled isFullscreen " + isFullscreen);
//    }


    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, long currentTime) {
        long currentSeconds = (int) (currentTime / 1000);
        long totalSeconds = (int) (playerViewController.getDurationSec());

        double percentage = 0;
        if (totalSeconds > 0) {
            percentage = (((double) currentSeconds) / totalSeconds) * 100;
        }
        LOGD(TAG, "onKPlayerPlayheadUpdate " +  currentSeconds + "/" + totalSeconds + " => " + (int)percentage + "%");
        mSeekBar.setProgress((int)percentage);
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
        LOGD(TAG, "onTrackItemClick switchTrackIndex: " + switchTrackIndex);
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
            LOGE(TAG, "----------------");
            for (TrackFormat track : mPlayer.getTrackManager().getAudioTrackList()) {
                LOGD(TAG, track.toString());
            }
            LOGE(TAG, "----------------");
            for (TrackFormat track : mPlayer.getTrackManager().getVideoTrackList()) {
                LOGE(TAG, track.toString());
            }
            LOGE(TAG, "----------------");
            for (TrackFormat track : mPlayer.getTrackManager().getTextTrackList()) {
                LOGD(TAG, track.toString());
            }
            LOGE(TAG, "----------------");
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

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
                menu,
                R.id.media_route_menu_item);
        return true;
    }
//    @Override
//        public boolean onCreateOptionsMenu(Menu menu) {
//            super.onCreateOptionsMenu(menu);
//            getMenuInflater().inflate(R.menu.menu, menu);
//            CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
//                    menu,
//                    R.id.media_route_menu_item);
//            mediaRouteMenuItem =
//                    CastButtonFactory.setUpMediaRouteButton(getApplicationContext(),
//                            menu,
//                            R.id.media_route_menu_item);
//            return true;
//    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        super.onCreateOptionsMenu(menu);
//        getMenuInflater().inflate(R.menu.menu, menu);
//        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(getApplicationContext(), menu,
//                R.id.media_route_menu_item);
//        mQueueMenuItem = menu.findItem(R.id.action_show_queue);
//        showIntroductoryOverlay();
//        return true;
//    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        menu.findItem(R.id.action_show_queue).setVisible(
//                (mCastSession != null) && mCastSession.isConnected());
//        return super.onPrepareOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Intent intent;
//        if (item.getItemId() == R.id.action_settings) {
//            intent = new Intent(VideoBrowserActivity.this, CastPreference.class);
//            startActivity(intent);
//        } else if (item.getItemId() == R.id.action_show_queue) {
//            intent = new Intent(VideoBrowserActivity.this, QueueListViewActivity.class);
//            startActivity(intent);
//        }
//        return true;
//    }
}
