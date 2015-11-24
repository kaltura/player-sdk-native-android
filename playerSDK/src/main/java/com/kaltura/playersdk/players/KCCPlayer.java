package com.kaltura.playersdk.players;

import android.content.Context;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.kaltura.playersdk.types.PlayerStates;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by nissimpardo on 23/11/15.
 */
public class KCCPlayer implements KPlayerController.KPlayer {
    private Context mContext;
    private MediaInfo mMediaInfo;
    private MediaMetadata mMovieMetadata;
    private VideoCastManager mCastManager;
    private String mPlayerSource;
    private float mCurrentPlaybackTime = 0;

    public KCCPlayer(Context context, String applicationId) {
        mContext = context;
        mCastManager = VideoCastManager.initialize(context, applicationId, null, null);
        mCastManager.enableFeatures(VideoCastManager.FEATURE_NOTIFICATION | VideoCastManager.FEATURE_DEBUGGING);
        mCastManager.setContext(context);
        mMovieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
    }

    @Override
    public void setPlayerListener(KPlayerListener listener) {

    }

    @Override
    public void setPlayerCallback(KPlayerCallback callback) {

    }

    @Override
    public void setPlayerSource(String playerSource) {
        mPlayerSource = playerSource;
    }

    @Override
    public String getPlayerSource() {
        return mPlayerSource;
    }

    @Override
    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mCurrentPlaybackTime = currentPlaybackTime;
    }

    @Override
    public float getCurrentPlaybackTime() {
        int pos = 0;
        try {
            if ( mCastManager.isRemoteMediaLoaded() ) {
                //convert to milliseconds
                pos = (int)(mCastManager.getCurrentMediaPosition() );
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return pos;
    }

    @Override
    public float getDuration() {
        int duration = 0;
        try {
            if ( mCastManager.isRemoteMediaLoaded() ) {
                duration = (int) mCastManager.getMediaDuration();
            }
        } catch (TransientNetworkDisconnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoConnectionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return duration;
    }

    @Override
    public void play() {
        try {
            if ( mMediaInfo == null ) {
                mMediaInfo =  new MediaInfo.Builder(mPlayerSource)
                        .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
                        .setContentType("video/mp4")
                        .setMetadata(mMovieMetadata)
                        .build();

                mCastManager.loadMedia(mMediaInfo, true, (int)(mCurrentPlaybackTime * 1000));
//                mCastManager.getRemoteMediaPlayer().setOnStatusUpdatedListener(this);
//                if ( mTimer == null ) {
//                    mTimer = new Timer();
//                }
//                mTimer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        int newPos = getCurrentPosition();
//                        if (newPos > 0) {
//                            mListenerExecutor.executeOnProgressUpdate(newPos);
//                            if (newPos >= getDuration()) {
//                                mListenerExecutor.executeOnStateChanged(PlayerStates.END);
//                            }
//                        }
//                    }
//                }, 0, PLAYHEAD_UPDATE_INTERVAL);
//
////                mListenerExecutor.executeOnStateChanged(PlayerStates.PLAY);
//
//            } else if ( mCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_PAUSED ) {
//                mCastManager.play();
//                mListenerExecutor.executeOnStateChanged(PlayerStates.PLAY);
            }


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void changeSubtitleLanguage(String languageCode) {

    }

    @Override
    public void removePlayer() {

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

    }
}
