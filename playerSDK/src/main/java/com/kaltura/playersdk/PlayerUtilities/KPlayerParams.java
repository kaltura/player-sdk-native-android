package com.kaltura.playersdk.PlayerUtilities;

/**
 * Created by nissopa on 6/29/15.
 */
public class KPlayerParams {
    private String sourceURL;
    private String drmKyey;
    private String adTagURL;

    public String getSourceURL() {
        return sourceURL;
    }

    public void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }

    public String getDrmKyey() {
        return drmKyey;
    }

    public void setDrmKyey(String drmKyey) {
        this.drmKyey = drmKyey;
    }

    public String getAdTagURL() {
        return adTagURL;
    }

    public void setAdTagURL(String adTagURL) {
        this.adTagURL = adTagURL;
    }
}
