package com.kaltura.playersdk.players;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.google.android.libraries.mediaframework.exoplayerextensions.RendererBuilderFactory;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.VideoSurfaceView;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by noamt on 18/01/2016.
 */
public class KExoPlayer extends FrameLayout implements KPlayer, ExoplayerWrapper.PlaybackListener {

    private static final String TAG = "KExoPlayer";
    private static final long PLAYHEAD_UPDATE_INTERVAL = 200;
    @NonNull private KPlayerListener mPlayerListener = noopPlayerListener();
    @NonNull private KPlayerCallback mPlayerCallback = noopEventListener();
    @NonNull private PlayerState mSavedState = new PlayerState();
    @NonNull private Handler mPlaybackTimeReporter = new Handler(Looper.getMainLooper());
    private String mSourceURL;
    private boolean mShouldCancelPlay;
    private ExoplayerWrapper mExoPlayer;
    private Readiness mReadiness = Readiness.Idle;
    private KPlayerExoDrmCallback mDrmCallback;
    private VideoSurfaceView mSurfaceView;
    private boolean mSeeking;
    private boolean mBuffering = false;

    public static Set<MediaFormat> supportedFormats(Context context) {
        Set<MediaFormat> set = new HashSet<>();
        // Clear dash and mp4 are always supported by this player.
        set.add(MediaFormat.dash_clear);
        set.add(MediaFormat.mp4_clear);
        
        // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Make sure Widevine is supported.
            if (MediaDrm.isCryptoSchemeSupported(ExoplayerUtil.WIDEVINE_UUID)) {
                set.add(MediaFormat.dash_widevine);
            }
        }
        return set;
    }
    
    public KExoPlayer(Context context) {
        super(context);
    }

    private KPlayerListener noopPlayerListener() {
        return new KPlayerListener() {
            public void eventWithValue(KPlayer player, String eventName, String eventValue) {}
            public void eventWithJSON(KPlayer player, String eventName, String jsonValue) {}
            public void contentCompleted(KPlayer currentPlayer) {}
        };
    }
    
    private KPlayerCallback noopEventListener() {
        return new KPlayerCallback() {
            public void playerStateChanged(int state) {}
        };
    }
    
    // KPlayer implementation
    @Override
    public void setPlayerListener(@NonNull KPlayerListener listener) {
        mPlayerListener = listener;
    }

    @Override
    public void setPlayerCallback(@NonNull KPlayerCallback callback) {
        mPlayerCallback = callback;
    }

    @Override
    public void setPlayerSource(String playerSource) {

        mSourceURL = playerSource;
        
        prepare();
    }
    
    private Video.VideoType getVideoType() {
        String videoFileName = Uri.parse(mSourceURL).getLastPathSegment();
        switch (videoFileName.substring(videoFileName.lastIndexOf('.')).toLowerCase()) {
            case ".mpd": 
                return Video.VideoType.DASH; 
            case ".mp4": 
                return Video.VideoType.MP4; 
            case ".m3u8": 
                return Video.VideoType.HLS;
            default: 
                return Video.VideoType.OTHER;
        }
        
    }

    private boolean isPlaying() {
        return mExoPlayer != null
                && mExoPlayer.getPlaybackState() == ExoPlayer.STATE_READY
                && mExoPlayer.getPlayWhenReady();
    }



    private void prepare() {
        
        if (mReadiness != Readiness.Idle) {
            Log.d(TAG, "Already preparing");
            return;
        }
        
        mReadiness = Readiness.Preparing;

        mDrmCallback = new KPlayerExoDrmCallback();
        Video video = new Video(mSourceURL, getVideoType());
        final ExoplayerWrapper.RendererBuilder rendererBuilder = RendererBuilderFactory
                .createRendererBuilder(getContext(), video, mDrmCallback);

        mSurfaceView = new VideoSurfaceView( getContext() );
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mExoPlayer = new ExoplayerWrapper(rendererBuilder);
                Surface surface = holder.getSurface();
                if (surface != null && surface.isValid()) {
                    mExoPlayer.setSurface(surface);
                } else {
                    Log.e(TAG, "Surface not ready yet");
                    return;
                }
                mExoPlayer.addListener(KExoPlayer.this);

                mExoPlayer.prepare();

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged(" + format + "," + width + "," + height + ")");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
            }
        });
        this.addView(mSurfaceView, layoutParams);
    }

    private float kplayerTime(long exoPlayerTime) {
        return exoPlayerTime / 1000f;
    }
    
    private long exoPlayerTime(float kplayerTime) {
        return (long) (kplayerTime * 1000);
    }
    
    @Override
    public void setCurrentPlaybackTime(float time) {
        mSeeking = true;
        stopPlaybackTimeReporter();
        if (mExoPlayer != null) {
            mExoPlayer.seekTo(exoPlayerTime(time));
        }
    }

    @Override
    public float getCurrentPlaybackTime() {
        if (mExoPlayer != null) {
            return kplayerTime(mExoPlayer.getCurrentPosition());
        }
        return 0;
    }

    @Override
    public float getDuration() {
        if (mExoPlayer != null) {
            return kplayerTime(mExoPlayer.getDuration());
        }
        return 0;
    }

    @Override
    public void play() {

        if (isPlaying()) {
            return;
        }
        
        if (mShouldCancelPlay) {
            mShouldCancelPlay = false;
            return;
        }
        
        if (mReadiness == Readiness.Idle) {
            prepare();
            return;
        }
        
        setPlayWhenReady(true);
        
        if (mSavedState.position != 0) {
            setCurrentPlaybackTime(mSavedState.position);
            mSavedState.position = 0;
        }

        startPlaybackTimeReporter();
    }
    
    @Override
    public void pause() {
        stopPlaybackTimeReporter();
        if (this.isPlaying() && mExoPlayer != null) {
            setPlayWhenReady(false);
        }
    }
    
    private void startPlaybackTimeReporter() {
        mPlaybackTimeReporter.removeMessages(0); // Stop reporter if already running
        mPlaybackTimeReporter.post(new Runnable() {
            @Override
            public void run() {
                if (mExoPlayer != null) {
                    maybeReportPlaybackTime();
                    mPlaybackTimeReporter.postDelayed(this, PLAYHEAD_UPDATE_INTERVAL);
                }
            }
        });
    }

    private void stopPlaybackTimeReporter() {
        Log.d(TAG, "remove handler callbacks");
        mPlaybackTimeReporter.removeMessages(0);
    }

    private void maybeReportPlaybackTime() {
        float position = getCurrentPlaybackTime();
        if (position != 0 && position < getDuration() && isPlaying()) {
            mPlayerListener.eventWithValue(KExoPlayer.this, KPlayerListener.TimeUpdateKey, Float.toString(position));
        }
    }

    @Override
    public void changeSubtitleLanguage(String languageCode) {
        // TODO
    }

    private void saveState() {
        if (mExoPlayer != null) {
            mSavedState.set(isPlaying(), getCurrentPlaybackTime());
        } else {
            mSavedState.set(false, 0);
        }
    }



    @Override
    public void removePlayer() {
        saveState();
        stopPlaybackTimeReporter();
        pause();
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
        mReadiness = Readiness.Idle;
    }
    
    @Override
    public void recoverPlayer() {
        // TODO
    }

    @Override
    public void setShouldCancelPlay(boolean shouldCancelPlay) {
        mShouldCancelPlay = shouldCancelPlay;
        // TODO
    }

    @Override
    public void setLicenseUri(final String licenseUri) {
        mDrmCallback.setLicenseUri(licenseUri);
    }


    // PlaybackListener
    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "PlayerStateChanged: " + playbackState);
        switch ( playbackState ) {
            case ExoPlayer.STATE_IDLE:
                if ( mSeeking ) {
                    mSeeking = false;
                }
                break;
            case ExoPlayer.STATE_PREPARING:
                break;
            case ExoPlayer.STATE_BUFFERING:
                mPlayerListener.eventWithValue(this, KPlayerListener.BufferingChangeKey, "true");
                mBuffering = true;
                break;
            case ExoPlayer.STATE_READY:
                if (mBuffering) {
                    mPlayerListener.eventWithValue(this, KPlayerListener.BufferingChangeKey, "false");
                    mBuffering = false;
                }
                if (mReadiness == Readiness.Ready && !playWhenReady) {
                    mPlayerListener.eventWithValue(this, KPlayerListener.PauseKey, null);
                }
                // ExoPlayer is ready.
                if (mReadiness != Readiness.Ready) {
                    mReadiness = Readiness.Ready;
                    // TODO what about mShouldResumePlayback?
                    mPlayerListener.eventWithValue(this, KPlayerListener.DurationChangedKey, Float.toString(this.getDuration()));
                    mPlayerListener.eventWithValue(this, KPlayerListener.LoadedMetaDataKey, "");
                    mPlayerListener.eventWithValue(this, KPlayerListener.CanPlayKey, null);
                    mPlayerCallback.playerStateChanged(KPlayerCallback.CAN_PLAY);
                }
                if (mSeeking) {
                    // ready after seeking
                    mPlayerListener.eventWithValue(this, KPlayerListener.SeekedKey, null);
                    mSeeking = false;
                    startPlaybackTimeReporter();
                }

                if (playWhenReady) {
                    mPlayerListener.eventWithValue(this, KPlayerListener.PlayKey, null);
                }
                break;

            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "state ended");
                if (mExoPlayer != null) {
                    Log.d(TAG, "state ended: set play when ready false");
                    setPlayWhenReady(false);
                }
                if (mExoPlayer != null) {
                    Log.d(TAG, "state ended: seek to 0");
                    setCurrentPlaybackTime(0);
                }
                if (playWhenReady) {
                    mPlayerListener.contentCompleted(this);
                    mPlayerCallback.playerStateChanged(KPlayerCallback.ENDED);
                } 
                stopPlaybackTimeReporter();
                break;
        }
    }
    

    private void setPlayWhenReady(boolean shouldPlay) {
        if (mExoPlayer != null) {
            mExoPlayer.setPlayWhenReady(shouldPlay);
        }
        setKeepScreenOn(shouldPlay);
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "player error", e);
        // TODO: anything?
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        mSurfaceView.setVideoWidthHeightRatio((float)width / height);
    }
    
    // Utility classes
    private enum Readiness {
        Idle,
        Preparing,
        Ready
    }

    private class PlayerState {
        boolean playing;
        float position;

        void set(boolean playing, float position) {
            this.playing = playing;
            this.position = position;
        }
    }
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
class KPlayerExoDrmCallback implements MediaDrmCallback {

    private static final String TAG = "KPlayerDrmCallback";
    private static final long MAX_LICENCE_URI_WAIT = 8000;
    private String mLicenseUri;
    private final Object mLicenseLock = new Object();

    KPlayerExoDrmCallback() {
        Log.d(TAG, "KPlayerDrmCallback created");
    }

    @Override
    public byte[] executeProvisionRequest(UUID uuid, MediaDrm.ProvisionRequest request) {
        throw new UnsupportedOperationException("We don't have a provisioning service");
    }

    @Override
    public byte[] executeKeyRequest(UUID uuid, MediaDrm.KeyRequest request) throws IOException {

        Map<String, String> headers = new HashMap<>(1);
        headers.put("Content-Type", "application/octet-stream");

        // The license uri arrives on a different thread (typically the main thread).
        // If this method is called before the uri has arrived, we have to wait for it.
        // mLicenseLock is the wait lock.
        synchronized (mLicenseLock) {
            // No uri? wait.
            if (mLicenseUri == null) {
                try {
                    mLicenseLock.wait(MAX_LICENCE_URI_WAIT);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupted", e);
                }
            }
            // Still no uri? throw.
            if (mLicenseUri == null) {
                throw new IllegalStateException("licenseUri cannot be null");
            }
            // Execute request.
            byte[] response = ExoplayerUtil.executePost(mLicenseUri, request.getData(), headers);
            Log.d(TAG, "response data (b64): " + Base64.encodeToString(response, 0));
            return response;
        }
    }

    public void setLicenseUri(String licenseUri) {
        synchronized (mLicenseLock) {
            mLicenseUri = licenseUri;
            // notify executeKeyRequest() that we have the license uri.
            mLicenseLock.notify();
        }
    }
}
