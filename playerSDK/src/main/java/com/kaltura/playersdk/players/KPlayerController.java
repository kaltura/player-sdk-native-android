package com.kaltura.playersdk.players;

import android.content.Context;
import android.text.StaticLayout;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.kaltura.playersdk.Helpers.KStringUtilities;

import java.util.Objects;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerListener{
    private KPlayer player;
    private KPlayerControllerListener listener;
    private String playerClassName;
    private String src;
    private String adTagURL;
    private float currentPlaybackTime;
    private int adPlayerHeight;
    private String locale;
    private RelativeLayout parentViewController;
    private String key;
    private float currentTime;
    private boolean isSeeked;
    private boolean contentEnded;
    private Context context;

    public static String KPlayerClassName = "com.kaltura.playersdk.players.KPlayer";
    public static String KWVPlayerClassName = "KWVPlayer";
    public static String KCCPlayerClassName = "KCCPlayer";

    public interface KPlayer {
        public void setPlayerListener(KPlayerListener listener);
        public KPlayerListener getPlayerListener();
        public void setPlayerSource(String playerSource);
        public String getPlayerSource();
        public void setCurrentPlaybackTime(float currentPlaybackTime);
        public float getCurrentPlaybackTime();
        public float getDuration();
        public void initWithParentView(RelativeLayout parentView);
        public void play();
        public void pause();
        public void changeSubtitleLanguage(String languageCode);
        public void removePlayer();
        public void setDRMKey(String drmKey);
        public boolean isKPlayer();
    }


    public interface KPlayerControllerListener extends KPlayerListener {
        public void allAdsCompleted();
    }

    public KPlayerController(String className, Context context) {
        this.playerClassName = className;
        this.context = context;
    }

    public void addPlayerToController(RelativeLayout playerViewController) {
        this.parentViewController = playerViewController;
        if (this.getPlayer() == null) {
            Log.d("ERROR", "NO PLAYER CREATED");
        } else {
            this.player.setDRMKey(this.key);
        }
    }

    public void switchPlayer(String playerClassName, String key) {
        this.playerClassName = playerClassName;
        this.key = key;
    }

    public void changeSubtitleLanguage(String isoCode) {

    }

    public void removePlayer() {

    }

    public void setPlayer(KPlayer player) {
        this.player = player;
    }

    public KPlayer getPlayer() {
        if (this.player == null && this.playerClassName != null && this.playerClassName.length() > 0) {
            try {
                Class _class = Class.forName(this.playerClassName);
                if (_class != null) {
                    Object player = _class.getDeclaredConstructor(Context.class).newInstance(this.context);
                    if (player instanceof KPlayer) {
                        this.player = (KPlayer)player;
                        this.player.initWithParentView(this.parentViewController);
                        this.player.setPlayerListener(this);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this.player;
    }

    public void setListener(KPlayerControllerListener listener) {
        this.listener = listener;
    }

    public KPlayerControllerListener getListener() {
        return this.listener;
    }

    public void setPlayerClassName(String playerClassName) {
        this.playerClassName = playerClassName;
    }

    public String getPlayerClassName() {
        return this.playerClassName;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
        this.player.setPlayerSource(src);
    }

    public String getAdTagURL() {
        return adTagURL;
    }

    public void setAdTagURL(String adTagURL) {
        this.adTagURL = adTagURL;
    }

    public float getCurrentPlaybackTime() {
        return this.player.getCurrentPlaybackTime();
    }

    public void setCurrentPlaybackTime(int currentPlaybackTime) {
        this.player.setCurrentPlaybackTime(currentPlaybackTime);
    }

    public int getAdPlayerHeight() {
        return adPlayerHeight;
    }

    public void setAdPlayerHeight(int adPlayerHeight) {
        this.adPlayerHeight = adPlayerHeight;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    @Override
    public void eventWithValue(KPlayer currentPlayer, String eventName, String eventValue) {
        KStringUtilities event = new KStringUtilities(eventName);
        if (this.key != null && currentPlayer.isKPlayer() && (event.isPlay() || event.isSeeked())) {
            this.currentTime = this.player.getCurrentPlaybackTime();
            this.player.removePlayer();
            this.player = null;
            this.addPlayerToController(this.parentViewController);
            this.setSrc(this.src);
            this.isSeeked = event.isSeeked();
        } else if (!currentPlayer.isKPlayer() && event.canPlay()) {
            if (this.currentTime > 0) {
                this.player.setCurrentPlaybackTime(this.currentTime);
            }
            if (!this.isSeeked) {
                this.player.play();
            }
        } else {
            this.listener.eventWithValue(currentPlayer, eventName, eventValue);
        }
    }

    @Override
    public void eventWithJSON(KPlayer player, String eventName, String jsonValue) {
        this.listener.eventWithJSON(player, eventName, jsonValue);
    }

    @Override
    public void contentCompleted(KPlayer currentPlayer) {
        this.contentEnded = true;
    }

}
