package com.kaltura.playersdk;

import android.app.Activity;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.kaltura.hlsplayersdk.HLSPlayerViewController;
import com.kaltura.hlsplayersdk.types.PlayerStates;
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

public class HLSPlayer extends FrameLayout implements VideoPlayerInterface, TextTracksInterface, AlternateAudioTracksInterface, QualityTracksInterface, com.kaltura.hlsplayersdk.events.OnPlayheadUpdateListener, com.kaltura.hlsplayersdk.events.OnErrorListener, com.kaltura.hlsplayersdk.events.OnPlayerStateChangeListener, com.kaltura.hlsplayersdk.events.OnProgressListener, com.kaltura.hlsplayersdk.events.OnQualityTracksListListener{

    private HLSPlayerViewController mPlayer;
    private OnPlayerStateChangeListener mPlayerStateChangeListener;
    private OnPlayheadUpdateListener mPlayheadUpdateListener;
    private OnQualityTracksListListener mQualityTracksListener;
    private OnProgressListener mProgressListener;
    private OnErrorListener mErrorListener;

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
    public void switchTextTrack(int newIndex) {
        mPlayer.switchTextTrack(newIndex);

    }

    /////////////////////////////////////////////////////////
    //
    //      HlsPlayerSDK Listeners
    //
    ////////////////////////////////////////////////////////


    @Override
    public void registerQualityTracksList(final OnQualityTracksListListener listener) {
        mQualityTracksListener = listener;
        mPlayer.registerQualityTracksList(listener != null ? this : null);
    }

    @Override
    public void registerQualitySwitchingChange(OnQualitySwitchingListener listener) {
        //mPlayer.registerQualitySwitchingChange(listener);

    }

    @Override
    public void registerAudioTracksList(OnAudioTracksListListener listener) {
        //mPlayer.registerAudioTracksList(listener);
    }

    @Override
    public void registerAudioSwitchingChange(OnAudioTrackSwitchingListener listener) {
        //mPlayer.registerAudioSwitchingChange(listener);

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

    @Override
    public void registerError(final OnErrorListener listener) {
        mErrorListener = listener;
        mPlayer.registerError(listener != null ? this : null);
    }

    @Override
    public void registerPlayheadUpdate(final OnPlayheadUpdateListener listener) {
        mPlayheadUpdateListener = listener;
        mPlayer.registerPlayheadUpdate(listener != null ? this : null);

    }

    @Override
    public void removePlayheadUpdateListener() {
        mPlayheadUpdateListener = null;
        mPlayer.removePlayheadUpdateListener();

    }

    @Override
    public void registerProgressUpdate(final OnProgressListener listener) {
        mProgressListener = listener;
        mPlayer.registerProgressUpdate(listener != null ? this : null);

    }

    @Override
    public void registerPlayerStateChange(final OnPlayerStateChangeListener listener) {
        mPlayerStateChangeListener = listener;
        mPlayer.registerPlayerStateChange(listener != null ? this : null);
    }

    @Override
    public boolean onStateChanged(PlayerStates state) {
        if (mPlayerStateChangeListener != null) {
            com.kaltura.playersdk.types.PlayerStates kState;
            switch (state) {
                case START:
                    kState = com.kaltura.playersdk.types.PlayerStates.START;
                    break;
                case LOAD:
                    kState = com.kaltura.playersdk.types.PlayerStates.LOAD;
                    break;
                case PLAY:
                    kState = com.kaltura.playersdk.types.PlayerStates.PLAY;
                    break;
                case PAUSE:
                    kState = com.kaltura.playersdk.types.PlayerStates.PAUSE;
                    break;
                case END:
                    kState = com.kaltura.playersdk.types.PlayerStates.END;
                    break;
                case SEEKING:
                    kState = com.kaltura.playersdk.types.PlayerStates.SEEKING;
                    break;
                case SEEKED:
                    kState = com.kaltura.playersdk.types.PlayerStates.SEEKED;
                    break;
                default:
                    kState = com.kaltura.playersdk.types.PlayerStates.START;
            }
            return mPlayerStateChangeListener.onStateChanged(kState);
        }

        return false;

    }


    @Override
    public void onPlayheadUpdated(int msec) {
        if (mPlayheadUpdateListener != null){
            mPlayheadUpdateListener.onPlayheadUpdated(msec);
        }
    }

    @Override
    public void onProgressUpdate(int progress) {
        if ( mProgressListener != null) {
            mProgressListener.onProgressUpdate(progress);
        }
    }

    @Override
    public void OnQualityTracksList(List<com.kaltura.hlsplayersdk.QualityTrack> list, int defaultTrackIndex) {
        if (mQualityTracksListener != null){
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
            mQualityTracksListener.OnQualityTracksList(newList, defaultTrackIndex);
        }
    }

    @Override
    public void onError(int errorCode, String errorMessage) {
        if (mErrorListener != null) {
            mErrorListener.onError(errorCode, errorMessage);
        }
    }
}
