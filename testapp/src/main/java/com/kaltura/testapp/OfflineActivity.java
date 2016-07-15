package com.kaltura.testapp;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.LocalAssetsManager;
import com.kaltura.playersdk.PlayerViewController;

import java.util.ArrayList;
import java.util.HashMap;

import helpers.AssetsFetcher;
import helpers.VideoDownloader;

public class OfflineActivity extends Activity implements DemoAdapter.MyClickListener, PlayerFragment.OnFragmentInteractionListener, PlayerViewController.SourceURLProvider {
    private HashMap<String, String> values;
    private PlayerFragment mPlayerFragment;
    DemoAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);
        ArrayList<HashMap<String, Object>> cells = AssetsFetcher.loadJSONArrayFromAssets(this, "demoParams.json");
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setBackgroundColor(Color.RED);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DemoAdapter(this, cells);
        adapter.setOnItemClickListener(this);
        recyclerView.setAdapter(adapter);
    }

    private HashMap<String, String> getValues() {
        if (values == null) {
            values = new HashMap<>();
        }
        return values;
    }


    private void fetchFile(final DownloadCell view, final String title, String fileName, final String url) {
        view.setCellState(false);
        VideoDownloader downloader = new VideoDownloader(this, fileName, new VideoDownloader.VideoDownloaderListener() {
            @Override
            public void onDownloadFinished(String fileName) {
                view.setCellState(true);
                if (title != null) {
                    view.getTextView().setText(title);
                    getValues().put("OfflineURL", fileName);
                } else {
                    ArrayList<?> test = AssetsFetcher.loadConfigFromFile(OfflineActivity.this);
                    if (test != null) {
                        HashMap<String, String> config = (HashMap<String, String>) test.get(0);
                        adapter.loadRemoteConfig(config);
                    }
                }
            }

            @Override
            public void onProgressUpdated(float progress) {
                 view.updateProgress(progress);
            }
        });
        downloader.execute(url);
    }


    private void getLicensce(final DownloadCell view) {
        LocalAssetsManager.registerAsset(this, getConfig(), getValues().get("FlavourId"), getValues().get("OfflineURL"), new LocalAssetsManager.AssetRegistrationListener() {
            @Override
            public void onRegistered(String assetPath) {
                view.post(new Runnable() {
                    @Override
                    public void run() {view.getTextView().setText("Licensced");}
                });
            }

            @Override
            public void onFailed(String assetPath, Exception error) {
                view.post(new Runnable() {
                    @Override
                    public void run() {view.getTextView().setText("Licensce Error");}
                });
            }
        });
    }

    private KPPlayerConfig getConfig() {
        KPPlayerConfig config = new KPPlayerConfig(getValues().get("Domain"), getValues().get("UIConf"), getValues().get("PartnerId"));
        config.setEntryId(getValues().get("EntryId"));
        config.setKS(getValues().get("KS"));
        return config;
    }

    @Override
    public void onRowClick(DownloadCell v, int position) {
        switch (position) {
            case 8:
                if (v.getTextView().getText().equals("Get Licensce")) {
                    getLicensce(v);
                } else if (getValues().get("Offline URL") != null) {
                    fetchFile(v, "Get Licensce", getValues().get("EntryId"), getValues().get("Offline URL"));
                }
                break;
            case 10:
                boolean isPlayer = false;
                if (mPlayerFragment == null) {
                    mPlayerFragment = new PlayerFragment();
                    isPlayer = true;
                }
                findViewById(R.id.recyclerView).setVisibility(View.INVISIBLE);
                mPlayerFragment.setPlayerConfig(getConfig());
                getFragmentManager().beginTransaction()
                        .setCustomAnimations(R.animator.slide_up, R.animator.slide_down)
                        .add(R.id.fragment_container, mPlayerFragment)
                        .addToBackStack(mPlayerFragment.getClass().getName())
                        .commit();
                if (!isPlayer) {
                    mPlayerFragment.resumePlayer();
                }
                break;

            case 11:
                String url = null;
                if (getValues().get("Config URl") != null) {
                    url = getValues().get("Config URl");
                } else {
                    SharedPreferences preferences = getSharedPreferences("KalturaPrefs", MODE_PRIVATE);
                    if (preferences != null) {
                        url = preferences.getString("configURL", null);
                    }
                }
                if (url == null) {
                    SharedPreferences.Editor editor = getSharedPreferences("KalturaPrefs", MODE_PRIVATE).edit();
                    url = "http://192.168.160.195/demoConfig.json";
                    editor.putString("configURL", url);
                    editor.apply();
                }
                fetchFile(v, null, "config.json", url);
                break;
        }
    }

    @Override
    public void onTextChanged(String key, String text) {
        if (key.equals("Config URl")) {
            SharedPreferences.Editor editor = getSharedPreferences("KalturaPrefs", MODE_PRIVATE).edit();
            editor.putString("configURL", text);
            editor.apply();
        }
        getValues().put(key, text);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.d("URI", uri.toString());
    }

    @Override
    public String getURL(String entryId, String currentURL) {
        return getValues().get("OfflineURL");
    }
}
