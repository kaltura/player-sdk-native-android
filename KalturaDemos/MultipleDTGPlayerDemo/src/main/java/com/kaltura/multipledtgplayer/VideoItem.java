package com.kaltura.multipledtgplayer;

import com.kaltura.dtg.ContentManager;
import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadState;
import com.kaltura.playersdk.KPPlayerConfig;

import java.io.File;

/**
 * Created by Gleb on 9/28/16.
 */

public class VideoItem {

    public KPPlayerConfig config;
    public String flavorId;
    public String name;
    public String remoteUrl;
    public boolean isSelected;
    private ContentManager mContentManager;

    VideoItem(KPPlayerConfig config, String flavorId, String remoteUrl, String name) {
        this.config = config;
        this.flavorId = flavorId;
        this.remoteUrl = remoteUrl;
        this.name = name;
    }

    public void setContentManager(ContentManager contentManager) {
        mContentManager = contentManager;
    }

    public boolean isDownloaded() {
        DownloadItem item = findDownloadItem();
        return item != null && item.getState() == DownloadState.COMPLETED;
    }

    public DownloadState getState() {
        DownloadItem item = findDownloadItem();
        if (item != null) {
            return item.getState();
        }
        return null;
    }

    public DownloadItem findDownloadItem() {
        DownloadItem item = mContentManager.createItem(config.getEntryId(), remoteUrl);
        if (item == null) {
            item = mContentManager.findItem(config.getEntryId());
        }
        return item;
    }

    public String getLocalPath() {
        File f = mContentManager.getLocalFile(config.getEntryId());
        if (f != null) {
            return f.getAbsolutePath();
        }
        return null;
    }
}
