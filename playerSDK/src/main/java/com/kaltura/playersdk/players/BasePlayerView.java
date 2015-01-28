package com.kaltura.playersdk.players;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;

/**
 * Created by itayi on 1/28/15.
 */
public abstract class BasePlayerView extends FrameLayout {

    public BasePlayerView(Context context) {
        super(context);
    }

    public BasePlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BasePlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public abstract String getVideoUrl();
    public abstract void setVideoUrl(String url);

    public abstract int getDuration();

    public abstract void play();

    public abstract void pause();

    public abstract void stop();

    public abstract void seek(int msec);

    public abstract boolean isPlaying();

    public abstract boolean canPause();

    // events
    public abstract void registerPlayerStateChange(OnPlayerStateChangeListener listener);

    public abstract void registerError(OnErrorListener listener);

    public abstract void registerPlayheadUpdate(OnPlayheadUpdateListener listener);

    public abstract void removePlayheadUpdateListener();

    public abstract void registerProgressUpdate(OnProgressListener listener);

    /**
     * Set starting point in milliseconds for the next play
     * @param point
     */
    public abstract void setStartingPoint(int point);

    /**
     * Some players require release when application goes to background
     */
    public abstract void release();

    /**
     * Recover from release
     */
    public abstract void recoverRelease();
}
