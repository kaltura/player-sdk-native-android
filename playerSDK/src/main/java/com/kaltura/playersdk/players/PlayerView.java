package com.kaltura.playersdk.players;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.types.PlayerStates;

/**
 * Created by michalradwantzor on 9/15/13.
 */
public class PlayerView extends BasePlayerView {
    //TODO make configurable
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;

    private String mVideoUrl;
    private int mStartPos = 0;
    private boolean mShouldResumePlayback = false;
    private VideoView mVideoView;

    private Handler mHandler;
    final private Runnable runnable = new Runnable() {
 	   @Override
 	   public void run() {
 		  try {
                    int position = getCurrentPosition();
                    if ( position != 0 && isPlaying() ) {
                        mListenerExecutor.executeOnPlayheadUpdated(position);
                    }
               } catch(IllegalStateException e){
                    e.printStackTrace();
               }

 		 mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
 	   }
 	};



    public PlayerView(Context context) {
        super(context);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((LayoutParams.MATCH_PARENT), (LayoutParams.MATCH_PARENT));
        mVideoView = new VideoView(getContext());
        addView(mVideoView, lp);
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                pause();
                mVideoView.seekTo(0);
                updateStopState();
                mListenerExecutor.executeOnStateChanged(PlayerStates.END);
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mListenerExecutor.executeOnStateChanged(PlayerStates.START);
                mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                        mListenerExecutor.executeOnStateChanged(PlayerStates.SEEKED);
                    }
                });

                mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int progress) {
                        mListenerExecutor.executeOnProgressUpdate(progress);
                    }
                });
            }
        });
        
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                int errorCode = OnErrorListener.ERROR_UNKNOWN;
                switch (extra) {
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

                mListenerExecutor.executeOnError(errorCode,"");
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
        mVideoView.setVideoURI(Uri.parse(url));
    }

    @Override
    public int getDuration() {
        return mVideoView.getDuration();
    }

    public int getCurrentPosition() {
        return mVideoView.getCurrentPosition();
    }

    
    @Override
    public void play() {
    	if ( !this.isPlaying() ) {
            mVideoView.start();
            if ( mStartPos != 0 ) {
                mVideoView.seekTo(mStartPos);
            	mStartPos = 0;
            }
            if ( mHandler == null ) {
            	mHandler = new Handler();
            	mHandler.postDelayed(runnable, PLAYHEAD_UPDATE_INTERVAL);
            }
            mListenerExecutor.executeOnStateChanged(PlayerStates.PLAY);
    	}
    }

    @Override
    public void pause() {
    	if ( this.isPlaying() ) {
            mVideoView.pause();
            mListenerExecutor.executeOnStateChanged(PlayerStates.PAUSE);
    	}
    }

    @Override
    public void stop() {
        mVideoView.stopPlayback();
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
        mListenerExecutor.executeOnStateChanged(PlayerStates.SEEKING);
        mVideoView.seekTo(msec);
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

    @Override
    public boolean isPlaying() {
        return mVideoView.isPlaying();
    }

    @Override
    public boolean canPause() {
        return mVideoView.canPause();
    }
}


