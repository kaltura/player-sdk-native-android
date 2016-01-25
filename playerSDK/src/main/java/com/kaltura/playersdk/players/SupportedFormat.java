package com.kaltura.playersdk.players;

/**
 * Created by noamt on 25/01/2016.
 */
public class SupportedFormat {
    public String mimeType;
    public String pathExt;
    public String drm;

    public SupportedFormat(String mimeType, String pathExt, String drm) {
        this.mimeType = mimeType;
        this.pathExt = pathExt;
        this.drm = drm;
    }

    public static final SupportedFormat MP4_CLEAR = new SupportedFormat("video/mp4", ".mp4", "clear");
    public static final SupportedFormat DASH_CLEAR = new SupportedFormat("application/dash+xml", ".mpd", "clear");
    public static final SupportedFormat DASH_WIDEVINE = new SupportedFormat("application/dash+xml", ".mpd", "widevine");
    public static final SupportedFormat CLASSIC_WIDEVINE = new SupportedFormat("video/wvm", ".wvm", "widevine");
    public static final SupportedFormat HLS_CLEAR = new SupportedFormat("application/vnd.apple.mpegurl", ".m3u8", "clear");
}
