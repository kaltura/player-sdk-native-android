package com.kaltura.playersdk.players;

import android.content.Context;
import android.media.MediaPlayer;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nissimpardo on 22/10/15.
 */
public class KAdIMAPlayer extends VideoView implements VideoAdPlayer {
    private final List<VideoAdPlayer.VideoAdPlayerCallback> mAdCallbacks =
            new ArrayList<VideoAdPlayerCallback>(1);
    private MediaController mMediaController;
    private PlaybackState mState;
    private KAdIMAPlayerProgress mListener;

    private enum PlaybackState {
        STOPPED, PAUSED, PLAYING
    }

    public interface KAdIMAPlayerProgress {
        public void adProgress(float currentPosition, float duration);
    }

    public KAdIMAPlayer(Context context) {
        super(context);
        init();
    }

    private void init() {
        mState = PlaybackState.STOPPED;
//        mMediaController = new MediaController(getContext());
//        mMediaController.setAnchorView(mAnchor);
        super.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.reset();
                mp.setDisplay(getHolder());
                mState = PlaybackState.STOPPED;
                for (VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onEnded();
                }
            }
        });

        super.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mState = PlaybackState.STOPPED;
                for (VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onError();
                }

                // Returning true signals to MediaPlayer that we handled the error. This will
                // prevent the completion handler from being called.
                return true;
            }
        });
    }

    public void setListener(KAdIMAPlayerProgress listener) {
        mListener = listener;
    }

    public void remove() {
        setVisibility(View.INVISIBLE);
    }

    public void resume() {
        setVisibility(View.VISIBLE);
    }

    @Override
    public void playAd() {
        start();
    }

    @Override
    public void loadAd(String s) {
        setVideoPath(s);
    }

    @Override
    public void stopAd() {
        stopPlayback();
    }

    @Override
    public void pauseAd() {
        pause();
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
        if (getDuration() <= 0) {
            return VideoProgressUpdate.VIDEO_TIME_NOT_READY;
        }
        mListener.adProgress((float)getCurrentPosition() / 1000, (float)getDuration() / 1000);
        return new VideoProgressUpdate(getCurrentPosition(), getDuration());
    }

    @Override
    public void start() {
        super.start();
        // Fire callbacks before switching playback state.
        switch (mState) {
            case STOPPED:
                for (VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onPlay();
                }
                break;
            case PAUSED:
                for (VideoAdPlayerCallback callback : mAdCallbacks) {
                    callback.onResume();
                }
                break;
            default:
                // Already playing; do nothing.
        }
        mState = PlaybackState.PLAYING;
    }

    @Override
    public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener) {
        // The OnCompletionListener can only be implemented by SampleVideoPlayer.
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOnErrorListener(MediaPlayer.OnErrorListener listener) {
        // The OnErrorListener can only be implemented by SampleVideoPlayer.
        throw new UnsupportedOperationException();
    }

    @Override
    public void stopPlayback() {
        super.stopPlayback();
        mState = PlaybackState.STOPPED;
    }

    @Override
    public void pause() {
        super.pause();
        mState = PlaybackState.PAUSED;
        for (VideoAdPlayerCallback callback: mAdCallbacks) {
            callback.onPause();
        }
    }

}
