package com.kaltura.playersdk.players;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.kaltura.playersdk.casting.KCastProviderImpl;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.helpers.KIMAManager;
import com.kaltura.playersdk.helpers.KIMAManagerEvents;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.interfaces.KCastProvider;
import com.kaltura.playersdk.interfaces.KIMAManagerListener;
import com.kaltura.playersdk.interfaces.KMediaControl;
import com.kaltura.playersdk.tracks.KTrackActions;
import com.kaltura.playersdk.tracks.KTracksManager;
import com.kaltura.playersdk.tracks.TrackType;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;

/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerCallback, ContentProgressProvider, KMediaControl, KIMAManagerListener {
    private static final String TAG = "KPlayerController";
    private KPlayer player;
    private KTracksManager tracksManager;
    private KTrackActions.EventListener tracksEventListener = null;
    private KTrackActions.VideoTrackEventListener videoTrackEventListener = null;
    private KTrackActions.AudioTrackEventListener audioTrackEventListener = null;
    private KTrackActions.TextTrackEventListener textTrackEventListener = null;

    private String src;
    private String adTagURL;
    private int adPlayerHeight;
    private String locale;
    private RelativeLayout parentViewController;
    private KIMAManager imaManager;
    private WeakReference<Activity> mActivity;
    private KPlayerListener playerListener;
    private boolean isIMAActive = false;
    private boolean isPlayerCanPlay = false;
    private boolean switchingBackFromCasting = false;
    private FrameLayout adPlayerContainer;
    private RelativeLayout mAdControls;
    private boolean isBackgrounded = false;
    private float mCurrentPlaybackTime = 0;
    private boolean isPlaying = false;
    private UIState currentState = UIState.Idle;
    private SeekCallback mSeekCallback;
    private boolean isContentCompleted = false;
    private boolean prepareWithConfigurationMode = false;
    private String mAdMimeType;
    private int mAdPreferredBitrate;
    private String newSourceDuringBg = null;
    private int mContentPreferredBitrate = -1;
    private long mPlayLastClickTime = 0;
    private long mPauseLastClickTime = 0;


    private KCastProviderImpl mCastProvider;
    private KChromeCastPlayer mCastPlayer;

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
            playerListener.eventWithJSON(player, KPlayerListener.AdRemainingTimeChangeKey, jsonValue);
        }
    }

    @Override
    public void onAdError(String errorMsg) {
        playerListener.eventWithValue(player, KPlayerListener.AdsLoadErrorKey, null); // update web that we have AD Loading error
        removeAdPlayer();
        ((View)player).setVisibility(View.VISIBLE);
        player.setShouldCancelPlay(false);
        play();

    }


    public void setTracksEventListener(KTrackActions.EventListener tracksEventListener) {
        this.tracksEventListener = tracksEventListener;
    }

    public void setVideoTrackEventListener(KTrackActions.VideoTrackEventListener videoTrackEventListener) {
        this.videoTrackEventListener = videoTrackEventListener;
        if (videoTrackEventListener == null && tracksManager != null) {
            tracksManager.removeVideoTrackEventListener();
        }
    }

    public void setAudioTrackEventListener(KTrackActions.AudioTrackEventListener audioTrackEventListener) {
        this.audioTrackEventListener = audioTrackEventListener;
        if (audioTrackEventListener == null && tracksManager != null) {
            tracksManager.removeAudioTrackEventListener();
        }
    }

    public void setTextTrackEventListener(KTrackActions.TextTrackEventListener textTrackEventListener) {
        this.textTrackEventListener = textTrackEventListener;
        if (textTrackEventListener == null && tracksManager != null) {
            tracksManager.removeTextTrackEventListener();
        }
    }

    public void setCastProvider(final KCastProvider castProvider) {
        mCastProvider = (KCastProviderImpl)castProvider;
        mCastProvider.setInternalListener(new KCastProviderImpl.InternalListener() {
            @Override
            public void onStartCasting(KChromeCastPlayer remoteMediaPlayer) {
                mCastPlayer = remoteMediaPlayer;
                mCastPlayer.load(player.getCurrentPlaybackTime());
                mCastProvider.getProviderListener().onCastMediaRemoteControlReady(remoteMediaPlayer);
                remoteMediaPlayer.addListener(this);
            }

            @Override
            public void onCastStateChanged(String state) {
                playerListener.eventWithValue(player, state, "");
            }

            @Override
            public void onStopCasting() {
                if (mCastPlayer != null) {
                    player.setCurrentPlaybackTime(mCastPlayer.getCurrentPosition());
                    mCastPlayer.removeListeners();
                    mCastPlayer = null;
                }
                mCastProvider = null;
                currentState = UIState.Pause;
                play();
            }


            @Override
            public void onCastMediaStateChanged(KCastMediaRemoteControl.State state) {
                if (playerListener == null) {
                    return;
                }
                switch (state) {
                    case Loaded:
                        boolean isPlayingBeforeCast = player.isPlaying();
                        player.pause();
                        playerListener.eventWithValue(player, "hideConnectingMessage", null);
                        playerListener.eventWithValue(player, KPlayerListener.DurationChangedKey, Float.toString(getDuration() / 1000f));
                        playerListener.eventWithValue(player, KPlayerListener.LoadedMetaDataKey, "");
                        playerListener.eventWithValue(player, KPlayerListener.CanPlayKey, null);
                        if (isPlayingBeforeCast) {
                            playerListener.eventWithValue(player, KPlayerListener.PlayKey, null);
                        }
                        break;
                    case Playing:
                        playerListener.eventWithValue(player, KPlayerListener.PlayKey, null);
                        break;
                    case Pause:
                        playerListener.eventWithValue(player, KPlayerListener.PauseKey, null);
                        break;
                    case Seeked:
                        playerListener.eventWithValue(player, KPlayerListener.SeekedKey, null);
                        break;
                    case Ended:
                        playerListener.eventWithValue(player, KPlayerListener.EndedKey, null);
                        break;
                }
            }

            @Override
            public void onCastMediaProgressUpdate(long currentPosition) {
                if (playerListener != null) {
                    playerListener.eventWithValue(player, KPlayerListener.TimeUpdateKey, Float.toString(currentPosition / 1000f));
                }
            }
        });
        mCastProvider.prepareChannel();
    }

    private enum UIState {
        Idle,
        Play,
        Pause,
        Seeking,
        Replay
    }

    public static Set<KMediaFormat> supportedFormats(Context context) {
        // TODO: dynamically determine available players, use reflection.

        Set<KMediaFormat> formats = new HashSet<>();

        // All known players
        formats.addAll(KExoPlayer.supportedFormats(context));
        formats.addAll(KWVCPlayer.supportedFormats(context));
        return formats;
    }


    public KPlayerController(KPlayerListener listener) {
        playerListener = listener;
        this.parentViewController = (RelativeLayout)listener;
    }

    public void addPlayerToController(boolean isWVClassic) {
        Context context = parentViewController.getContext();
        if (isWVClassic) {
            player = new KWVCPlayer(context);
        } else {
            player = new KExoPlayer(context);
            if (prepareWithConfigurationMode) {
                player.setPrepareWithConfigurationMode();
            }
        }
        player.setPlayerListener(playerListener);
        player.setPlayerCallback(this);
    }

    public KTracksManager getTracksManager() {
        return tracksManager;
    }

    public void play() {
        if (player == null && mCastProvider == null) {
            return;
        }
        if (SystemClock.elapsedRealtime() - mPlayLastClickTime < 1000) {
            LOGD(TAG, "PLAY REJECTED");
            return;
        }
        mPlayLastClickTime = SystemClock.elapsedRealtime();
        if (currentState != UIState.Play) {
            currentState = UIState.Play;
            if (isBackgrounded && isIMAActive) {
                imaManager.resume();
                return;
            }
            if (isIMAActive) {
                return;
            }
            if (mCastProvider == null) {
                if (player != null) {
                    player.play();
                    if (isBackgrounded) {
                        //if go to background on buffering and playback starting need to pause and change to playing
                        player.pause();
                        isPlaying = true;
                    }
                }
            } else {
                if (mCastProvider.getCastMediaRemoteControl() != null) {
                    mCastProvider.getCastMediaRemoteControl().play();
                }
            }
        }
    }

    @Override
    public void start() {
        play();
    }

    @Override
    public void pause() {
        if (player == null && mCastProvider == null) {
            return;
        }
        if (SystemClock.elapsedRealtime() - mPauseLastClickTime < 1000) {
            LOGD(TAG, "PAUSE REJECTED");
            return;
        }

        mPauseLastClickTime = SystemClock.elapsedRealtime();
        if (currentState != UIState.Pause) {
            currentState = UIState.Pause;
            if (mCastProvider == null) {
                if (isBackgrounded && isIMAActive) {
                    if (imaManager != null) {
                        imaManager.pause();
                    }
                } else {
                    if (player != null) {
                        player.pause();
                    }
                }
            } else {
                if (mCastProvider.getCastMediaRemoteControl() != null) {
                    mCastProvider.getCastMediaRemoteControl().pause();
                }
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
        if (player != null) {
            return player.isPlaying();
        }
        return false;
    }

    @Override
    public int getCurrentPosition() {
        if (player != null) {
            return (int) player.getCurrentPlaybackTime();
        }
        return 0;
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
        return player != null && player.isPlaying();
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
                if (imaManager != null) {
                    imaManager.pause();
                }
            }
        }
    }

    public void recoverPlayer() {
        isBackgrounded = false;
        if (isIMAActive && imaManager != null) {
            imaManager.resume();
        } else if (player != null) {
            if (newSourceDuringBg != null){
                setSrc(newSourceDuringBg);
                newSourceDuringBg = null;
            }
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
        return player;
    }

    public String getSrc() {
        return this.src;
    }


    public void setSrc(String newSrc) {
        Context context = parentViewController.getContext();
        if (isBackgrounded && imaManager != null){
            newSourceDuringBg = newSrc;
            return;
        }
        isPlayerCanPlay = false;
        if (switchingBackFromCasting) {
            switchingBackFromCasting = false;
            return;
        }

        // Select player
        String path = Uri.parse(newSrc).getPath();

        boolean isWVClassic = path.endsWith(".wvm");
        if (this.src == null) {
            addPlayerToController(isWVClassic);
        } else {
            String prevPath = Uri.parse(this.src).getPath();
            String curFileType = path.substring(path.lastIndexOf("."));
            String prevFileType = prevPath.substring(prevPath.lastIndexOf("."));
            if (!curFileType.equals(prevFileType) && (path.endsWith(".wvm") || prevPath.endsWith(".wvm"))) {
                if (imaManager != null) {
                    mActivity = null;
                    removeAdPlayer();
                }
                addPlayerToController(isWVClassic);
            }
        }
        parentViewController.removeView((View) player);
        player.removePlayer();
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        this.parentViewController.addView((View) this.player, parentViewController.getChildCount() - 1, lp);

        this.src = newSrc;
        player.setPlayerSource(this.src);
    }

    public void setLicenseUri(String uri) {
        if (player != null) {
            player.setLicenseUri(uri);
        }
    }


    public void initIMA(String adTagURL, String adMimeType, int adPreferredBitrate, Activity activity) {
        ((View)player).setVisibility(View.INVISIBLE);
        isIMAActive = true;
        mAdMimeType = adMimeType;
        mAdPreferredBitrate = adPreferredBitrate;
        if (player != null) {
            player.setShouldCancelPlay(true);
        }
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
        imaManager = new KIMAManager(mActivity.get(), adPlayerContainer, mAdControls, adTagURL, mAdMimeType, mAdPreferredBitrate);
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
        if (player != null) {
            return player.getCurrentPlaybackTime() / 1000f;
        }
        return 0f;
    }

    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        LOGD(TAG, "setCurrentPlaybackTime " + currentPlaybackTime);
        if (mCastProvider == null) {
            if (isPlayerCanPlay) {
                if (player != null) {
                    player.setCurrentPlaybackTime((long) (currentPlaybackTime * 1000));
                }
                if (currentPlaybackTime == 0.01f) {
                    currentState = UIState.Replay;
                }
            } else {
                mCurrentPlaybackTime = currentPlaybackTime;
            }
        } else {
            if (mCastProvider.getCastMediaRemoteControl() != null) {
                mCastProvider.getCastMediaRemoteControl().seek((long) currentPlaybackTime * 1000);
            }
        }
    }

    public void setContentPreferredBitrate(int PreferredBitrate) {
        mContentPreferredBitrate = PreferredBitrate;
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

        //LOGE(TAG, "XXXX playerStateChanged " + state);
        switch (state) {
            case KPlayerCallback.CAN_PLAY:
                LOGD(TAG, "playerStateChanged CAN_PLAY");
                tracksManager = new KTracksManager(player);
                if (videoTrackEventListener != null) {
                    getTracksManager().setVideoTrackEventListener(videoTrackEventListener);
                }
                if (audioTrackEventListener != null) {
                    getTracksManager().setAudioTrackEventListener(audioTrackEventListener);
                }
                if (textTrackEventListener != null) {
                    getTracksManager().setTextTrackEventListener(textTrackEventListener);
                }

                if (tracksEventListener != null) {
                    //send tracks to app
                    tracksEventListener.onTracksUpdate(tracksManager);
                }

                //Send tracks to webView
                sendTracksList(TrackType.TEXT);
                sendTracksList(TrackType.AUDIO);
                sendTracksList(TrackType.VIDEO);

                if (mContentPreferredBitrate != -1) {
                    if (tracksManager != null) {
                        tracksManager.switchTrackByBitrate(TrackType.VIDEO, mContentPreferredBitrate);
                    }
                }

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
                LOGD(TAG, "playerStateChanged  ENDED");
                if (imaManager != null) {
                    isContentCompleted = true;
                    isIMAActive = true;
                    imaManager.contentComplete();
                } else {
                    playerListener.eventWithValue(player, KPlayerListener.SeekedKey, null);
                    playerListener.eventWithValue(player, KPlayerListener.EndedKey, null);
                }
                break;
            case KPlayerCallback.SEEKED:
                LOGD(TAG, "playerStateChanged CAN_PLAY currentState " + currentState.name());

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

    private void sendTracksList(TrackType trackType) {
        LOGD(TAG, "sendTracksList: " + trackType);
        switch(trackType) {
            case AUDIO:
                playerListener.eventWithJSON(getPlayer(), KPlayerListener.AudioTracksReceivedKey, tracksManager.getTrackListAsJson(TrackType.AUDIO).toString());
                break;
            case TEXT:
                playerListener.eventWithJSON(getPlayer(), KPlayerListener.TextTracksReceivedKey,  tracksManager.getTrackListAsJson(TrackType.TEXT).toString());
                break;
            case VIDEO:
                playerListener.eventWithJSON(getPlayer(), KPlayerListener.FlavorsListChangedKey,  tracksManager.getTrackListAsJson(TrackType.VIDEO).toString());
                break;
        }
    }

    public void attachView() {
        if (player != null) {
            player.attachSurfaceViewToPlayer();
        }
    }

    public void detachView() {
        if (player != null) {
            player.detachSurfaceViewFromPlayer();
        }
    }

    public void setPrepareWithConfigurationMode(boolean prepareWithConfigurationMode) {
        this.prepareWithConfigurationMode = prepareWithConfigurationMode;
    }

}
