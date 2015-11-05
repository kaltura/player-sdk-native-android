package com.kaltura.playersdk.players;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.example.kplayersdk.R;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.kaltura.playersdk.helpers.IMAVideoPlayerController;
import com.kaltura.playersdk.helpers.KIMAManager;
import com.kaltura.playersdk.helpers.KStringUtilities;

import java.lang.ref.WeakReference;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerListener {
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
    private WeakReference<Activity> mActivity;
    private KPlayer switchedPlayer = null;
    private KPlayerListener playerListener;
    private float mStartPos;
    private boolean isIMAActive = false;
    private boolean isPlayerCanPlay = false;
    private IMAVideoPlayerController imaVideoPlayerController;
    private boolean mIsAdPlaying;


    @Override
    public void eventWithValue(KPlayer player, String eventName, String eventValue) {
        KStringUtilities state = new KStringUtilities(eventName);
        if (imaManager == null && !mIsAdPlaying && state.canPlay()) {
            isPlayerCanPlay = true;
            if (mActivity != null) {
                addAdPlayer();
            }
        } else if (mIsAdPlaying && state.isPlay()) {
            imaVideoPlayerController.onPlay();
        } else if (mIsAdPlaying && state.isPause()) {
            imaVideoPlayerController.onPause();
        } else if (mIsAdPlaying && state.isEnded()) {
            imaVideoPlayerController.onCompleted();
        }
        if (!mIsAdPlaying) {
            playerListener.eventWithValue(player, eventName, eventValue);
        }
    }

    @Override
    public void eventWithJSON(KPlayer player, String eventName, String jsonValue) {
        if (eventName.equals(KIMAManager.AllAdsCompletedKey)) {
            isIMAActive = false;
            mActivity.clear();
            mActivity = null;
            removeAdPlayer();
        }
        playerListener.eventWithJSON(player, eventName, jsonValue);
    }

    @Override
    public void contentCompleted(KPlayer currentPlayer) {
        if (!isIMAActive) {
            player.setCurrentPlaybackTime(0);
            playerListener.eventWithValue(player, "ended", null);
        } else if (isIMAActive && currentPlayer == null) {
            isIMAActive = false;
            player.setShouldCancelPlay(true);
            playerListener.eventWithValue(player, "ended", null);
        }
        playerListener.contentCompleted(currentPlayer);
    }

    public interface KPlayer {
        public void setPlayerListener(KPlayerListener listener);
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
        public void setShouldCancelPlay(boolean shouldCancelPlay);
    }


    public KPlayerController(KPlayer player, KPlayerListener listener) {
        this.player = player;
        playerListener = listener;
        player.setPlayerListener(this);
//        this.player.setPlayerListener(this);
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
//        player.setPlayerCallback(null);
        parentViewController.removeView((View) player);
        player = newPlayer;
        ViewGroup.LayoutParams currLP = parentViewController.getLayoutParams();
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
        ((View)player).setBackgroundColor(Color.RED);
        parentViewController.addView((View) player, parentViewController.getChildCount() - 1, lp);
//        player.setPlayerCallback(this);
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
        if (!isIMAActive) {
            this.src = src;
            this.player.setPlayerSource(src);
        }
    }


    public void initIMA(String adTagURL, Activity activity) {
        isIMAActive = true;
        player.setShouldCancelPlay(true);
        this.adTagURL = adTagURL;
        mActivity = new WeakReference<Activity>(activity);
        if (isPlayerCanPlay) {
            addAdPlayer();
        }
    }

    private void addAdPlayer() {

        // Add adPlayer view
//        FrameLayout adPlayerContainer = new FrameLayout(mActivity.get());
//        ViewGroup.LayoutParams lp = parentViewController.getLayoutParams();
//        lp = new ViewGroup.LayoutParams(lp.width, lp.height);
//        LayoutInflater inflater = (LayoutInflater)parentViewController.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        FrameLayout adPlayerContainer = (FrameLayout)inflater.inflate(R.layout.ad_player, parentViewController, false);
//        parentViewController.addView(adPlayerContainer, parentViewController.getChildCount() - 1);

        // Add IMA UI controls view
        RelativeLayout adUiControls = new RelativeLayout(parentViewController.getContext());
        ViewGroup.LayoutParams curLP = parentViewController.getLayoutParams();
        ViewGroup.LayoutParams controlsLP = new ViewGroup.LayoutParams(curLP.width, curLP.height);
        parentViewController.addView(adUiControls, controlsLP);
        imaVideoPlayerController = new IMAVideoPlayerController(player);
        imaVideoPlayerController.setAdPlayerListener(new IMAVideoPlayerController.AdPlayerListener() {
            @Override
            public void setIsAdPlaying(boolean isAdPlaying) {
                mIsAdPlaying = isAdPlaying;
            }
        });
        // Initialize IMA manager
        imaManager = new KIMAManager(mActivity.get(), imaVideoPlayerController, adUiControls, adTagURL);
//        imaManager.setPlayerListener(this);
//        imaManager.setPlayerCallback(this);

        imaManager.requestAds(imaVideoPlayerController);
    }

    private void removeAdPlayer() {
        imaManager = null;
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


}
