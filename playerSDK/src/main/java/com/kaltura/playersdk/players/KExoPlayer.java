package com.kaltura.playersdk.players;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.MediaDrm;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.FrameLayout;

import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecUtil;
import com.google.android.exoplayer.drm.MediaDrmCallback;
import com.google.android.exoplayer.drm.UnsupportedDrmException;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.exoplayer.util.Util;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerUtil;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;
import com.google.android.libraries.mediaframework.exoplayerextensions.RendererBuilderFactory;
import com.google.android.libraries.mediaframework.exoplayerextensions.Video;
import com.google.android.libraries.mediaframework.layeredvideo.VideoSurfaceView;
import com.kaltura.playersdk.LanguageTrack;
import com.kaltura.playersdk.QualityTrack;
import com.kaltura.playersdk.types.TrackType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    private boolean mPassedPlay = false;

    private SurfaceHolder.Callback mSurfaceCallback;

    public static Set<MediaFormat> supportedFormats(Context context) {
        Set<MediaFormat> set = new HashSet<>();
        // Clear dash and mp4 are always supported by this player.
        set.add(MediaFormat.dash_clear);
        set.add(MediaFormat.mp4_clear);
        set.add(MediaFormat.hls_clear);

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

    @Override
    public boolean isPlaying() {
        return mExoPlayer != null
                && mExoPlayer.getPlayWhenReady();
    }

     public void hide(){
     }

    public void show(){
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
        mSurfaceCallback = new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (mExoPlayer == null) {
                    mExoPlayer = new ExoplayerWrapper(rendererBuilder);
                    Surface surface = holder.getSurface();
                    if (surface != null && surface.isValid()) {
                        mExoPlayer.setSurface(surface);
                    } else {
                        String errMsg = "Surface not ready yet";
                        Log.e(TAG, errMsg);
                        mPlayerListener.eventWithValue(KExoPlayer.this, KPlayerListener.ErrorKey, errMsg);
                        return;
                    }
                    mExoPlayer.addListener(KExoPlayer.this);
                    mExoPlayer.prepare();

                } else {
                    mExoPlayer.setSurface(holder.getSurface());
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
        this.addView(mSurfaceView, layoutParams);
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
    public void freezePlayer() {
        if (mExoPlayer != null) {
            mExoPlayer.setBackgrounded(true);
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
        if (mSurfaceView != null && mSurfaceCallback != null) {
            mSurfaceView.getHolder().removeCallback(mSurfaceCallback);
        }
        mReadiness = Readiness.Idle;
    }
    
    @Override
    public void recoverPlayer() {
        if (mExoPlayer != null) {
            mExoPlayer.setBackgrounded(false);
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

    private void savePlayerState() {
        saveState();
        pause();
    }

    private void recoverPlayerState() {
        setCurrentPlaybackTime(mSavedState.position);
        if (mSavedState.playing) {
            play();
        }
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
                    sendTracksList(TrackType.TEXT);
                    sendTracksList(TrackType.AUDIO);
                    sendTracksList(TrackType.VIDEO);
                    // TODO what about mShouldResumePlayback?
                    mPlayerListener.eventWithValue(this, KPlayerListener.DurationChangedKey, Float.toString(this.getDuration() / 1000f));
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

                if (mPassedPlay && playWhenReady) {
                    mPassedPlay = false;
                    mPlayerListener.eventWithValue(this, KPlayerListener.PlayKey, null);
                }
                break;

            case ExoPlayer.STATE_ENDED:
                Log.d(TAG, "state ended");
//                if (mExoPlayer != null) {
//                    Log.d(TAG, "state ended: set play when ready false");
//                    setPlayWhenReady(false);
//                }
//                if (mExoPlayer != null) {
//                    Log.d(TAG, "state ended: seek to 0");
//                    setCurrentPlaybackTime(0);
//                }
                if (playWhenReady) {
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
        String errMsg = "Player Error";
        Log.e(TAG, errMsg, e);
        String errorString = "";
        if (e instanceof UnsupportedDrmException) {
            // Special case DRM failures.
            UnsupportedDrmException unsupportedDrmException = (UnsupportedDrmException) e;
            errorString = (Util.SDK_INT < 18) ? "error_drm_not_supported"
                    : unsupportedDrmException.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                    ? "error_drm_unsupported_scheme" : "error_drm_unknown";
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
        if (!"".equals(errorString)){
            Log.e(TAG, errorString);
            errorString += "-";
        }

        mPlayerListener.eventWithValue(KExoPlayer.this, KPlayerListener.ErrorKey, TAG + "-" + errMsg + "-" +  errorString  + e.getMessage());
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        mSurfaceView.setVideoWidthHeightRatio((float)width / height);
    }

    @Override
    public List<String> getTracksList(TrackType trackType) {
        List<String> tracksList = new ArrayList<>();
        //ExoplayerWrapper.TYPE_AUDIO
        //ExoplayerWrapper.TYPE_TEXT
        //ExoplayerWrapper.TYPE_VIDEO
        int exoTrackType = getExoTrackType(trackType);
        int trackCount = mExoPlayer.getTrackCount(exoTrackType);
        //menu.add(MENU_GROUP_TRACKS, DemoPlayer.TRACK_DISABLED + ID_OFFSET, Menu.NONE, R.string.off);
        for (int i = 0; i < trackCount; i++) {
            tracksList.add(getTrackName(mExoPlayer.getTrackFormat(exoTrackType, i)));
        }
        //menu.findItem(player.getSelectedTrack(trackType) + ID_OFFSET).setChecked(true);
        return tracksList;
    }

    private JSONObject getTracksAsJson(TrackType trackType) {
        JSONObject resultJsonObj = new JSONObject();
        try {
            JSONArray tracksJsonArray = new JSONArray();
            int exoTrackType = getExoTrackType(trackType);
            int trackCount = mExoPlayer.getTrackCount(exoTrackType);
            String resultJsonKey = "";

            if (TrackType.VIDEO.equals(trackType)){
                resultJsonKey = "tracks";
                for (int index = 0; index < trackCount; index++) {
                    if (TrackType.VIDEO.equals(trackType)) {
                        com.google.android.exoplayer.MediaFormat mediaFormat = mExoPlayer.getTrackFormat(exoTrackType, index);
                        QualityTrack qualityTrack = new QualityTrack();
                        qualityTrack.setAssetId(String.valueOf(index));
                        qualityTrack.setBandwidth(mediaFormat.bitrate);
                        qualityTrack.setType(mediaFormat.mimeType);
                        qualityTrack.setHeight(mediaFormat.height);
                        qualityTrack.setWidth(mediaFormat.width);
                        tracksJsonArray.put(qualityTrack.toJSONObject());
                    }
                }
            }else if (TrackType.TEXT.equals(trackType) || TrackType.AUDIO.equals(trackType)) {
                    resultJsonKey = "languages";
                for (int index = 0; index < trackCount; index++) {
                    com.google.android.exoplayer.MediaFormat mediaFormat = mExoPlayer.getTrackFormat(exoTrackType, index);
                    LanguageTrack languageTrack = new LanguageTrack();
                    languageTrack.setIndex(index);
                    languageTrack.setKind("subtitle"); //"caption"??
                    languageTrack.setTitle(getTrackName(mExoPlayer.getTrackFormat(exoTrackType, index)));
                    String trackId = (mediaFormat.trackId != null) ? mediaFormat.trackId: "Auto";
                    languageTrack.setLanguage(trackId);
                    languageTrack.setSrclang(trackId);
                    languageTrack.setLabel(trackId);
                    tracksJsonArray.put(languageTrack.toJSONObject());
                }
            }
            resultJsonObj.put(resultJsonKey, tracksJsonArray);
        } catch (JSONException ex) {
            ex.printStackTrace();
            return null;
        }
        Log.e(TAG, "Track/Lang Json Result  " + resultJsonObj.toString());
        return resultJsonObj;
    }

    @Override
    public int getTrackCount(TrackType trackType) {
        return getTracksList(trackType).size();
    }

    @Override
    public int getCurrentTrackIndex(TrackType trackType) {
        return mExoPlayer.getSelectedTrack(getExoTrackType(trackType));
    }

    @Override
    public void switchTrack(TrackType trackType, int newIndex) {
        int exoTrackType = ExoplayerWrapper.TRACK_DISABLED;
        if (trackType == null){
            return;
        }
        mExoPlayer.setSelectedTrack(getExoTrackType(trackType), newIndex);
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

    public com.google.android.exoplayer.MediaFormat getTrackFormat(TrackType trackType, int index){
        return mExoPlayer.getTrackFormat(getExoTrackType(trackType), index);
    }

    public String getTrackName(com.google.android.exoplayer.MediaFormat format) {
        if (format.adaptive) {
            return "auto";
        }
        String trackName;
        if (MimeTypes.isVideo(format.mimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(format),
                    buildBitrateString(format)), buildTrackIdString(format));
        } else if (MimeTypes.isAudio(format.mimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(joinWithSeparator(buildLanguageString(format),
                    buildAudioPropertyString(format)), buildBitrateString(format)),
                    buildTrackIdString(format));
        } else {
            trackName = joinWithSeparator(joinWithSeparator(buildLanguageString(format),
                    buildBitrateString(format)), buildTrackIdString(format));
        }
        return trackName.length() == 0 ? "unknown" : trackName;
    }

    @Override
    public void setCaptionListener(ExoplayerWrapper.CaptionListener listener) {
        mExoPlayer.setCaptionListener(listener);
    }

    @Override
    public void setMetadataListener(ExoplayerWrapper.Id3MetadataListener listener) {
        mExoPlayer.setMetadataListener(listener);
    }

    private String buildResolutionString(com.google.android.exoplayer.MediaFormat format) {
        return format.width == com.google.android.exoplayer.MediaFormat.NO_VALUE || format.height == com.google.android.exoplayer.MediaFormat.NO_VALUE
                ? "" : format.width + "x" + format.height;
    }

    private String buildAudioPropertyString(com.google.android.exoplayer.MediaFormat format) {
        return format.channelCount == com.google.android.exoplayer.MediaFormat.NO_VALUE || format.sampleRate == com.google.android.exoplayer.MediaFormat.NO_VALUE
                ? "" : format.channelCount + "ch, " + format.sampleRate + "Hz";
    }

    private String buildLanguageString(com.google.android.exoplayer.MediaFormat format) {
        return TextUtils.isEmpty(format.language) || "und".equals(format.language) ? ""
                : format.language;
    }

    private static String buildBitrateString(com.google.android.exoplayer.MediaFormat format) {
        return format.bitrate == com.google.android.exoplayer.MediaFormat.NO_VALUE ? ""
                : String.format(Locale.US, "%.2fMbit", format.bitrate / 1000000f);
    }

    private String joinWithSeparator(String first, String second) {
        return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
    }

    private String buildTrackIdString(com.google.android.exoplayer.MediaFormat format) {
        return format.trackId == null ? "" : format.trackId;
    }

    private void sendTracksList(TrackType trackType) {
        Log.d(TAG, "sendTracksList with:" + trackType);
        switch(trackType) {
            case AUDIO:
                mPlayerListener.eventWithJSON(KExoPlayer.this, KPlayerListener.AudioTracksReceivedKey, getTracksAsJson(TrackType.AUDIO).toString());
                break;
            case TEXT:
                mPlayerListener.eventWithJSON(KExoPlayer.this, KPlayerListener.TextTracksReceivedKey,  getTracksAsJson(TrackType.TEXT).toString());
                break;
            case VIDEO:
                mPlayerListener.eventWithJSON(KExoPlayer.this, KPlayerListener.FlavorsListChangedKey,  getTracksAsJson(TrackType.VIDEO).toString());
                break;
        }
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
