package com.kaltura.multipledtgplayer;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.kaltura.dtg.ContentManager;
import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadState;
import com.kaltura.dtg.DownloadStateListener;
import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.LocalAssetsManager;
import com.kaltura.playersdk.utils.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;

/**
 * Created by Gleb on 9/28/16.
 */

public class VideoItemsLoader implements DownloadItemView.OnItemListener, DownloadStateListener {

    private List<VideoItem> mVideoItems;
    private ViewGroup mParent;
    private ContentManager mContentManager;
    private Context mContext;

    public VideoItemsLoader(Context context) {
        mContext = context;
        mVideoItems = new ArrayList<>();
        mContentManager = ContentManager.getInstance(context);
        mContentManager.addDownloadStateListener(this);
        mContentManager.start();
    }

    public void loadItems(String fileName) {

        String itemsString = Utilities.readAssetToString(mContext, fileName);

        try {
            JSONObject content = new JSONObject(itemsString);
            JSONObject baseConfig = content.getJSONObject("baseConfig");
            JSONObject items = content.getJSONObject("items");


            for (Iterator<String> it = items.keys(); it.hasNext(); ) {
                String key = it.next();

                KPPlayerConfig config = KPPlayerConfig.fromJSONObject(baseConfig);
                JSONObject jsonItem = items.getJSONObject(key);

                String entryId = getString(jsonItem, "entryId");
                if (entryId == null) {
                    continue;
                }
                config.setEntryId(entryId);
                String remoteUrl = getString(jsonItem, "remoteUrl");
                String flavorId = getString(jsonItem, "flavorId");

                config.setLocalContentId(key);

                config.addConfig("autoPlay", "true");

                VideoItem item = new VideoItem(config, flavorId, remoteUrl, key);
                item.setContentManager(mContentManager);
                mVideoItems.add(item);
            }

        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), "Error parsing json", e);
        }
    }

    private String getString(JSONObject jsonObject, String key) {
        return jsonObject.isNull(key) ? null : jsonObject.optString(key);
    }

    public void attachToParent(ViewGroup parent) {
        mParent = parent;
        parent.removeAllViews();
        int itemId = 0;
        for (VideoItem videoItem : mVideoItems) {
            DownloadItemView itemView = new DownloadItemView(parent.getContext());
            itemView.bind(videoItem);
            itemView.setItemId(itemId);
            itemView.setOnItemListener(this);
            parent.addView(itemView);
            itemId++;
        }
    }

    public void updateAll() {
        int itemId = 0;
        for (VideoItem videoItem : mVideoItems) {
            DownloadItemView itemView = (DownloadItemView)mParent.getChildAt(itemId);
            itemView.bind(videoItem);
            itemView.setItemId(itemId);
            itemView.setOnItemListener(this);
            itemId++;
        }
    }

    public VideoItem getItem(int position) {
        if (position >= 0 && position < mVideoItems.size()) {
            return mVideoItems.get(position);
        }
        return null;
    }

    public VideoItem findItemByMediaId(String entryId) {
        for (VideoItem videoItem : mVideoItems) {
            if (videoItem.config.getEntryId().equals(entryId)) {
                return videoItem;
            }
        }
        return null;
    }

    public int getItemPositionByMediaId(String entryId) {
        for (int i=0;i<mVideoItems.size();i++) {
            if (mVideoItems.get(i).config.getEntryId().equals(entryId)) {
                return i;
            }
        }
        return -1;
    }

    public DownloadItemView getView(int position) {
        if (position >= 0 && position < mParent.getChildCount()) {
            return (DownloadItemView)mParent.getChildAt(position);
        }
        return null;
    }

    public VideoItem getSelectedItem() {
        for (VideoItem videoItem : mVideoItems) {
            if (videoItem.isSelected) {
                return videoItem;
            }
        }
        return null;
    }

    @Override
    public void onCheck(int itemId, boolean isChecked) {
        for (int i=0;i<mVideoItems.size();i++) {
            VideoItem item = mVideoItems.get(i);
            if (i == itemId) {
                item.isSelected = isChecked;
            }
            else {
                item.isSelected = false;
                getView(i).bind(item);
            }
        }
    }

    @Override
    public void onDownloadClick(int itemId, DownloadState downloadState) {
        DownloadItem item = getItem(itemId).findDownloadItem();
        if (item != null) {
            LOGD("Download state click", "state = " + item.getState().name());
            switch (item.getState()) {
                case NEW:
                    item.loadMetadata();
                    break;
                case INFO_LOADED:
                case PAUSED:
                    item.startDownload();
                    break;
                case IN_PROGRESS:
                    item.pauseDownload();
                    break;
            }
        }
        if (item != null && item.getState() != DownloadState.COMPLETED) {
            item.loadMetadata();
        }
    }

    public String getLocalPath(String itemId) {
        File localFile = mContentManager.getLocalFile(itemId);
        if (localFile != null) {
            return localFile.getAbsolutePath();
        }
        return "";
        //return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() + "/" + itemId + ".mp4";
    }

    public String getUrl(String entryId) {
        return mContentManager.getPlaybackURL(entryId);
    }

    @Override
    public void onDownloadComplete(DownloadItem item) {
        LOGD("Download state", "completed");
        String localPath = getLocalPath(item.getItemId());
        VideoItem videoItem = findItemByMediaId(item.getItemId());
        if (localPath != null && videoItem != null) {
            LocalAssetsManager.registerAsset(mContext, videoItem.config, videoItem.flavorId, localPath, new LocalAssetsManager.AssetRegistrationListener() {
                @Override
                public void onRegistered(String assetPath) {
                    LOGD("Download state", "Register successful");
                }

                @Override
                public void onFailed(String assetPath, Exception error) {
                    LOGD("Download state", "Register failed " + error.getMessage());
                }
            });
        }
        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }
    }

    @Override
    public void onProgressChange(DownloadItem item, long downloadedBytes) {
        LOGD("Download state", "onProgressChange: downloaded " + downloadedBytes);
        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }
    }

    @Override
    public void onDownloadStart(DownloadItem item) {
        LOGD("Download state", "onDownloadStart");

        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }
    }

    @Override
    public void onDownloadPause(DownloadItem item) {
        LOGD("Download state", "onDownloadPause");
        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }

    }

    @Override
    public void onDownloadStop(DownloadItem item) {
        LOGD("Download state", "onDownloadStop");
        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }
    }

    @Override
    public void onDownloadMetadata(DownloadItem item, Exception error) {
        LOGD("Download state", "onDownloadMetaData state = " + item.getState().toString());
        DownloadState state = item.getState();
        if (state == DownloadState.INFO_LOADED || state == DownloadState.NEW) {
            DownloadItem.TrackSelector trackSelector = item.getTrackSelector();
            if (trackSelector != null) {
                List<DownloadItem.Track> downloadedVideoTracks = trackSelector.getDownloadedTracks(DownloadItem.TrackType.VIDEO);

                List<DownloadItem.Track> availableTracks = trackSelector.getAvailableTracks(DownloadItem.TrackType.AUDIO);
                if (availableTracks.size() > 0) {
                    trackSelector.setSelectedTracks(DownloadItem.TrackType.AUDIO, availableTracks);
                }
                try {
                    trackSelector.apply();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            item.startDownload();
        }
    }

    @Override
    public void onTracksAvailable(DownloadItem item, DownloadItem.TrackSelector trackSelector) {
        LOGD("Download state", "onTracksAvailable");

        // Select lowest-resolution video
        List<DownloadItem.Track> videoTracks = trackSelector.getAvailableTracks(DownloadItem.TrackType.VIDEO);
        DownloadItem.Track minVideo = Collections.min(videoTracks, DownloadItem.Track.bitrateComparator);
        trackSelector.setSelectedTracks(DownloadItem.TrackType.VIDEO, Collections.singletonList(minVideo));
    }
}
