package com.kaltura.playersdk.players;

import com.kaltura.playersdk.types.KPlayerState;

/**
 * Created by nissopa on 6/30/15.
 */
public interface KPlayerCallback {
    /**
     * Called when the current video starts playing from the beginning.
     */
    void onPlay();

    /**
     * Called when the current video has completed playback to the end of the video.
     */
    void onCompleted();

    /**
     * Called when an error occurs during video playback.
     */
    void onError();

    void playerStateChanged(KPlayerState state);
}

