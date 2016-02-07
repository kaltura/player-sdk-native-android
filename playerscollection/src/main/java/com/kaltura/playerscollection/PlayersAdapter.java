package com.kaltura.playerscollection;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;

import java.util.ArrayList;

/**
 * Created by nissimpardo on 31/01/16.
 */
public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder> implements KPEventListener {
    private Context mContext;
    private ArrayList<String> mEntryIds;
    private PlayerViewController mPlayingPlayer;

    public class PlayerViewHolder extends RecyclerView.ViewHolder {

        public PlayerViewHolder(View itemView) {
            super(itemView);
        }
    }

    public PlayersAdapter(Context context, ArrayList<String> entryIds) {
        mContext = context;
        mEntryIds = entryIds;
    }

//    public PlayersAdapter() {
//        super();
//    }


    @Override
    public void onViewRecycled(PlayerViewHolder holder) {
        Log.d("check", "onViewRecycled");
    }

    @Override
    public PlayerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d("Called", "onCreateViewHolder");
        KPPlayerConfig config = new KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271");
        config.setEntryId(mEntryIds.get(viewType));
        PlayerViewController view = (PlayerViewController)LayoutInflater.from(parent.getContext()).inflate(R.layout.player, parent, false);
        // If you want to play only one player at a time just comment out the next line
//        view.addEventListener(this);
        view.initWithConfiguration(config);
        view.loadPlayerIntoActivity((Activity)mContext);
        PlayerViewHolder viewHolder = new PlayerViewHolder(view);
        return viewHolder;
    }



    @Override
    public void onBindViewHolder(PlayerViewHolder holder, int position) {
        Log.d("Called", "onBindViewHolder");
//        Log.d("position :", Integer.toString(position));
//        ((PlayerViewController)holder.itemView).changeMedia(mEntryIds.get(position));
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mEntryIds.size();
    }


    @Override
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        if (state == KPlayerState.PAUSED && playerViewController == mPlayingPlayer) {
            mPlayingPlayer = null;
        }
        if (state == KPlayerState.PLAYING) {
            if (mPlayingPlayer != null) {
                mPlayingPlayer.sendNotification("doPause", null);
            }
            mPlayingPlayer = playerViewController;
        }
    }

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {

    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {

    }
}
