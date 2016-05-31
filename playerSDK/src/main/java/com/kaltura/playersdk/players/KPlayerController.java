package com.kaltura.playersdk.players;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.gms.common.api.GoogleApiClient;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.helpers.KIMAManager;
import com.kaltura.playersdk.helpers.KIMAManagerEvents;
import com.kaltura.playersdk.interfaces.KIMAManagerListener;
import com.kaltura.playersdk.interfaces.KMediaControl;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerCallback, ContentProgressProvider, KMediaControl, KIMAManagerListener {
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
    private FrameLayout adPlayerContainer;
    private RelativeLayout mAdControls;
    private boolean isBackgrounded = false;
    private float mCurrentPlaybackTime = 0;
    private boolean isPlaying = false;
    private UIState currentState = UIState.Idle;
    private SeekCallback mSeekCallback;
    private boolean isContentCompleted = false;

    @Override
    public void onAdEvent(AdEvent.AdEventType eventType, String jsonValue) {
        if (playerListener != null) {
            playerListener.eventWithJSON(player, KIMAManagerEvents.eventName(eventType), jsonValue);
        }
        switch (eventType) {
            case CONTENT_RESUME_REQUESTED:
                ((View)player).setVisibility(View.VISIBLE);

                isIMAActive = false;
                player.setShouldCancelPlay(false);
                player.play();
                break;
            case CONTENT_PAUSE_REQUESTED:
                isIMAActive = true;
                pause();
                ((View)player).setVisibility(View.INVISIBLE);
                break;
            case ALL_ADS_COMPLETED:
                if (isContentCompleted && playerListener != null) {
                    playerListener.eventWithValue(player, KPlayerListener.EndedKey, null);
                    isContentCompleted = false;
                }
                removeAdPlayer();
                break;
        }
    }

    @Override
    public void onAdUpdateProgress(String jsonValue) {
        if (playerListener != null) {
            playerListener.eventWithJSON(player, "adRemainingTimeChange", jsonValue);
        }
    }

    @Override
    public void onAdError(String errorMsg) {
        removeAdPlayer();
        ((View)player).setVisibility(View.VISIBLE);
        isIMAActive = false;
        player.play();
    }


    private enum UIState {
        Idle,
        Play,
        Pause,
        Seeking,
        Replay
    }

    public static Set<MediaFormat> supportedFormats(Context context) {
        // TODO: dynamically determine available players, use reflection.

        Set<MediaFormat> formats = new HashSet<>();

        // All known players
        formats.addAll(KExoPlayer.supportedFormats(context));
        formats.addAll(KWVCPlayer.supportedFormats(context));
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
        if (currentState != UIState.Play) {
            currentState = UIState.Play;
            if (isBackgrounded && isIMAActive) {
                imaManager.resume();
                return;
            }
            if (isIMAActive) {
                return;
            }
            if (!isCasting) {
                player.play();
                if (isBackgrounded) {
                    //if go to background on buffering and playback starting need to pause and change to playing
                    player.pause();
                    isPlaying = true;
                }
            } else {
                castPlayer.play();
            }
        }
    }

    @Override
    public void start() {
        play();
    }

    @Override
    public void pause() {
        if (currentState != UIState.Pause) {
            currentState = UIState.Pause;
            if (!isCasting) {
                if (isBackgrounded && isIMAActive) {
                    if (imaManager != null) {
                        imaManager.pause();
                    }
                } else {
                    player.pause();
                }
            } else {
                castPlayer.pause();
            }
        }
    }

    @Override
    public void seek(double seconds) {
        currentState = UIState.Seeking;
        setCurrentPlaybackTime((float) seconds);
    }

    @Override
    public void replay() {
        setCurrentPlaybackTime(0.01f);
    }

    @Override
    public boolean canPause() {
        return player.isPlaying();
    }

    @Override
    public int getCurrentPosition() {
        return (int) player.getCurrentPlaybackTime();
    }

    @Override
    public int getDuration() {
        if (player != null) {
            return (int) player.getDuration();
        }
        return 0;
    }

    @Override
    public boolean isPlaying() {
        return player != null &&  player.isPlaying();
    }

    @Override
    public boolean canSeekBackward() {
        return getDuration() > getCurrentPosition();
    }

    @Override
    public boolean canSeekForward() {
        return getCurrentPlaybackTime() > 0;
    }

    @Override
    public void seek(long milliSeconds, SeekCallback callback) {
        mSeekCallback = callback;
        seek(milliSeconds / 1000f);
    }

    @Override
    public KPlayerState state() {
        return null;
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
        player.setPlayerCallback(this);
        player.setPlayerListener(playerListener);
        player.setCurrentPlaybackTime(castPlayer.getCurrentPlaybackTime());
        player.play();
        removeCastPlayer();
    }

    public void removeCastPlayer() {
        castPlayer.removePlayer();
        castPlayer.setPlayerCallback(null);
        castPlayer.setPlayerListener(null);
        castPlayer = null;
    }

    public void changeSubtitleLanguage(String isoCode) {

    }

    public void savePlayerState() {
//        isBackgrounded = isOnBackground;
        if (player != null) {
            isPlaying = player.isPlaying() || isIMAActive;
            pause();
        } else {
            isPlaying = false;
        }
    }

    public void recoverPlayerState() {
        if (isPlaying) {
            if (isIMAActive && imaManager != null) {
                imaManager.resume();
            } else if (player != null) {
                play();
            }
        }
    }

    public void removePlayer(boolean shouldSaveState) {
        isBackgrounded = true;
        if (player != null) {
            if (shouldSaveState) {
                savePlayerState();
            } else {
                isPlaying = false;
                pause();
            }
            if (!isIMAActive) {
                player.freezePlayer();
            } else {
                imaManager.pause();
            }
        }
    }

    public void recoverPlayer() {
        isBackgrounded = false;
        if (isIMAActive && imaManager != null) {
            imaManager.resume();
        } else if (player != null) {
            recoverPlayerState();
            player.recoverPlayer(isPlaying);
        }
    }

    public void reset() {
        isBackgrounded = false;
        if (imaManager != null) {
            removeAdPlayer();
        }
        if (player != null) {
            player.removePlayer();
        }
    }

    public void destroy() {
        isBackgrounded = false;
        if (imaManager != null) {
            removeAdPlayer();
        }
        if (player != null) {
            player.removePlayer();
            player = null;
        }
        playerListener = null;
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
        isPlayerCanPlay = false;
        if (switchingBackFromCasting) {
            switchingBackFromCasting = false;
            return;
        }

        Context context = parentViewController.getContext();
        boolean shouldReplacePlayer = false;
        if (this.player != null) {
            if (imaManager != null) {
                mActivity = null;
                removeAdPlayer();
            }
            parentViewController.removeView((View) this.player);
            this.player.removePlayer();
            shouldReplacePlayer = true;
        }

        // Select player
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
        ((View)player).setVisibility(View.INVISIBLE);
        isIMAActive = true;
        player.setShouldCancelPlay(true);
        this.adTagURL = adTagURL;
        mActivity = new WeakReference<>(activity);
        if (isPlayerCanPlay) {
            addAdPlayer();
        }
    }

    private void addAdPlayer() {
        ((View)player).setVisibility(View.INVISIBLE);

        // Add adPlayer view
        adPlayerContainer = new FrameLayout(mActivity.get());
        ViewGroup.LayoutParams lp = parentViewController.getLayoutParams();
        lp = new ViewGroup.LayoutParams(lp.width, lp.height);
        parentViewController.addView(adPlayerContainer, parentViewController.getChildCount() - 1, lp);

        // Add IMA UI KMediaControl view
        mAdControls = new RelativeLayout(parentViewController.getContext());
        ViewGroup.LayoutParams curLP = parentViewController.getLayoutParams();
        ViewGroup.LayoutParams controlsLP = new ViewGroup.LayoutParams(curLP.width, curLP.height);
        parentViewController.addView(mAdControls, controlsLP);

        // Initialize IMA manager
        imaManager = new KIMAManager(mActivity.get(), adPlayerContainer, mAdControls, adTagURL);
        imaManager.setListener(this);
        imaManager.requestAds(this);
    }

    private void removeAdPlayer() {
        if (parentViewController != null) {
            mActivity = null;
            imaManager.destroy();
            imaManager = null;
            isIMAActive = false;
            parentViewController.removeView(adPlayerContainer);
            adPlayerContainer = null;
            parentViewController.removeView(mAdControls);
            mAdControls = null;
        }
    }

    public float getCurrentPlaybackTime() {
        return this.player.getCurrentPlaybackTime() / 1000f;
    }

    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        if (!isCasting) {
            if (isPlayerCanPlay) {
                this.player.setCurrentPlaybackTime((long) (currentPlaybackTime * 1000));
                if (currentPlaybackTime == 0.01f) {
                    currentState = UIState.Replay;
                }
            } else {
                mCurrentPlaybackTime = currentPlaybackTime;
            }
        } else {
            castPlayer.setCurrentPlaybackTime((long)currentPlaybackTime * 1000);
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
        if (player == null || player.getDuration() <= 0) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        return new VideoProgressUpdate(player.getCurrentPlaybackTime(), player.getDuration());
    }
    // [END ContentProgressProvider region]

    @Override
    public void playerStateChanged(int state) {
        switch (state) {
            case KPlayerCallback.CAN_PLAY:
                isPlayerCanPlay = true;
                if (mActivity != null && !isIMAActive) {
                    addAdPlayer();
                }
                if (mCurrentPlaybackTime > 0) {
                    player.setCurrentPlaybackTime((long) (mCurrentPlaybackTime * 1000));
                    mCurrentPlaybackTime = 0;
                }
                break;
            case KPlayerCallback.ENDED:
                if (imaManager != null) {
                    isContentCompleted = true;
                    isIMAActive = true;
                    imaManager.contentComplete();
                } else {
                    playerListener.eventWithValue(player, KPlayerListener.EndedKey, null);
                }
                break;
            case KPlayerCallback.SEEKED:
                if (currentState == UIState.Play || currentState == UIState.Replay) {
                    play();
                }
                if (mSeekCallback != null) {
                    mSeekCallback.seeked(player.getCurrentPlaybackTime());
                    mSeekCallback = null;
                }
                break;
        }
    }
}
