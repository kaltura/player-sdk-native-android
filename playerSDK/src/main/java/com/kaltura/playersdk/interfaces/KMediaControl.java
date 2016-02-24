package com.kaltura.playersdk.interfaces;

/**
 * Created by nissimpardo on 23/02/16.
 */
public interface KMediaControl {
    void play();
    void pause();
    void seek(double seconds);
    void replay();
}
