package com.kaltura.dtgplayer;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.kaltura.dtg.ContentManager;
import com.kaltura.dtg.DownloadItem;
import com.kaltura.dtg.DownloadState;
import com.kaltura.dtg.DownloadStateListener;
import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.LocalAssetsManager;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPErrorEventListener;
import com.kaltura.playersdk.events.KPStateChangedEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;
import com.kaltura.playersdk.utils.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements KPErrorEventListener, KPStateChangedEventListener {

    private static final String TAG = "MainActivity";

    private PlayerViewController mPlayer;
    private ViewGroup mPlayerContainer;
    private boolean mPlayerDetached;
    private Item mSelectedItem;

    private ContentManager mContentManager;

    private PlayerViewController.SourceURLProvider mSourceURLProvider = new PlayerViewController.SourceURLProvider() {
        @Override
        public String getURL(String entryId, String currentURL) {

            String playbackURL = mContentManager.getPlaybackURL(entryId);

            return playbackURL;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        loadItems();

        mPlayerContainer = (ViewGroup) findViewById(R.id.layout_player_container);

        mContentManager = ContentManager.getInstance(this);
        DownloadStateListener downloadStateListener = new DownloadStateListener() {
            @Override
            public void onDownloadComplete(DownloadItem item) {

            }

            @Override
            public void onProgressChange(DownloadItem item, long downloadedBytes) {

            }

            @Override
            public void onDownloadStart(DownloadItem item) {

            }

            @Override
            public void onDownloadPause(DownloadItem item) {

            }

            @Override
            public void onDownloadStop(DownloadItem item) {

            }

            @Override
            public void onDownloadMetadata(DownloadItem item, Exception error) {

                DownloadItem.TrackSelector trackSelector = item.getTrackSelector();

                List<DownloadItem.Track> downloadedVideoTracks = trackSelector.getDownloadedTracks(DownloadItem.TrackType.VIDEO);

                List<DownloadItem.Track> availableTracks = trackSelector.getAvailableTracks(DownloadItem.TrackType.AUDIO);
                if (availableTracks.size() > 0) {
                    trackSelector.setSelectedTracks(DownloadItem.TrackType.AUDIO, availableTracks);
                }
                try {
                    trackSelector.apply();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                item.startDownload();
            }

            @Override
            public void onTracksAvailable(DownloadItem item, DownloadItem.TrackSelector trackSelector) {
                // Select lowest-resolution video
                List<DownloadItem.Track> videoTracks = trackSelector.getAvailableTracks(DownloadItem.TrackType.VIDEO);
                DownloadItem.Track minVideo = Collections.min(videoTracks, DownloadItem.Track.bitrateComparator);
                trackSelector.setSelectedTracks(DownloadItem.TrackType.VIDEO, Collections.singletonList(minVideo));
            }
        };
        mContentManager.addDownloadStateListener(downloadStateListener);
        mContentManager.start();
        
        setButtonAction(R.id.btn_download, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadItem item = mContentManager.createItem(mSelectedItem.getDownloadItemId(), mSelectedItem.contentUrl);
                if (item == null) {
                    uiLog("Already exists");
                    item = mContentManager.findItem(mSelectedItem.getDownloadItemId());
                }
                if (item != null) {
                    item.loadMetadata();
                }
            }
        });

        setButtonAction(R.id.btn_register, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadItem downloadItem = mSelectedItem.findDownloadItem();
                if (downloadItem == null || downloadItem.getState() != DownloadState.COMPLETED) {
                    uiLog("Content is not downloaded");
                    return;
                }

                KPPlayerConfig config = mSelectedItem.config;
                LocalAssetsManager.registerAsset(MainActivity.this, config, null, mSelectedItem.getLocalPath(), new LocalAssetsManager.AssetRegistrationListener() {
                    @Override
                    public void onRegistered(String assetPath) {
                        uiLog("Register successful", null);
                    }

                    @Override
                    public void onFailed(String assetPath, Exception error) {
                        uiLog("Register failed", error);
                    }
                });
            }
        });

        setButtonAction(R.id.btn_play, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPlayer();//.getMediaControl().start();
            }
        });

        setButtonAction(R.id.btn_detach, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayer != null) {
                    mPlayerContainer.removeView(mPlayer);
                    mPlayerDetached = true;
                }
            }
        });

        setButtonAction(R.id.btn_status, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalAssetsManager.checkAssetStatus(MainActivity.this, mSelectedItem.getLocalPath(), new LocalAssetsManager.AssetStatusListener() {
                    @Override
                    public void onStatus(String assetPath, long expiryTimeSeconds, long availableTimeSeconds) {
                        uiLog("expiryTime:" + expiryTimeSeconds);
                    }
                });
            }
        });

        setButtonAction(R.id.btn_unregister, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LocalAssetsManager.unregisterAsset(MainActivity.this, mSelectedItem.config, mSelectedItem.getLocalPath(), new LocalAssetsManager.AssetRemovalListener() {
                    @Override
                    public void onRemoved(String assetPath) {
                        Log.d(TAG, "Removed " + assetPath);
                    }
                });
            }
        });
    }


    private PlayerViewController getPlayer() {

        KPPlayerConfig config = mSelectedItem.config;

        if (mPlayer == null) {
            mPlayer = new PlayerViewController(this);
            mPlayerContainer.addView(mPlayer, new ViewGroup.LayoutParams(mPlayerContainer.getLayoutParams()));

            mPlayer.loadPlayerIntoActivity(this);

            mPlayer.initWithConfiguration(config);

            mPlayer.setCustomSourceURLProvider(mSourceURLProvider);

            mPlayer.setOnKPErrorEventListener(this);
            mPlayer.setOnKPStateChangedEventListener(this);


        } else {
            if (mPlayerDetached) {
                mPlayerContainer.addView(mPlayer, new ViewGroup.LayoutParams(mPlayerContainer.getLayoutParams()));
                mPlayerDetached = false;
            }

            mPlayer.changeConfiguration(config);
        }
        return mPlayer;
    }

    void setButtonAction(int buttonId, View.OnClickListener clickListener) {
        Button button = (Button) findViewById(buttonId);
        assert button != null;
        button.setOnClickListener(clickListener);
    }

    void uiLog(Object obj) {
        uiLog(obj, null);
    }

    void uiLog(Object obj, Exception e) {
        final String text = obj == null ? "<null>" : obj.toString();
        final TextView textView = ((TextView) findViewById(R.id.txt_log));
        assert textView != null;

        Log.d(TAG, text, e);

        textView.post(new Runnable() {
            @Override
            public void run() {
                textView.append(text + "\n\n");
            }
        });
    }

    @Override
    public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
        uiLog("onKPlayerStateChanged:" + state);
    }

    @Override
    public void onKPlayerError(PlayerViewController playerViewController, KPError error) {
        uiLog("onKPlayerError", error.getException());
    }

    class Item {
        KPPlayerConfig config;
        String contentUrl;
        String name;

        Item(KPPlayerConfig config, String contentUrl, String name) {
            this.config = config;
            this.contentUrl = contentUrl;
            this.name = name;
        }

        @Override
        public String toString() {
            String status = isDownloaded() ? "downloaded" : "online";
            return name + " - " + status;
        }

        boolean isDownloaded() {

            DownloadItem item = findDownloadItem();

            return item != null && item.getState() == DownloadState.COMPLETED;
        }

        DownloadItem findDownloadItem() {
            return mContentManager.findItem(getDownloadItemId());
        }

        String getLocalPath() {
            return mContentManager.getLocalFile(getDownloadItemId()).getAbsolutePath();
        }

        private String getDownloadItemId() {
            return config.getEntryId();
        }
    }

    String getString(JSONObject jsonObject, String key) {
        return jsonObject.isNull(key) ? null : jsonObject.optString(key);
    }

    void loadItems() {

        String itemsString = Utilities.readAssetToString(this, "content.json");

        ArrayList<Item> contentItems = new ArrayList<>();

        try {
            JSONObject content = new JSONObject(itemsString);
            JSONObject baseConfig = content.getJSONObject("baseConfig");
            JSONObject items = content.getJSONObject("items");


            for (Iterator<String> it = items.keys(); it.hasNext(); ) {
                String key = it.next();

                KPPlayerConfig config = KPPlayerConfig.fromJSONObject(baseConfig);
                JSONObject jsonItem = items.getJSONObject(key);

                String entryId = getString(jsonItem, "entryId");
                if (entryId == null) {
                    continue;
                }
                config.setEntryId(entryId);
                String remoteUrl = getString(jsonItem, "remoteUrl");
                String flavorId = getString(jsonItem, "flavorId");

                config.setLocalContentId(key);

                config.addConfig("autoPlay", "true");

                Item item = new Item(config, remoteUrl, key);

                contentItems.add(item);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing json", e);
        }

        Spinner spinner = (Spinner) findViewById(R.id.spn_content);
        assert spinner != null;

        ArrayAdapter<Item> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, contentItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedItem = (Item) parent.getSelectedItem();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mSelectedItem = null;
            }
        });
    }
}
