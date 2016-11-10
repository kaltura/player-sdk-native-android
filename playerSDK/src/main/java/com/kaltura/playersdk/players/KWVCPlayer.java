package com.kaltura.playersdk.players;

import android.content.Context;
import android.drm.DrmErrorEvent;
import android.drm.DrmEvent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.drm.WidevineDrmClient;



import java.util.Collections;
import java.util.Set;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;
import static com.kaltura.playersdk.utils.LogUtils.LOGI;
import static com.kaltura.playersdk.utils.LogUtils.LOGW;


/**
 * Created by noamt on 10/27/15.
 */
public class KWVCPlayer
        extends FrameLayout
        implements KPlayer {

    private static final String TAG = "KWVCPlayer";

    private static final long PLAYHEAD_UPDATE_INTERVAL = 200;
    @Nullable private VideoView mPlayer;
    private String mAssetUri;
    private String mLicenseUri;
    private WidevineDrmClient mDrmClient;
    @NonNull private KPlayerListener mListener;
    @NonNull private KPlayerCallback mCallback;
    private boolean mShouldCancelPlay;
    private boolean mShouldPlayWhenReady;
    @Nullable private PlayheadTracker mPlayheadTracker;
    private PrepareState mPrepareState;
    @NonNull private PlayerState mSavedState;
    private boolean isFirstPreparation = true;
    private int mCurrentPosition;
    private boolean mWasDestroyed;
    private String mLastSentEvent = "";

    public static Set<KMediaFormat> supportedFormats(Context context) {
        if (WidevineDrmClient.isSupported(context)) {
            return Collections.singleton(KMediaFormat.wvm_widevine);
        }
        return Collections.emptySet();
    }

    /**
     * Construct a new Widevine Classic player.
     * @param context
     * @throws UnsupportedOperationException if Widevine Classic is not supported by platform.
     */
    public KWVCPlayer(Context context) {
        super(context);
        mDrmClient = new WidevineDrmClient(context);
        mDrmClient.setEventListener(new WidevineDrmClient.EventListener() {
            @Override
            public void onError(final DrmErrorEvent event) {
                mShouldCancelPlay = true;
                KWVCPlayer.this.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.ErrorKey, "DRM error");
                    }
                });
            }

            @Override
            public void onEvent(DrmEvent event) {

            }
        });

        mSavedState = new PlayerState();

        // Set no-op listeners so we don't have to check for null on use
        setPlayerListener(null);
        setPlayerCallback(null);
    }

    // Convert file:///local/path/a.wvm to /local/path/a.wvm
    // Convert http://example.com/path/a.wvm to widevine://example.com/path/a.wvm
    // Every else remains the same.
    public static String getWidevineAssetPlaybackUri(String assetUri) {
        if (assetUri.startsWith("file:")) {
            assetUri = Uri.parse(assetUri).getPath();
        } else if (assetUri.startsWith("http:")) {
            assetUri = assetUri.replaceFirst("^http:", "widevine:");
        }
        return assetUri;
    }

    // Convert file:///local/path/a.wvm to /local/path/a.wvm
    // Convert widevine://example.com/path/a.wvm to http://example.com/path/a.wvm
    // Everything else remains the same.
    public static String getWidevineAssetAcquireUri(String assetUri) {
        if (assetUri.startsWith("file:")) {
            assetUri = Uri.parse(assetUri).getPath();
        } else if (assetUri.startsWith("widevine:")) {
            assetUri = assetUri.replaceFirst("widevine", "http");
        }
        return assetUri;
    }


    @Override
    public void setPlayerListener(KPlayerListener listener) {
        if (listener == null) {
            // Create a no-op listener
            listener = new KPlayerListener() {
                public void eventWithValue(KPlayer player, String eventName, String eventValue) {}
                public void eventWithJSON(KPlayer player, String eventName, String jsonValue) {}
                public void asyncEvaluate(String expression, String expressionID, PlayerViewController.EvaluateListener evaluateListener) {}
                public void contentCompleted(KPlayer currentPlayer) {}
            };
        }
        mListener = listener;
    }

    @Override
    public void setPlayerCallback(KPlayerCallback callback) {
        if (callback == null) {
            // Create a no-op callback
            callback = new KPlayerCallback() {
                public void playerStateChanged(int state) {}
            };
        }
        mCallback = callback;
    }

    @Override
    public void setPlayerSource(String source) {

        mAssetUri = source;

        if (mLicenseUri != null) {
            isFirstPreparation = true;
            preparePlayer();
        } else {
            LOGD(TAG, "setPlayerSource: waiting for licenseUri.");
        }
    }

    @Override
    public void setLicenseUri(String licenseUri) {
        mLicenseUri = licenseUri;
        if (mAssetUri != null) {
            preparePlayer();
        } else {
            LOGD(TAG, "setLicenseUri: Waiting for assetUri.");
        }
    }

    @Override
    public long getCurrentPlaybackTime() {
        int currentPos = (mPlayer != null) ? mPlayer.getCurrentPosition() : 0;
        LOGD(TAG, "getCurrentPlaybackTime: current position = " + currentPos);
        return currentPos;
    }

    @Override
    public void setCurrentPlaybackTime(long currentPlaybackTime) {
        if (mPlayer != null) {
            LOGD(TAG, "setCurrentPlaybackTime: seekTo currentPlaybackTime " + currentPlaybackTime);
            mPlayer.seekTo((int) (currentPlaybackTime));
        }
    }

    @Override
    public long getDuration() {
        return mPlayer != null ? mPlayer.getDuration() : 0;
    }

    @Override
    public void play() {

        // If already playing, don't do anything.
        if (mWasDestroyed || mPlayer == null || mPlayer.isPlaying()) {
            return;
        }

        // If play should be canceled, don't even start.
        if (mShouldCancelPlay) {
            mShouldCancelPlay = false;
            return;
        }

        // If not prepared, ask player to start when prepared.
        if (mPrepareState != PrepareState.Prepared) {
            mShouldPlayWhenReady = true;
            preparePlayer();
            return;
        }

        if (mSavedState.position != 0) {
            mShouldPlayWhenReady = true;
            setCurrentPlaybackTime(mSavedState.position); // will start playing after seek complete
            return;
            //mSavedState.position = 0;
        }
        mPlayer.requestFocus();
        mPlayer.start();

        if (mPlayheadTracker == null) {
            mPlayheadTracker = new PlayheadTracker();
        }
        mPlayheadTracker.start();
        changePlayPauseState("play");
    }

    @Override
    public void pause() {
        if (mWasDestroyed) {
            return;
        }
        if (mPrepareState == PrepareState.Ended) {
            return;
        }

        if (mPlayer != null) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();
                changePlayPauseState("pause");
            }
        }
        stopPlayheadTracker();
    }

    private void changePlayPauseState(final String state) {
        if (mPlayer == null || state == null) {
            return;
        }
        mPlayer.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (state == KPlayerListener.PauseKey) {
                    if (mLastSentEvent == KPlayerListener.PauseKey){
                        return;
                    }
                    if (mPlayer != null && (!mPlayer.isPlaying()) || mLastSentEvent == KPlayerListener.PlayKey) {
                        mLastSentEvent = KPlayerListener.PauseKey;
                        mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.PauseKey, null);
                        return;
                    }
                } else if (state == KPlayerListener.PlayKey) {
                    if (mLastSentEvent == KPlayerListener.PlayKey){
                        return;
                    }
                    if (mPlayer != null && (mPlayer.isPlaying() || mLastSentEvent == KPlayerListener.PauseKey)) {
                        LOGD(TAG, "changePlayPauseState SEND PLAYKEY");
                        mLastSentEvent = KPlayerListener.PlayKey;
                        mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.PlayKey, null);
                        return;
                    }
                } else {
                    LOGE(TAG, "Unsupported state " + state + " was used in changePlayPauseState");
                    return;

                }
                changePlayPauseState(state);
            }
        }, 100);
    }

    @Override
    public boolean isPlaying() {
        if (mPlayer != null) {
            return mPlayer.isPlaying();
        }
        return false;
    }

    @Override
    public void switchToLive() {
        LOGW(TAG, "switchToLive is not implemented for Widevine Classic player");
    }

    @Override
    public TrackFormat getTrackFormat(TrackType trackType, int index) {
        return null;
    }

    @Override
    public int getTrackCount(TrackType trackType) {
        return 0;
    }

    @Override
    public int getCurrentTrackIndex(TrackType trackType) {
        return -1;
    }

    @Override
    public void switchTrack(TrackType trackType, int newIndex) {

    }

    @Override
    public void attachSurfaceViewToPlayer() {
        // not required in case of multiplayer and WV classic

    }

    @Override
    public void detachSurfaceViewFromPlayer() {
        // not required in case of multiplayer and WV classic

    }

    @Override
    public void setPrepareWithConfigurationMode() {

    }
    @Override
    public void setPrepareWithConfigurationModeOff() {

    }

    public void savePosition() {
        if(mPlayer != null) {
            LOGD(TAG, "savePosition  mPlayer.getCurrentPosition() = " + mPlayer.getCurrentPosition() + " but  mCurrentPosition = " + mCurrentPosition);
            mSavedState.position = mCurrentPosition;//mPlayer.getCurrentPosition();

        }
    }

    private void savePlayerState() {
        savePosition();
        saveState();
        pause();
    }

    private void recoverPlayerState() {
        LOGD(TAG, "recoverPlayer mSavedState.position = " + mSavedState.position + " mCurrentPosition: " + mCurrentPosition + " getCurrentPlaybackTime() = " + getCurrentPlaybackTime());

        if(getCurrentPlaybackTime() != mCurrentPosition) {
            LOGD(TAG, "recoverPlayer seekTo mCurrentPosition = " + mCurrentPosition);
            mPlayer.seekTo(mCurrentPosition);
            mShouldPlayWhenReady = mSavedState.playing;

        } else if (mSavedState.playing){
            play();
        }
    }

    @Override
    public void freezePlayer() {
//        if (mPlayer != null) {
//            savePlayerState();
//            mPlayer.suspend();
//        }
    }

    private void saveState() {

        if (mPlayer != null) {
            if (!mPlayer.isPlaying()) {
                mShouldPlayWhenReady = false;
            }
            LOGD(TAG, "saveState mPlayer.getCurrentPosition() = " + mPlayer.getCurrentPosition() + " mCurrentPosition = " + mCurrentPosition);
            mSavedState.set(mPlayer.isPlaying(), mCurrentPosition);
            if (mPlayer.getDuration() == mPlayer.getCurrentPosition()) {
                mSavedState.set(false, mPlayer.getCurrentPosition());
            }

        } else {
            LOGD(TAG, "saveState mPlayer == null, position = 0");
            mSavedState.set(false, 0);
        }
    }

    @Override
    public void removePlayer() {
        LOGD(TAG, "removePlayer");
        savePosition();
        saveState();
        pause();
        if (mPlayer != null) {
            mPlayer.stopPlayback();
            removeView(mPlayer);
            mPlayer = null;
        }
        stopPlayheadTracker();
        mPrepareState = PrepareState.NotPrepared;
    }

    private void stopPlayheadTracker() {
        if (mPlayheadTracker != null) {
            mCurrentPosition = (int) mPlayheadTracker.getPlaybackTime() * 1000;
            mPlayheadTracker.stop();
            mPlayheadTracker = null;
        }
    }

    @Override
    public void recoverPlayer(boolean isPlaying) {
        LOGD(TAG, "recoverPlayer mSavedState.position = " + mSavedState.position + " mSavedState.playing: " + mSavedState.playing + " mCurrentPosition: " + mCurrentPosition);

        if (mPlayer != null) {
            LOGD(TAG, "inside recoverPlayer mCurrentPosition = " + mCurrentPosition + " isPlaying = " + isPlaying);
            mSavedState.set(mSavedState.playing, mCurrentPosition);
            mPlayer.resume();

            mWasDestroyed = false;
            LOGD(TAG, "recover PLAY");

            play();
            LOGD(TAG, "recover SEEK");
            setCurrentPlaybackTime(mCurrentPosition);
            if (!isPlaying) {
                LOGD(TAG, "recover PAUSE");
                pause();
            }
        }
    }

    @Override
    public void setShouldCancelPlay(boolean shouldCancelPlay) {
        mShouldCancelPlay = shouldCancelPlay;
    }

    private void preparePlayer() {

        if (mAssetUri==null || mLicenseUri==null) {
            String errMsg  = "Prepare error: both assetUri and licenseUri must be set";
            LOGE(TAG, errMsg);
            mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.ErrorKey, errMsg);
            return;
        }

        if (mPrepareState == PrepareState.Preparing) {
            LOGD(TAG, "Already preparing");
            return;
        }

        String widevineUri = getWidevineAssetPlaybackUri(mAssetUri);

        // Start preparing.
        mPrepareState = PrepareState.Preparing;
        mPlayer = new VideoView(getContext());
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        this.addView(mPlayer, lp);

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mCallback.playerStateChanged(KPlayerCallback.ENDED);
            }
        });
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                String errMsg = "VideoView:onError what = " + what;
                LOGE(TAG, errMsg);
                if (what == -38) {
                    return true;
                }
                if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED || what == MediaPlayer.MEDIA_ERROR_UNKNOWN || what == MediaPlayer.MEDIA_ERROR_IO) {
                    //mp.reset();
                    stopPlayheadTracker();
                    mWasDestroyed = true;
                    mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.PauseKey, null);
                    return true;
                }

                mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.ErrorKey, TAG + "-" + errMsg + "(" + what + "," + extra + ")");
                return true; // prevents the VideoView error popups
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    LOGI(TAG, "onInfo(" + what + "," + extra + ")");
                    return false;
                }
            });
        }
        mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                mPrepareState = PrepareState.Prepared;

                final KWVCPlayer kplayer = KWVCPlayer.this;

                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        LOGD(TAG, "XXX state = onSeekComplete " + mShouldPlayWhenReady + " mPrepareState = " + mPrepareState);
                        if (mCurrentPosition == -1) {
                            mCurrentPosition = mPlayer.getCurrentPosition();
                            return;
                        }
                        if (mPrepareState == PrepareState.Ended) {
                            return;
                        }

                        if(mShouldPlayWhenReady){
                            mShouldPlayWhenReady = false;
                            mSavedState.set(true, 0);
                            play();
                        } else {
                            saveState();
                        }
                        mListener.eventWithValue(kplayer, KPlayerListener.SeekedKey, null);
                        mCallback.playerStateChanged(KPlayerCallback.SEEKED);
                    }
                });

//                mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
//                    @Override
//                    public void onBufferingUpdate(MediaPlayer mp, int percent) {
//                        if (!mWasDestroyed) {
//                            int currPos = mp.getCurrentPosition();
//                            LOGD(TAG, "percent = " + percent + " " + currPos + "/" + mp.getDuration());
//                            //NO NEED TO SEND THIS EXTRA DATA ---- mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.BufferingChangeKey, (percent < 99 && currPos < mp.getDuration()) ? "true" : "false");
//                        }
//                    }
//                });

                if (mSavedState.playing) {
                    LOGD(TAG, "mSavedState.playing = TRUE");
                    // we were already playing, so just resume playback from the saved position
                    mShouldPlayWhenReady = true;
                    long currentPBTime = getCurrentPlaybackTime();
                    if(currentPBTime != mSavedState.position) { //if we need seek first - play will be activate on seek complete
                        LOGD(TAG, "inside setOnPreparedListener seekTo " + mSavedState.position +  " but  getCurrentPlaybackTime() = " + currentPBTime);
                        mCurrentPosition = mSavedState.position;
                        mPlayer.seekTo(mSavedState.position);
                    }

                    //mCallback.playerStateChanged(KPlayerCallback.SEEKED);
                    if (mSavedState.playing) {
                        LOGD(TAG, "SENDING PLAY KEY");
                        mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.PlayKey, null);
                    }

                } else {
                    if(!mShouldCancelPlay) {
                        if (isFirstPreparation) {
                            LOGD(TAG, "STARTING FIRST PLAY");
                            isFirstPreparation = false;
                            mListener.eventWithValue(kplayer, KPlayerListener.DurationChangedKey, Float.toString(kplayer.getDuration() / 1000f));
                            mListener.eventWithValue(kplayer, KPlayerListener.LoadedMetaDataKey, "");
                            mListener.eventWithValue(kplayer, KPlayerListener.CanPlayKey, null);
                            mCallback.playerStateChanged(KPlayerCallback.CAN_PLAY);
                        }
                        if (mShouldPlayWhenReady) {
                            mShouldPlayWhenReady = false;
                            play();
                        }
                    } else {
                        pause();
                    }

                }
            }
        });
        mPlayer.getHolder().addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(SurfaceHolder holder) {
                LOGD(TAG, "surfaceRedrawNeeded");
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                LOGD(TAG, "surfaceCreated");
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LOGD(TAG, "surfaceChanged");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LOGD(TAG, "surfaceDestroyed");
                mWasDestroyed = true;
                savePlayerState();
            }
        });
        mPlayer.setVideoURI(Uri.parse(widevineUri));

        String assetAcquireUri = getWidevineAssetAcquireUri(mAssetUri);

        if(mDrmClient.needToAcquireRights(assetAcquireUri)) {
            mDrmClient.acquireRights(assetAcquireUri, mLicenseUri);
        }
    }

    private enum PrepareState {
        NotPrepared,
        Preparing,
        Prepared,
        Ended
    }

    private class PlayerState {
        boolean playing;
        int position;

        void set(boolean playing, int position) {
            this.playing = playing;
            this.position = position;
        }
    }

    class PlayheadTracker {
        Handler mHandler;
        float playbackTime;
        Runnable mRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (mPlayer.getCurrentPosition() == mPlayer.getDuration()){
                        LOGD(TAG, "--- Video onCompletion ---");
                        mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.TimeUpdateKey, Float.toString(mPlayer.getDuration() / 1000f));
                        mSavedState.playing = false;
                        mSavedState.position = mPlayer.getDuration();
                        mPlayer.pause();
                        mShouldPlayWhenReady = false;

                        mLastSentEvent = KPlayerListener.EndedKey;

                        mPlayer.seekTo(mPlayer.getDuration());
                        mPlayer.resume();
                        mCallback.playerStateChanged(KPlayerCallback.ENDED);
                        mPrepareState = PrepareState.Ended;
                        stopPlayheadTracker();
                        mPlayer.seekTo(mPlayer.getDuration());
                        savePlayerState();
                        mCurrentPosition = mPlayer.getDuration();
                        mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.TimeUpdateKey, Float.toString(mCurrentPosition/ 1000f));
                        return;

                    }

                    if (mPlayer != null && mPlayer.isPlaying()) {
                        int currPos = mPlayer.getCurrentPosition();
                        LOGD(TAG, "progress status = " + currPos + "/" + mPlayer.getDuration());
                        if (currPos > mPlayer.getDuration()) {
                            playbackTime = mPlayer.getDuration() / 1000f;
                        } else {
                            playbackTime = currPos / 1000f;
                            mCurrentPosition = currPos;
                        }

                        mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.TimeUpdateKey, Float.toString(playbackTime));
                    }

                } catch (IllegalStateException e) {
                    String errMsg = "Player Error ";
                    LOGE(TAG, errMsg + e.getMessage());
                    mListener.eventWithValue(KWVCPlayer.this, KPlayerListener.ErrorKey, errMsg + e.getMessage());

                }
                if (mHandler != null) {
                    mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
                }
            }
        };

        float getPlaybackTime() {
            return playbackTime;
        }

        void start() {
            if (mHandler == null) {
                mHandler = new Handler(Looper.getMainLooper());
                mHandler.postDelayed(mRunnable, PLAYHEAD_UPDATE_INTERVAL);
            } else {
                LOGD(TAG, "Tracker is already started");
            }
        }

        void stop() {
            if (mHandler != null) {
                mHandler.removeCallbacks(mRunnable);
                mHandler = null;
            } else {
                LOGD(TAG, "Tracker is not started, nothing to stop");
            }
        }
    }
}
