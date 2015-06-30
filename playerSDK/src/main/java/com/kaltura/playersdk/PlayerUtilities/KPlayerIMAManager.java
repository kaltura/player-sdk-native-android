package com.kaltura.playersdk.PlayerUtilities;

import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.kaltura.playersdk.players.KPlayerController;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nissopa on 6/25/15.
 */
public class KPlayerIMAManager implements ContentProgressProvider, VideoAdPlayer {
    private KPlayerController.KPlayer mPlayer;
    private boolean mIsAdPresented;
    private float mContentPosition;
    private boolean mIsContentCompleted;

    private final List<VideoAdPlayerCallback> mAdCallbacks = new ArrayList<VideoAdPlayerCallback>(1);

    public KPlayerIMAManager(KPlayerController.KPlayer player) {
        mPlayer = player;
    }

    public void onPlay() {
        if (mIsAdPresented) {
            for (VideoAdPlayerCallback callback: mAdCallbacks) {
                callback.onPlay();
            }
        }
    }

    public void onError() {
        if (mIsAdPresented) {
            for (VideoAdPlayerCallback callback: mAdCallbacks) {
                callback.onError();
            }
        }
    }

    public void onContentCompleted() {
        if (mIsAdPresented) {
            for (VideoAdPlayerCallback callback: mAdCallbacks) {
                callback.onEnded();
            }
        } else {
            mIsContentCompleted = true;
        }
    }

    public void pauseContentForAdPlayback() {
        mContentPosition = mPlayer.getCurrentPlaybackTime();
//        mPlayer.stop();
    }

    public void resumeContentAfterAdPlayback(String contentURL) {
        if (contentURL != null || !contentURL.isEmpty()) {
            return;
        }
        mIsAdPresented = false;
        mPlayer.setPlayerSource(contentURL);
        mPlayer.setCurrentPlaybackTime(mContentPosition);
        if (mIsContentCompleted) {
//            mPlayer.stop();
        } else {
            mPlayer.play();
        }
    }


    @Override
    public VideoProgressUpdate getContentProgress() {
        if (mIsAdPresented || mPlayer.getDuration() <= 0) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        return new VideoProgressUpdate((long)mPlayer.getCurrentPlaybackTime() * 1000, (long)mPlayer.getDuration() * 1000);
    }

    @Override
    public void playAd() {
        mIsAdPresented = true;
        mPlayer.play();
    }

    @Override
    public void loadAd(String s) {
        mIsAdPresented = true;
        mPlayer.setPlayerSource(s);
    }

    @Override
    public void stopAd() {
//        mPlayer.stop();
    }

    @Override
    public void pauseAd() {

    }

    @Override
    public void resumeAd() {

    }

    @Override
    public void addCallback(VideoAdPlayerCallback videoAdPlayerCallback) {

    }

    @Override
    public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {

    }


    @Override
    public VideoProgressUpdate getAdProgress() {
        return null;
    }
}
