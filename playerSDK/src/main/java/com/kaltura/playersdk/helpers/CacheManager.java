package com.kaltura.playersdk.helpers;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
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
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.kaltura.playersdk.helpers.KStringUtilities.md5;


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
    private String mBaseURL;
    private float mCacheSize = 0;
    private String mCachePath;

    
    private void logCacheHit(Uri url, String fileId) {
        Log.d(TAG, "CACHE HIT: " + fileId + " : " + url);
    }

    private void logCacheMiss(Uri url, String fileId) {
        Log.d(TAG, "CACHE MISS: " + fileId + " : " + url);
    }
    
    private void logCacheIgnored(Uri url) {
        Log.d(TAG, "CACHE IGNORE: " + url);
    }

    private void logCacheSaved(Uri url, String fileId) {
        Log.d(TAG, "CACHE SAVED: " + fileId + " : " + url);
    }

    private void logCacheDeleted(String fileId) {
        Log.d(TAG, "CACHE DELETE: " + fileId);
    }



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

    public void setBaseURL(String baseURL) {
        mBaseURL = baseURL;
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

    private boolean shouldStore(Uri uri, Map<String, String> headers, String method) {
        
        if (!method.equalsIgnoreCase("GET")) {
            return false;   // only cache GETs
        }
        
        if (!(uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
            return false;   // only cache http(s)
        }
        
        String uriString = uri.toString();
        JSONObject conditions = getCacheConditions();

        String key = uriString.startsWith(mBaseURL) ? "withDomain" : "substring";

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
        long cahceSize = (long)(mCacheSize * 1024 * 1024);
        long actualCacheSize = Math.min(cahceSize, freeBytesInternal);
        long currentCacheSize = mSQLHelper.cacheSize();
        boolean shouldDeleteLessUsedFiles = currentCacheSize + newCacheSize > actualCacheSize;
        if (shouldDeleteLessUsedFiles) {
            mSQLHelper.deleteLessUsedFiles(currentCacheSize + newCacheSize - actualCacheSize, new CacheSQLHelper.KSQLHelperDeleteListener() {
                @Override
                public void fileDeleted(String fileId) {
                    File deletedFile = new File(getCachePath(), fileId);
                    if (deletedFile.exists()) {
                        deletedFile.delete();
                        logCacheDeleted(fileId);
                    }
                }
            });
        }
    }

    private void setRequestParams(HttpURLConnection connection, Map<String, String> headers, String method) {
        try {
            connection.setRequestMethod(method);
        } catch (ProtocolException e) {
            Log.e(TAG, "Invalid method " + method, e);
            // This can't really happen. But if it did, and we're on a debug build, the app should crash.
            throw new IllegalArgumentException(e);
        }
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }
    
    public boolean removeCachedResponse(Uri requestUrl) {
        String fileId = getCacheFileId(requestUrl);

        if (!mSQLHelper.removeFile(fileId)) {
            Log.e(TAG, "Failed to remove cache entry for request: " + requestUrl);
            return false;
        } else {
            File file = new File(getCachePath(), fileId);

            if (!file.delete()) {
                Log.e(TAG, "Failed to delete file for request: " + requestUrl);
                return false;
            }
        }
        logCacheDeleted(fileId);

        return true;
    }
    
    public void cacheResponse(Uri requestUrl) throws IOException {
        WebResourceResponse resp = getResponse(requestUrl, Collections.<String, String>emptyMap(), "GET");
        InputStream inputStream = resp.getData();

        // Must fully read the input stream so that it gets cached. But we don't need the data now.
        byte[] buffer = new byte[1024];
        try {
            //noinspection StatementWithEmptyBody
            while (inputStream.read(buffer, 0, buffer.length) >= 0);
        } finally {
            inputStream.close();
        }
    }
    
    public WebResourceResponse getResponse(final Uri requestUrl, Map<String, String> headers, String method) throws IOException {
        if (!shouldStore(requestUrl, headers, method)) {
            logCacheIgnored(requestUrl);
            return null;
        }

        final InputStream inputStream;
        String fileName = getCacheFileId(requestUrl);
        File targetFile = new File(getCachePath(), fileName);
        String contentType;
        String encoding = null;
        HashMap<String, Object> fileParams = mSQLHelper.fetchParamsForFile(fileName);
        
        if (mSQLHelper.sizeForId(fileName) > 0 && fileParams != null) {
            logCacheHit(requestUrl, fileName);
            
            FileInputStream fileInputStream = new FileInputStream(targetFile);
            inputStream = new BufferedInputStream(fileInputStream);
            contentType = (String)fileParams.get(CacheSQLHelper.MimeType);
            encoding = (String)fileParams.get(CacheSQLHelper.Encoding);
            mSQLHelper.updateDate(fileName);
            return new WebResourceResponse(contentType, encoding, inputStream);

        } else {
            logCacheMiss(requestUrl, fileName);
            
            return getResponseFromNetwork(requestUrl, headers, method, fileName, targetFile);
        }
    }

    @NonNull
    private WebResourceResponse getResponseFromNetwork(final Uri requestUrl, Map<String, String> headers, String method, String fileName, File targetFile) throws IOException {
        String contentType;
        InputStream inputStream = null;
        URL url = new URL(requestUrl.toString());
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            setRequestParams(connection, headers, method);
            connection.connect();
            contentType = connection.getContentType();

            if (contentType == null) {
                contentType = "";
            }
            String[] contentTypeParts = TextUtils.split(contentType, ";");
            String encoding = null;
            if (contentTypeParts.length >= 2) {
                contentType = contentTypeParts[0].trim();
                encoding = contentTypeParts[1].trim();
            }
            mSQLHelper.addFile(fileName, contentType, encoding);
            inputStream = new CachingInputStream(targetFile.getAbsolutePath(), url.openStream(), new CachingInputStream.KInputStreamListener() {
                @Override
                public void streamClosed(long fileSize, String filePath) {
                    int trimLength = getCachePath().length();
                    String fileId = filePath.substring(trimLength);
                    mSQLHelper.updateFileSize(fileId, fileSize);
                    logCacheSaved(requestUrl, fileId);
                    deleteLessUsedFiles(fileSize);
                    connection.disconnect();
                }
            });
            return new WebResourceResponse(contentType, encoding, inputStream);
        } finally {
            // if inputStream wasn't created, streamClosed() will not get called and the connection may leak. 
            if (inputStream == null) {
                connection.disconnect();
            }
        }

    }

    @NonNull
    private String getCacheFileId(Uri requestUrl) {
        if (requestUrl.getFragment() != null) {
            String localContentId = KStringUtilities.extractFragmentParam(requestUrl, "localContentId");
            if (!TextUtils.isEmpty(localContentId)) {
                return md5("contentId:" + localContentId);
            }
        }
        return md5(requestUrl.toString());
    }
}
