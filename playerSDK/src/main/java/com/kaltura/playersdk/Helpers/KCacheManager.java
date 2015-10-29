package com.kaltura.playersdk.Helpers;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by nissimpardo on 25/10/15.
 */
public class KCacheManager {
    private static KCacheManager ourInstance = new KCacheManager();
    private HashMap<String, Object> mCacheConditions;
    private Context mContext;
    private KSQLHelper mSQLHelper;
    private String mHost;
    private int mCacheSize = 0;
    private String mCachePath;

    public static KCacheManager getInstance() {
        return ourInstance;
    }

    private KCacheManager() {
    }

    public void setContext(Context context) {
        mContext = context;
        mSQLHelper = new KSQLHelper(context);
    }

    public void setHost(String host) {
        mHost = host;
    }

    public void setCacheSize(int cacheSize) {
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

    private HashMap<String, Object> getCacheConditions() {
        if (mCacheConditions == null) {
            StringBuffer sb = new StringBuffer();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(mContext.getAssets().open(
                        "CachedStrings.json")));
                String temp;
                while ((temp = br.readLine()) != null)
                    sb.append(temp);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    br.close(); // stop reading
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Type type = new TypeToken<HashMap<String, Object>>(){}.getType();
            Gson gson = new Gson();
            mCacheConditions = gson.fromJson(sb.toString(), type);
        }
        return mCacheConditions;
    }

    public LinkedTreeMap<String, Object> getSubStrings() {
         return (LinkedTreeMap)getCacheConditions().get("substring");
    }

    public LinkedTreeMap<String, Object> getWithDomain() {
        return (LinkedTreeMap)getCacheConditions().get("withDomain");
    }

    public boolean shouldStore(Uri uri) throws URISyntaxException {
        if (uri.getHost().equals(mHost)) {
            for (String key: getWithDomain().keySet()) {
                if (uri.toString().contains(key)) {
                    return true;
                }
            }
        } else {
            for (String key: getSubStrings().keySet()) {
                if (uri.toString().contains(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void deleteLessUsedFiles(long newCacheSize) {
        long freeBytesInternal = new File(mContext.getFilesDir().getAbsoluteFile().toString()).getFreeSpace();
//        long freeBytesExternal = new File(getExternalFilesDir(null).toString()).getFreeSpace();
        long cahceSize = mCacheSize * 1024 * 1024;
        long actualCacheSize = Math.max(cahceSize, freeBytesInternal);
        boolean shouldDeleteLessUsedFiles = mSQLHelper.cacheSize() + newCacheSize > actualCacheSize;
        if (shouldDeleteLessUsedFiles) {
            mSQLHelper.deleteLessUsedFiles(mSQLHelper.cacheSize() + newCacheSize - actualCacheSize);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public WebResourceResponse getResponse(WebResourceRequest request) throws IOException, URISyntaxException {
        if (!shouldStore(request.getUrl())) {
            return null;
        }
        InputStream inputStream = null;
        final String fileName = KStringUtilities.md5(request.getUrl().toString());
        String filePath = getCachePath() + fileName;
        String contentType = null;
        String encoding = null;
        HashMap<String, Object> fileParams = mSQLHelper.fetchParamsForFile(fileName);
        if (mSQLHelper.sizeForId(fileName) > 0 && fileParams != null) {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            inputStream = new BufferedInputStream(fileInputStream);
            contentType = (String)fileParams.get(KSQLHelper.MimeType);
            encoding = (String)fileParams.get(KSQLHelper.Encoding);
            mSQLHelper.updateDate(fileName);
        } else {
            URL url = null;
            HttpURLConnection connection = null;
            try {
                url = new URL(request.getUrl().toString());
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(request.getMethod());
                for (String key : request.getRequestHeaders().keySet()) {
                    connection.setRequestProperty(key, request.getRequestHeaders().get(key));
                }
                connection.connect();
                contentType = connection.getContentType();
                String[] contentTypeParts = TextUtils.split(contentType, ";");
                if (contentTypeParts.length >= 2) {
                    contentType = contentTypeParts[0].trim();
                    encoding = contentTypeParts[1].trim();
                }
                mSQLHelper.addFile(fileName, contentType, encoding);
                inputStream = new KInputStream(filePath, url.openStream(), new KInputStream.KInputStreamListener() {
                    @Override
                    public void streamClosed(long fileSize, String filePath) {
                        deleteLessUsedFiles(fileSize);
                        int trimLength = (int)mContext.getFilesDir().length() + 1;
                        String fileId = filePath.substring(trimLength);
                        mSQLHelper.updateFileSize(fileId, fileSize);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Log.d("Stored Responses", contentType + " " + encoding + " " + request.getUrl().toString());
        return new WebResourceResponse(contentType, encoding, inputStream);
    }
}
