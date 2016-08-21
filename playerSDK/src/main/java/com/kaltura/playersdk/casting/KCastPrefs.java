package com.kaltura.playersdk.casting;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Gleb on 8/11/16.
 */
public class KCastPrefs {

    private static final String PREFS_NAME = "CCPrefs";

    public static final String SESSION_ID = "session_id";
    public static final String CHANNEL_PARAMS = "channel_params";

    public static void save(Context c, String key, String value) {
        SharedPreferences.Editor editor = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(key, value).commit();
    }

    public static void saveArray(Context c, String key, String... value) {
        SharedPreferences.Editor editor = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putStringSet(key, convertToSet(value)).commit();
    }

    public static String loadString(Context c, String key) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    public static String[] loadArray(Context c, String key) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> set = prefs.getStringSet(key, null);
        if (set != null) {
            return convertToArray(set);
        }
        return null;
    }

    private static Set<String> convertToSet(String... array) {
        Set<String> set = new HashSet<>();
        for (int i=0;i<array.length;i++) {
            set.add(array[i]);
        }
        return set;
    }

    private static String[] convertToArray(Set<String> set) {
        String[] array = new String[set.size()];
        int i = 0;
        for (String item : set) {
            array[i] = item;
            i++;
        }
        return array;
    }
}