package com.kaltura.playersdk.interfaces;

import java.util.HashMap;
import java.util.List;

/**
 * Created by nissimpardo on 11/07/16.
 */
public interface KCastMediaRemoteControl {
    void play();
    void pause();
    void seek(long position);
    boolean isPlaying();
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
    void switchTextTrack(int index);

    void setTextTracks(HashMap<String, Integer> textTrackHash);
    void setVideoTracks(List<Integer> videoTracksList);
    HashMap<String, Integer> getTextTracks();
    List<Integer> getVideoTracks();

    interface KCastMediaRemoteControlListener {
        void onCastMediaProgressUpdate(long currentPosition);
        void onCastMediaStateChanged(State state);
        void onTextTrackSwitch(int trackIndex);
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
