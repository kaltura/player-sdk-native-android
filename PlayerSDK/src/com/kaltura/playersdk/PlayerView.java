package com.kaltura.playersdk;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
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
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;

    private String mVideoUrl;
    private OnPlayerStateChangeListener mPlayerStateListener;
    private MediaPlayer.OnPreparedListener mPreparedListener;
    private OnPlayheadUpdateListener mPlayheadUpdateListener;
    private OnProgressListener mProgressListener;
    private int mStartPos = 0;
    
    private Handler mHandler;
    private Runnable runnable = new Runnable() {
 	   @Override
 	   public void run() {
 		  try {
      		int position = getCurrentPosition();
      		if ( position != 0 && mPlayheadUpdateListener != null && isPlaying() )
      			mPlayheadUpdateListener.onPlayheadUpdated(position);
	      	} catch(IllegalStateException e){
	  	        e.printStackTrace(); 
	  	    }

 		 mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
 	   }
 	};

    public PlayerView(Context context) {
        super(context);

        super.setOnCompletionListener( new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                pause();
                seekTo( 0 );
                updateStopState();
                mPlayerStateListener.onStateChanged(PlayerStates.END);
            }
        });

        super.setOnPreparedListener( new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mPlayerStateListener.onStateChanged(PlayerStates.START);
                mediaPlayer.setOnSeekCompleteListener( new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                    	mPlayerStateListener.onStateChanged(PlayerStates.SEEKED);
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
            }
        });

    }

    @Override
    public void setStartingPoint(int point) {
        mStartPos = point;
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
    	if ( !this.isPlaying() ) {
            super.start();
            if ( mStartPos != 0 ) {
            	super.seekTo( mStartPos );
            	mStartPos = 0;
            }
            if ( mHandler == null ) {
            	mHandler = new Handler();
            }
            mHandler.postDelayed(runnable, PLAYHEAD_UPDATE_INTERVAL);
            mPlayerStateListener.onStateChanged(PlayerStates.PLAY);
    	}
    }

    @Override
    public void pause() {
    	if ( this.isPlaying() ) {
            super.pause();
            mPlayerStateListener.onStateChanged(PlayerStates.PAUSE);	
    	}
    }

    @Override
    public void stop() {
        super.stopPlayback();
        updateStopState();
    }
    
    private void updateStopState() {
        if ( mHandler != null ) {
        	mHandler.removeCallbacks( runnable );
        }
       
    }

    @Override
    public void seek(int msec) {
    	mPlayerStateListener.onStateChanged(PlayerStates.SEEKING);
        super.seekTo(msec);
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
    public void removePlayheadUpdateListener() {
    	mPlayheadUpdateListener = null;
    }

    @Override
    public void registerProgressUpdate ( OnProgressListener listener ) {
        mProgressListener = listener;
    }
    
   
}


