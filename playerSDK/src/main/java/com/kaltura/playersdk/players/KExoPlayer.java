package com.kaltura.playersdk.players;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
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
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.tracks.TrackFormat;
import com.kaltura.playersdk.tracks.TrackType;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

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
    private KState mReadiness = KState.IDLE;
    private KPlayerExoDrmCallback mDrmCallback;
    private VideoSurfaceView mSurfaceView;
    private com.google.android.exoplayer.text.SubtitleLayout mSubtView;
    private boolean mSeeking;
    private boolean mBuffering = false;
    private boolean mPassedPlay = false;
    private boolean prepareWithConfigurationMode = false;
    private boolean isFirstPlayback = true;



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
            public void asyncEvaluate(String expression, String expressionID, PlayerViewController.EvaluateListener evaluateListener) {}
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
        mReadiness = KState.IDLE;
        isFirstPlayback = true;
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
        if (mReadiness != KState.IDLE) {
            LOGD(TAG, "Already preparing");
            return;
        }

        mReadiness = KState.PREPARING;

        boolean offline = mSourceURL.startsWith("file://");
        mDrmCallback = new KPlayerExoDrmCallback(getContext(), offline);
        Video video = new Video(mSourceURL, getVideoType());
        final ExoplayerWrapper.RendererBuilder rendererBuilder = RendererBuilderFactory
                .createRendererBuilder(getContext(), video, mDrmCallback);

        mSurfaceView = new VideoSurfaceView(getContext());
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
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

        mSurfaceCallback = new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mExoPlayer != null && mExoPlayer.getSurface() == null) {
                    mExoPlayer.setSurface(holder.getSurface());
                    mReadiness = KState.READY;
                    mExoPlayer.addListener(KExoPlayer.this);
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                LOGD(TAG, "surfaceChanged(" + format + "," + width + "," + height + ")");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                LOGD(TAG, "surfaceDestroyed");
                if (mExoPlayer != null) {
                    mExoPlayer.blockingClearSurface();
                    mExoPlayer.removeListener(KExoPlayer.this);
                }
            }
        };

        mSurfaceView.getHolder().addCallback(mSurfaceCallback);
        LOGD(TAG, "KExoPlaer prepareWithConfigurationMode " + prepareWithConfigurationMode);
        if(!prepareWithConfigurationMode) {
            this.addView(mSurfaceView, layoutParams);
            mSubtView = new com.google.android.exoplayer.text.SubtitleLayout(getContext());
            this.addView(mSubtView, layoutParams);
        }
    }

    @Override
    public void setCurrentPlaybackTime(long time) {
        mSeeking = true;
        mReadiness = KState.SEEKING;
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

        if (isPlaying() || mReadiness == KState.PLAYING) {
            mPassedPlay = true;
            return;
        }
        LOGD(TAG, "action: play called");
        if (mShouldCancelPlay) {
            mShouldCancelPlay = false;
            mReadiness = KState.IDLE;
            return;
        }

        if (mReadiness == KState.IDLE && mReadiness != KState.ENDED) {
            prepare();
            mReadiness = KState.PLAYING;
            return;
        }
        mPassedPlay = true;
        mReadiness = KState.PLAYING;

        setPlayWhenReady(true);

        if (mSavedState.position != 0) {
            setCurrentPlaybackTime(mSavedState.position);
            mSavedState.position = 0;
        }

        startPlaybackTimeReporter();
    }

    @Override
    public void pause() {
        KState prevState = mReadiness;
        if (mReadiness == KState.PAUSED) {
            return;
        }

        LOGD(TAG, "action: pause called");
        mReadiness = KState.PAUSED;

        stopPlaybackTimeReporter();
        if (isPlaying()) {
            setPlayWhenReady(false);
        }
        if (prevState == KState.IDLE) {
            setPlayWhenReady(true);
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
        LOGD(TAG, "remove handler callbacks");
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
        mReadiness = KState.IDLE;
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
            LOGD(TAG, "KExoPlayer attachSurfaceViewToPlayer " + prepareWithConfigurationMode);
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

    @Override
    public void setPrepareWithConfigurationModeOff() {
        prepareWithConfigurationMode = false;
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
        LOGD(TAG, "PlayerStateChanged: " + playbackState + " playWhenReady: " + playWhenReady + " mPassedPlay: " + mPassedPlay + " mReadiness: " + mReadiness + " mSeeking: " + mSeeking);

        switch (playbackState) {
            case ExoPlayer.STATE_IDLE:
                if (mSeeking) {
                    mSeeking = false;
                }
                break;
            case ExoPlayer.STATE_PREPARING:
                break;
            case ExoPlayer.STATE_BUFFERING:
                LOGD(TAG, "STATE_BUFFERING mReadiness: " + mReadiness + " playWhenReady: " + playWhenReady);
                if (mPlayerListener != null) {
                    mPlayerListener.eventWithValue(this, KPlayerListener.BufferingChangeKey, "true");
                    mBuffering = true;
                }
                break;
            case ExoPlayer.STATE_READY:
                LOGD(TAG, "STATE_READY mReadiness: " + mReadiness + " mSeeking: " + mSeeking + " mBuffering: " + mBuffering + " playWhenReady: " + playWhenReady);
                if (mPlayerListener != null && mPlayerCallback != null) {
                    if (mBuffering) {
                        mPlayerListener.eventWithValue(this, KPlayerListener.BufferingChangeKey, "false");
                        mBuffering = false;
                    }
                    if ((mReadiness == KState.READY || mReadiness == KState.PAUSED) && !playWhenReady) {
                        LOGD(TAG, "Change to PauseKey");
                        mPlayerListener.eventWithValue(this, KPlayerListener.PauseKey, null);
                    }

                    // ExoPlayer is ready.

                    if (isFirstPlayback) {
                        isFirstPlayback = false;
                        mReadiness = KState.READY;
                        // TODO what about mShouldResumePlayback?
                        mPlayerListener.eventWithValue(this, KPlayerListener.DurationChangedKey, Float.toString(this.getDuration() / 1000f));
                        mPlayerListener.eventWithValue(this, KPlayerListener.LoadedMetaDataKey, "");
                        mPlayerListener.eventWithValue(this, KPlayerListener.CanPlayKey, null);
                        mPlayerCallback.playerStateChanged(KPlayerCallback.CAN_PLAY);
                    }
                    if (mSeeking) {
                        // ready after seeking
                        mReadiness = KState.READY;
                        mPlayerListener.eventWithValue(this, KPlayerListener.SeekedKey, null);
                        mPlayerCallback.playerStateChanged(KPlayerCallback.SEEKED);
                        mSeeking = false;
                        startPlaybackTimeReporter();
                    }

                    if (mPassedPlay && playWhenReady) {
                        mPassedPlay = false;
                        LOGD(TAG, "Change to PlayKey");
                        mPlayerListener.eventWithValue(this, KPlayerListener.PlayKey, null);
                    }
                }
                break;

            case ExoPlayer.STATE_ENDED:
                LOGD(TAG, "STATE_ENDED mReadiness: " + mReadiness + " playWhenReady: " + playWhenReady + " mBuffering: " + mBuffering);

                if (mReadiness == KState.IDLE || mReadiness == KState.PAUSED) {
                    return;
                }
                if (mReadiness == KState.SEEKING && playWhenReady) {
                    mPlayerListener.eventWithValue(this, KPlayerListener.SeekedKey, null);
                }

                if (playWhenReady) {
                    mPlayerCallback.playerStateChanged(KPlayerCallback.ENDED);
                    mReadiness = KState.IDLE;
                }
                else {
                    if (mBuffering) {
                        mPlayerListener.eventWithValue(this, KPlayerListener.BufferingChangeKey, "false");
                        mPlayerListener.eventWithValue(this, KPlayerListener.SeekedKey, null);
                    }
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
        LOGE(TAG, errMsg, e);
        String errorString = "";
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            errorString = (Util.SDK_INT < 18) ? "error_drm_not_supported"
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? "error_drm_unsupported_scheme" : "error_drm_unknown";
        } else if (e instanceof ExoPlaybackException && e.getCause() instanceof FileNotFoundException) {
            errorString = "DRM License Unavailable"; // probably license issue
        } else if (e instanceof ExoPlaybackException && e.getCause() instanceof BehindLiveWindowException) {
            LOGE(TAG, "Recovering BehindLiveWindowException"); // happens if network is bad and no more chunk in hte buffer
            mExoPlayer.prepare();
            return;
        } else if (e instanceof ExoPlaybackException && e.getCause() instanceof android.media.MediaCodec.CryptoException) {
            errorString = "DRM Error. Trying to recover"; // probably license issue
            mExoPlayer.prepare();
            return;
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
        else if (e.getCause() instanceof com.google.android.exoplayer.upstream.HttpDataSource.HttpDataSourceException) {
            mExoPlayer.prepare();
            errorString = "HttpDataSourceException . Trying to recover";
            LOGE(TAG, errorString);
            return;
        } else if (e.getCause() instanceof java.net.UnknownHostException) {
            mExoPlayer.prepare();
            errorString = "UnknownHostException . Trying to recover";
            LOGE(TAG, errorString);
            return;
        } else if (e.getCause() instanceof java.net.ConnectException) {
            mExoPlayer.prepare();
            errorString = "ConnectException . Trying to recover";
            LOGE(TAG, errorString);
            return;
        }
        else if (e.getCause() instanceof java.lang.IllegalStateException) {
            mExoPlayer.prepare();
            errorString = "IllegalStateException . Trying to recover";
            LOGE(TAG, errorString);
            return;
        }
        if (!"".equals(errorString)) {
            LOGE(TAG, errorString);
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
            LOGE(TAG, "getTrackCount mExoPlayer = null");
            return 0;
        }

    }

    @Override
    public int getCurrentTrackIndex(TrackType trackType) {
        if (mExoPlayer != null) {
            return mExoPlayer.getSelectedTrack(getExoTrackType(trackType));
        } else {
            LOGE(TAG, "getCurrentTrackIndex mExoPlayer = null");
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
            LOGE(TAG, "switchTrack mExoPlayer = null");
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
        LOGD(TAG, "subTitle = " + sb.toString());
        if (mSubtView != null) {
            mSubtView.setCues(cues);
        }
    }

    @Override
    public void onId3Metadata(List<Id3Frame> id3Frames) {
        for (Id3Frame id3Frame : id3Frames) {
            if (id3Frame instanceof TxxxFrame) {
                TxxxFrame txxxFrame = (TxxxFrame) id3Frame;
                LOGD(TAG, String.format("ID3 TimedMetadata %s: description=%s, value=%s", txxxFrame.id,
                        txxxFrame.description, txxxFrame.value));
            } else if (id3Frame instanceof PrivFrame) {
                PrivFrame privFrame = (PrivFrame) id3Frame;
                LOGD(TAG, String.format("ID3 TimedMetadata %s: owner=%s", privFrame.id, privFrame.owner));
            } else if (id3Frame instanceof GeobFrame) {
                GeobFrame geobFrame = (GeobFrame) id3Frame;
                LOGD(TAG, String.format("ID3 TimedMetadata %s: mimeType=%s, filename=%s, description=%s",
                        geobFrame.id, geobFrame.mimeType, geobFrame.filename, geobFrame.description));
            } else {
                LOGD(TAG, String.format("ID3 TimedMetadata %s", id3Frame.id));
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

    private class PlayerState {
        boolean playing;
        long position;

        void set(boolean playing, long position) {
            this.playing = playing;
            this.position = position;
        }
    }
}
