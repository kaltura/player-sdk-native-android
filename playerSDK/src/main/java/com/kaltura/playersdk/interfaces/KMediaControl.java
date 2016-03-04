package com.kaltura.playersdk.interfaces;

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
}
