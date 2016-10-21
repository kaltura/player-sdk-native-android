package com.kaltura.playersdk.drm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.io.FileNotFoundException;

/**
 * Created by noamt on 04/05/2016.
 */
class OfflineKeySetStorage {

    private static final String SHARED_PREFS_NAME = "OfflineDrmStore";
    private final SharedPreferences mSettings;

    OfflineKeySetStorage(Context context) {
        mSettings = context.getSharedPreferences(SHARED_PREFS_NAME, 0);
    }

    public void storeKeySetId(byte[] initData, byte[] keySetId) {
        String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
        String encodedKeySetId = Base64.encodeToString(keySetId, Base64.NO_WRAP);
        mSettings.edit()
                .putString(encodedInitData, encodedKeySetId)
                .apply();
    }

    public byte[] loadKeySetId(byte[] initData) throws FileNotFoundException {
        String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
        String encodedKeySetId = mSettings.getString(encodedInitData, null);

        if (encodedKeySetId == null) {
            throw new FileNotFoundException("Can't load keySetId");
        }

        return Base64.decode(encodedKeySetId, 0);
    }

    public void removeKeySetId(byte[] initData) {
        String encodedInitData = Base64.encodeToString(initData, Base64.NO_WRAP);
        mSettings.edit()
                .remove(encodedInitData)
                .apply();
    }
}
