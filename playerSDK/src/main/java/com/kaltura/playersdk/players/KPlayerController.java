package com.kaltura.playersdk.players;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.google.ads.interactivemedia.v3.api.AdEvent;
import com.google.ads.interactivemedia.v3.api.player.ContentProgressProvider;
import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.kaltura.playersdk.Helpers.KStringUtilities;
import com.kaltura.playersdk.PlayerUtilities.KIMAHandler;
import com.kaltura.playersdk.PlayerUtilities.KPlayerIMAManager;
import com.kaltura.playersdk.PlayerUtilities.KPlayerParams;

import java.util.HashMap;


/**
 * Created by nissopa on 6/14/15.
 */
public class KPlayerController implements KPlayerListener {
    private KPlayer mPlayer;
    private KPlayerListener listener;
    private float currentPlaybackTime;
    private int adPlayerHeight;
    private String locale;
    private Context context;
    private RelativeLayout mParentView;
    private KPlayerParams mPlayerParams;





    public interface KPlayer {
        public void setPlayerListener(KPlayerListener listener);
        public void setCurrentPlaybackTime(float currentPlaybackTime);
        public float getCurrentPlaybackTime();
        public float getDuration();
        public void play();
        public void pause();
        public void removePlayer();
        public void setPlayerSource(String source);
    }

    public KPlayerController(KPlayer player, Context context, RelativeLayout parentView) {
        mPlayer = player;
        mPlayer.setPlayerListener(this);
        this.context = context;
        addPlayerToController(parentView);
    }

    public void addPlayerToController(RelativeLayout playerViewController) {
        mParentView = playerViewController;
        ViewGroup.LayoutParams currLP = playerViewController.getLayoutParams();

        // Add background view
        RelativeLayout mBackgroundRL = new RelativeLayout(playerViewController.getContext());
        mBackgroundRL.setBackgroundColor(Color.BLACK);
        playerViewController.addView(mBackgroundRL, currLP);

        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(currLP.width, currLP.height);
        playerViewController.addView((View) mPlayer, lp);
    }

    public void switchPlayer(KPlayer newPlayer) {
        if (((View)mPlayer).getParent().equals(mParentView)) {
            mParentView.removeView((View)mPlayer);
        }
        if (mParentView.getChildCount() > 1) {
            mParentView.addView((View)newPlayer, mParentView.getChildCount() - 1, ((View)mPlayer).getLayoutParams());
        }
        mPlayer.setPlayerListener(null);
        mPlayer = newPlayer;
        mPlayer.setPlayerListener(this);
    }

    public void setSource(String source) {
        playerParams().setSourceURL(source);
        mPlayer.setPlayerSource(source);
    }

    public KPlayerParams playerParams() {
        if (mPlayerParams == null) {
            mPlayerParams = new KPlayerParams();
        }
        return mPlayerParams;
    }

    public KPlayer getPlayer() {
        return mPlayer;
    }



    public void changeSubtitleLanguage(String isoCode) {

    }

    public void removePlayer() {

    }


//    public KPlayer getPlayer() {
//        if (this.player == null && this.playerClassName != null && this.playerClassName.length() > 0) {
//            try {
//                Class _class = Class.forName(this.playerClassName);
//                if (_class != null) {
//                    Object player = _class.getDeclaredConstructor(Context.class).newInstance(this.context);
//                    if (player instanceof KPlayer) {
//                        this.player = (KPlayer)player;
//                        this.player.initWithParentView(this.parentViewController);
//                        this.player.setPlayerListener(this);
//                    }
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        return this.player;
//    }

    public void setListener(KPlayerListener listener) {
        this.listener = listener;
    }

    public KPlayerListener getListener() {
        return this.listener;
    }


    public float getCurrentPlaybackTime() {
        return mPlayer.getCurrentPlaybackTime();
    }

    public void setCurrentPlaybackTime(float currentPlaybackTime) {
        mPlayer.setCurrentPlaybackTime(currentPlaybackTime);
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


    // KPlayerListener Methods
    @Override
    public void eventWithValue(KPlayer currentPlayer, String eventName, String eventValue) {
        this.listener.eventWithValue(currentPlayer, eventName, eventValue);
    }

    @Override
    public void eventWithJSON(KPlayer player, String eventName, String jsonValue) {
        this.listener.eventWithJSON(player, eventName, jsonValue);
    }

    @Override
    public void contentCompleted(KPlayer currentPlayer) {

    }

}
