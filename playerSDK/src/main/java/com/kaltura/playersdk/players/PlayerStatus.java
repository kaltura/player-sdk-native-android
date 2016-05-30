package com.kaltura.playersdk.players;

/**
 * Created by gilad.nadav on 30/05/2016.
 */

public class PlayerStatus {


    private boolean playing;
    private long position;
    private int previousState;
    private int currentState;

    public PlayerStatus() {
        this.playing = false;
        this.position = 0;
        this.previousState = PlayerStates.IDLE;
        this.currentState  = PlayerStates.IDLE;
    }

    public PlayerStatus(boolean playing, long position) {
        this.playing = playing;
        this.position = position;
    }

    void set(boolean playing, long position) {
        this.playing = playing;
        this.position = position;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void setPlaying(boolean playing) {
        this.playing = playing;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    public int getPreviousState() {
        return previousState;
    }

    public void setPreviousState(int previousState) {
        this.previousState = previousState;
    }

    public int getCurrentState() {
        return currentState;
    }

    public void setCurrentState(int currentState) {
        this.currentState = currentState;
    }
}