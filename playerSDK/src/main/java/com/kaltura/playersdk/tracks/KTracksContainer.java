package com.kaltura.playersdk.tracks;

import java.util.List;

/**
 * Created by gilad.nadav on 26/05/2016.
 */
public class KTracksContainer {

    private List<TrackFormat> audioTrackList;
    private List<TrackFormat> textTrackList;
    private List<TrackFormat> videoTrackList;

    public KTracksContainer() {}

    public KTracksContainer(List<TrackFormat> audioTrackList, List<TrackFormat> textTrackList, List<TrackFormat> videoTrackList) {
        this.audioTrackList = audioTrackList;
        this.textTrackList = textTrackList;
        this.videoTrackList = videoTrackList;
    }

    public List<TrackFormat> getAudioTrackList() {
        return audioTrackList;
    }

    public void setAudioTrackList(List<TrackFormat> audioTrackList) {
        this.audioTrackList = audioTrackList;
    }

    public List<TrackFormat> getTextTrackList() {
        return textTrackList;
    }

    public void setTextTrackList(List<TrackFormat> textTrackList) {
        this.textTrackList = textTrackList;
    }

    public List<TrackFormat> getVideoTrackList() {
        return videoTrackList;
    }

    public void setVideoTrackList(List<TrackFormat> videoTrackList) {
        this.videoTrackList = videoTrackList;
    }
}
