package com.kaltura.hlsplayersdk;

import android.os.Handler;
import android.os.HandlerThread;

/**
* Utility thread to allow queueing Runnables asynchronously.
*/
public class HLSUtilityThread extends HandlerThread
{
    private Handler mHandler = null;

    public HLSUtilityThread(String name)
    {
        super(name);
        start();
        setHandler(new Handler(getLooper()));
    }

    public Handler getHandler() {
        return mHandler;
    }

    private void setHandler(Handler mHandler) {
        this.mHandler = mHandler;
    }

    public void post(Runnable r)
    {
        getHandler().post(r);
    }
}
