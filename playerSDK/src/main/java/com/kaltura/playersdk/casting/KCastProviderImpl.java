package com.kaltura.playersdk.casting;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.players.KChromeCastPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;


/**
 * Created by nissimpardo on 29/05/16.
 */
public class KCastProviderImpl implements com.kaltura.playersdk.interfaces.KCastProvider {
    private static final String TAG = "KCastProviderImpl";
    public static  String nameSpace = "urn:x-cast:com.kaltura.cast.player";
    private String mCastAppID;
    private KCastProviderListener mProviderListener;
    private Context mContext;

    private KCastKalturaChannel mChannel;

    private boolean mApplicationStarted = false;
    private boolean mCastButtonEnabled = false;
    private boolean mGuestModeEnabled = false; // do not enable paring the cc from guest network
    private KCastMediaRemoteControl mCastMediaRemoteControl;

    private String mSessionId;
    private CastSession mCastSession;

    private InternalListener mInternalListener;
    public CastSession getCastSession() {
        return mCastSession;
    }

    public interface InternalListener extends KCastMediaRemoteControl.KCastMediaRemoteControlListener {
        void onStartCasting(CastSession castSession, KChromeCastPlayer remoteMediaPlayer);
        void onCastStateChanged(String state);
        void onStopCasting();
    }

    public void setInternalListener(InternalListener internalListener) {
        mInternalListener = internalListener;
    }

    public KCastProviderListener getProviderListener() {
        return mProviderListener;
    }



    public KCastKalturaChannel getChannel() {
        return mChannel;
    }

    @Override
    public void startReceiver(Context context, String appID, boolean guestModeEnabled, final CastSession castSession) {

        mCastSession = castSession;
        mSessionId   = castSession.getSessionId();
        mChannel     = new KCastKalturaChannel(nameSpace, new KCastKalturaChannel.KCastKalturaChannelListener() {
            @Override
            public void readyForMedia(final String[] params) {
                sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
                // Receiver send the new content
                if (params != null) {
                    mCastMediaRemoteControl = new KChromeCastPlayer(mCastSession);
                    ((KChromeCastPlayer) mCastMediaRemoteControl).setMediaInfoParams(params);
                    if (mInternalListener != null) {
                        mInternalListener.onStartCasting(mCastSession, (KChromeCastPlayer) mCastMediaRemoteControl);
                    }
                }
            }

            @Override
            public void textTeacksRecived(HashMap<String, Integer> textTrackHash) {
                getCastMediaRemoteControl().setTextTracks(textTrackHash);
            }

            @Override
            public void videoTracksReceived(List<Integer> videoTracksList) {
                getCastMediaRemoteControl().setVideoTracks(videoTracksList);
            }
        });

        try {
            castSession.setMessageReceivedCallbacks(nameSpace, mChannel);
        } catch (IOException e) {
            Log.e(TAG, "Exception while creating channel", e);
        }
        castSession.sendMessage(nameSpace, "{\"type\":\"show\",\"target\":\"logo\"}");
        mApplicationStarted = true;
    }

    @Override
    public void startReceiver(Context context, String appID, CastSession castSession) {
        startReceiver(context, appID, false, castSession);
    }

    @Override
    public void disconnectFromCastDevice() {

        if (mInternalListener != null) {
            //mInternalListener.onCastStateChanged("hideConnectingMessage");
            mInternalListener.onCastStateChanged("chromecastDeviceDisConnected");
            mInternalListener.onStopCasting();
        }
        teardown();
    }

    @Override
    public KCastDevice getSelectedCastDevice() {
        if (mCastSession != null) {
            return new KCastDevice(mCastSession.getCastDevice());
        }
        return null;
    }

    @Override
    public void setKCastProviderListener(KCastProviderListener listener) {
        mProviderListener = listener;
    }

    @Override
    public KCastMediaRemoteControl getCastMediaRemoteControl() {
        return mCastMediaRemoteControl;
    }
    
    public boolean hasMediaSession(boolean validateCastConnectingState) {
       return mCastMediaRemoteControl != null && mCastMediaRemoteControl.hasMediaSession(true);
    }

    private void teardown() {
        if (mCastSession != null) {
            LOGD(TAG, "START TEARDOWN mApiClient.isConnected() = " + mCastSession.isConnected() + " mApiClient.isConnecting() = " + mCastSession.isConnecting()) ;
            if (mCastSession != null) {
                if (mCastSession.getRemoteMediaClient() != null) {
                    mCastSession.getRemoteMediaClient().removeListener(null);
                    try {
                        mCastSession.removeMessageReceivedCallbacks(nameSpace);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (mApplicationStarted) {
                boolean isConnected = mCastSession.isConnected();
                boolean isConnecting = mCastSession.isConnecting();
                if (isConnected || isConnecting) {
                    try {
                        if (isConnected) {
                            mCastSession.getRemoteMediaClient().stop();
                        }
                        if (mChannel != null) {
                                mCastSession.removeMessageReceivedCallbacks(nameSpace);
                                mChannel = null;
                        }
                    } catch (IOException e) {
                             e.printStackTrace();
                        }
                }
                mApplicationStarted = false;
            }
            mCastSession = null;
        }
        mSessionId = null;
    }


    public void sendMessage(final String message) {
        if (mCastSession != null) {
            mCastSession.sendMessage(nameSpace, message).setResultCallback(new ResultCallbacks<Status>() {
                @Override
                public void onSuccess(@NonNull Status status) {
                    LOGD(TAG, "Message Sent OK: namespace:" + nameSpace + " message:" + message);
                }

                @Override
                public void onFailure(@NonNull Status status) {
                    LOGE(TAG, "Sending message failed");
                }
            });
        }
    }
}
