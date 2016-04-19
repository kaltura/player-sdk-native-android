package com.kaltura.playersdk.interfaces;

import com.kaltura.playersdk.events.KPlayerState;

/**
 * Created by nissimpardo on 23/02/16.
 */
public interface KMediaControl {
    void start();
    void pause();
    void seek(double seconds);
    void replay();
    boolean canPause();
    int getCurrentPosition();
    int getDuration();
    boolean isPlaying();
    boolean canSeekBackward();
    boolean canSeekForward();
    void seek(long milliSeconds, SeekCallback callback);
    interface SeekCallback {
        void seeked(long milliSeconds);
    }
    KPlayerState state();
}
