package com.kaltura.playersdk.players;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.kaltura.playersdk.widevine.WidevineDrmClient;

/**
 * Created by noamt on 10/27/15.
 */
public class KWVCPlayer 
        extends FrameLayout
        implements KPlayerController.KPlayer {

    private static final String TAG = "KWVCPlayer";
    private static final long PLAYHEAD_UPDATE_INTERVAL = 200;
    @Nullable private VideoView mPlayer;
    private String mAssetUri;
    private String mLicenseUri;
    private WidevineDrmClient mDrmClient;
    @NonNull private KPlayerListener mListener;
    @NonNull private KPlayerCallback mCallback;
    private Handler mTimeUpdateHandler;
    private boolean mShouldCancelPlay;
    private boolean mShouldPlayWhenReady;

    private enum PrepareState {
        NotPrepared,
        Preparing,
        Prepared
    }
    private PrepareState mPrepareState;

    private class PlayerState {
        boolean playing;
        int position;
        
        void set(boolean playing, int position) {
            this.playing = playing;
            this.position = position;
        }
    }
    @NonNull private PlayerState mSavedState;


    public KWVCPlayer(Context context) {
        super(context);
        mDrmClient = new WidevineDrmClient(context);
        mSavedState = new PlayerState();
        
        // Set no-op listeners so we don't have to check for null on use
        setPlayerListener(null);
        setPlayerCallback(null);
    }

    @Override
    public void setPlayerListener(KPlayerListener listener) {
        if (listener == null) {
            // Create a no-op listener
            listener = new KPlayerListener() {
                public void eventWithValue(KPlayerController.KPlayer player, String eventName, String eventValue) {}
                public void eventWithJSON(KPlayerController.KPlayer player, String eventName, String jsonValue) {}
                public void contentCompleted(KPlayerController.KPlayer currentPlayer) {}
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

        // There is a known issue with some devices, reported mainly against Samsung devices.
        // When playing http://example.com/file.wvm, try widevine://example.com/file.wvm instead.
        // We already know this is a .wvm URL, it was checked by KPlayerController.
        // Only do this replacement for http -- NOT https.
        source = source.replaceFirst("^http:", "widevine:");
        
        mAssetUri = source;

        if (mLicenseUri != null) {
            preparePlayer();
        } else {
            Log.d(TAG, "setPlayerSource: waiting for licenseUri.");
        }
    }

    @Override
    public String getPlayerSource() {
        return mAssetUri;
    }

    @Override
    public void setLicenseUri(String licenseUri) {
        mLicenseUri = licenseUri;

        if (mAssetUri != null) {
            preparePlayer();
        } else {
            Log.d(TAG, "setLicenseUri: Waiting for assetUri.");
        }
    }
    
    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        if (mPlayer != null) {
            mPlayer.seekTo((int) (currentPlaybackTime * 1000));
        }
    }

    @Override
    public float getCurrentPlaybackTime() {
        return mPlayer != null ? mPlayer.getCurrentPosition() / 1000f : 0;
    }

    @Override
    public float getDuration() {
        return mPlayer != null ? mPlayer.getDuration() / 1000f : 0;
    }

    @Override
    public void play() {
        
        // If already playing, don't do anything.
        if (mPlayer != null && mPlayer.isPlaying()) {
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
        
        assert mPlayer != null;
        mPlayer.start();

        if (mTimeUpdateHandler == null) {
            mTimeUpdateHandler = new Handler(Looper.getMainLooper());
        } else {
            mTimeUpdateHandler.removeCallbacks(null);
        }

        mTimeUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    float playbackTime = 0;
                    if (mPlayer != null && mPlayer.isPlaying()) {
                        playbackTime = getCurrentPlaybackTime();
//                        Log.i(TAG, "TimeUpdate: " + playbackTime);
                        mListener.eventWithValue(KWVCPlayer.this, KPlayer.TimeUpdateKey, String.valueOf(playbackTime));
                    }
                    
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Error", e);
                }
                mTimeUpdateHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
            }
        }, PLAYHEAD_UPDATE_INTERVAL);
        
        mListener.eventWithValue(this, KPlayer.PlayKey, null);
    }

    @Override
    public void pause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
        saveState();
    }

    @Override
    public void changeSubtitleLanguage(String languageCode) {
        // TODO: forward to player
    }

    private void saveState() {
        if (mPlayer != null) {
            mSavedState.set(mPlayer.isPlaying(), mPlayer.getCurrentPosition());
        } else {
            mSavedState.set(false, 0);
        }
    }

    @Override
    public void removePlayer() {
        saveState();
        if (mPlayer != null) {
            mPlayer.stopPlayback();
            removeView(mPlayer);
            mPlayer = null;
        }
        mTimeUpdateHandler.removeCallbacks(null);
        mPrepareState = PrepareState.NotPrepared;
    }

    @Override
    public void recoverPlayer() {
        
    }

    @Override
    public boolean isKPlayer() {
        return false;
    }

    @Override
    public void setShouldCancelPlay(boolean shouldCancelPlay) {
        mShouldCancelPlay = shouldCancelPlay;
    }
    
    private void preparePlayer() {

        if (mPrepareState == PrepareState.Preparing) {
            return;
        }

        // Make sure we have both licenseUri and assetUri
        // This is a private method and the callers make sure both of those fields are set.
        assert mAssetUri!=null;
        assert mLicenseUri!=null;
        
        mPrepareState = PrepareState.Preparing;
        
        // now really prepare
        mPlayer = new VideoView(getContext());
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        this.addView(mPlayer, lp);

        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mListener.contentCompleted(KWVCPlayer.this);
                mCallback.playerStateChanged(KPlayerController.ENDED);
            }
        });
        mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.e(TAG, "VideoView:onError(" + what + "," + extra + ")");
                // TODO
                return false;
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    Log.i(TAG, "onInfo(" + what + "," + extra + ")");
                    // TODO
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
                        saveState();
                        mListener.eventWithValue(kplayer, KPlayer.SeekedKey, null);
                    }
                });

                if (mSavedState.playing) {
                    // we were already playing, so just resume playback from the saved position
                    mPlayer.seekTo(mSavedState.position);
                    play();
                } else {
                    mListener.eventWithValue(kplayer, KPlayer.DurationChangedKey, String.valueOf(kplayer.getDuration()));
                    mListener.eventWithValue(kplayer, KPlayer.LoadedMetaDataKey, "");
                    mListener.eventWithValue(kplayer, KPlayer.CanPlayKey, null);
                    mCallback.playerStateChanged(KPlayerController.CAN_PLAY);

                    if (mShouldPlayWhenReady) {
                        play();
                        mShouldPlayWhenReady = false;
                    }
                }
            }
        });
        mPlayer.setVideoURI(Uri.parse(mAssetUri));
        mDrmClient.acquireRights(mAssetUri, mLicenseUri);
    }
}
