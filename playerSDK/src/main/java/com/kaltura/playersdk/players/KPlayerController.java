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
import com.google.android.gms.common.api.GoogleApiClient;
import com.kaltura.playersdk.helpers.KIMAManager;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerCallback, ContentProgressProvider, KPlayerListener {
    private static final String TAG = "KPlayerController";
    private KPlayer player;
    private String src;
    private String adTagURL;
    private int adPlayerHeight;
    private String locale;
    private RelativeLayout parentViewController;
    private KIMAManager imaManager;
    private KCCRemotePlayer castPlayer;
    private WeakReference<Activity> mActivity;
    private KPlayerListener playerListener;
    private boolean isIMAActive = false;
    private boolean isPlayerCanPlay = false;
    private boolean isCasting = false;
    private boolean switchingBackFromCasting = false;

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
            playerListener.eventWithValue(player, KPlayerListener.EndedKey, null);
        } else if (currentPlayer == null) {
            isIMAActive = false;
            player.setShouldCancelPlay(true);
            playerListener.eventWithValue(player, KPlayerListener.EndedKey, null);
        }
        playerListener.contentCompleted(currentPlayer);
    }

    public static Set<MediaFormat> supportedFormats(Context context) {
        // TODO: dynamically determine available players, use reflection.

        Set<MediaFormat> formats = new HashSet<>();

        // All known players
        formats.addAll(KExoPlayer.supportedFormats(context));
        formats.addAll(KWVCPlayer.supportedFormats(context));
        //formats.addAll(KHLSPlayer.supportedFormats(context));
        
        return formats;
    }


    public KPlayerController(KPlayerListener listener) {
        playerListener = listener;
        this.parentViewController = (RelativeLayout)listener;
    }

    public void addPlayerToController() {
//        this.parentViewController = playerViewController;
        ViewGroup.LayoutParams currLP = this.parentViewController.getLayoutParams();

        // Add background view
        RelativeLayout mBackgroundRL = new RelativeLayout(this.parentViewController.getContext());
        mBackgroundRL.setBackgroundColor(Color.BLACK);
        this.parentViewController.addView(mBackgroundRL, parentViewController.getChildCount() - 1, currLP);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
        this.parentViewController.addView((View)this.player, parentViewController.getChildCount() - 1, lp);
    }

    public void replacePlayer() {
        ViewGroup.LayoutParams currLP = this.parentViewController.getLayoutParams();
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
        this.parentViewController.addView((View)this.player, 1, lp);
    }

    public void play() {
        if (isIMAActive) {
            return;
        }
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

    public void startCasting(GoogleApiClient apiClient) {
        player.pause();
        isCasting = true;
        if (castPlayer == null) {
            castPlayer = new KCCRemotePlayer(apiClient, new KCCRemotePlayer.KCCRemotePlayerListener() {
                @Override
                public void remoteMediaPlayerReady() {
                    castPlayer.setPlayerCallback(KPlayerController.this);
                    castPlayer.setPlayerListener(playerListener);
                    castPlayer.setPlayerSource(src);
                    ((View)player).setVisibility(View.INVISIBLE);
                }

                @Override
                public void mediaLoaded() {
                    castPlayer.setCurrentPlaybackTime(player.getCurrentPlaybackTime());
                }
            });
        }
    }

    public void stopCasting() {
        isCasting = false;
        switchingBackFromCasting = true;
        ((View) player).setVisibility(View.VISIBLE);
        castPlayer.removePlayer();
        player.setPlayerCallback(this);
        player.setPlayerListener(playerListener);
        player.setCurrentPlaybackTime(castPlayer.getCurrentPlaybackTime());
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
        if (player != null) {
            player.removePlayer();
            player = null;
        }
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

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        if (switchingBackFromCasting) {
            switchingBackFromCasting = false;
            return;
        }

        Context context = parentViewController.getContext();
        boolean shouldReplacePlayer = false;
        if (this.player != null) {
            parentViewController.removeView((View)this.player);
            this.player.removePlayer();
            shouldReplacePlayer = true;
        }

        // maybe change player
        String path = Uri.parse(src).getPath();
        if (path.endsWith(".wvm")) {
            // Widevine Classic
            this.player = new KWVCPlayer(context);
        } else {
            this.player = new com.kaltura.playersdk.players.KExoPlayer(context);
        }
        if (shouldReplacePlayer) {
            replacePlayer();
        } else {
            addPlayerToController();
        }
        this.player.setPlayerListener(playerListener);
        this.player.setPlayerCallback(this);
        this.src = src;
        this.player.setPlayerSource(src);
    }

    public void setLicenseUri(String uri) {
        this.player.setLicenseUri(uri);
    }


    public void initIMA(String adTagURL, Activity activity) {
        isIMAActive = true;
        player.setShouldCancelPlay(true);
        this.adTagURL = adTagURL;
        mActivity = new WeakReference<>(activity);
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
            case KPlayerCallback.CAN_PLAY:
                isPlayerCanPlay = true;
                if (mActivity != null) {
                    addAdPlayer();
                }
                break;
            case KPlayerCallback.SHOULD_PLAY:
                isIMAActive = false;
                player.setShouldCancelPlay(false);
                player.play();
                break;
            case KPlayerCallback.SHOULD_PAUSE:
                isIMAActive = true;
                player.pause();
                break;
            case KPlayerCallback.ENDED:
                if (imaManager != null) {
                    isIMAActive = true;
                    imaManager.contentComplete();
                } else {
                    contentCompleted(null);
                }
                break;
        }
    }

}
