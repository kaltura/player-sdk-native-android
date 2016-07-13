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
    public void switchTrackByBitrate(TrackType trackType, int prefaredBitrateKBit) {
        Log.d(TAG, "switchTrackByBitrate : " + trackType.name() + " prefaredBitrateKBit : " + prefaredBitrateKBit);
        if (TrackType.TEXT.equals(trackType)){
            return;
        }

        int prevBitrate = 0;

        List<TrackFormat> tracksList = null;
        if (TrackType.AUDIO.equals(trackType)){
            tracksList = getAudioTrackList();

        } else if (TrackType.VIDEO.equals(trackType)){
           tracksList =  getVideoTrackList();
        } else {
            //unsupported track type
            return;
        }

        if (tracksList.size() == 1) {
            Log.d(TAG, "Skip switchTrackByBitrate, tracksList.size() == 1");
            return;
        }
        for (int i = 0 ; i <  tracksList.size() ; i++) {
            Log.d(TAG, "i : " + i + " (size - 1) = " + (tracksList.size() - 1));

            if (tracksList.get(i).bitrate == -1) {
                Log.d(TAG, "prefaredBitrateKBit : " + prefaredBitrateKBit + " bitrate : + Auto");
                if (prefaredBitrateKBit == -1) {
                    switchTrack(trackType, i);
                    return;
                }
                continue;
            }
            int bitrate = tracksList.get(i).bitrate / 1000;
            Log.d(TAG, i + "-" + bitrate + "/" + prefaredBitrateKBit  + "----" + (tracksList.size() - 1));
            if (bitrate >= prefaredBitrateKBit && (i-1) > 0){
                if (Math.abs(bitrate - prefaredBitrateKBit) <= Math.abs(prevBitrate - prefaredBitrateKBit)) {
                    Log.d(TAG, "switchTrack0 index = " + (i) + " " + tracksList.get(i).bitrate / 1000);
                    switchTrack(trackType, i);
                }
                else {
                    Log.d(TAG, "switchTrack1 index = " + (i-1)  + " " + tracksList.get(i-1).bitrate / 1000);
                    switchTrack(trackType, i-1);
                }
                return;
            }
            else if (bitrate >= prefaredBitrateKBit && (i-1) == 0){
                Log.d(TAG, "switchTrack2 index = " + (i) + " " + tracksList.get(i).bitrate / 1000);
                switchTrack(trackType, i);
                return;
            }
            else if (prefaredBitrateKBit >= bitrate && i != (tracksList.size() - 1)) {
                prevBitrate = bitrate;
                continue;
            }
            else if (prefaredBitrateKBit >= bitrate && i == (tracksList.size() - 1)) {
                Log.d(TAG, "switchTrack3 : index = " + (i) + " " + tracksList.get(i).bitrate / 1000);
                switchTrack(trackType, i);
            }
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
