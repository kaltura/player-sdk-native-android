package com.kaltura.playersdk.players;

import com.kaltura.playersdk.PlayerViewController;

/**
 * Created by nissopa on 6/14/15.
 */
public interface KPlayerListener {
    String PlayKey = "play";
    String PauseKey = "pause";
    String DurationChangedKey = "durationChange";
    String LoadedMetaDataKey = "loadedmetadata";
    String TimeUpdateKey = "timeupdate";
    String ProgressKey = "progress";
    String EndedKey = "ended";
    String SeekedKey = "seeked";
    String CanPlayKey = "canplay";
    String BufferingChangeKey = "bufferchange";
    String ErrorKey = "error";
    String TextTracksReceivedKey     = "textTracksReceived";
    String AudioTracksReceivedKey    = "audioTracksReceived";
    String FlavorsListChangedKey     = "flavorsListChanged";
    String SourceSwitchingStartedKey = "sourceSwitchingStarted";
    String SourceSwitchingEndKey     = "sourceSwitchingEnd";
    String AdRemainingTimeChangeKey  = "adRemainingTimeChange";
    String AdsLoadErrorKey           = "adsLoadError";
    String AdDurationChangeKey = "AdSupport_AdUpdateDuration";

    void eventWithValue(KPlayer player, String eventName, String eventValue);
    void eventWithJSON(KPlayer player, String eventName, String jsonValue);
    void asyncEvaluate(String expression, String expressionID, PlayerViewController.EvaluateListener evaluateListener);
}


