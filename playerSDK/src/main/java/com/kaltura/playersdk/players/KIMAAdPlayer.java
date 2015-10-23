package com.kaltura.playersdk.players;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.SimpleVideoPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nissopa on 7/2/15.
 */
public class KIMAAdPlayer implements  ExoplayerWrapper.PlaybackListener{
    private ViewGroup mAdUIContainer;
    private KAdIMAPlayer mAdPlayer;
    private Activity mActivity;
//    private SimpleVideoPlayer mAdPlayer;
    private KIMAAdPlayerEvents mListener;
//    private final List<VideoAdPlayerCallback> mAdCallbacks =
//            new ArrayList<VideoAdPlayerCallback>(1);


    // [START VideoAdPlayer region]
//    @Override
//    public void playAd() {
//        if (mAdPlayer != null) {
//            mAdPlayer.play();
//        }
//    }
//
//    @Override
//    public void loadAd(String s) {
//        setAdPlayerSource(s);
//    }
//
//    @Override
//    public void stopAd() {
//        if (mAdPlayer != null) {
//            mAdPlayer.pause();
//        }
//    }
//
//    @Override
//    public void pauseAd() {
//        if (mAdPlayer != null) {
//            mAdPlayer.pause();
//        }
//    }
//
//    @Override
//    public void resumeAd() {
//        if (mAdPlayer != null) {
//            mAdPlayer.play();
//        }
//    }
//
//    @Override
//    public void addCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
//        mAdCallbacks.add(videoAdPlayerCallback);
//    }
//
//    @Override
//    public void removeCallback(VideoAdPlayerCallback videoAdPlayerCallback) {
//        mAdCallbacks.remove(videoAdPlayerCallback);
//    }
//
//    @Override
//    public VideoProgressUpdate getAdProgress() {
//        if (mAdPlayer == null) {
//            return new VideoProgressUpdate(0, 0);
//        }
//        if (mAdPlayer.getDuration() <= 0) {
//            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
//        }
//        if (mListener != null) {
//            mListener.adDidProgress(mAdPlayer.getCurrentPosition() / 1000, mAdPlayer.getDuration() / 1000);
//        }
//        return new VideoProgressUpdate(mAdPlayer.getCurrentPosition(), mAdPlayer.getDuration());
//    }
    // [END VideoAdPlayer region]

    // [START ExoplayerWrapper.PlaybackListener region]
    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_READY:
                if (playWhenReady) {
//                    for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//                        callback.onPlay();
//                    }
                }
                break;
            case ExoPlayer.STATE_ENDED:
//                for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//                    callback.onEnded();
//                }
                break;
        }
    }

    @Override
    public void onError(Exception e) {
//        for (VideoAdPlayer.VideoAdPlayerCallback callback : mAdCallbacks) {
//            callback.onError();
//        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {

    }
    // [END ExoplayerWrapper.PlaybackListener region]


    public interface KIMAAdPlayerEvents {
        public void adDidProgress(long toTome, long totalTime);
    }

    public KIMAAdPlayer(Activity activity, KAdIMAPlayer adPlayer, ViewGroup adUIContainer) {
        mActivity = activity;
        mAdPlayer = adPlayer;
        mAdUIContainer = adUIContainer;
    }

    public void setKIMAAdEventListener(KIMAAdPlayerEvents listener) {
        mListener = listener;
    }

    public ViewGroup getAdUIContainer() {
        return mAdUIContainer;
    }

    private void setAdPlayerSource(String src) {
//        Video source = new Video(src.toString(), Video.VideoType.MP4);
//        mAdPlayer = new SimpleVideoPlayer(mActivity, mPlayerContainer, source, "", true);
//        mAdPlayer.addPlaybackListener(this);
//        mPlayerContainer.setVisibility(View.VISIBLE);
//        mAdPlayer.moveSurfaceToForeground();
//        mAdPlayer.disableSeeking();
//        mAdPlayer.hideTopChrome();
        mAdPlayer.setVideoPath(src);
    }

    public void removeAd() {
//        if (mAdPlayer != null) {
//            mAdPlayer.release();
//            mAdPlayer.moveSurfaceToBackground();
//            mPlayerContainer.setVisibility(View.INVISIBLE);
//            mAdPlayer = null;
//        }
    }

    public void release() {
//        if (mAdPlayer != null) {
//            mAdPlayer.release();
//            mAdPlayer.moveSurfaceToBackground();
//        }
//        mAdUIContainer = null;
//        mPlayerContainer = null;
    }
}
