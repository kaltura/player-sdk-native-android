package com.kaltura.playersdk.tracks;

import com.kaltura.playersdk.players.KPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by gilad.nadav on 25/05/2016.
 */
public class KTracksManager implements  KTrackActions {

    public static String TAG = "TracksManager";

    private KPlayer player;
    private KTracksContainer tracksContainer;
    private KTrackActions.VideoTrackEventListener videoTrackEventListener = null;
    private KTrackActions.AudioTrackEventListener audioTrackEventListener = null;
    private KTrackActions.TextTrackEventListener  textTrackEventListener = null;


    public KTracksManager(KPlayer player) {
        this.player = player;
        initTrackManagerLists();
    }

    public void setVideoTrackEventListener(KTrackActions.VideoTrackEventListener videoTrackEventListener) {
        this.videoTrackEventListener = videoTrackEventListener;
    }
    public void setAudioTrackEventListener(KTrackActions.AudioTrackEventListener audioTrackEventListener) {
        this.audioTrackEventListener = audioTrackEventListener;
    }
    public void setTextTrackEventListener(KTrackActions.TextTrackEventListener textTrackEventListener) {
        this.textTrackEventListener = textTrackEventListener;
    }

    public void removeVideoTrackEventListener() {
        this.videoTrackEventListener = null;
    }
    public void removeAudioTrackEventListener() {
        this.audioTrackEventListener = null;
    }
    public void removeTextTrackEventListener() {
        this.textTrackEventListener = null;
    }

    @Override
    public void switchTrack(TrackType trackType, int newIndex) {
        if (isAvailableTracksRelevant(trackType)) {
            LOGD(TAG, "switchTrack for " + trackType.name() + " newIndex = " + newIndex);
            player.switchTrack(trackType, newIndex);
            switch (trackType) {
                case VIDEO:
                    if (videoTrackEventListener != null) {
                        videoTrackEventListener.onVideoTrackChanged(newIndex);
                    }
                    break;
                case AUDIO:
                    if (audioTrackEventListener != null) {
                        audioTrackEventListener.onAudioTrackChanged(newIndex);
                    }
                    break;
                case TEXT:
                    if (textTrackEventListener != null) {
                        textTrackEventListener.onTextTrackChanged(newIndex);
                    }
                    break;
            }
        } else {
            LOGD(TAG, "switchTrack " + trackType.name() + "skipped Reason: track count  < 2");
        }
    }

    @Override
    public void switchTrackByBitrate(TrackType trackType, final int preferredBitrateKBit) {
        LOGD(TAG, "switchTrackByBitrate : " + trackType.name() + " preferredBitrateKBit : " + preferredBitrateKBit);
        if (TrackType.TEXT.equals(trackType)){
            return;
        }

        List<TrackFormat> tracksList = null;
        if (TrackType.AUDIO.equals(trackType)){
            tracksList = getAudioTrackList();

        } else if (TrackType.VIDEO.equals(trackType)){
            tracksList =  getVideoTrackList();
        } else {
            //unsupported track type
            return;
        }

        if (tracksList == null) {
            return;
        }

        if (tracksList.size() <= 2) {
            LOGD(TAG, "Skip switchTrackByBitrate, tracksList.size() <= 2");
            return;
        }

        TrackFormat autoTrackFormat = null;
        if (tracksList.get(0).bitrate == -1) {
            autoTrackFormat = tracksList.get(0);
            tracksList.remove(0);
        }

        Comparator <TrackFormat> tracksComperator = new Comparator<TrackFormat>() {
            @Override
            public int compare(TrackFormat track1, TrackFormat track2) {
                if (Math.abs(track1.bitrate - preferredBitrateKBit*1000) > Math.abs(track2.bitrate - preferredBitrateKBit*1000)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        };

        SortedSet<TrackFormat> bitrateSet = new TreeSet<TrackFormat>(tracksComperator);
        bitrateSet.addAll(tracksList);
        LOGD(TAG, "preferred bitrate selected = " +  bitrateSet.first());
        switchTrack(trackType, bitrateSet.first().index);

        //adding Auto again after removing it for comperator ignorance
        if (autoTrackFormat != null) {
            tracksList.add(0, autoTrackFormat);
        }
    }

    @Override
    public TrackFormat getCurrentTrack(TrackType trackType) {
        int currnetTrackIndex = player.getCurrentTrackIndex(trackType);
        if (currnetTrackIndex == -1){
            return TrackFormat.getDefaultTrackFormat(trackType);
        }
        List<TrackFormat> tracksList = getTracksList(trackType);
        LOGD(TAG, "getCurrentTrack " + trackType.name() + " tracksList size = " + tracksList.size() + " currentIndex = " + currnetTrackIndex);
        if (tracksList != null && currnetTrackIndex <= (tracksList.size() - 1)) {
            return tracksList.get(currnetTrackIndex);
        }
        return null;
    }

    @Override
    public List<TrackFormat> getAudioTrackList() {
        return tracksContainer.getAudioTrackList();
    }

    @Override
    public List<TrackFormat> getTextTrackList() {
        return tracksContainer.getTextTrackList();
    }

    @Override
    public List<TrackFormat> getVideoTrackList() {
        return tracksContainer.getVideoTrackList();
    }


    private void initTrackManagerLists() {
        tracksContainer = new KTracksContainer(getTracksList(TrackType.AUDIO),getTracksList(TrackType.TEXT),getTracksList(TrackType.VIDEO));

    }

    private void clearTrackManagerLists() {
        tracksContainer.getAudioTrackList().clear();
        tracksContainer.setAudioTrackList(null);
        tracksContainer.getTextTrackList().clear();
        tracksContainer.setTextTrackList(null);
        tracksContainer.getVideoTrackList().clear();
        tracksContainer.setVideoTrackList(null);
    }


    public int getTracksCount(TrackType trackType) {
        return getTracksList(trackType).size();
    }


    public JSONObject getTrackListAsJson(TrackType trackType) {
        JSONObject resultJsonObj = new JSONObject();
        JSONArray tracksJsonArray = new JSONArray();
        int trackCount = getTracksCount(trackType);
        String resultJsonKey = getJsonRootKey(trackType);
        List<TrackFormat> tracksList = getTracksList(trackType);
        for (TrackFormat tf : tracksList) {
            // for webView we filter the -1 bitrate since it is added automatically in the web layer
            if (TrackType.VIDEO.equals(trackType) && tf.bitrate == -1) {
                continue;
            }
            tracksJsonArray.put(tf.toJSONObject());
        }

        try {
            resultJsonObj.put(resultJsonKey, tracksJsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        LOGD(TAG, "Track/Lang JSON Result  " + resultJsonObj.toString());
        return resultJsonObj;
    }

    public String getTrackName(TrackFormat trackFormat) {
        if (trackFormat != null) {
            if (trackFormat.adaptive) {
                return "auto";
            }
            return trackFormat.trackLabel;
        }
        return "";
    }

    private List<TrackFormat> getTrackFormatsByType(TrackType trackType) {
        List<TrackFormat> trackList = new ArrayList<>();
        int trackCount = player.getTrackCount(trackType);
        for (int i = 0; i < trackCount; i++) {
            trackList.add(player.getTrackFormat(trackType, i));
        }
        return trackList;
    }


    private boolean isAvailableTracksRelevant(TrackType trackType) {
        int tracksCount = getTracksCount(trackType);
        if (tracksCount > 1) {
            return true;
        } else if (tracksCount == 1) {
            if (!getTracksList(trackType).get(0).trackLabel.contains("auto") && !getTracksList(trackType).get(0).trackLabel.contains("Auto")) {
                return true;
            }
        }
        return false;
    }

    private String getJsonRootKey(TrackType trackType) {
        if (TrackType.VIDEO.equals(trackType)) {
            return "tracks";
        } else if (TrackType.AUDIO.equals(trackType) || TrackType.TEXT.equals(trackType)) {
            return "languages";
        }
        return "";
    }

    private List<TrackFormat> getTracksList(TrackType trackType) {

        List<TrackFormat> tracksList = null;
        switch (trackType) {
            case AUDIO:
                tracksList = getTrackFormatsByType(TrackType.AUDIO);
                break;
            case TEXT:
                tracksList = getTrackFormatsByType(TrackType.TEXT);
                break;
            case VIDEO:
                tracksList = getTrackFormatsByType(TrackType.VIDEO);
                break;
            default:
                break;
        }
        return tracksList;
    }
}
