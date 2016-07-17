package com.kaltura.playersdk.players;

public enum KMediaFormat {
    mp4_clear("mp4", "video/mp4", ".mp4", null),
    dash_clear("dash", "application/dash+xml", ".mpd", null),
    dash_widevine("dash", "application/dash+xml", ".mpd", "widevine"),
    wvm_widevine("wvm", "video/wvm", ".wvm", "widevine"),
    hls_clear("hls", "application/x-mpegURL", ".m3u8", null),
    hls_fairplay("hls", "application/vnd.apple.mpegurl", ".m3u8", "fairplay");

    public final String shortName;
    public final String mimeType;
    public final String pathExt;
    public final String drm;

    KMediaFormat(String shortName, String mimeType, String pathExt, String drm) {
        this.shortName = shortName;
        this.mimeType = mimeType;
        this.pathExt = pathExt;
        this.drm = drm;
    }
}
