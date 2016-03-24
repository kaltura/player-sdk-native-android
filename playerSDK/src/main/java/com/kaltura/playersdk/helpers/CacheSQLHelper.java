package com.kaltura.playersdk.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nissimpardo on 26/10/15.
 */
public class CacheSQLHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "KCache.db";
    private static final String TAG = "CacheSQLHelper";
    private final String TABLE_NAME = "KCacheTable";
    private final String id = "_id";
    public static final String Encoding = "Encoding";
    public static final String MimeType = "MimeType";
    public static final String LastUsed = "LastUsed";
    public static final String Size = "Size";

    private SQLiteDatabase mDatabase;

    public interface KSQLHelperDeleteListener {
        void fileDeleted(String fileId);
    }

    public CacheSQLHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_PRODUCTS_TABLE = "Create table IF NOT EXISTS " +
                TABLE_NAME + "("
                + id + " TEXT," + Encoding
                + " TEXT," + MimeType + " TEXT," + LastUsed + " INTEGER," + Size + " INTEGER)";
        db.execSQL(CREATE_PRODUCTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    private SQLiteDatabase db() {
        // Keep db open.
        if (mDatabase == null) {
            synchronized (this) {
                if (mDatabase == null) {
                    mDatabase = getWritableDatabase();
                }
            }
        }
        return mDatabase;
    }


    @Override
    protected void finalize() throws Throwable {
        // Finalizers are unpredictable, but Android's CloseGuards use them.
        try {
            if (mDatabase != null) {
                mDatabase.close();
                mDatabase = null;
            }
        } finally {
            super.finalize();
        }
    }

    public void addFile(String fileName, String mimeType, String encoding) {
        ContentValues values = new ContentValues();
        values.put(id, fileName);
        values.put(MimeType, mimeType);
        values.put(Encoding, encoding);
        values.put(LastUsed, 0);
        values.put(Size, 0);
        try {
            if (entryExists(fileName)) {
                db().update(TABLE_NAME, values, this.id + "=?", new String[]{fileName});
            } else {
                db().insert(TABLE_NAME, null, values);
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error adding file, fileName=" + fileName);
        }
    }

    private boolean entryExists(String id) {
        Cursor c = null;
        try {
            c = db().query(TABLE_NAME, null, this.id + "=?", new String[]{id}, null, null, null);
            return c.getCount() > 0;
        } finally {
            quietClose(c);
        }
    }

    public boolean removeFile(String fileId) {
        try {
            db().delete(TABLE_NAME, id + "=?", new String[]{fileId});
            return true;
        } catch (SQLiteException e) {
            return false;
        }
    }

    public long cacheSize() {
        String query = "SELECT SUM(Size) FROM KCacheTable";
        Cursor cursor = null; 
        long size = 0;
        try {
            cursor = db().rawQuery(query, null);
            if (cursor.moveToFirst()) {
                size = cursor.getInt(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting total cache size", e);
        } finally {
            quietClose(cursor);
        }
        
        return size;
    }

    public void updateDate(String fileId) {
        ContentValues data = new ContentValues();
        data.put(LastUsed, System.currentTimeMillis());
        try {
            db().update(TABLE_NAME, data, id + "=?", new String[]{fileId});
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating entry date", e);
        }
    }

    public void updateFileSize(String fileId, long fileSize) {
        ContentValues data = new ContentValues();
        data.put(Size, fileSize);
        data.put(LastUsed, System.currentTimeMillis());
        try {
            db().update(TABLE_NAME, data, id + "=?", new String[]{fileId});
        } catch (SQLiteException e) {
            Log.e(TAG, "Error updating entry size", e);
        }
    }

    private void quietClose(Cursor c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed closing " + c);
        }
    }
    
    public long sizeForId(String fileId) {
        Cursor cursor = null;
        long size = 0;
        try {
            cursor = db().query(TABLE_NAME, new String[]{Size}, "id=?", new String[]{id}, null, null, null);
            if (cursor.moveToFirst()) {
                size = cursor.getInt(0);
            }
        } finally {
            quietClose(cursor);
        }
        return size;
    }

    public void deleteLessUsedFiles(long sizeToDelete, KSQLHelperDeleteListener listener) {
        
        SQLiteDatabase db = db();

        ArrayList<String> deletedIds = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NAME, new String[]{id, Size}, null, null, null, null, LastUsed + " DESC");
            while (cursor.moveToNext()) {
                String fileId = cursor.getString(0);
                int size = cursor.getInt(1);
                deletedIds.add(fileId);
                sizeToDelete -= size;
                if (sizeToDelete <= 0) {
                    break;
                }
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error getting list of files to delete");
        } finally {
            quietClose(cursor);
        }
        
        
        for (String fileId : deletedIds) {
            try {
                db.delete(TABLE_NAME, id + "=?", new String[]{fileId});
                listener.fileDeleted(fileId);
            } catch (SQLiteException e) {
                Log.e(TAG, "Error deleting entry (lessUsed) " + fileId);
            }
        }
    }

    public HashMap<String, Object> fetchParamsForFile(String fileName) {
        SQLiteDatabase db = db();
        Cursor cursor = null;

        HashMap<String, Object> params = null;
        try {
            cursor = db.query(TABLE_NAME, new String[]{id, Encoding, MimeType}, id + "=?", new String[]{fileName}, null, null, null);
            if (cursor.moveToFirst()) {
                params = new HashMap<>();
                params.put(Encoding, cursor.getString(1));
                params.put(MimeType, cursor.getString(2));
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "Error fetching params for " + fileName);
        } finally {
            quietClose(cursor);
        }
        return params;
    }
}
