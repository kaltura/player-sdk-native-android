package com.kaltura.helpers;

import android.content.Context;
import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by nissimpardo on 05/01/16.
 */
public class VideoDownloader extends AsyncTask<String, Long, String> {
    private String mFileName;
    private VideoDownloaderListener mListener;
    private Integer mFileLength;
    private String mCachePath;
    private Context mContext;

    public interface VideoDownloaderListener {
        void onDownloadFinished(String fileName);
        void onProgressUpdated(float progress);
    }
    public VideoDownloader(Context context, String fileName, VideoDownloaderListener listener) {
        super();
        mContext = context;
        mFileName = fileName;
        mListener = listener;
    }

    private String getCachePath() {
        if (mCachePath == null) {
            mCachePath = mContext.getFilesDir() + "/videos/";
            File cacheDir = new File(mCachePath);
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
        }
        return mCachePath;
    }

    @Override
    protected String doInBackground(String... params) {
        String link = params[0];
        try {
            URL url = new URL(link);
            if (url.getPath().endsWith(".mp4")) {
                mFileName += ".mp4";
            } else if (url.getPath().endsWith(".wvm")) {
                mFileName += ".wvm";
            }
            URLConnection connection = url.openConnection();
            connection.connect();
            mFileLength = connection.getContentLength();
            InputStream inputStream = new BufferedInputStream(url.openStream());
            OutputStream outputStream = new FileOutputStream(getCachePath() + mFileName);
            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while ((count = inputStream.read(data)) != -1) {
                total += count;
                // publishing the progress....
                if (mFileLength > 0) {
                    publishProgress(total);
                }
                outputStream.write(data, 0, count);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Long... values) {
        if (mFileLength > 0) {
            mListener.onProgressUpdated(values[0] / mFileLength.floatValue());
        }
    }

    @Override
    protected void onPostExecute(String s) {
        mListener.onDownloadFinished(getCachePath() + mFileName);
    }
}
