package com.kaltura.playersdk;

import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;

/**
 * Created by michalradwantzor on 9/15/13.
 * Every video that will be used by Kaltura Player SDK should implement this interface
 */
public interface VideoPlayerInterface {

    public String getVideoUrl();
    public void setVideoUrl(String url);

    public int getDuration();

    public void play();

    public void pause();

    public void stop();

    public void seek(int msec);
    
    public boolean isPlaying();
    
    public boolean canPause();

    // events
    public void registerPlayerStateChange(OnPlayerStateChangeListener listener);

    public void registerError(OnErrorListener listener);

    public void registerPlayheadUpdate(OnPlayheadUpdateListener listener);

    public void removePlayheadUpdateListener();

    public void registerProgressUpdate(OnProgressListener listener);
    
    /**
     * Set starting point in milliseconds for the next play
     * @param point
     */
    public void setStartingPoint(int point);
    
    /**
     * Some players require release when application goes to background
     */
    public void release();
    
    /**
     * Recover from release
     */
    public void recoverRelease();

}
