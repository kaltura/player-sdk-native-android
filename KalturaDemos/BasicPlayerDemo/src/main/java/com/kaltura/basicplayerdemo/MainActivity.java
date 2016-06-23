package com.kaltura.basicplayerdemo;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.casting.KCastDevice;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.interfaces.KCastProvider;
import com.kaltura.playersdk.types.KPError;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, KPEventListener {
    private static final String TAG = "BasicPlayerDemo";
    private Button mPlayPauseButton;
    private SeekBar mSeekBar;
    private PlayerViewController mPlayer;
    private boolean onCreate = false;
    private ArrayList<KCastDevice> mRouterInfos = new ArrayList<>();
    private boolean isCCActive = false;
    private Button ccButton;
    private ImageButton mMediaRouteButtonDiscon;
    private ImageButton mMediaRouteButtonCon;
    KCastProvider mCastProvider;

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
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);

        View mCustomView = mInflater.inflate(R.layout.action_bar, null);
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
        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);

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
//                presentCCDevices();
                startCC();
            }
        });
        onCreate = true;
        getPlayer();

    }


    private void startCC() {

        mCastProvider = PlayerViewController.createCastProvider();
        mCastProvider.setKCastProviderListener(new KCastProvider.KCastProviderListener() {
            @Override
            public void onDeviceCameOnline(KCastDevice device) {
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
            }

            @Override
            public void onDeviceDisconnected() {
                mRouterInfos.clear();
                mCastProvider.startScan(getApplicationContext(), "C43947A1");
                mMediaRouteButtonDiscon.setVisibility(View.VISIBLE);
                mMediaRouteButtonCon.setVisibility(View.INVISIBLE);
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
            mPlayer.loadPlayerIntoActivity(this);

            KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.43.rc11/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
            config.setAutoPlay(true);
            mPlayPauseButton.setText("Pause");
            config.addConfig("chromecast.plugin", "true");
            config.addConfig("chromecast.applicationID", "C43947A1");
            config.addConfig("chromecast.useKalturaPlayer", "true");
            config.addConfig("chromecast.receiverLogo", "true");



            mPlayer.initWithConfiguration(config);
            mPlayer.addEventListener(new KPEventListener() {
                @Override
                public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {

                }

                @Override
                public void onKPlayerError(PlayerViewController playerViewController, KPError error) {

                }

                @Override
                public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {

                }

                @Override
                public void onKPlayerFullScreenToggeled(final PlayerViewController playerViewController, boolean isFullscrenn) {
                    setContentView(playerViewController);
                }
            });
            if (mCastProvider != null) {
                mPlayer.setCastProvider(mCastProvider);
            }
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
                mCastProvider.connectToDevice(mRouterInfos.get(item));
//                getPlayer();
//                mPlayer.getKCastRouterManager().connectDevice(mRouterInfos.get(item).getRouterId());
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
            mPlayPauseButton.setText("Play");
        } else if (state == KPlayerState.PLAYING) {
//            findViewById(R.id.replay).setVisibility(View.INVISIBLE);
            mPlayPauseButton.setText("Pause");
        }
    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
        mSeekBar.setProgress((int) (currentTime / (playerViewController.getMediaControl().getDuration() / 1000f) * 100));
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        Log.e(TAG, "Error Received:" + error.getErrorMsg());
    }
}
