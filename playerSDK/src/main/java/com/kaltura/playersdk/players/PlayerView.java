package com.kaltura.playersdk.players;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.kaltura.playersdk.events.Listener;
import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
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
                        OnPlayheadUpdateListener.PlayheadUpdateInputObject input = new OnPlayheadUpdateListener.PlayheadUpdateInputObject();
                        input.msec = position;
                        executeListener(Listener.EventType.PLAYHEAD_UPDATE_LISTENER_TYPE, input);
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
                OnPlayerStateChangeListener.PlayerStateChangeInputObject input = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
                input.state = PlayerStates.END;
                executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, input);
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                OnPlayerStateChangeListener.PlayerStateChangeInputObject input = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
                input.state = PlayerStates.START;
                executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, input);
                mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                        OnPlayerStateChangeListener.PlayerStateChangeInputObject input = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
                        input.state = PlayerStates.SEEKED;
                        executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, input);
                    }
                });

                mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                    @Override
                    public void onBufferingUpdate(MediaPlayer mp, int progress) {
                        OnProgressListener.ProgressInputObject input = new OnProgressListener.ProgressInputObject();
                        input.progress = progress;
                        executeListener(Listener.EventType.PROGRESS_LISTENER_TYPE, input);
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
                OnErrorListener.ErrorInputObject input = new OnErrorListener.ErrorInputObject();
                input.errorMessage = "";
                input.errorCode = errorCode;
                executeListener(Listener.EventType.ERROR_LISTENER_TYPE, input);

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
            OnPlayerStateChangeListener.PlayerStateChangeInputObject input = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
            input.state = PlayerStates.PLAY;
            executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, input);
    	}
    }

    @Override
    public void pause() {
    	if ( this.isPlaying() ) {
            mVideoView.pause();
            OnPlayerStateChangeListener.PlayerStateChangeInputObject input = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
            input.state = PlayerStates.PAUSE;
            executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, input);
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
        OnPlayerStateChangeListener.PlayerStateChangeInputObject input = new OnPlayerStateChangeListener.PlayerStateChangeInputObject();
        input.state = PlayerStates.SEEKING;
    	executeListener(Listener.EventType.PLAYER_STATE_CHANGE_LISTENER_TYPE, input);
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


