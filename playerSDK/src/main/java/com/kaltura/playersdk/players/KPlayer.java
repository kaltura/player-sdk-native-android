package com.kaltura.playersdk.players;

/**
 * Created by noamt on 07/02/2016.
 */
public interface KPlayer {

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

    void recoverPlayer();

    void setShouldCancelPlay(boolean shouldCancelPlay);

    void setLicenseUri(String licenseUri);

    boolean isPlaying();

    void attachSurfaceViewToPlayer();

    void detachSurfaceViewFromPlayer();

    boolean isDRMSrc();
}
