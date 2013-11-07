package com.kaltura.playersdk;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.widget.VideoView;

import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.types.PlayerStates;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public class PlayerView extends VideoView implements VideoPlayerInterface {
    //TODO make configurable
    public static int PLAYHEAD_UPDATE_INTERVAL = 100;

    private String mVideoUrl;
    private OnPlayerStateChangeListener mPlayerStateListener;
    private MediaPlayer.OnPreparedListener mPreparedListener;
    private OnPlayheadUpdateListener mPlayheadUpdateListener;
    private OnProgressListener mProgressListener;
    private Timer mTimer;

 //  private int mForcedWidth = 0;
 //   private int mForcedHeight = 0;
    
    
 //   private double mOriginRatio = 0.0;

    public PlayerView(Context context) {
        super(context);

        super.setOnCompletionListener( new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stop();
            }
        });

        super.setOnPreparedListener( new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mPlayerStateListener.onStateChanged(PlayerStates.START);
                mediaPlayer.setOnSeekCompleteListener( new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                        if ( mediaPlayer.isPlaying() ) {
                            mPlayerStateListener.onStateChanged(PlayerStates.PLAY);
                        } else {
                            mPlayerStateListener.onStateChanged(PlayerStates.PAUSE);
                        }
                    }
                });

                if ( mPreparedListener != null ) {
                    mPreparedListener.onPrepared(mediaPlayer);
                }

                mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int progress) {
                        if ( mProgressListener != null ) {

                            mProgressListener.onProgressUpdate(progress);
                        }
                    }
                });
                
             /*   mediaPlayer.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {

					@Override
					public void onVideoSizeChanged(MediaPlayer arg0, int arg1,
							int arg2) {
						mOriginRatio = (double) arg0.getVideoWidth() / arg0.getVideoHeight();
						invalidate();
					}
                	
                });*/
            }
        });

    }

    @Override
    public String getVideoUrl() {
        return mVideoUrl;
    }

    @Override
    public void setVideoUrl(String url) {
        mVideoUrl = url;
        super.setVideoURI(Uri.parse(url));
    }

    @Override
    public int getDuration() {
        return super.getDuration();
    }

    public int getCurrentPosition() {
        return super.getCurrentPosition();
    }

    @Override
    public boolean getIsPlaying() {
        return super.isPlaying();
    }

    @Override
    public void play() {
        super.start();
        if ( mTimer == null ) {
            mTimer = new Timer();
        }
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mPlayheadUpdateListener.onPlayheadUpdated(getCurrentPosition());
            }
        }, 0, PLAYHEAD_UPDATE_INTERVAL);

        mPlayerStateListener.onStateChanged(PlayerStates.PLAY);
    }

    @Override
    public void pause() {
        super.pause();
        mPlayerStateListener.onStateChanged(PlayerStates.PAUSE);
    }

    @Override
    public void stop() {
        super.stopPlayback();
        if ( mTimer != null ) {
            mTimer.cancel();
            mTimer = null;
        }
        mPlayerStateListener.onStateChanged(PlayerStates.END);
    }

    @Override
    public void seek(int msec) {
        super.seekTo(msec);
        mPlayerStateListener.onStateChanged(PlayerStates.LOAD);
    }

    @Override
    public void registerPlayerStateChange( OnPlayerStateChangeListener listener) {
        mPlayerStateListener = listener;
    }

    @Override
    public void registerReadyToPlay( MediaPlayer.OnPreparedListener listener) {
        mPreparedListener = listener;
    }

    @Override
    public void registerError( MediaPlayer.OnErrorListener listener) {
        super.setOnErrorListener(listener);

    }

    @Override
    public void registerPlayheadUpdate( OnPlayheadUpdateListener listener ) {
        mPlayheadUpdateListener = listener;
    }

    @Override
    public void registerProgressUpdate ( OnProgressListener listener ) {
        mProgressListener = listener;
    }

    /*
    public void setDimensions(int w, int h) {
    	mForcedWidth = w;
    	mForcedHeight = h;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    	//setMeasuredDimension( 600,300);
    	if ( mForcedWidth != 0 && mForcedHeight != 0 && mOriginRatio != 0 ) {
    		double width;
    		double height;
    		if ( mOriginRatio > 1 )
			{
    			width = mForcedWidth;
				height = width / mOriginRatio;
				
				if ( height > mForcedHeight)
				{
					height = mForcedHeight;
					width = height * mOriginRatio;
				}
			}
			else
			{
				height = mForcedHeight;
				width = height * mOriginRatio;
				
				if ( width > mForcedWidth)
				{
					width = mForcedWidth;
					height = width / mOriginRatio;
				}
			}
    		
    		setMeasuredDimension( (int) width, (int) height);
    	}
    	else {
    		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    	}

    }*/
}


