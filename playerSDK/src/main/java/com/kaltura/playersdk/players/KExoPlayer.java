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
import android.view.accessibility.CaptioningManager;
import android.widget.FrameLayout;

import com.google.android.exoplayer.BehindLiveWindowException;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.metadata.id3.GeobFrame;
import com.google.android.exoplayer.metadata.id3.Id3Frame;
import com.google.android.exoplayer.metadata.id3.PrivFrame;
import com.google.android.exoplayer.metadata.id3.TxxxFrame;
import com.google.android.exoplayer.text.CaptionStyleCompat;
import com.google.android.exoplayer.text.Cue;
import com.google.android.exoplayer.text.SubtitleLayout;
import com.google.android.exoplayer.util.Util;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.google.android.libraries.mediaframework.exoplayerextensions.RendererBuilderFactory;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.VideoSurfaceView;
import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by noamt on 18/01/2016.
 */
public class KExoPlayer extends FrameLayout implements KPlayer, ExoplayerWrapper.PlaybackListener ,ExoplayerWrapper.CaptionListener, ExoplayerWrapper.Id3MetadataListener {

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
    private com.google.android.exoplayer.text.SubtitleLayout mSubtView;
    private boolean mSeeking;
    private boolean mBuffering = false;
    private boolean mPassedPlay = false;
    private boolean prepareWithConfigurationMode = false;



    private SurfaceHolder.Callback mSurfaceCallback;

    public static Set<KMediaFormat> supportedFormats(Context context) {
        Set<KMediaFormat> set = new HashSet<>();
        // Clear dash and mp4 are always supported by this player.
        set.add(KMediaFormat.dash_clear);
        set.add(KMediaFormat.mp4_clear);
        set.add(KMediaFormat.hls_clear);

        // Encrypted dash is only supported in Android v4.3 and up -- needs MediaDrm class.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // Make sure Widevine is supported.
            if (MediaDrm.isCryptoSchemeSupported(ExoplayerUtil.WIDEVINE_UUID)) {
                set.add(KMediaFormat.dash_widevine);
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

    @Override
    public boolean isPlaying() {
        return mExoPlayer != null
                && mExoPlayer.getPlayWhenReady();
    }

    @Override
    public void switchToLive() {
        mExoPlayer.seekTo(0);
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

        mSurfaceView = new VideoSurfaceView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        if (mExoPlayer == null) {
            mExoPlayer = new ExoplayerWrapper(rendererBuilder);
            Surface surface = mSurfaceView.getHolder().getSurface();
            if (surface != null) {
                mExoPlayer.setSurface(surface);
            }

            mExoPlayer.setCaptionListener(this);
            mExoPlayer.setMetadataListener(this);

            configureSubtitleView();
            mExoPlayer.addListener(this);
            mExoPlayer.prepare();
        }
        mSurfaceCallback = new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                 if (mExoPlayer != null && mExoPlayer.getSurface() == null) {
                     mExoPlayer.setSurface(holder.getSurface());
                     mReadiness = Readiness.Ready;
                     mExoPlayer.addListener(KExoPlayer.this);
                 }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.d(TAG, "surfaceChanged(" + format + "," + width + "," + height + ")");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed");
                if (mExoPlayer != null) {
                    mExoPlayer.blockingClearSurface();
                    mExoPlayer.removeListener(KExoPlayer.this);
                }
            }
        };

        mSurfaceView.getHolder().addCallback(mSurfaceCallback);
        Log.d(TAG, "KExoPlaer prepareWithConfigurationMode " + prepareWithConfigurationMode);
        if(!prepareWithConfigurationMode) {
            this.addView(mSurfaceView, layoutParams);
            mSubtView = new com.google.android.exoplayer.text.SubtitleLayout(getContext());
            this.addView(mSubtView, layoutParams);
        }
    }

    @Override
    public void setCurrentPlaybackTime(long time) {
        mSeeking = true;
        stopPlaybackTimeReporter();
        if (mExoPlayer != null) {
            mExoPlayer.seekTo(time);
        }
    }

    @Override
    public long getCurrentPlaybackTime() {
        if (mExoPlayer != null) {
            return mExoPlayer.getCurrentPosition();
        }
        return 0;
    }

    @Override
    public long getDuration() {
        if (mExoPlayer != null) {
            return mExoPlayer.getDuration();
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
        mPassedPlay = true;
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
        if (isPlaying()) {
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
        long position = getCurrentPlaybackTime();
        if (position != 0 && position < getDuration() && isPlaying()) {
            mPlayerListener.eventWithValue(KExoPlayer.this, KPlayerListener.TimeUpdateKey, Float.toString(position / 1000f));
        }
    }

    private void saveState() {
        if (mExoPlayer != null) {
            mSavedState.set(isPlaying(), getCurrentPlaybackTime());
        } else {
            mSavedState.set(false, 0);
        }
    }

    @Override
    public void freezePlayer() {
        if (mExoPlayer != null && mExoPlayer.getSurface() == null) {
            mExoPlayer.setBackgrounded(true);
        }
    }

    @Override
    public void removePlayer() {
        stopPlaybackTimeReporter();
        pause();
        if (mExoPlayer != null) {
            mExoPlayer.release();
            mExoPlayer = null;
        }
        mReadiness = Readiness.Idle;
    }

    @Override
    public void recoverPlayer(boolean isPlaying) {
        if (mExoPlayer != null && mExoPlayer.getSurface() == null) {
            mExoPlayer.setBackgrounded(false);
            if (isPlaying) {
                mPlayerListener.eventWithValue(this, KPlayerListener.PlayKey, null);
            }
        }
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

    @Override
    public void attachSurfaceViewToPlayer() {
        if (prepareWithConfigurationMode) {
            Log.d(TAG, "KExoPlayer attachSurfaceViewToPlayer " + prepareWithConfigurationMode);
            LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
            this.addView(mSurfaceView, layoutParams);
            mSubtView = new com.google.android.exoplayer.text.SubtitleLayout(getContext());
            this.addView(mSubtView, layoutParams);
        }
    }

    @Override
    public void detachSurfaceViewFromPlayer() {
        if (prepareWithConfigurationMode) {
            this.removeView(mSubtView);
            this.removeView(mSurfaceView);
        }
    }

    @Override
    public void setPrepareWithConfigurationMode() {
        prepareWithConfigurationMode = true;
    }



//    private void savePlayerState() {
//        saveState();
//        pause();
//    }
//
//    private void recoverPlayerState() {
//        setCurrentPlaybackTime(mSavedState.position);
//        if (mSavedState.playing) {
//            play();
//        }
//    }


    // PlaybackListener
    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        Log.d(TAG, "PlayerStateChanged: " + playbackState);

        switch (playbackState) {
            case ExoPlayer.STATE_IDLE:
                if (mSeeking) {
                    mSeeking = false;
                }
                break;
            case ExoPlayer.STATE_PREPARING:
                break;
            case ExoPlayer.STATE_BUFFERING:
                if (mPlayerListener != null) {
                   mPlayerListener.eventWithValue(this, KPlayerListener.BufferingChangeKey, "true");
                   mBuffering = true;
                 }
                break;
            case ExoPlayer.STATE_READY:
                if (mPlayerListener != null && mPlayerCallback != null) {
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
                        mPlayerListener.eventWithValue(this, KPlayerListener.DurationChangedKey, Float.toString(this.getDuration() / 1000f));
                        mPlayerListener.eventWithValue(this, KPlayerListener.LoadedMetaDataKey, "");
                        mPlayerListener.eventWithValue(this, KPlayerListener.CanPlayKey, null);
                        mPlayerCallback.playerStateChanged(KPlayerCallback.CAN_PLAY);
                    }
                    if (mSeeking) {
                        // ready after seeking
                        mReadiness = Readiness.Ready;
                        mPlayerListener.eventWithValue(this, KPlayerListener.SeekedKey, null);
                        mPlayerCallback.playerStateChanged(KPlayerCallback.SEEKED);
                        mSeeking = false;
                        startPlaybackTimeReporter();
                    }

                    if (mPassedPlay && playWhenReady) {
                        mPassedPlay = false;
                        mPlayerListener.eventWithValue(this, KPlayerListener.PlayKey, null);
                    }
                }
                break;

            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "state ended");
                if (playWhenReady) {
                    mPlayerCallback.playerStateChanged(KPlayerCallback.ENDED);
                    mReadiness = Readiness.Idle;
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
        String errMsg = "Player Error";
        Log.e(TAG, errMsg, e);
        String errorString = "";
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            errorString = (Util.SDK_INT < 18) ? "error_drm_not_supported"
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? "error_drm_unsupported_scheme" : "error_drm_unknown";
        } else if (e instanceof ExoPlaybackException && e.getCause() instanceof BehindLiveWindowException) {
            Log.e(TAG, "Recovering BehindLiveWindowException"); // happens if network is bad and no more chunk in hte buffer
            mExoPlayer.prepare();
            return;
        } else if (e instanceof ExoPlaybackException && e.getCause() instanceof android.media.MediaCodec.CryptoException) {
            errorString = "DRM Error"; // probably license issue
        } else if (e instanceof ExoPlaybackException
                && e.getCause() instanceof MediaCodecTrackRenderer.DecoderInitializationException) {
            // Special case for decoder initialization failures.
            MediaCodecTrackRenderer.DecoderInitializationException decoderInitializationException =
                    (MediaCodecTrackRenderer.DecoderInitializationException) e.getCause();
            if (decoderInitializationException.decoderName == null) {
                if (decoderInitializationException.getCause() instanceof MediaCodecUtil.DecoderQueryException) {
                    errorString = "error_querying_decoders ";
                } else if (decoderInitializationException.secureDecoderRequired) {
                    errorString = "error_no_secure_decoder " +
                            decoderInitializationException.mimeType;
                } else {
                    errorString = "error_no_decoder " +
                            decoderInitializationException.mimeType;
                }
            } else {
                errorString = "error_instantiating_decoder " +
                        decoderInitializationException.decoderName;
            }
        }
        if (!"".equals(errorString)) {
            Log.e(TAG, errorString);
            errorString += "-";
        }

        mPlayerListener.eventWithValue(KExoPlayer.this, KPlayerListener.ErrorKey, TAG + "-" + errMsg + "-" + errorString + e.getMessage());
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        mSurfaceView.setVideoWidthHeightRatio((float) width / height);
    }

    @Override
    public TrackFormat getTrackFormat(TrackType trackType, int index) {
        com.google.android.exoplayer.MediaFormat mediaFormat = mExoPlayer.getTrackFormat(getExoTrackType(trackType), index);
        return new TrackFormat(trackType, index, mediaFormat);

    }

    @Override
    public int getTrackCount(TrackType trackType) {
        if (mExoPlayer != null) {
            return mExoPlayer.getTrackCount(getExoTrackType(trackType));
        } else {
            Log.e(TAG, "getTrackCount mExoPlayer = null");
            return 0;
        }

    }

    @Override
    public int getCurrentTrackIndex(TrackType trackType) {
        if (mExoPlayer != null) {
            return mExoPlayer.getSelectedTrack(getExoTrackType(trackType));
        } else {
            Log.e(TAG, "getCurrentTrackIndex mExoPlayer = null");
            return -1;
        }
    }

    @Override
    public void switchTrack(TrackType trackType, int newIndex) {
        int exoTrackType = ExoplayerWrapper.TRACK_DISABLED;
        if (trackType == null){
            return;
        }
        if (mExoPlayer != null) {
            mExoPlayer.setSelectedTrack(getExoTrackType(trackType), newIndex);
        } else {
            Log.e(TAG, "switchTrack mExoPlayer = null");
        }
    }

    private int getExoTrackType(TrackType trackType) {
        int exoTrackType = ExoplayerWrapper.TRACK_DISABLED;
        switch (trackType){
            case AUDIO:
                exoTrackType = ExoplayerWrapper.TYPE_AUDIO;
                break;
            case VIDEO:
                exoTrackType = ExoplayerWrapper.TYPE_VIDEO;
                break;
            case TEXT:
                exoTrackType = ExoplayerWrapper.TYPE_TEXT;
                break;
        }
        return exoTrackType;
    }

    @Override
    public void onCues(List<Cue> cues) {
        StringBuilder sb = new StringBuilder();
        for (Cue cue : cues){
            sb.append(cue.text);
        }
        Log.d(TAG, "subTitle = " + sb.toString());
        if (mSubtView != null) {
            mSubtView.setCues(cues);
        }
    }

    @Override
    public void onId3Metadata(List<Id3Frame> id3Frames) {
        for (Id3Frame id3Frame : id3Frames) {
            if (id3Frame instanceof TxxxFrame) {
                TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
                Log.i(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
                        txxxFrame.description, txxxFrame.value));
            } else if (id3Frame instanceof PrivFrame) {
                PrivFrame privFrame = (PrivFrame) id3Frame;
                Log.i(TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
            } else if (id3Frame instanceof GeobFrame) {
                GeobFrame geobFrame = (GeobFrame) id3Frame;
                Log.i(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
            } else {
                Log.i(TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
            }
        }
    }

    private void configureSubtitleView() {
        CaptionStyleCompat style;
        float fontScale;
        if (Util.SDK_INT >= 19) {
            style = getUserCaptionStyleV19();
            fontScale = getUserCaptionFontScaleV19();
        } else {
            style = CaptionStyleCompat.DEFAULT;
            fontScale = 1.0f;
        }
        if (mSubtView != null) {
            mSubtView.setStyle(style);
            mSubtView.setFractionalTextSize(SubtitleLayout.DEFAULT_TEXT_SIZE_FRACTION * fontScale);
        }
    }

    @TargetApi(19)
    private float getUserCaptionFontScaleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
        return captioningManager.getFontScale();
    }

    @TargetApi(19)
    private CaptionStyleCompat getUserCaptionStyleV19() {
        CaptioningManager captioningManager =
                (CaptioningManager) getContext().getSystemService(Context.CAPTIONING_SERVICE);
        return CaptionStyleCompat.createFromCaptionStyle(captioningManager.getUserStyle());
    }

    // Utility classes
    private enum Readiness {
        Idle,
        Preparing,
        Ready
    }

    private class PlayerState {
        boolean playing;
        long position;

        void set(boolean playing, long position) {
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
