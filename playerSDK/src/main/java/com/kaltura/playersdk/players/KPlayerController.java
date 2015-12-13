package com.kaltura.playersdk.players;

import android.app.Activity;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.gms.common.api.GoogleApiClient;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.helpers.KIMAManager;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.ref.WeakReference;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerCallback, ContentProgressProvider, KPlayerListener {
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
    private KCCRemotePlayer castPlayer;
    private WeakReference<Activity> mActivity;
    private KPlayer switchedPlayer = null;
    private KPlayerListener playerListener;
    private float mStartPos;
    private boolean isIMAActive = false;
    private boolean isPlayerCanPlay = false;
    private boolean isCasting = false;
    private boolean switchingBackFromCasting = false;

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
        public void setShouldCancelPlay(boolean shouldCancelPlay);
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
        player.setPlayerListener(null);
        player.setPlayerCallback(null);
        parentViewController.removeView((View) player);
        player.removePlayer();
        player = newPlayer;
        ViewGroup.LayoutParams currLP = parentViewController.getLayoutParams();
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
        parentViewController.addView((View) player, parentViewController.getChildCount() - 1, lp);
        player.setPlayerCallback(this);
        player.setPlayerListener(playerListener);
    }

    public void play() {
        if (!isCasting) {
            player.play();
        } else {
            castPlayer.play();
        }
    }

    public void pause() {
        if (!isCasting) {
            player.pause();
        } else {
            castPlayer.pause();
        }
    }

    public void startCasting(GoogleApiClient apiClient, final String nameSpace) {
        isCasting = true;
//        player.pause();
        if (castPlayer == null) {
            castPlayer = new KCCRemotePlayer(apiClient, nameSpace, new KCCRemotePlayer.KCCRemotePlayerListener() {
                @Override
                public void remoteMediaPlayerReady() {
//                    castPlayer.setPlayerCallback(KPlayerController.this);
//                    castPlayer.setPlayerListener(playerListener);
//                    if (nameSpace != null) {
//                        castPlayer.setPlayerSource(src);
//                    } else {
//                        playerListener.eventWithValue(castPlayer, "hideConnectingMessage", null);
//                        playerListener.eventWithValue(castPlayer, "chromecastDeviceConnected", null);
//                    }
//                    ((View)player).setVisibility(View.INVISIBLE);
                }

                @Override
                public void mediaLoaded() {
                    castPlayer.setCurrentPlaybackTime(player.getCurrentPlaybackTime());
                }
            });
            castPlayer.setPlayerCallback(KPlayerController.this);
            castPlayer.setPlayerListener(playerListener);
            if (nameSpace == null) {
                castPlayer.setPlayerSource(src);
            } else {
                playerListener.eventWithValue(castPlayer, "hideConnectingMessage", null);
                playerListener.eventWithValue(castPlayer, "chromecastDeviceConnected", null);
            }
            ((View)player).setVisibility(View.INVISIBLE);
        }
    }

    public void stopCasting() {
        isCasting = false;
        switchingBackFromCasting = true;
        player.setCurrentPlaybackTime(castPlayer.getCurrentPlaybackTime());
        castPlayer.removePlayer();
        ((View) player).setVisibility(View.VISIBLE);
        player.setPlayerCallback(this);
        player.setPlayerListener(playerListener);
        player.play();
    }

    public void removeCastPlayer() {
        castPlayer.setPlayerCallback(null);
        castPlayer.setPlayerListener(null);
        castPlayer = null;
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
        if (!isIMAActive && !switchingBackFromCasting) {
            this.src = src;
            this.player.setPlayerSource(src);
        } else if (switchingBackFromCasting) {
            switchingBackFromCasting = false;
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
        if (!isCasting) {
            this.player.setCurrentPlaybackTime(currentPlaybackTime);
        } else {
            castPlayer.setCurrentPlaybackTime(currentPlaybackTime);
        }
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
