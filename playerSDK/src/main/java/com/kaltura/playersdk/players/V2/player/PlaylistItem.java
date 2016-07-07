package com.kaltura.playersdk.players.V2.player;

import java.util.List;

/**
 * Created by gilad.nadav on 06/07/2016.
 */
public class PlaylistItem {
    private List<MediaSource> mediaSources;

    public PlaylistItem(List<MediaSource> mediaSources) {
        this.mediaSources = mediaSources;
    }

    public List<MediaSource> getMediaSources() {
        return mediaSources;
    }
}
