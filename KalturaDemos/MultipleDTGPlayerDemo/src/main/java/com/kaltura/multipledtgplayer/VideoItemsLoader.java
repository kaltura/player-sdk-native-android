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
import java.util.regex.Pattern;

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


                JSONObject jsonItem = items.getJSONObject(key);
                String mediaId = getString(jsonItem, "mediaId");

                JSONObject kpConfig = new JSONObject(getKPConfigJSON(mediaId));
                KPPlayerConfig config = KPPlayerConfig.fromJSONObject(kpConfig);
                String vootDeploy = config.getServerURL().replaceAll("/mwEmbed/mwEmbedFrame.php","");
                //String vootDeploy = "http://player-as.ott.kaltura.com/225/v2.48.1_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.4";
                final String nextPng = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Next.png";
                final String prvPng  = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Previous.png";
                final String kidsPlay  = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Play.png";
                final String kidsPause = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Kids/images/Pause.png";
                final String adultPlay  = vootDeploy +  "/kwidget-ps/ps/modules/viacom/resources/Player_Adult/images/Play.png";
                final String adultPause = vootDeploy + "/kwidget-ps/ps/modules/viacom/resources/Player_Adult/images/Pause.png";
                final String watermark  = "https://voot-kaltura.s3.amazonaws.com/voot-watermark.png";
                config.getCacheConfig().addIncludePattern(Pattern.compile(watermark, Pattern.LITERAL));
                config.getCacheConfig().addIncludePattern(Pattern.compile(nextPng, Pattern.LITERAL));
                config.getCacheConfig().addIncludePattern(Pattern.compile(prvPng, Pattern.LITERAL));
                config.getCacheConfig().addIncludePattern(Pattern.compile(kidsPlay, Pattern.LITERAL));
                config.getCacheConfig().addIncludePattern(Pattern.compile(kidsPause, Pattern.LITERAL));
                config.getCacheConfig().addIncludePattern(Pattern.compile(adultPlay, Pattern.LITERAL));
                config.getCacheConfig().addIncludePattern(Pattern.compile(adultPause, Pattern.LITERAL));

                config.getCacheConfig().addIncludePattern(".*kwidget-ps/ps/modules/viacom/resources/.+/images.*");

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


    private String getKPConfigJSON(String mediaId) {
        String configString= "{\n" +
                "  \"base\": {\n" +
                // "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.48.6_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.7/mwEmbed/mwEmbedFrame.php\",\n" +
                // "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.48.7_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.11/mwEmbed/mwEmbedFrame.php\",\n" +
                "    \"server\": \"http://player-as.ott.kaltura.com/225/v2.48.9_viacom_v0.31_v0.4.1_viacom_proxy_v0.4.12/mwEmbed//mwEmbedFrame.php\",\n" +
                "    \"partnerId\": \"\",\n" +
                "    \"uiConfId\": \"32626752\"\n" +
                "  },\n" +
                "  \"extra\": {\n" +
                "    \"watermark.plugin\": \"true\",\n" +
                "    \"watermark.img\": \"https://voot-kaltura.s3.amazonaws.com/voot-watermark.png\",\n" +
                "    \"watermark.title\": \"Viacom18\",\n" +
                "    \"watermark.cssClass\": \"topRight\",\n" +
                "    \"controlBarContainer.hover\": true,\n" +
                "    \"controlBarContainer.plugin\": true,\n" +
                "    \"kidsPlayer.plugin\": true,\n" +
                "    \"nextBtnComponent.plugin\": true,\n" +
                "    \"prevBtnComponent.plugin\": true,\n" +
                "    \"liveCore.disableLiveCheck\": true,\n" +
                // "    \"tvpapiGetLicensedLinks.plugin\": true,\n" +
                "    \"TVPAPIBaseUrl\": \"http://tvpapi-as.ott.kaltura.com/v3_4/gateways/jsonpostgw.aspx?m=\",\n" +
                "    \"proxyData\": {\n" +
                "      \"config\": {\n" +
                "        \"flavorassets\": {\n" +
                "          \"filters\": {\n" +
                "            \"include\": {\n" +
                "              \"Format\": [\n" +
                "                \"dash Mobile\"\n" +
                "              ]\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      },\n" +
                "      \"MediaID\": \"" + mediaId + "\",\n" +
                "      \"iMediaID\": \"" + mediaId + "\",\n" +
                "      \"mediaType\": \"0\",\n" +
                "      \"picSize\": \"640x360\",\n" +
                "      \"withDynamic\": \"false\",\n" +
                "      \"initObj\": {\n" +
                "        \"ApiPass\": \"11111\",\n" +
                "        \"ApiUser\": \"tvpapi_225\",\n" +
                "        \"DomainID\": 0,\n" +
                "        \"Locale\": {\n" +
                "          \"LocaleCountry\": \"null\",\n" +
                "          \"LocaleDevice\": \"null\",\n" +
                "          \"LocaleLanguage\": \"null\",\n" +
                "          \"LocaleUserState\": \"Unknown\"\n" +
                "        },\n" +
                "        \"Platform\": \"Cellular\",\n" +
                "        \"SiteGuid\": \"\",\n" +
                "        \"UDID\": \"aa5e1b6c96988d68\"\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
        return configString;
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
        LOGD("download", "completed");
        String localPath = getLocalPath(item.getItemId());
        VideoItem videoItem = findItemByMediaId(item.getItemId());
        if (localPath != null && !localPath.isEmpty() && videoItem != null) {
            LocalAssetsManager.registerAsset(mContext, videoItem.config, null, localPath, new LocalAssetsManager.AssetRegistrationListener() {
                @Override
                public void onRegistered(String assetPath) {
                    LOGD("download", "Register successfully");
                }

                @Override
                public void onFailed(String assetPath, Exception error) {
                    LOGD("download", "Register failed " + error.getMessage());
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
        LOGD("onProgressChange", "downloaded " + downloadedBytes);
        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }
    }

    @Override
    public void onDownloadStart(DownloadItem item) {
        LOGD("onDownloadStart", "");

        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }
    }

    @Override
    public void onDownloadPause(DownloadItem item) {
        LOGD("onDownloadPause", "");
        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }

    }

    @Override
    public void onDownloadStop(DownloadItem item) {
        LOGD("onDownloadStop", "");
        DownloadItemView view = getView(getItemPositionByMediaId(item.getItemId()));
        if (view != null) {
            view.bind(item);
        }
    }

    @Override
    public void onDownloadMetadata(DownloadItem item, Exception error) {
        LOGD("onDownloadMetadata", "");

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

    @Override
    public void onTracksAvailable(DownloadItem item, DownloadItem.TrackSelector trackSelector) {
        // Select lowest-resolution video
        List<DownloadItem.Track> videoTracks = trackSelector.getAvailableTracks(DownloadItem.TrackType.VIDEO);
        DownloadItem.Track minVideo = Collections.min(videoTracks, DownloadItem.Track.bitrateComparator);
        trackSelector.setSelectedTracks(DownloadItem.TrackType.VIDEO, Collections.singletonList(minVideo));
    }
}