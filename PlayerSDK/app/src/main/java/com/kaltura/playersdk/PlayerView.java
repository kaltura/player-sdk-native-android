package com.kaltura.playersdk;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.widget.VideoView;

import com.kaltura.playersdk.events.OnErrorListener;
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
    private OnPlayheadUpdateListener mPlayheadUpdateListener;
    private OnProgressListener mProgressListener;
    private OnErrorListener mErrorListener;
    private int mStartPos = 0;
    private boolean mShouldResumePlayback = false;
    
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
                if ( mPlayerStateListener!= null )
                	mPlayerStateListener.onStateChanged(PlayerStates.END);
            }
        });

        super.setOnPreparedListener( new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
            	if ( mPlayerStateListener!=null )
            		mPlayerStateListener.onStateChanged(PlayerStates.START);
                mediaPlayer.setOnSeekCompleteListener( new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                    	if ( mPlayerStateListener!=null )
                    		mPlayerStateListener.onStateChanged(PlayerStates.SEEKED);
                    }
                });

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
        
        super.setOnErrorListener( new MediaPlayer.OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				if ( mErrorListener!=null ) {
					int errorCode = OnErrorListener.ERROR_UNKNOWN;
					switch( extra ) {
					case MediaPlayer.MEDIA_ERROR_IO:
						errorCode = OnErrorListener.MEDIA_ERROR_IO;
						break;
					case MediaPlayer.MEDIA_ERROR_MALFORMED:
						errorCode = OnErrorListener.MEDIA_ERROR_MALFORMED;
						break;
					case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
						errorCode = OnErrorListener.MEDIA_ERROR_NOT_VALID;
						break;
					case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
						errorCode = OnErrorListener.MEDIA_ERROR_TIMED_OUT;
						break;
					case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
						errorCode = OnErrorListener.MEDIA_ERROR_UNSUPPORTED;
						break;
					}
					
					mErrorListener.onError( errorCode, "");
				}
				
				return false;
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
    public void play() {
    	if ( !this.isPlaying() ) {
            super.start();
            if ( mStartPos != 0 ) {
            	super.seekTo( mStartPos );
            	mStartPos = 0;
            }
            if ( mHandler == null ) {
            	mHandler = new Handler();
            	mHandler.postDelayed(runnable, PLAYHEAD_UPDATE_INTERVAL);
            }
            if ( mPlayerStateListener!=null )
            	mPlayerStateListener.onStateChanged(PlayerStates.PLAY);
    	}
    }

    @Override
    public void pause() {
    	if ( this.isPlaying() ) {
            super.pause();
            if ( mPlayerStateListener!=null )
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
        	mHandler = null;
        }
       
    }

    @Override
    public void seek(int msec) {
    	if ( mPlayerStateListener!=null )
    		mPlayerStateListener.onStateChanged(PlayerStates.SEEKING);
        super.seekTo(msec);
    }

    @Override
    public void registerPlayerStateChange( OnPlayerStateChangeListener listener) {
        mPlayerStateListener = listener;
    }

    @Override
    public void registerError( OnErrorListener listener) {
       mErrorListener = listener;

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

	@Override
	public void release() {
		mShouldResumePlayback = isPlaying();
		pause();
		
	}

	@Override
	public void recoverRelease() {
		if ( mShouldResumePlayback ) {
			play();
		}
		mShouldResumePlayback = false;
		
	}
    
   
}


