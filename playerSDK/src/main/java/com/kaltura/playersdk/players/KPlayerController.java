package com.kaltura.playersdk.players;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.kaltura.playersdk.helpers.KIMAManager;

import java.lang.ref.WeakReference;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerCallback, ContentProgressProvider, KPlayerListener {
    private static final String TAG = "KPlayerController";
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

    public static final int CAN_PLAY = 1;
    public static final int SHOULD_PAUSE = 2;
    public static final int SHOULD_PLAY = 3;
    public static final int ENDED = 4;

    @Override
    public void eventWithValue(KPlayer player, String eventName, String eventValue) {
        playerListener.eventWithValue(player, eventName, eventValue);
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
        void setPlayerListener(KPlayerListener listener);
        void setPlayerCallback(KPlayerCallback callback);
        void setPlayerSource(String playerSource);
        String getPlayerSource();
        void setCurrentPlaybackTime(float currentPlaybackTime);
        float getCurrentPlaybackTime();
        float getDuration();
        void play();
        void pause();
        void changeSubtitleLanguage(String languageCode);
        void removePlayer();
        void recoverPlayer();
        boolean isKPlayer();
        void setShouldCancelPlay(boolean shouldCancelPlay);
        void setLicenseUri(String licenseUri);
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
        
        if (isIMAActive) {
            return;
        }

        Context context = parentViewController.getContext();
        
        // maybe change player
        String path = Uri.parse(src).getPath();
        if (path.endsWith(".m3u8")) {
            // HLS
            switchPlayer(new KHLSPlayer(context));
        } else if (path.endsWith(".wvm")) {
            // Widevine Classic
            switchPlayer(new KWVCPlayer(context));
        }
        
        this.src = src;
        this.player.setPlayerSource(this.src);
    }
    
    public void setLicenseUri(String uri) {
        this.player.setLicenseUri(uri);
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
        FrameLayout adPlayerContainer = new FrameLayout(mActivity.get());
        ViewGroup.LayoutParams lp = parentViewController.getLayoutParams();
        lp = new ViewGroup.LayoutParams(lp.width, lp.height);
        parentViewController.addView(adPlayerContainer, parentViewController.getChildCount() - 1, lp);

        // Add IMA UI controls view
        RelativeLayout adUiControls = new RelativeLayout(parentViewController.getContext());
        ViewGroup.LayoutParams curLP = parentViewController.getLayoutParams();
        ViewGroup.LayoutParams controlsLP = new ViewGroup.LayoutParams(curLP.width, curLP.height);
        parentViewController.addView(adUiControls, controlsLP);

        // Initialize IMA manager
        imaManager = new KIMAManager(mActivity.get(), adPlayerContainer, adUiControls, adTagURL);
        imaManager.setPlayerListener(this);
        imaManager.setPlayerCallback(this);
        imaManager.requestAds(this);
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
                } else {
                    contentCompleted(null);
                }
                break;
        }
    }

}
