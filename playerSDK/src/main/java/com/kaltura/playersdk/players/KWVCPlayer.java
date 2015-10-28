package com.kaltura.playersdk.players;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
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
    private VideoView mPlayer;
    private String mAssetUri;
    private String mLicenseUri;
    private WidevineDrmClient mDrmClient;
    private KPlayerListener mListener;
    private KPlayerCallback mCallback;
    private Handler mTimeUpdateHandler;
    private int mStartPos;
    private boolean mShouldCancelPlay;
    private boolean mPrepared;
    
    private class PlayerState {
        boolean playing;
        int position;
    }
    private PlayerState mSavedState = new PlayerState();


    public KWVCPlayer(Context context) {
        super(context);
        mDrmClient = new WidevineDrmClient(context);

        // Create default empty listeners, so that we don't have to ask != null every time.
        mListener = new KPlayerListener() {
            public void eventWithValue(KPlayerController.KPlayer player, String eventName, String eventValue) {}
            public void eventWithJSON(KPlayerController.KPlayer player, String eventName, String jsonValue) {}
            public void contentCompleted(KPlayerController.KPlayer currentPlayer) {}
        };
        
        mCallback = new KPlayerCallback() {
            public void playerStateChanged(int state) {}
        };
        
    }

    @Override
    public void setPlayerListener(KPlayerListener listener) {
        mListener = listener;
    }

    @Override
    public void setPlayerCallback(KPlayerCallback callback) {
        mCallback = callback;
    }

    @Override
    public void setPlayerSource(String playerSource) {
        mAssetUri = playerSource;

        preparePlayer();
    }

    @Override
    public String getPlayerSource() {
        return mAssetUri;
    }

    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mPlayer.seekTo((int) (currentPlaybackTime*1000));
        mListener.eventWithValue(this, KPlayer.SeekedKey, null);
    }

    @Override
    public float getCurrentPlaybackTime() {
        return mPlayer.getCurrentPosition() / 1000f;
    }

    @Override
    public float getDuration() {
        return mPlayer.getDuration() / 1000f;
    }

    @Override
    public void play() {
        
        if (mPlayer != null && mPlayer.isPlaying()) {
            return;
        }

        if (mShouldCancelPlay) {
            mShouldCancelPlay = false;
            return;
        }
        if ( !mPrepared ) {
            preparePlayer();
        }

        mPlayer.start();
        if (mStartPos != 0) {
            mPlayer.seekTo(mStartPos);
            mStartPos = 0;
        }


        if (mTimeUpdateHandler == null) {
            mTimeUpdateHandler = new Handler(Looper.getMainLooper());
        } else {
            mTimeUpdateHandler.removeCallbacks(null);
        }

        mTimeUpdateHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mPlayer.isPlaying()) {
                        float playbackTime = getCurrentPlaybackTime();
                        Log.i(TAG, "TimeUpdate: " + playbackTime);
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
        mPlayer.pause();
    }

    @Override
    public void changeSubtitleLanguage(String languageCode) {
        // TODO: forward to player
    }

    private void saveState() {
        mSavedState.playing = mPlayer.isPlaying();
        mSavedState.position = mPlayer.getCurrentPosition();
    }

    @Override
    public void removePlayer() {
        saveState();
        mPlayer.stopPlayback();
        removeView(mPlayer);
        mPlayer = null;
        mTimeUpdateHandler.removeCallbacks(null);
        mPrepared = false;
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
    
    @Override
    public void setLicenseUri(String licenseUri) {
        mLicenseUri = licenseUri;

        preparePlayer();
    }

    private void preparePlayer() {
        // make sure we have both licenseUri and assetUri
        if (mAssetUri == null) {
            Log.d(TAG, "assetUri is missing, can't play yet.");
            return;
        }
        if (mLicenseUri == null) {
            Log.d(TAG, "licenseUri is missing, can't play yet.");
            return;
        }
        
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
                mPrepared = true;
                KWVCPlayer player = KWVCPlayer.this;
                if (mSavedState.playing) {
                    mPlayer.seekTo(mSavedState.position);
                    play();
                } else {
                    mListener.eventWithValue(player, KPlayer.DurationChangedKey, String.valueOf(player.getDuration()));
                    mListener.eventWithValue(player, KPlayer.LoadedMetaDataKey, "");
                    mListener.eventWithValue(player, KPlayer.CanPlayKey, null);
                    mCallback.playerStateChanged(KPlayerController.CAN_PLAY);
                }
            }
        });
        mPlayer.setVideoURI(Uri.parse(mAssetUri));
        mDrmClient.acquireRights(mAssetUri, mLicenseUri);
    }
}
