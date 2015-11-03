package com.kaltura.playersdk.helpers;

import com.kaltura.playersdk.players.KPlayerCallback;
import com.kaltura.playersdk.players.KPlayerListener;

/**
 * Created by nissopa on 7/7/15.
 */
public class KPlayerParams {
    private float duration;
    private float currentPlaybackTime;
    private String playerSource;
    private KPlayerListener playerListener;
    private KPlayerCallback playerCallback;

    public KPlayerListener getPlayerListener() {
        return playerListener;
    }

    public void setPlayerListener(KPlayerListener playerListener) {
        this.playerListener = playerListener;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public float getCurrentPlaybackTime() {
        return currentPlaybackTime;
    }

    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        this.currentPlaybackTime = currentPlaybackTime;
    }

    public String getPlayerSource() {
        return playerSource;
    }

    public void setPlayerSource(String playerSource) {
        this.playerSource = playerSource;
    }

    public KPlayerCallback getPlayerCallback() {
        return playerCallback;
    }

    public void setPlayerCallback(KPlayerCallback playerCallback) {
        this.playerCallback = playerCallback;
    }


}
