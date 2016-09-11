package com.kaltura.playersdk.casting;

import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.cast.framework.CastSession;
import com.google.android.gms.cast.framework.SessionManager;
import com.google.android.gms.cast.framework.SessionManagerListener;
import com.kaltura.playersdk.interfaces.KCastProvider;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;

/**
 * Created by gilad.nadav on 09/09/2016.
 */
public class KCastSessionManagerListener implements SessionManagerListener<CastSession> {

    private static final String TAG = "KCastSessionManagerListener";
    private AppCompatActivity mActivity;
    private SessionManager mSessionManager;
    private KCastProvider mCastProvider;
    private String mCastApplicationID;

    public KCastSessionManagerListener(AppCompatActivity activity,SessionManager sessionManager) {
        this.mActivity = activity;
        this.mSessionManager = sessionManager;
    }

    public KCastSessionManagerListener(AppCompatActivity activity, SessionManager castSessionManager, String castApplicationID) {
        this.mActivity = activity;
        this.mSessionManager = castSessionManager;
        this.mCastApplicationID = castApplicationID;
    }

    public AppCompatActivity getActivity() {
        return mActivity;
    }

    public void setActivity(AppCompatActivity activity) {
        this.mActivity = activity;
    }

    public SessionManager getSessionManager() {
        return mSessionManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.mSessionManager = sessionManager;
    }

    public KCastProvider getCastProvider() {
        return mCastProvider;
    }

    public void setCastProvider(KCastProvider castProvider) {
        this.mCastProvider = castProvider;
    }

    @Override
    public void onSessionEnded(CastSession session, int error) {
        mActivity.invalidateOptionsMenu();
        LOGD(TAG, "XX onSessionEnded");
    }

    @Override
    public void onSessionResumed(CastSession session, boolean wasSuspended) {
        mActivity.invalidateOptionsMenu();
        LOGD(TAG, "XX onSessionResumed");

    }

    @Override
    public void onSessionStarted(CastSession session, String sessionId) {
        mActivity.invalidateOptionsMenu();
        startCC();
        LOGD(TAG, "XX onSessionStarted");
    }

    @Override
    public void onSessionStarting(CastSession session) {
        LOGD(TAG, "XX onSessionStarting");
    }

    @Override
    public void onSessionStartFailed(CastSession session, int error) {
        LOGD(TAG, "XX onSessionStartFailed");

    }

    @Override
    public void onSessionEnding(CastSession session) {
        if (mCastProvider != null) {
            mCastProvider.disconnectFromCastDevice();
        }
        LOGD(TAG, "XX onSessionEnding");

    }

    @Override
    public void onSessionResuming(CastSession session, String sessionId) {
        LOGD(TAG, "XX onSessionResuming");

    }

    @Override
    public void onSessionResumeFailed(CastSession session, int error) {
        LOGD(TAG, "XX onSessionResumeFailed");

    }

    @Override
    public void onSessionSuspended(CastSession session, int reason) {
        LOGD(TAG, "XX onSessionSuspended");

    }


    public void startCC() {
        mCastProvider = KCastFactory.createCastProvider();
        mCastProvider.startReceiver(getActivity().getApplicationContext(),mCastApplicationID, false, mSessionManager.getCurrentCastSession());
        //((KCastProviderImpl)mCastProvider).getProviderListener().onDeviceConnected();
    }
}
