package com.kaltura.playersdk;

import android.content.Context;
import android.widget.RelativeLayout;

import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;

public class HLSPlayer extends RelativeLayout implements VideoPlayerInterface {

	public HLSPlayer(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
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
	public int getDuration() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void play() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() {
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
	public boolean isPlaying() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canPause() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void registerPlayerStateChange(OnPlayerStateChangeListener listener) {
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

	@Override
	public void release() {
		// TODO Auto-generated method stub

	}

	@Override
	public void recoverRelease() {
		// TODO Auto-generated method stub

	}

}
