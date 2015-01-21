package com.kaltura.playersdk;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodec.CryptoException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.widget.FrameLayout;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.FrameworkSampleSource;
import com.google.android.exoplayer.MediaCodecAudioTrackRenderer;
import com.google.android.exoplayer.MediaCodecTrackRenderer.DecoderInitializationException;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.SampleSource;
import com.google.android.exoplayer.TrackRenderer;
import com.google.android.exoplayer.VideoSurfaceView;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper.RendererBuilder;
import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.types.PlayerStates;

/**
 * A wrapper around {@link ExoPlayer} that provides a higher level interface. It can be prepared
 * with one of a number of {@link RendererBuilder} classes to suit different use cases (e.g. DASH,
 * SmoothStreaming and so on).
 */
public class KalturaPlayer extends FrameLayout implements ExoPlayer.Listener, MediaCodecVideoTrackRenderer.EventListener, VideoPlayerInterface, Callback {

    private int NUM_OF_RENFERERS = 2;
    private static final String TAG = KalturaPlayer.class.getSimpleName();
    public static int PLAYHEAD_UPDATE_INTERVAL = 200;
    public static int BUFFER_WAIT_INTERVAL = 200;
    private static final int BUFFER_COUNTER_MAX = 5;
    private volatile ExoPlayer mExoPlayer;
    private String mVideoUrl = null;
    private OnPlayerStateChangeListener mPlayerStateChangeListener;
    private OnPlayheadUpdateListener mPlayheadUpdateListener;
    private OnProgressListener mProgressListener;
    private OnErrorListener mErrorListener;
    private int mBufferWaitCounter = 0;

    private boolean mIsReady = false;
    private int mStartPos = 0;
    private int mPrevProgress = 0;
    private VideoSurfaceView mSurfaceView;

    private boolean mPrepared = false;
    private boolean mSeeking = false;
    private boolean mShouldResumePlayback = false;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public KalturaPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        addSurface();
    }

    public KalturaPlayer(Context context) {
        super(context);
        addSurface();
    }

    private void preparePlayer() {
        mPrepared = true;
        mPrevProgress = 0;

        mExoPlayer = ExoPlayer.Factory.newInstance(NUM_OF_RENFERERS);
        mExoPlayer.addListener(this);
        mExoPlayer.seekTo(0);

        SampleSource sampleSource = new FrameworkSampleSource(getContext(), Uri.parse(mVideoUrl), null, NUM_OF_RENFERERS);
        Handler handler = new Handler(Looper.getMainLooper());
        TrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(sampleSource, null, true, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 0, handler, this, 50);
        TrackRenderer audioRenderer = new MediaCodecAudioTrackRenderer(sampleSource);


        Surface surface = mSurfaceView.getHolder().getSurface();
        if (videoRenderer == null || surface == null || !surface.isValid()) {
            // We're not ready yet.
            return;
        }
        mExoPlayer.prepare( videoRenderer, audioRenderer );
        mExoPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, surface);
    }

    private void addSurface() {
        mSurfaceView = new VideoSurfaceView( getContext() );
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        this.addView(mSurfaceView, lp);
        mSurfaceView.getHolder().addCallback(this);
    }

    @Override
    public String getVideoUrl() {
        return mVideoUrl;
    }

	@Override
	public void setVideoUrl(String url) {
		if ( !url.equals(mVideoUrl) ) {
			releasePlayer();
			mVideoUrl = url;
            if ( mPlayheadUpdateListener != null ) {
                mPlayheadUpdateListener.onPlayheadUpdated(0);
            }
            preparePlayer();
		}		
	}

    @Override
    public void play() {
        if ( !mPrepared ) {
            preparePlayer();
        }
        if ( !this.isPlaying() ) {
            int a = mExoPlayer.getPlaybackState();
            if(a == ExoPlayer.STATE_BUFFERING){
                startWaitingLoop();
                return;
            }
            mExoPlayer.setPlayWhenReady(true);
            if ( mStartPos != 0 ) {
                mExoPlayer.seekTo( mStartPos );
                mStartPos = 0;
            }

            mHandler.removeCallbacks(null);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if ( mExoPlayer != null ) {
                        try {
                            int position = mExoPlayer.getCurrentPosition();
                            if ( position != 0 && mPlayheadUpdateListener != null && isPlaying() )
                                mPlayheadUpdateListener.onPlayheadUpdated(position);
                        } catch(IllegalStateException e){
                            e.printStackTrace();
                        }

                        mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
                    }

                }
            });

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mExoPlayer != null) {
                        try {
                            int progress = mExoPlayer.getBufferedPercentage();
                            if (progress != 0 && mProgressListener != null && mPrevProgress != progress) {
                                mProgressListener.onProgressUpdate(progress);
                                mPrevProgress = progress;
                            }

                        }catch(IllegalStateException e){
                            e.printStackTrace();
                        }

                        mHandler.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
                    }
                }
            });

            if ( mPlayerStateChangeListener != null )
                mPlayerStateChangeListener.onStateChanged(PlayerStates.PLAY);
        }
    }

    private void startWaitingLoop(){
        mBufferWaitCounter = 0;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int state = mExoPlayer.getPlaybackState();
                if(state == ExoPlayer.STATE_BUFFERING){
                    Log.d(TAG, "buffer loop" + mBufferWaitCounter);
                    mBufferWaitCounter++;
                    if(mBufferWaitCounter > BUFFER_COUNTER_MAX){
                        mBufferWaitCounter = 0;
                        releasePlayer();
                        preparePlayer();
                        play();
                    }else{
                        mHandler.postDelayed(this, BUFFER_WAIT_INTERVAL);
                    }
                }else{
                    play();
                }
            }
        });
    }

    private void updateStopState() {
        if ( mHandler != null ) {
            Log.d(TAG, "remove handler callbacks");
            mHandler.removeCallbacks(null); //removes all runnables
        }

    }

    @Override
    public void pause() {
        if ( this.isPlaying() && mExoPlayer!=null ) {
            mExoPlayer.setPlayWhenReady(false);
            if ( mPlayerStateChangeListener!=null )
                mPlayerStateChangeListener.onStateChanged(PlayerStates.PAUSE);
        }
    }

    @Override
    public void stop() {
        pause();
        releasePlayer();

    }

    private void releasePlayer() {
        updateStopState();
        if (mExoPlayer != null) {
            Log.d(TAG, "Releasing ExoPlayer");
            mExoPlayer.release();
            Log.d(TAG, "ExoPlayer released");
            mExoPlayer = null;
        }
        mPrepared = false;
        mIsReady = false;
    }

    @Override
    public void seek(int msec) {
        if ( mIsReady ) {
            mSeeking = true;
            if ( mPlayerStateChangeListener!=null )
                mPlayerStateChangeListener.onStateChanged(PlayerStates.SEEKING);
            mExoPlayer.seekTo( msec );
        }
    }

    @Override
    public boolean isPlaying() {
        if ( mExoPlayer != null ) {
            return mExoPlayer.getPlayWhenReady();
        }
        return false;
    }

    @Override
    public boolean canPause() {
        return mIsReady;
    }

    @Override
    public void registerPlayerStateChange(OnPlayerStateChangeListener listener) {
        mPlayerStateChangeListener = listener;

    }

    @Override
    public void registerPlayheadUpdate(OnPlayheadUpdateListener listener) {
        mPlayheadUpdateListener = listener;

    }

    @Override
    public void removePlayheadUpdateListener() {
        mPlayheadUpdateListener = null;

    }

    @Override
    public void registerProgressUpdate(OnProgressListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setStartingPoint(int point) {
        mStartPos = point;
    }


    @Override
    public void onDecoderInitializationError(DecoderInitializationException arg0) {
        if ( mErrorListener!=null ) {
            mErrorListener.onError( OnErrorListener.ERROR_UNKNOWN, arg0.getMessage() );
        }

    }

    @Override
    public int getDuration() {
        if ( mExoPlayer!= null ) {
            return mExoPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public void onDrawnToSurface(Surface arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDroppedFrames(int arg0, long arg1) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
        mSurfaceView.setVideoWidthHeightRatio(height == 0 ? 1 : (float) width / height);

    }

    @Override
    public void onPlayWhenReadyCommitted() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onPlayerError(ExoPlaybackException arg0) {
        if ( mErrorListener!=null ) {
            mErrorListener.onError( OnErrorListener.ERROR_UNKNOWN, arg0.getMessage() );
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int arg1) {
        PlayerStates state = null;

        Log.d(TAG, "PlayerStateChanged: " + arg1);

        switch ( arg1 ) {
            case ExoPlayer.STATE_PREPARING:
            case ExoPlayer.STATE_BUFFERING:
                state = PlayerStates.LOAD;
                break;
            case ExoPlayer.STATE_READY:
                if ( !mIsReady ) {
                    state = PlayerStates.START;
                    mIsReady = true;
                } else if ( mSeeking ) {
                    state = PlayerStates.SEEKED;
                    mSeeking = false;
                }
                break;
            case ExoPlayer.STATE_IDLE:
                if ( mSeeking ) {
                    state = PlayerStates.SEEKED;
                    mSeeking = false;
                }
                break;
            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "state ended");
                if (mExoPlayer != null) {
                    Log.d(TAG, "state ended: set play when ready false");
                    mExoPlayer.setPlayWhenReady(false);
                }
                if (mExoPlayer != null) {
                    Log.d(TAG, "state ended: seek to 0");
                    mExoPlayer.seekTo(0);
                }
                updateStopState();
                state = PlayerStates.END;
                stop();
                break;

        }

        if ( state!=null && mPlayerStateChangeListener!=null ) {
            mPlayerStateChangeListener.onStateChanged(state);
        }
        //notify initial play
        if ( state == PlayerStates.START && playWhenReady ) {
            mPlayerStateChangeListener.onStateChanged(PlayerStates.PLAY);
        }

    }

    @Override
    public void registerError(OnErrorListener listener) {
        mErrorListener = listener;
    }

    @Override
    public void onCryptoError(CryptoException e) {
        if (mErrorListener != null) {
            mErrorListener.onError( OnErrorListener.MEDIA_ERROR_NOT_VALID, e.getMessage() );
        }

    }

    @Override
    public void release() {
        mShouldResumePlayback = isPlaying();
        stop();

    }

    @Override
    public void recoverRelease() {
        //do nothing. Handled by surface callback
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if ( mShouldResumePlayback ) {
            play();
        }
        mShouldResumePlayback = false;

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

}
