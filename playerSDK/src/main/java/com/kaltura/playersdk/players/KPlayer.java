package com.kaltura.playersdk.players;

import com.kaltura.playersdk.KTrackActions;

/**
 * Created by noamt on 07/02/2016.
 */
public interface KPlayer extends KTrackActions {

    void setPlayerListener(KPlayerListener listener);

    void setPlayerCallback(KPlayerCallback callback);

    void setPlayerSource(String playerSource);

    void setCurrentPlaybackTime(long playbackTime);

    long getCurrentPlaybackTime();

    long getDuration();

    void play();

    void pause();

    void changeSubtitleLanguage(String languageCode);

    void freezePlayer();

    void removePlayer();

    void recoverPlayer(boolean isPlaying);

    void setShouldCancelPlay(boolean shouldCancelPlay);

    void setLicenseUri(String licenseUri);

    boolean isPlaying();
}
