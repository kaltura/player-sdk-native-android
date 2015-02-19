package com.kaltura.hlsplayersdk;

import com.kaltura.hlsplayersdk.events.OnDurationChangedListener;
import com.kaltura.hlsplayersdk.events.OnErrorListener;
import com.kaltura.hlsplayersdk.events.OnPlayerStateChangeListener;
import com.kaltura.hlsplayersdk.events.OnPlayheadUpdateListener;
import com.kaltura.hlsplayersdk.events.OnProgressListener;

public interface VideoPlayerInterface {
    public String getVideoUrl();
    public void setVideoUrl(String url);

    public int getDuration();

    public boolean getIsPlaying();

    public void play();

    public void pause();

    public void stop();

    public void seek(int msec);
    
    public boolean isPlaying();
    
    public void close();

    // events
    public void registerPlayerStateChange(OnPlayerStateChangeListener listener);
    
    public void registerError(OnErrorListener listener);

    public void registerPlayheadUpdate(OnPlayheadUpdateListener listener);
    
    public void registerDurationChanged(OnDurationChangedListener listener);

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
