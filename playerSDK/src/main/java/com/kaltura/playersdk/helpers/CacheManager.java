package com.kaltura.playersdk.helpers;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.kaltura.playersdk.Utilities;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by nissimpardo on 25/10/15.
 */
public class CacheManager {
    private static final String TAG = "CacheManager";
    public static final String CACHED_STRINGS_JSON = "CachedStrings.json";
    private static CacheManager ourInstance = new CacheManager();
    private JSONObject mCacheConditions;
    private Context mContext;
    private CacheSQLHelper mSQLHelper;
    private String mHost;
    private float mCacheSize = 0;
    private String mCachePath;

    public static CacheManager getInstance() {
        return ourInstance;
    }

    private CacheManager() {
    }

    public void setContext(Context context) {
        mContext = context;
        if (mSQLHelper == null) {
            mSQLHelper = new CacheSQLHelper(context);
        }
    }

    public void setHost(String host) {
        mHost = host;
    }

    public void setCacheSize(float cacheSize) {
        mCacheSize = cacheSize;
    }

    private String getCachePath() {
        if (mCachePath == null) {
            mCachePath = mContext.getFilesDir() + "/kaltura/";
            File cacheDir = new File(mCachePath);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        }
        return mCachePath;
    }
    
    private JSONObject getCacheConditions() {
        
        if (mCacheConditions == null) {
            String string = Utilities.readAssetToString(mContext, CACHED_STRINGS_JSON);
            if (string != null) {
                try {
                    mCacheConditions = new JSONObject(string);
                } catch (JSONException e) {
                    Log.e(TAG, "Invalid json", e);
                }
            }
        }

        return mCacheConditions;
    }

    public boolean shouldStore(Uri uri) throws URISyntaxException {
        String uriString = uri.toString();
        JSONObject conditions = getCacheConditions();

        String key = mHost.equals(uri.getHost()) ? "withDomain" : "substring";

        try {
            JSONObject object = conditions.getJSONObject(key);
            for (Iterator<String> it = object.keys(); it.hasNext(); ) {
                String str = it.next();
                if (uriString.contains(str)) {
                    return true;
                }
            }

        } catch (JSONException e) {
            Log.w(TAG, "Can't find required configuration data in " + CACHED_STRINGS_JSON, e);
        }

        return false;
    }

    private void deleteLessUsedFiles(long newCacheSize) {
        long freeBytesInternal = new File(mContext.getFilesDir().getAbsoluteFile().toString()).getFreeSpace();
//        long freeBytesExternal = new File(getExternalFilesDir(null).toString()).getFreeSpace();
        long cahceSize = (long)(mCacheSize * 1024 * 1024);
        long actualCacheSize = Math.min(cahceSize, freeBytesInternal);
        Log.d("KalturaCacheSize", String.valueOf(mSQLHelper.cacheSize()));
        boolean shouldDeleteLessUsedFiles = mSQLHelper.cacheSize() + newCacheSize > actualCacheSize;
        if (shouldDeleteLessUsedFiles) {
            mSQLHelper.deleteLessUsedFiles(mSQLHelper.cacheSize() + newCacheSize - actualCacheSize, new CacheSQLHelper.KSQLHelperDeleteListener() {
                @Override
                public void fileDeleted(String fileId) {
                    File lessUsedFile = new File(getCachePath() + fileId);
                    if (lessUsedFile.exists()) {
                        lessUsedFile.delete();
                    }
                }
            });
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private Uri getUri(Object request) {
        if (request instanceof String) {
            return Uri.parse((String)request);
        }
        if (request instanceof WebResourceRequest) {
            return ((WebResourceRequest)request).getUrl();
        }
        return null;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private HttpURLConnection appendHeaders(HttpURLConnection connection, Object request) {
        if (request instanceof WebResourceRequest) {
            try {
                connection.setRequestMethod(((WebResourceRequest)request).getMethod());
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            for (String key : ((WebResourceRequest)request).getRequestHeaders().keySet()) {
                connection.setRequestProperty(key, ((WebResourceRequest)request).getRequestHeaders().get(key));
            }
        }
        return connection;
    }

    public WebResourceResponse getResponse(Object request) throws IOException, URISyntaxException {
        Uri requestUrl = getUri(request);
        if (!shouldStore(requestUrl)) {
            return null;
        }
        InputStream inputStream = null;
        final String fileName = KStringUtilities.md5(requestUrl.toString());
        String filePath = getCachePath() + fileName;
        String contentType = null;
        String encoding = null;
        HashMap<String, Object> fileParams = mSQLHelper.fetchParamsForFile(fileName);
        if (mSQLHelper.sizeForId(fileName) > 0 && fileParams != null) {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            inputStream = new BufferedInputStream(fileInputStream);
            contentType = (String)fileParams.get(CacheSQLHelper.MimeType);
            encoding = (String)fileParams.get(CacheSQLHelper.Encoding);
            mSQLHelper.updateDate(fileName);
        } else {
            URL url = null;
            HttpURLConnection connection = null;
            try {
                url = new URL(requestUrl.toString());
                connection = (HttpURLConnection) url.openConnection();
                appendHeaders(connection, request);
                connection.connect();
                contentType = connection.getContentType();
                if (contentType == null) {
                    contentType = "";
                }
                String[] contentTypeParts = TextUtils.split(contentType, ";");
                if (contentTypeParts.length >= 2) {
                    contentType = contentTypeParts[0].trim();
                    encoding = contentTypeParts[1].trim();
                }
                mSQLHelper.addFile(fileName, contentType, encoding);
                inputStream = new CachingInputStream(filePath, url.openStream(), new CachingInputStream.KInputStreamListener() {
                    @Override
                    public void streamClosed(long fileSize, String filePath) {
                        int trimLength = getCachePath().length();
                        String fileId = filePath.substring(trimLength);
                        mSQLHelper.updateFileSize(fileId, fileSize);
                        deleteLessUsedFiles(fileSize);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading URL " + requestUrl, e);
                return null;
            }
        }
        Log.d("Stored Responses", contentType + " " + encoding + " " + requestUrl.toString());
        return new WebResourceResponse(contentType, encoding, inputStream);
    }
}
