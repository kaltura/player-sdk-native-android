package com.kaltura.localassetsdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.LocalAssetsManager;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.Utilities;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements KPEventListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_WRITE_STORAGE = 200;

    private PlayerViewController mPlayer;
    private ViewGroup mPlayerContainer;
    private boolean mPlayerDetached;
    private Item mSelectedItem;
    private ArrayList<Item> mContentItems;
    private HashMap<String, Integer> mContentMap;

    private PlayerViewController.SourceURLProvider mSourceURLProvider = new PlayerViewController.SourceURLProvider() {
        @Override
        public String getURL(String entryId, String currentURL) {
            
            Item item = mContentItems.get(mContentMap.get(entryId));

            if (item != null) {
                if (item.isDownloaded()) {
                    return item.localPath;
                } else {
                    return item.contentUrl;
                }
            }
            return null;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        askPermission();
        loadItems();
        
        mPlayerContainer = (ViewGroup) findViewById(R.id.layout_player_container);
        
        setButtonAction(R.id.btn_register, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectedItem.localPath == null) {
                    uiLog("Content is not downloaded");
                    return;
                }
                KPPlayerConfig config = mSelectedItem.config;
                LocalAssetsManager.registerAsset(MainActivity.this, config, mSelectedItem.flavorId, mSelectedItem.localPath, new LocalAssetsManager.AssetRegistrationListener() {
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
        
        setButtonAction(R.id.btn_load, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPlayer().getMediaControl().start();
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


    }

    private void askPermission() {
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //if permission granted
                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private PlayerViewController getPlayer() {

        KPPlayerConfig config = mSelectedItem.config;
        
        
        if (mPlayer == null) {
            mPlayer = new PlayerViewController(this);
            mPlayerContainer.addView(mPlayer, new ViewGroup.LayoutParams(mPlayerContainer.getLayoutParams()));
            
            mPlayer.loadPlayerIntoActivity(this);

            mPlayer.initWithConfiguration(config);
            
            mPlayer.setCustomSourceURLProvider(mSourceURLProvider);

            mPlayer.addEventListener(this);
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

    @Override
    public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
    }

    @Override
    public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscrenn) {
    }
    
    class Item {
        KPPlayerConfig config;
        String flavorId;
        String contentUrl;
        String localPath;
        String name;

        public Item(KPPlayerConfig config, String flavorId, String contentUrl, String localPath, String name) {
            this.config = config;
            this.flavorId = flavorId;
            this.contentUrl = contentUrl;
            this.localPath = localPath;
            this.name = name;
        }

        @Override
        public String toString() {
            String status = isDownloaded() ? "downloaded" : "online";
            return name + " - " + status; 
        }
        
        public boolean isDownloaded() {
            return localPath != null && new File(localPath).canRead();
        }
    }
    
    String getString(JSONObject jsonObject, String key) {
        return jsonObject.isNull(key) ? null : jsonObject.optString(key);
    }
    
    void loadItems() {

        String itemsString = Utilities.readAssetToString(this, "content.json");

        mContentItems = new ArrayList<>();
        mContentMap = new HashMap<>();


        try {
            JSONObject content = new JSONObject(itemsString);
            JSONObject baseConfig = content.getJSONObject("baseConfig");
            JSONArray items = content.getJSONArray("items");

            for (int i=0; i<items.length(); i++) {
                JSONObject jsonItem = items.getJSONObject(i);
                KPPlayerConfig config = KPPlayerConfig.fromJSONObject(baseConfig);
                String entryId = jsonItem.getString("entryId");
                config.setEntryId(entryId);
                String localPath = getString(jsonItem, "localPath");
                String remoteUrl = getString(jsonItem, "remoteUrl");
                String flavorId = getString(jsonItem, "flavorId");
                Item item = new Item(config, flavorId, remoteUrl, localPath, getString(jsonItem, "name"));
                
                mContentItems.add(item);
                mContentMap.put(entryId, i);
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing json", e);
        }

        Spinner spinner = (Spinner) findViewById(R.id.spn_content);
        assert spinner != null;

        ArrayAdapter<Item> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, mContentItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedItem = (Item) parent.getSelectedItem();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
