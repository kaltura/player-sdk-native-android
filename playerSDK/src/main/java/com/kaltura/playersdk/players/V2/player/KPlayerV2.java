package com.kaltura.playersdk.players.V2.player;

/**
 * Created by gilad.nadav on 06/07/2016.
 */

import com.kaltura.playersdk.players.V2.ad.AdBreak;
import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;

import java.util.List;

/**
 * Created by noamt on 07/02/2016.
 */
public interface KPlayerV2 {

    void load(List<PlaylistItem> playlist, AdBreak.KAdConfig advertising);	//Loads a new playlist and advertising options into the player.
    void load(List<PlaylistItem> playlist);	//Loads a new playlist into the player.
    void load(PlaylistItem playlistItem);	//Loads media into the player.
    PlayerState getState();	     //Returns the player's current playback state
    void play();	             //Toggle playing state of a video. If playing, pauses. If paused, resumes.
    void play(boolean state);	//Starts or suspends playback.
    void pause();	            //Toggle playing state of a video. If playing, pauses. If paused, resumes.
    void pause(boolean state);	//Suspend or resume playback.
    void stop();	            //Stops the player and unloads the currently playing media file
    void seek(int position);	//Seeks the currently playing media to the specified position.
    long getPosition();	        //Returns the current playback position
    long getDuration();         //Returns the duration of the current media

    TrackFormat getTrackFormat(TrackType trackType, int index);
    int getTrackCount(TrackType trackType);
    int getCurrentTrackIndex(TrackType trackType);
    void switchTrack(TrackType trackType, int newIndex);

    //
    //    void setPlayerListener(KPlayerListener listener);
    //
    //    void setPlayerCallback(KPlayerCallback callback);
    //
    //    void freezePlayer();
    //
    //    void removePlayer();
    //
    //    void recoverPlayer(boolean isPlaying);
    //
    //    void setShouldCancelPlay(boolean shouldCancelPlay);
    //
    //    void setLicenseUri(String licenseUri);
    //
    //    void switchToLive();
    //


}