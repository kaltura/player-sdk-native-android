package com.kaltura.playersdk.players;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer.ExoPlayer;
import com.kaltura.playersdk.Helpers.KIMAManager;

import java.lang.ref.WeakReference;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerCallback, ContentProgressProvider {
    private KPlayer player;
    private String playerClassName;
    private String src;
    private String adTagURL;
    private float currentPlaybackTime;
    private int adPlayerHeight;
    private String locale;
    private RelativeLayout parentViewController;
    private String key;
    private float currentTime;
    private boolean isSeeked;
    private boolean contentEnded;
    private boolean playerReady;
    private KIMAManager imaManager;
    private Activity mActivity;
    private KPlayer switchedPlayer = null;
    private KPlayerListener playerListener;
    private float mStartPos;
    private boolean isIMAActive = false;
    private boolean isPlayerCanPlay = false;
    private RelativeLayout mAdIMAPlayerHolder;


    public static final int CAN_PLAY = 1;
    public static final int SHOULD_PAUSE = 2;
    public static final int SHOULD_PLAY = 3;
    public static final int ENDED = 4;

    public interface KPlayer {
        public void setPlayerListener(KPlayerListener listener);
        public void setPlayerCallback(KPlayerCallback callback);
        public void setPlayerSource(String playerSource);
        public String getPlayerSource();
        public void setCurrentPlaybackTime(float currentPlaybackTime);
        public float getCurrentPlaybackTime();
        public float getDuration();
        public void play();
        public void pause();
        public void changeSubtitleLanguage(String languageCode);
        public void removePlayer();
        public void recoverPlayer();
        public boolean isKPlayer();
    }


    public KPlayerController(KPlayer player, KPlayerListener listener) {
        this.player = player;
        playerListener = listener;
        this.player.setPlayerListener(listener);
        this.player.setPlayerCallback(this);
    }

    public void addPlayerToController(RelativeLayout playerViewController) {
        this.parentViewController = playerViewController;
        ViewGroup.LayoutParams currLP = playerViewController.getLayoutParams();

        // Add background view
        RelativeLayout mBackgroundRL = new RelativeLayout(playerViewController.getContext());
        mBackgroundRL.setBackgroundColor(Color.BLACK);
        playerViewController.addView(mBackgroundRL, currLP);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
        playerViewController.addView((View) this.player, lp);
    }

    public void switchPlayer(KPlayer newPlayer) {
//        this.playerClassName = playerClassName;
//        this.key = key;
        player.setPlayerListener(null);
        player.setPlayerCallback(null);
        parentViewController.removeView((View) player);
        player = newPlayer;
        ViewGroup.LayoutParams currLP = parentViewController.getLayoutParams();
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
        ((View)player).setBackgroundColor(Color.RED);
        parentViewController.addView((View) player, parentViewController.getChildCount() - 1, lp);
        player.setPlayerCallback(this);
        player.setPlayerListener(playerListener);
    }

    public float getDuration() {
        if (player != null) {
            return player.getDuration();
        }
        return 0;
    }

    public void changeSubtitleLanguage(String isoCode) {

    }

    public void removePlayer() {
        if (player != null) {
            player.removePlayer();
        }
    }

    public void recoverPlayer() {
        if (player != null) {
            player.recoverPlayer();
        }
    }

    public void destroy() {
        player.removePlayer();
        player = null;
        playerListener = null;
        if (imaManager != null) {
            imaManager.destroy();
        }
    }


    public void setPlayer(KPlayer player) {
        this.player = player;
    }

    public KPlayer getPlayer() {
        return this.player;
    }


    public void setPlayerClassName(String playerClassName) {
        this.playerClassName = playerClassName;
    }

    public String getPlayerClassName() {
        return this.playerClassName;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
        if (!isIMAActive) {
            this.player.setPlayerSource(src);
        }
    }


    public void initIMA(String adTagURL, Activity activity) {
        isIMAActive = true;
        this.adTagURL = adTagURL;
        mActivity = activity;
        if (isPlayerCanPlay) {
            addAdPlayer();
        }
    }

    private void addAdPlayer() {

        // Add adPlayer view
//        FrameLayout adPlayerContainer = new FrameLayout(mActivity);
//        KAdIMAPlayer adIMAPlayer = new KAdIMAPlayer(mActivity);
//        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams)parentViewController.getLayoutParams();
//        lp = new FrameLayout.LayoutParams(lp.width, lp.height);
//        lp.gravity = (Gravity.CENTER);
//        adIMAPlayer.setLayoutParams(lp);
//        parentViewController.addView(adIMAPlayer, parentViewController.getChildCount() - 1, lp);
        mAdIMAPlayerHolder = new RelativeLayout(mActivity);
        mAdIMAPlayerHolder.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
        RelativeLayout.LayoutParams adPlayerLayout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        adPlayerLayout.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
//        adPlayerLayout.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
//        adPlayerLayout.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
//        adPlayerLayout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        KAdIMAPlayer adIMAPlayer = new KAdIMAPlayer(mActivity);
        adIMAPlayer.setLayoutParams(adPlayerLayout);
        mAdIMAPlayerHolder.addView(adIMAPlayer);
        parentViewController.addView(mAdIMAPlayerHolder, parentViewController.getChildCount()- 1);

        // Add IMA UI controls view
        RelativeLayout adUiControls = new RelativeLayout(parentViewController.getContext());
        ViewGroup.LayoutParams curLP = parentViewController.getLayoutParams();
        ViewGroup.LayoutParams controlsLP = new ViewGroup.LayoutParams(curLP.width, curLP.height);
        parentViewController.addView(adUiControls, controlsLP);

        // Initialize IMA manager
        imaManager = new KIMAManager(mActivity, adIMAPlayer, adUiControls, adTagURL);
        imaManager.setPlayerListener(playerListener);
        imaManager.setPlayerCallback(this);
        imaManager.requestAds(this, adUiControls);
    }

    public float getCurrentPlaybackTime() {
        return this.player.getCurrentPlaybackTime();
    }

    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        this.player.setCurrentPlaybackTime(currentPlaybackTime);
    }

    public int getAdPlayerHeight() {
        return adPlayerHeight;
    }

    public void setAdPlayerHeight(int adPlayerHeight) {
        this.adPlayerHeight = adPlayerHeight;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }


    // [START ContentProgressProvider region]
    @Override
    public VideoProgressUpdate getContentProgress() {
        if (player.getDuration() <= 0) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        return new VideoProgressUpdate((long)player.getCurrentPlaybackTime() * 1000, (long)player.getDuration() * 1000);
    }
    // [END ContentProgressProvider region]

    @Override
    public void playerStateChanged(int state) {
        switch (state) {
            case KPlayerController.CAN_PLAY:
                isPlayerCanPlay = true;
                if (mActivity != null) {
                    addAdPlayer();
                }
                if (switchedPlayer != null) {
//                    parentViewController.removeView((View)player);
//                    player.setPlayerListener(null);
//                    player.setPlayerCallback(null);
//                    ViewGroup.LayoutParams currLP = parentViewController.getLayoutParams();
//                    ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
//                    parentViewController.addView((View)switchedPlayer, parentViewController.getChildCount() - 1, lp);
//                    switchedPlayer.setPlayerCallback(this);
//                    switchedPlayer.setPlayerListener(playerListener);
                }
                break;
            case KPlayerController.SHOULD_PLAY:
                player.play();
                break;
            case KPlayerController.SHOULD_PAUSE:
                player.pause();
                break;
            case KPlayerController.ENDED:
                if (imaManager != null) {
                    imaManager.contentComplete();
                }
                break;
        }
    }

}
