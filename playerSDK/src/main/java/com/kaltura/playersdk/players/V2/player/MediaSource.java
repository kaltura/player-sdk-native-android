package com.kaltura.playersdk.players.V2.player;

/**
 * Created by gilad.nadav on 06/07/2016.
 */
public class MediaSource {
    private String sourceURL;
    private String imageURL;
    private String description;
    private String title;

    public MediaSource(String sourceURL, String imageURL, String description, String title) {
        this.sourceURL = sourceURL;
        this.imageURL = imageURL;
        this.description = description;
        this.title = title;
    }

    public String getSourceURL() {
        return sourceURL;
    }

    public void setSourceURL(String sourceURL) {
        this.sourceURL = sourceURL;
    }

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
