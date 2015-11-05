package com.kaltura.playersdk.helpers;

import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.kaltura.playersdk.players.KPlayer;
import com.kaltura.playersdk.players.KPlayerCallback;
import com.kaltura.playersdk.players.KPlayerController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nissimpardo on 05/11/15.
 */
public class IMAVideoPlayerController implements VideoAdPlayer, ContentProgressProvider {
    private KPlayerController.KPlayer mPlayer;
    private boolean mIsAdDisplayed;
    private final List<VideoAdPlayerCallback> mAdCallbacks =
            new ArrayList<VideoAdPlayerCallback>(1);
    private float mSavedVideoPosition;
    private AdPlayerListener mListener;

    public interface AdPlayerListener {
        public void setIsAdPlaying(boolean isAdPlaying);
    }

    public IMAVideoPlayerController(KPlayerController.KPlayer player) {
        mPlayer = player;

    }
    public void setAdPlayerListener(AdPlayerListener listener) {
        mListener = listener;
    }

    public void onPlay() {
        if (mIsAdDisplayed) {
            for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                callback.onPlay();
            }
        }
    }

    public void onPause() {
        if (mIsAdDisplayed) {
            for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                callback.onPause();
            }
        }
    }

    public void onCompleted() {
        if (mIsAdDisplayed) {
            for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
                callback.onResume();
            }
        }
    }


    @Override
    public VideoProgressUpdate getContentProgress() {
        if (mIsAdDisplayed || mPlayer.getDuration() <= 0) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        return new VideoProgressUpdate((long)mPlayer.getCurrentPlaybackTime() * 1000,
                (long)mPlayer.getDuration() * 1000);
    }

    @Override
    public void playAd() {
        mListener.setIsAdPlaying(true);
        mPlayer.play();
    }

    @Override
    public void loadAd(String s) {
        mPlayer.setPlayerSource(s);
    }

    @Override
    public void stopAd() {
        mPlayer.pause();
    }

    @Override
    public void pauseAd() {
        mPlayer.pause();
    }

    @Override
    public void resumeAd() {
        playAd();
    }

    @Override
    public void addCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
        mAdCallbacks.add(videoAdPlayerCallback);
    }

    @Override
    public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
        mAdCallbacks.remove(videoAdPlayerCallback);
    }

    @Override
    public VideoProgressUpdate getAdProgress() {
        if (!mIsAdDisplayed || mPlayer.getDuration() <= 0) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        return new VideoProgressUpdate((long)mPlayer.getCurrentPlaybackTime() * 1000,
                (long)mPlayer.getDuration() * 1000);
    }
}
