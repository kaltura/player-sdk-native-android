package com.kaltura.playersdk.players;

import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;

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

    void freezePlayer();

    void removePlayer();

    void recoverPlayer(boolean isPlaying);

    void setShouldCancelPlay(boolean shouldCancelPlay);

    void setLicenseUri(String licenseUri);

    boolean isPlaying();

    void switchToLive();

    TrackFormat getTrackFormat(TrackType trackType, int index);

    int getTrackCount(TrackType trackType);

    int getCurrentTrackIndex(TrackType trackType);

    void switchTrack(TrackType trackType, int newIndex);

    void attachSurfaceViewToPlayer();

    void detachSurfaceViewFromPlayer();

    void setPrepareWithConfigurationMode();

}
