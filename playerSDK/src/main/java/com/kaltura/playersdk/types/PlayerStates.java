package com.kaltura.playersdk.types;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public enum PlayerStates {
    START("start"),
    LOAD("load"),
    PLAY("play"),
    PAUSE("pause"),
    END("ended"),
    SEEKING("seeking"),
    SEEKED("seeked");

    private String label = "";

    PlayerStates(String label){
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
