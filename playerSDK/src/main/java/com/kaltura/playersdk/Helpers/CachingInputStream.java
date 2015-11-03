package com.kaltura.playersdk.helpers;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by nissimpardo on 25/10/15.
 */
public class CachingInputStream extends BufferedInputStream {
    private String mFilePath;
    private BufferedOutputStream mOutputStream;
    private KInputStreamListener mListener;
    private long fileSize = 0;

    public interface KInputStreamListener {
        public void streamClosed(long fileSize, String filePath);
    }

    public CachingInputStream(InputStream in) {
        super(in);
    }

    public CachingInputStream(String filePath, InputStream inputStream, KInputStreamListener listener) {
        super(inputStream);
        mFilePath = filePath;
        mListener = listener;
    }

    private BufferedOutputStream getOutputStream() throws FileNotFoundException {
        if (mOutputStream == null) {
            mOutputStream = new BufferedOutputStream(new FileOutputStream(mFilePath));
        }
        return mOutputStream;
    }

    @Override
    public void close() throws IOException {
        getOutputStream().close();
        super.close();
        mListener.streamClosed(fileSize, mFilePath);
    }

    @Override
    public synchronized int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        int bytesRead = super.read(buffer, byteOffset, byteCount);
        fileSize += bytesRead;
        if (bytesRead > 0) {
            getOutputStream().write(buffer, 0, bytesRead);
        }
        return bytesRead;
    }
}
