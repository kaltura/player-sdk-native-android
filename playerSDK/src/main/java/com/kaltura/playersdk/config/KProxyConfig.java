package com.kaltura.playersdk.config;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gleb on 10/7/16.
 */

public class KProxyConfig {
    private List<String> mFilters;

    public KProxyConfig() {
        mFilters = new ArrayList<>();
    }

    public void addFilter(String filter) {
        mFilters.add(filter);
    }

    public JSONObject toJson() {
        if (!mFilters.isEmpty()) {
            try {
                JSONObject obj = new JSONObject();
                JSONObject flavorAssets = new JSONObject();
                JSONObject filters = new JSONObject();
                JSONObject include = new JSONObject();
                JSONArray formats = new JSONArray();
                for (String filter : mFilters) {
                    formats.put(filter);
                }
                include.put("Format", formats);
                filters.put("include", include);
                flavorAssets.put("filters", filters);
                obj.put("flavorassets",flavorAssets);
                return obj;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
