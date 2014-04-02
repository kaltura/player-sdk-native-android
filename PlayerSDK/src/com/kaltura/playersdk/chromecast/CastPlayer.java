package com.kaltura.playersdk.chromecast;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Handler;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaMetadata;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.common.images.WebImage;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.exceptions.CastException;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.kaltura.playersdk.VideoPlayerInterface;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.types.PlayerStates;

public class CastPlayer implements VideoPlayerInterface {
	
	private String mVideoUrl;
	private MediaInfo mMediaInfo;
	
	private MediaMetadata mMovieMetadata;
	private Context mContext;
	private VideoCastManager mCastManager;
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;

    private OnPlayerStateChangeListener mPlayerStateListener;
    private MediaPlayer.OnPreparedListener mPreparedListener;
    private OnPlayheadUpdateListener mPlayheadUpdateListener;
    private OnProgressListener mProgressListener;
    private OnErrorListener mErrorListener;
    private Timer mTimer;
    private int mStartPos = 0;
    private SettingsContentObserver mSettingsContentObserver;
	
	public CastPlayer( Context context, String title, String subtitle, String studio, String thumbUrl, String videoUrl) {
		mContext = context;
		mCastManager = ChromecastHandler.getVideoCastManager(context);
		mVideoUrl = videoUrl;
	 	mMovieMetadata = new MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE);
	 	mMediaInfo = null;
	 	//These params can't be empty, set defaults if values are missing
	 	String movieTitle = (title != null )? title : "title";
	 	String movieSubTitle = (subtitle != null )? title : "subtitle";
	 	String movieStudio = (studio != null )? studio : "studio";
	 	String movieThumb = (thumbUrl != null )? thumbUrl : "noThumb";
	 	
	 	mMovieMetadata.putString(MediaMetadata.KEY_SUBTITLE, movieTitle);
	 	mMovieMetadata.putString(MediaMetadata.KEY_TITLE, movieSubTitle);
	 	mMovieMetadata.putString(MediaMetadata.KEY_STUDIO, movieStudio);
        mMovieMetadata.addImage(new WebImage(Uri.parse(movieThumb)));
        
        
        
        mSettingsContentObserver = new SettingsContentObserver( new Handler() ); 
        mContext.getContentResolver().registerContentObserver( 
            android.provider.Settings.System.CONTENT_URI, true, 
            mSettingsContentObserver );
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

	}

	@Override
	public int getDuration() {
		int duration = 0;
		try {
			if ( mCastManager.isRemoteMediaLoaded() ) {
				duration = (int) mCastManager.getMediaDuration();
			}
		} catch (TransientNetworkDisconnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return duration;
	}

	@Override
	public boolean getIsPlaying() {
		// TODO Auto-generated method stub
		return isPlaying();
	}
	
	public int getCurrentPosition() {
        int pos = 0;
        try {
			if ( mCastManager.isRemoteMediaLoaded() ) {
				//convert to milliseconds
				pos = (int)(mCastManager.getCurrentMediaPosition() );
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if ( mTimer != null ) {
	            mTimer.cancel();
	            mTimer = null;
			}
		}
        return pos;
    }

	@Override
	public void play() {
		try {
			if ( mMediaInfo == null ) {
				mMediaInfo =  new MediaInfo.Builder(mVideoUrl)
		                .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
		                .setContentType("video/mp4")
		                .setMetadata(mMovieMetadata)
		                .build();
		        
				mCastManager.loadMedia(mMediaInfo, true, mStartPos);
				mStartPos = 0;
				if ( mTimer == null ) {
	                mTimer = new Timer();
	            }
	            mTimer.schedule(new TimerTask() {
	                @Override
	                public void run() {
                		int newPos = getCurrentPosition();
                		if ( newPos > 0 ) 
                		{
                			if ( mPlayheadUpdateListener != null ){
                				mPlayheadUpdateListener.onPlayheadUpdated(newPos);                				
                			}
                			if ( newPos >= getDuration() ) {
                    			mPlayerStateListener.onStateChanged(PlayerStates.END);
                    		}
                		}	
	                }
	            }, 0, PLAYHEAD_UPDATE_INTERVAL);
				mPlayerStateListener.onStateChanged(PlayerStates.PLAY);
//		    	mCastManager.startCastControllerActivity(mContext, mMediaInfo,
//		       			0, true);
			} else if ( mCastManager.getPlaybackStatus() == MediaStatus.PLAYER_STATE_PAUSED ) {
				mCastManager.play();
				mPlayerStateListener.onStateChanged(PlayerStates.PLAY);
			}

	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Override
	public void pause() {
		try {
			if ( mCastManager.isRemoteMediaLoaded() && !mCastManager.isRemoteMoviePaused() ) {
				mCastManager.pause();
				mPlayerStateListener.onStateChanged(PlayerStates.PAUSE);
			}

		} catch (CastException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransientNetworkDisconnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void stop() {	
		if ( mCastManager.isConnected() ) {
			mCastManager.disconnect();
			ChromecastHandler.selectedDevice = null;
		}
			
        if ( mTimer != null ) {
            mTimer.cancel();
            mTimer = null;
        }
        
        mContext.getContentResolver().unregisterContentObserver(mSettingsContentObserver);
        
        mMediaInfo = null;
        //mPlayerStateListener.onStateChanged(PlayerStates.END);

	}

	@Override
	public void seek(int msec) {
		try {
			mCastManager.seek(msec);
		} catch (TransientNetworkDisconnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean isPlaying() {
		boolean isPlaying = false;
		try {
			isPlaying = mCastManager.isRemoteMoviePlaying();
		} catch (TransientNetworkDisconnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return isPlaying;
	}

	@Override
	public boolean canPause() {
		try {
			if ( mCastManager.isRemoteMediaLoaded() )
				return true;
		} catch (TransientNetworkDisconnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
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
	    
	    
	    
	    private class SettingsContentObserver extends ContentObserver {

    	   public SettingsContentObserver(Handler handler) {
    	      super(handler);
    	   } 

    	   @Override
    	   public boolean deliverSelfNotifications() {
    	      return super.deliverSelfNotifications(); 
    	   }

    	   @Override
    	   public void onChange(boolean selfChange) {
    	      super.onChange(selfChange);
    	      
    	      AudioManager audio = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    	      int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
    	        
    	      try {
				mCastManager.setVolume(currentVolume);
			} catch (CastException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransientNetworkDisconnectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoConnectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	   }
    	}

}
