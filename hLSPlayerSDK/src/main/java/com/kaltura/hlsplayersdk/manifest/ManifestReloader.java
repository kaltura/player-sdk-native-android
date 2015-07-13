package com.kaltura.hlsplayersdk.manifest;

import java.util.Timer;
import java.util.TimerTask;

import android.util.Log;

import com.kaltura.hlsplayersdk.manifest.ManifestParser.ReloadEventListener;

public class ManifestReloader
{
	private ReloadEventListener videoListener = null;
	private ReloadEventListener altAudioListener = null;
	private ReloadEventListener subtitleListener = null;

	private ManifestGetHandler videoGetHandler = null;
	private ManifestGetHandler altAudioGetHandler = null;
	private ManifestGetHandler subtitleGetHandler = null;
	
	private Timer reloadTimer = null;
	private long timerDelay = 10000;
	private long lastTimerStart = 0;
	
	public interface ManifestGetHandler
	{
		ManifestParser getVideoManifestToReload();
		ManifestParser getAltAudioManifestToReload();
		ManifestParser getSubtitleManifestToReload();
	}
	
	public void setVideoSource(ReloadEventListener listener, ManifestGetHandler getHandler )
	{
		videoListener = listener;
		videoGetHandler = getHandler;
	}
	
	public void setAltAudioSource(ReloadEventListener listener, ManifestGetHandler getHandler )
	{
		altAudioListener = listener;
		altAudioGetHandler = getHandler;
	}
	
	public void setSubtitleSource(ReloadEventListener listener, ManifestGetHandler getHandler )
	{
		subtitleListener = listener;
		subtitleGetHandler = getHandler;
	}
	
	public void setDelay(long delay)
	{
		timerDelay = delay;
	}
	
	/*
	 *  start(long delay)
	 *  
	 *  Starts a reload with a delay in ms. Only schedules a new reload if the new reload would complete
	 *  before any existing reload.
	 *  
	 */
	public void start(long delay)
	{
		// If our next expected timer completion is shorter than the delay we would set, don't extend it.
		long curTime = System.currentTimeMillis();
		if (reloadTimer != null)
		{
			long expectedCompletion = lastTimerStart + timerDelay;
			if (curTime + delay > expectedCompletion)
			{
				return;
			}
		}

		setDelay(delay);
		start();

	}
	
	/*
	 *  start()
	 *  
	 *  Starts the reload timer. Will schedule a reload, regardless of whether a reload is already in progress
	 *  
	 */
	public void start()
	{
		killTimer();
		
		reloadTimer = new Timer();

		lastTimerStart = System.currentTimeMillis();
		
		reloadTimer.schedule(new TimerTask()
		{
			public void run()
			{
				Log.i("StreamHandler.reloadTimerComplete.run", "Reload Timer Complete!");
				reload();
			}
			
		}, timerDelay);
	}
	
	public void stop()
	{
		killTimer();
	}
	
	private void killTimer()
	{
		if (reloadTimer != null)
		{
			reloadTimer.cancel();
			reloadTimer = null;
		}
	}
	
	public void reloadAltAudio()
	{
		ManifestParser altAudioManifest = altAudioGetHandler != null ? altAudioGetHandler.getAltAudioManifestToReload() : null;
		if (altAudioManifest != null)
		{
			if (altAudioListener == null) Log.w("ManifestReloader.reloadAltAudio", "altAudioListener = null");
			altAudioManifest.reload(altAudioListener);
		}
	}
	
	public void reload()
	{
		killTimer(); // In case the timer is active - don't wnat to do another reload in the middle of it
		
		ManifestParser videoManifest = videoGetHandler != null ? videoGetHandler.getVideoManifestToReload() : null;
		if (videoManifest != null)
		{
			videoManifest.reload(videoListener);
		}

		reloadAltAudio();

		ManifestParser subtitleManifest = subtitleGetHandler != null ? subtitleGetHandler.getSubtitleManifestToReload() : null;
		if (subtitleManifest != null)
		{
			subtitleManifest.reload(subtitleListener);
		}

	}
	
	
	
	
}
