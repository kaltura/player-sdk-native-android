/*
 * Copyright (c)  2015, Viacom 18 Media Private Limited. All Rights Reserved
 */

package com.kaltura.adplayerdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.util.Observable;

public class NetworkChangeReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        getObservable().connectionChanged(isInternetOn(context));
    }

    public static class NetworkObservable extends  Observable{
        private static NetworkObservable instance = null;

        private NetworkObservable() {
            // Exist to defeat instantiation.
        }

        public void connectionChanged(Boolean connected) {
            setChanged();
            notifyObservers(connected);
        }

        public static NetworkObservable getInstance() {
            if (instance == null) {
                instance = new NetworkObservable();
            }
            return instance;
        }

        @Override
        public int countObservers() {
            return super.countObservers();
        }
    }

    public static NetworkObservable getObservable() {
        return NetworkObservable.getInstance();
    }


    public static boolean isInternetOn(Context context) {
        ConnectivityManager conn = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        if (networkInfo != null) {
            return networkInfo.isConnected();
        }
        return false;
    }
}
