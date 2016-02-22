package com.kaltura.playersdk.players;

/**
 * Created by nissopa on 6/14/15.
 */
public interface KPlayerListener {
    String PlayKey = "play";
    String PauseKey = "pause";
    String DurationChangedKey = "durationchange";
    String LoadedMetaDataKey = "loadedmetadata";
    String TimeUpdateKey = "timeupdate";
    String ProgressKey = "progress";
    String EndedKey = "ended";
    String SeekedKey = "seeked";
    String CanPlayKey = "canplay";
    String FlavorsListChangedKey = "flavorsListChanged";
    String SourceSwitchingStartedKey = "sourceSwitchingStarted";
    String SourceSwitchingEndKey = "sourceSwitchingEnd";
    String BufferingChangeKey = "bufferchange";
    String TextTracksReceived = "textTracksReceived";

    void eventWithValue(KPlayer player, String eventName, String eventValue);
    void eventWithJSON(KPlayer player, String eventName, String jsonValue);
    void contentCompleted(KPlayer currentPlayer);
}


