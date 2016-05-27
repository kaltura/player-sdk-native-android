package com.kaltura.playersdk.tracks;

import android.util.Log;

import com.kaltura.playersdk.players.KPlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gilad.nadav on 25/05/2016.
 */
public class KTracksManager implements  KTrackActions {

    public static String TAG = "TracksManager";

    private KPlayer player;
    private KTracksContainer tracksContainer;


    public KTracksManager(KPlayer player) {
        this.player = player;
        initTrackManagerLists();
    }



    @Override
    public void switchTrack(TrackType trackType, int newIndex) {
        if (isAvailableTracksRelevant(trackType)) {
            Log.d(TAG, "switchTrack for " + trackType.name() + " newIndex = " + newIndex);
            player.switchTrack(trackType, newIndex);
        } else {
            Log.d(TAG, "switchTrack " + trackType.name() + "skipped Reason: track count  < 2");
        }
    }

    @Override
    public TrackFormat getCurrentTrack(TrackType trackType) {
        int currnetTrackIndex = player.getCurrentTrackIndex(trackType);
        if (currnetTrackIndex == -1){
            return TrackFormat.getDefaultTrackFormat(trackType);
        }
        List<TrackFormat> tracksList = getTracksList(trackType);
        Log.d(TAG, "getCurrentTrack " + trackType.name() + " tracksList size = " + tracksList.size() + " currentIndex = " + currnetTrackIndex);
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


    public JSONObject getTrackListAsJson(TrackType trackType, boolean isTracksEventListenerEnabled ) {
        JSONObject resultJsonObj = new JSONObject();
        JSONArray tracksJsonArray = new JSONArray();
        int trackCount = getTracksCount(trackType);
        String resultJsonKey = getJsonRootKey(trackType);
        List<TrackFormat> tracksList = getTracksList(trackType);
        for (TrackFormat tf : tracksList) {
            // for webView we filter the -1 bitrate since it is added automatically in the web layer
            if (TrackType.VIDEO.equals(trackType) &&
                tf.bitrate == -1 &&
                !isTracksEventListenerEnabled) {
                continue;
            }
            tracksJsonArray.put(tf.toJSONObject());
        }

//        //SORT THE LIST
//        if (TrackType.VIDEO.equals(trackType)) {
//            Collections.sort(videoTrackList, new Comparator<TrackFormat>() {
//                        @Override
//                        public int compare(TrackFormat lhs, TrackFormat rhs) {
//                            return rhs.bitrate - lhs.bitrate;
//                        }
//                    }
//            );
//        } else if (TrackType.TEXT.equals(trackType)) {
//            Collections.sort(textTrackList, new Comparator<TrackFormat>() {
//                        @Override
//                        public int compare(TrackFormat lhs, TrackFormat rhs) {
//                            return lhs.trackLabel.compareTo(rhs.trackLabel);
//                        }
//                    }
//            );
//        }
//        else if (TrackType.AUDIO.equals(trackType)) {
//            Collections.sort(audioTrackList, new Comparator<TrackFormat>() {
//                        @Override
//                        public int compare(TrackFormat lhs, TrackFormat rhs) {
//                            return lhs.trackLabel.compareTo(rhs.trackLabel);
//                        }
//                    }
//            );
//        }

        try {
            resultJsonObj.put(resultJsonKey, tracksJsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Track/Lang JSON Result  " + resultJsonObj.toString());
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
        Log.d(TAG, "isAvailableTracksRelevant trackType = " + trackType + ", tracksCount = " + tracksCount);
        if (tracksCount > 1) {
            return true;
        } else if (tracksCount == 1) {
            Log.d(TAG, "Single Track is " + getTracksList(trackType).get(0).trackLabel);
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
