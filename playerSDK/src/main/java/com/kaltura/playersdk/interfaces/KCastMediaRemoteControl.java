package com.kaltura.playersdk.interfaces;

/**
 * Created by nissimpardo on 11/07/16.
 */
public interface KCastMediaRemoteControl {
    void play();
    void pause();
    void seek(long position);
    void addListener(KCastMediaRemoteControlListener listener);
    void removeListeners();
    void setStreamVolume(double streamVolume);
    double getCurrentVolume();
    boolean isMute();
    void removeListener(KCastMediaRemoteControlListener listener);
    State getCastMediaRemoteControlState();
    long getCurrentPosition();
    long getDuration();
    boolean hasMediaSession(); // if there is conncetion esteblished  & working

    interface KCastMediaRemoteControlListener {
        void onCastMediaProgressUpdate(long currentPosition);
        void onCastMediaStateChanged(State state);
    }

    enum State {
        Idle,
        Playing,
        Pause,
        Loaded,
        Seeking,
        Seeked,
        Ended,
        VolumeChanged
    }
}
