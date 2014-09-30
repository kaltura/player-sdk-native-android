package com.kaltura.playersdk.ima;

import android.content.Context;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.AttributeSet;
import android.widget.VideoView;

import com.kaltura.playersdk.VideoPlayerInterface;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;

public class AdPlayer extends VideoView implements VideoPlayerInterface {

	public AdPlayer(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}
	
	public AdPlayer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AdPlayer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public String getVideoUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setVideoUrl(String url) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getIsPlaying() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void play() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void seek(int msec) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerPlayerStateChange(OnPlayerStateChangeListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerReadyToPlay(OnPreparedListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerError(OnErrorListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerPlayheadUpdate(OnPlayheadUpdateListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removePlayheadUpdateListener() {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerProgressUpdate(OnProgressListener listener) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setStartingPoint(int point) {
		// TODO Auto-generated method stub

	}

}
