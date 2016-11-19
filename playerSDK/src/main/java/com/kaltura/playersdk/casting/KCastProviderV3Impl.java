package com.kaltura.playersdk.casting;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.gms.cast.framework.CastContext;
import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.CastStateListener;
import com.google.android.gms.cast.framework.Session;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.kaltura.playersdk.interfaces.KCastMediaRemoteControl;
import com.kaltura.playersdk.interfaces.KCastProvider;
import com.kaltura.playersdk.players.KChromeCastPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

/**
 * Created by Gleb on 9/13/16.
 */
public class KCastProviderV3Impl implements KCastProvider {
    private static final String TAG = "KCastProviderImpl";
    public static  String nameSpace = "urn:x-cast:com.kaltura.cast.player";
    private SessionManager mSessionManager;
    private CastSession mCastSession;
    private CastContext mCastContext;
    private KCastProviderListener mProviderListener;
    private KCastMediaRemoteControl mCastMediaRemoteControl;
    private KCastKalturaChannel mChannel;
    private KCastInternalListener mInternalListener;
    private SessionManagerListener mSessionManagerListener = null;
    private Context mContext;
    private String mCastAppId;
    private boolean mApplicationStarted;
    private boolean isReconnedted  = true;
    private String mCastLogoUrl = "";
    private boolean isInSession = false;
    private boolean appInbg = false;

    private int numOfConnectedSenders = 0;
    //private boolean isPlayAfterEnded = false;
    //private String[] currentMediaParams;
    //@NonNull private KPlayerListener mPlayerListener = noopPlayerListener();


    public KCastProviderV3Impl(Context context, String castAppId, String logoUrl) {
        mContext = context;
        mCastContext = CastContext.getSharedInstance(context);
        mCastLogoUrl = logoUrl;
        //mCastContext.registerLifecycleCallbacksBeforeIceCreamSandwich(this, savedInstanceState);
        mSessionManager  = mCastContext.getSessionManager();
        mSessionManagerListener = createProviderSessionManagerListener();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
        mCastSession = mSessionManager.getCurrentCastSession();
        mCastAppId = castAppId;
    }

    @Override
    public void setCastProviderContext(Context newContext) {
        mContext = newContext;
        mCastContext = CastContext.getSharedInstance(newContext);
        mSessionManager = mCastContext.getSessionManager();
        if (mSessionManagerListener != null) {
            mSessionManager.removeSessionManagerListener(mSessionManagerListener);
        }
        mSessionManagerListener = createProviderSessionManagerListener();
        mSessionManager.addSessionManagerListener(mSessionManagerListener);
        mCastSession = mSessionManager.getCurrentCastSession();
    }

    @Override
    public int getNumOfConnectedSenders() {
        return numOfConnectedSenders;
    }

    public void setNumOfConnectedSenders(int numOfConnectedSenders) {
        this.numOfConnectedSenders = numOfConnectedSenders;
    }

    public void addCastStateListener(CastStateListener castStateListener) {
        mCastContext.addCastStateListener(castStateListener);
    }

    public void removeCastStateListener(CastStateListener castStateListener) {
        mCastContext.removeCastStateListener(castStateListener);
    }

    public void setInternalListener(KCastInternalListener internalListener) {
        mInternalListener = internalListener;
    }

    public String getCastLogoUrl() {
        return mCastLogoUrl;
    }

    public void setCastLogoUrl(String mCastLogoUrl) {
        this.mCastLogoUrl = mCastLogoUrl;
    }

    public KCastProviderListener getProviderListener() {
        return mProviderListener;
    }

//    private KPlayerListener noopPlayerListener() {
//        return new KPlayerListener() {
//            public void eventWithValue(KPlayer player, String eventName, String eventValue) {}
//            public void eventWithJSON(KPlayer player, String eventName, String jsonValue) {}
//            public void asyncEvaluate(String expression, String expressionID, PlayerViewController.EvaluateListener evaluateListener) {}
//            public void contentCompleted(KPlayer currentPlayer) {}
//        };
//    }

//    public void setPlayerListener(@NonNull KPlayerListener listener) {
//        mPlayerListener = listener;
//    }

    @Override
    public void init(Context context) {
        setCastProviderContext(context);
    }

    @Override
    public void startReceiver(Context context, boolean guestModeEnabled) {
        mCastSession = mSessionManager.getCurrentCastSession();
        if (mChannel == null) {
            mChannel = new KCastKalturaChannel(nameSpace, new KCastKalturaChannel.KCastKalturaChannelListener() {
                @Override
                public void readyForMedia(final String[] params) {
                    //if (!isRecconected()) {
                    sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
                    //}
                    // Receiver send the new content
                    if (params != null) {
                        //currentMediaParams = params;
                        mCastMediaRemoteControl = new KChromeCastPlayer(mCastSession);
                        ((KChromeCastPlayer) mCastMediaRemoteControl).setMediaInfoParams(params);
                        if (mInternalListener != null) {
                            mInternalListener.onStartCasting((KChromeCastPlayer) mCastMediaRemoteControl);
                        }
                    }
                }

                @Override
                public void ccOnSenderConnected(int numOfSendersConnected) {
                    LOGD(TAG, "ccOnSenderConnected :" + numOfSendersConnected);
                    setNumOfConnectedSenders(numOfSendersConnected);
                }
                @Override
                public void ccOnSenderDisconnected(int numOfSendersConnected) {
                    LOGD(TAG, "ccOnSenderDisconnected :" + numOfSendersConnected);
                    setNumOfConnectedSenders(numOfSendersConnected);
                }

                @Override
                public void ccUpdateAdDuration(int adDuration) {
                    //LOGD(TAG, "ccUpdateAdDuration :" + adDuration);
                    //mPlayerListener.eventWithValue(null, KPlayerListener.AdDurationChangeKey, String.valueOf(adDuration));
                }

                @Override
                public void ccUserInitiatedPlay() {
//                if (isPlayAfterEnded) {
//                    mCastMediaRemoteControl = new KChromeCastPlayer(mCastSession);
//                    ((KChromeCastPlayer) mCastMediaRemoteControl).setMediaInfoParams(currentMediaParams);
//                    if (mInternalListener != null) {
//                        mInternalListener.onStartCasting((KChromeCastPlayer) mCastMediaRemoteControl);
//                    }
//                    sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
//                    isPlayAfterEnded = false;
//                }
                }

                @Override
                public void ccReceiverAdOpen() {
                    LOGD(TAG, "ccReceiverAdOpen");
                    sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
                    mProviderListener.onCastReceiverAdOpen();
                }

                @Override
                public void ccReceiverAdComplete() {
                    LOGD(TAG, "ccReceiverAdComplete");
                    mProviderListener.onCastReceiverAdComplete();
                }

                @Override
                public void textTeacksRecived(HashMap<String, Integer> textTrackHash) {
                    if (getCastMediaRemoteControl() != null) {
                        getCastMediaRemoteControl().setTextTracks(textTrackHash);
                    }
                }

                @Override
                public void videoTracksReceived(List<Integer> videoTracksList) {
                    if (getCastMediaRemoteControl() != null) {
                        getCastMediaRemoteControl().setVideoTracks(videoTracksList);
                    }
                }

                @Override
                public void onCastReceiverError(String errorMsg, int errorCode) {
                    mProviderListener.onCastReceiverError(errorMsg, errorCode);
                }
            });
        }

        if (mCastSession != null) {
            try {
                mCastSession.setMessageReceivedCallbacks(nameSpace, mChannel);
            } catch (IOException e) {
                LOGE(TAG, "Exception while creating channel", e);
            }
            if (!isReconnected()) {
                if (!"".equals(mCastLogoUrl)) {
                    mCastSession.sendMessage(nameSpace, "{\"type\": \"setLogo\", \"logo\": \"" + mCastLogoUrl + "\"}");
                }
                if (!isCasting()) {
                    sendMessage("{\"type\":\"show\",\"target\":\"logo\"}");
                }
            }
            mApplicationStarted = true;
        }
    }

    @Override
    public void startReceiver(Context context) {
        startReceiver(context, false);
    }

    @Override
    public void showLogo() {
        sendMessage("{\"type\":\"show\",\"target\":\"logo\"}");
    }

    @Override
    public void hideLogo() {
        sendMessage("{\"type\":\"hide\",\"target\":\"logo\"}");
    }

    @Override
    public void disconnectFromCastDevice() {
        if (mInternalListener != null) {
            //mInternalListener.onCastStateChanged("hideConnectingMessage");
            mInternalListener.onCastStateChanged("chromecastDeviceDisConnected");
            mInternalListener.onStopCasting(appInbg);
            if (getNumOfConnectedSenders() == 1) {
                setNumOfConnectedSenders(0);
            }
        }
        teardown();
    }

    @Override
    public void setAppBackgroundState(boolean appBgState) {
        appInbg = appBgState;
    }

    @Override
    public boolean getAppBackgroundState() {
        return appInbg;
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

    @Override
    public boolean isReconnected() {
        return isReconnedted;
    }

    @Override
    public boolean isConnected() {
        if (mCastSession != null) {
            return mCastSession.isConnected();
        }
        if (CastContext.getSharedInstance(mContext) != null && CastContext.getSharedInstance(mContext).getSessionManager() != null) {
            mCastSession = CastContext.getSharedInstance(mContext).getSessionManager().getCurrentCastSession();
            if (mCastSession != null) {
                return mCastSession.isConnected();
            }
        }
        return false;
    }

    @Override
    public boolean isCasting() {
        if (mCastSession != null && mCastSession.getRemoteMediaClient() != null) {
            return mCastSession.getRemoteMediaClient().isPlaying() || mCastSession.getRemoteMediaClient().isPaused() || mCastSession.getRemoteMediaClient().getMediaInfo() != null;
        }
        return false;
    }

    @Override
    public long getStreamDuration() {
        if (mCastSession != null && mCastSession.getRemoteMediaClient() != null) {
            return mCastSession.getRemoteMediaClient().getStreamDuration();
        }
        return -1;
    }

    @Override
    public String getSessionEntryID() {
        String sessionEntryID = null;
        if (mCastSession != null && mCastSession.getRemoteMediaClient() != null &&
            mCastSession.getRemoteMediaClient().getMediaInfo() != null &&
            mCastSession.getRemoteMediaClient().getMediaInfo().getMetadata() != null) {
            sessionEntryID =  mCastSession.getRemoteMediaClient().getMediaInfo().getMetadata().getString(KChromeCastPlayer.KEY_ENTRY_ID);
        }

        if (sessionEntryID != null) {
            LOGD(TAG, "sessionEntryID = " + sessionEntryID);
        } else {
            LOGD(TAG, "sessionEntryID is not set");
        }
        return sessionEntryID;
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

    public CastSession getCastSession() {
        return mCastSession;
    }

    public void removeSessionManagerListener() {
        if (mSessionManagerListener != null) {
            mSessionManager.removeSessionManagerListener(mSessionManagerListener);
            mSessionManagerListener = null;
        }
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
    }



    private SessionManagerListener createProviderSessionManagerListener() {
        return  new SessionManagerListener() {
            @Override
            public void onSessionStarting(Session session) {
                LOGD(TAG, "SessionManagerListener onSessionStarting");
            }

            @Override
            public void onSessionStarted(Session session, String s) {

                isReconnedted = false;
                if (!isInSession) {
                    LOGD(TAG, "SessionManagerListener onSessionStarted");
                    startReceiver(mContext);
                }
                isInSession = true;
            }

            @Override
            public void onSessionStartFailed(Session session, int i) {
                LOGD(TAG, "SessionManagerListener onSessionStartFailed");
                isInSession = false;
            }

            @Override
            public void onSessionEnding(Session session) {
                LOGD(TAG, "SessionManagerListener onSessionEnding");
                disconnectFromCastDevice();
                isInSession = false;
            }

            @Override
            public void onSessionEnded(Session session, int i) {
                LOGD(TAG, "SessionManagerListener onSessionEnded");
                if (isInSession) {
                    disconnectFromCastDevice();
                    isInSession = false;
                }
            }

            @Override
            public void onSessionResuming(Session session, String s) {
                LOGD(TAG, "SessionManagerListener onSessionResuming");
            }

            @Override
            public void onSessionResumed(Session session, boolean b) {
                LOGD(TAG, "SessionManagerListener onSessionResumed");
                isInSession = true;
            }

            @Override
            public void onSessionResumeFailed(Session session, int i) {
                LOGD(TAG, "SessionManagerListener onSessionResumeFailed");
                disconnectFromCastDevice();
                isInSession = false;
            }

            @Override
            public void onSessionSuspended(Session session, int i) {
                LOGD(TAG, "SessionManagerListener onSessionSuspended");
                if (mCastSession != null && mCastSession.getRemoteMediaClient() != null) {
                    LOGD(TAG, "onSessionSuspended CURRENT POSITION = " + mCastSession.getRemoteMediaClient().getApproximateStreamPosition());
                }
            }
        };
    }
}
