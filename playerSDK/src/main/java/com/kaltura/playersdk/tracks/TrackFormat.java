package com.kaltura.playersdk.tracks;

import android.text.TextUtils;

import com.google.android.exoplayer.MediaFormat;
import com.google.android.exoplayer.util.MimeTypes;
import com.google.android.libraries.mediaframework.exoplayerextensions.ExoplayerWrapper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Created by gilad.nadav on 25/05/2016.
 */
public class TrackFormat {
    public static String TAG = "TrackFormat";

    public int       index;
    public TrackType trackType;
    public String    trackId;
    public int       bitrate;
    public int       channelCount;
    public int       sampleRate;
    public int       height;
    public int       width;
    public String    mimeType;
    public String    trackLabel;
    public String    language;
    public boolean   adaptive;


    public TrackFormat(TrackType trackType, int index, MediaFormat mediaFormat){
        this.index     = index;
        this.trackType = trackType;
        if (mediaFormat != null) {
            this.trackId = mediaFormat.trackId;
            this.bitrate = mediaFormat.bitrate;
            this.channelCount = mediaFormat.channelCount;
            this.sampleRate   = mediaFormat.sampleRate;
            this.height       = mediaFormat.height;
            this.width        = mediaFormat.width;
            this.mimeType     = mediaFormat.mimeType;
            this.language     = mediaFormat.language;
            this.adaptive     = mediaFormat.adaptive;
            this.trackLabel   = getTrackName();
        }

    }

    public void setTrackLabel(String newLabel) {
        this.trackLabel = newLabel;
    }

    public String getTrackName() {
        if (adaptive) {
            return "auto" + "-" + index;
        }
        String trackName;
        if (MimeTypes.isVideo(mimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(),
                    buildBitrateString()), buildTrackIdString());

        } else if (MimeTypes.isAudio(mimeType)) {
            trackName = buildTrackIdString();
            if (!"".equals(buildLanguageString())){
                trackName = buildLanguageString();
            }
        } else {
            trackName = buildTrackIdString();
            if (!"".equals(buildLanguageString())){
                trackName = buildLanguageString();
            }
        }
        return trackName.length() == 0 ? "unknown" : trackName;
    }

    public String getTrackLanguage() {
        return this.language.length() == 0 ? "unknown" : this.language;
    }

    public String getTrackFullName() {
        if (adaptive) {
            return "auto" + "-" + index;
        }
        String trackName;
        if (MimeTypes.isVideo(mimeType)) {
            trackName = joinWithSeparator(joinWithSeparator(buildResolutionString(),
                    buildBitrateString()), buildTrackIdString());

        } else if (MimeTypes.isAudio(mimeType)) {
            trackName = buildTrackIdString();
        } else {
            trackName = buildTrackIdString();
        }
        return trackName.length() == 0 ? "unknown" : trackName;

    }

    private String buildResolutionString() {
        return this.width == MediaFormat.NO_VALUE || this.height == MediaFormat.NO_VALUE
                ? "" : this.width + "x" + this.height;
    }

    private String buildAudioPropertyString() {
        return this.channelCount == MediaFormat.NO_VALUE || this.sampleRate == MediaFormat.NO_VALUE
                ? "" : this.channelCount + "ch, " + this.sampleRate + "Hz";
    }

    private String buildLanguageString() {
        return TextUtils.isEmpty(this.language) || "und".equals(this.language) ? ""
                : this.language;
    }

    private String buildBitrateString() {
        return this.bitrate == MediaFormat.NO_VALUE ? ""
                : String.format(Locale.US, "%.2fMbit", this.bitrate / 1000000f);
    }

    private String joinWithSeparator(String first, String second) {
        return first.length() == 0 ? second : (second.length() == 0 ? first : first + ", " + second);
    }

    private String buildTrackIdString() {
        return this.trackId == null ? "" : this.trackId;
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

    public JSONObject toJSONObject() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (TrackType.VIDEO.equals(trackType)) {
                jsonObject.put("assetid", String.valueOf(this.index));
                jsonObject.put("originalIndex", this.index);
                jsonObject.put("bandwidth", this.bitrate);
                jsonObject.put("type", this.mimeType);
                jsonObject.put("height", this.height);
                jsonObject.put("width", this.width);
            } else if (TrackType.AUDIO.equals(trackType) || TrackType.TEXT.equals(trackType)){
                // need id???
                // need mode???
                jsonObject.put("index", this.index);
                jsonObject.put("kind", "subtitle");
                jsonObject.put("label", this.trackLabel);
                jsonObject.put("language", this.language);
                jsonObject.put("title", getTrackFullName());
                String trackId = (this.trackId != null) ? this.trackId: "Auto";
                jsonObject.put("srclang", this.trackLabel); // maybe trackId???
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return jsonObject;
    }

    public static TrackFormat getDefaultTrackFormat(TrackType trackType) {
        TrackFormat defaultTrackFormat = new TrackFormat(trackType, -1, null);
        defaultTrackFormat.trackLabel = "Off";
        return defaultTrackFormat;
    }

    @Override
    public String toString() {
        return "TrackFormat{" +
                "index=" + index +
                ", trackType=" + trackType +
                ", trackId='" + trackId + '\'' +
                ", bitrate=" + bitrate +
                ", channelCount=" + channelCount +
                ", sampleRate=" + sampleRate +
                ", height=" + height +
                ", width=" + width +
                ", mimeType='" + mimeType + '\'' +
                ", trackLabel='" + trackLabel + '\'' +
                ", language='" + language + '\'' +
                ", adaptive=" + adaptive +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TrackFormat that = (TrackFormat) o;

        if (index != that.index) return false;
        if (bitrate != that.bitrate) return false;
        if (channelCount != that.channelCount) return false;
        if (sampleRate != that.sampleRate) return false;
        if (height != that.height) return false;
        if (width != that.width) return false;
        if (adaptive != that.adaptive) return false;
        if (trackType != that.trackType) return false;
        if (!trackId.equals(that.trackId)) return false;
        if (mimeType != null ? !mimeType.equals(that.mimeType) : that.mimeType != null)
            return false;
        if (!trackLabel.equals(that.trackLabel)) return false;
        return language != null ? language.equals(that.language) : that.language == null;

    }

    @Override
    public int hashCode() {
        int result = index;
        result = 31 * result + trackType.hashCode();
        result = 31 * result + trackId.hashCode();
        result = 31 * result + bitrate;
        result = 31 * result + channelCount;
        result = 31 * result + sampleRate;
        result = 31 * result + height;
        result = 31 * result + width;
        result = 31 * result + (mimeType != null ? mimeType.hashCode() : 0);
        result = 31 * result + trackLabel.hashCode();
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + (adaptive ? 1 : 0);
        return result;
    }
}

