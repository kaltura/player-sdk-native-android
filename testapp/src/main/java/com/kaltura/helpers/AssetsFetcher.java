package com.kaltura.helpers;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by nissimpardo on 03/01/16.
 */
public class AssetsFetcher {
    public static ArrayList<HashMap<String, Object>> loadJSONArrayFromAssets(Context context, String fileName) {
        String json = null;
        try {
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return convertToHashMap(buffer);
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

    }

    public static ArrayList<HashMap<String, Object>> loadConfigFromFile(Context context) {
        String json = null;
        File file = new File(context.getFilesDir() + "/videos/config.json");
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            int size = fileInputStream.available();
            byte[] buffer = new byte[size];
            fileInputStream.read(buffer);
            fileInputStream.close();
            return convertToHashMap(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static ArrayList<HashMap<String, Object>> convertToHashMap(byte[] data) {
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(new String(data, "UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        ArrayList<HashMap<String, Object>> params = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject obj = (JSONObject)jsonArray.get(i);
                HashMap<String, Object> hash = new HashMap<>();
                Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    hash.put(key, obj.get(key));
                }
                params.add(hash);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return params;
    }

}
