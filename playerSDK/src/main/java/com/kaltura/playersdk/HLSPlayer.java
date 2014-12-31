package com.kaltura.playersdk;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.playersdk.events.OnAudioTrackSwitchingListener;
import com.kaltura.playersdk.events.OnAudioTracksListListener;
import com.kaltura.playersdk.events.OnErrorListener;
import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.events.OnQualitySwitchingListener;
import com.kaltura.playersdk.events.OnQualityTracksListListener;
import com.kaltura.playersdk.events.OnTextTrackChangeListener;
import com.kaltura.playersdk.events.OnTextTrackTextListener;
import com.kaltura.playersdk.events.OnTextTracksListListener;
import com.kaltura.playersdk.types.TrackType;

import java.util.ArrayList;
import java.util.List;

public class HLSPlayer extends FrameLayout implements VideoPlayerInterface, TextTracksInterface, AlternateAudioTracksInterface, QualityTracksInterface {

    private HLSPlayerViewController mPlayer;
    public HLSPlayer(Activity activity) {
        super(activity);
        mPlayer = new HLSPlayerViewController(activity);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER);
        this.addView(mPlayer, lp);
        mPlayer.addComponents("", activity);
    }

    public void close(){
        mPlayer.close();
        mPlayer = null;
    }

    @Override
    public String getVideoUrl() {
        return mPlayer.getVideoUrl();
    }

    @Override
    public void setVideoUrl(String url) {
        if ( mPlayer.getVideoUrl() == null || !url.equals(mPlayer.getVideoUrl())) {
            mPlayer.setVideoUrl(url);
        }

    }

    @Override
    public int getDuration() {
        return mPlayer.getDuration();
    }

    @Override
    public void play() {
        mPlayer.play();
    }

    @Override
    public void pause() {
        mPlayer.pause();

    }

    @Override
    public void stop() {
        mPlayer.stop();

    }

    @Override
    public void seek(int msec) {
        mPlayer.seek(msec);

    }

    @Override
    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    @Override
    public boolean canPause() {
        //TODO add to HLSPlayerViewController

        return mPlayer != null;
    }

    @Override
    public void registerPlayerStateChange(final OnPlayerStateChangeListener listener) {
        if (listener == null){
            mPlayer.registerPlayerStateChange(null);
            return;
        }

        mPlayer.registerPlayerStateChange(new com.kaltura.hlsplayersdk.events.OnPlayerStateChangeListener() {

            @Override
            public boolean onStateChanged(com.kaltura.hlsplayersdk.types.PlayerStates hlsPlayerState) {
                com.kaltura.playersdk.types.PlayerStates state = com.kaltura.playersdk.types.PlayerStates.START;
                switch ( hlsPlayerState ) {
                    case START:
                        state = com.kaltura.playersdk.types.PlayerStates.START;
                        break;
                    case LOAD:
                        state = com.kaltura.playersdk.types.PlayerStates.LOAD;
                        break;
                    case PLAY:
                        state = com.kaltura.playersdk.types.PlayerStates.PLAY;
                        break;
                    case PAUSE:
                        state = com.kaltura.playersdk.types.PlayerStates.PAUSE;
                        break;
                    case END:
                        state = com.kaltura.playersdk.types.PlayerStates.END;
                        break;
                    case SEEKING:
                        state = com.kaltura.playersdk.types.PlayerStates.SEEKING;
                        break;
                    case SEEKED:
                        state = com.kaltura.playersdk.types.PlayerStates.SEEKED;
                        break;
                }
                return listener.onStateChanged(state);
            }

        });

    }

    @Override
    public void registerError(final OnErrorListener listener) {
        if (listener == null){
            mPlayer.registerError(null);
            return;
        }
        mPlayer.registerError(new com.kaltura.hlsplayersdk.events.OnErrorListener() {

            @Override
            public void onError(int errorCode, String errorMessage) {
                listener.onError(errorCode, errorMessage);

            }

        });

    }

    @Override
    public void registerPlayheadUpdate(final OnPlayheadUpdateListener listener) {
        if (listener == null){
            mPlayer.registerPlayheadUpdate(null);
            return;
        }
        mPlayer.registerPlayheadUpdate(new com.kaltura.hlsplayersdk.events.OnPlayheadUpdateListener() {

            @Override
            public void onPlayheadUpdated(int msec) {
                Log.w("HLSPlayed", "----------update playhead" + msec);
                listener.onPlayheadUpdated(msec);

            }

        });

    }

    @Override
    public void removePlayheadUpdateListener() {
        mPlayer.removePlayheadUpdateListener();

    }

    @Override
    public void registerProgressUpdate(final OnProgressListener listener) {
        mPlayer.registerProgressUpdate(new com.kaltura.hlsplayersdk.events.OnProgressListener() {

            @Override
            public void onProgressUpdate(int progress) {
                listener.onProgressUpdate(progress);
            }

        });
    }

    @Override
    public void setStartingPoint(int point) {
        mPlayer.setStartingPoint(point);

    }

    @Override
    public void release() {
        mPlayer.release();
        //TODO add to HLSPlayerViewController

    }

    @Override
    public void recoverRelease() {
        mPlayer.recoverRelease();
        //TODO add to HLSPlayerViewController

    }

    @Override
    public void setBufferTime(int newTime) {
        mPlayer.setBufferTime(newTime);

    }

    @Override
    public void switchQualityTrack(int newIndex) {
        mPlayer.switchQualityTrack(newIndex);

    }

    @Override
    public void setAutoSwitch(boolean autoSwitch) {
        mPlayer.setAutoSwitch(autoSwitch);

    }

    @Override
    public void registerQualityTracksList(final OnQualityTracksListListener listener) {
        mPlayer.registerQualityTracksList( new com.kaltura.hlsplayersdk.events.OnQualityTracksListListener() {

            @Override
            public void OnQualityTracksList( List<com.kaltura.hlsplayersdk.QualityTrack> list, int defaultTrackIndex) {
                List<QualityTrack> newList = new ArrayList<QualityTrack>();
                for ( int i=0; i < list.size(); i++ ) {
                    com.kaltura.hlsplayersdk.QualityTrack currentTrack = list.get(i);
                    QualityTrack newTrack = new QualityTrack();
                    newTrack.bitrate = currentTrack.bitrate;
                    newTrack.height = currentTrack.height;
                    newTrack.width = currentTrack.width;
                    newTrack.trackId = currentTrack.trackId;
                    newTrack.type = currentTrack.type == com.kaltura.hlsplayersdk.types.TrackType.VIDEO ? TrackType.VIDEO: TrackType.AUDIO;
                    newList.add(newTrack);
                }
                listener.OnQualityTracksList(newList, defaultTrackIndex);
            }
        });
    }

    @Override
    public void registerQualitySwitchingChange(
            OnQualitySwitchingListener listener) {
        //mPlayer.registerQualitySwitchingChange(listener);

    }

    @Override
    public float getLastDownloadTransferRate() {
        return mPlayer.getLastDownloadTransferRate();
    }

    @Override
    public float getDroppedFramesPerSecond() {
        return mPlayer.getDroppedFramesPerSecond();
    }

    @Override
    public float getBufferPercentage() {
        return mPlayer.getBufferPercentage();
    }

    @Override
    public int getCurrentQualityIndex() {
        return mPlayer.getCurrentQualityIndex();
    }

    @Override
    public void hardSwitchAudioTrack(int newAudioIndex) {
        mPlayer.hardSwitchAudioTrack(newAudioIndex);

    }

    @Override
    public void softSwitchAudioTrack(int newAudioIndex) {
        mPlayer.softSwitchAudioTrack(newAudioIndex);
    }

    @Override
    public void registerAudioTracksList(OnAudioTracksListListener listener) {
        //mPlayer.registerAudioTracksList(listener);
    }

    @Override
    public void registerAudioSwitchingChange(
            OnAudioTrackSwitchingListener listener) {
        //mPlayer.registerAudioSwitchingChange(listener);

    }

    @Override
    public void switchTextTrack(int newIndex) {
        mPlayer.switchTextTrack(newIndex);

    }

    @Override
    public void registerTextTracksList(OnTextTracksListListener listener) {
        //mPlayer.registerTextTracksList(listener);

    }

    @Override
    public void registerTextTrackChanged(OnTextTrackChangeListener listener) {
        //mPlayer.registerTextTrackChanged(listener);

    }

    @Override
    public void registerTextTrackText(OnTextTrackTextListener listener) {
        //mPlayer.registerTextTrackText(listener);

    }

}
