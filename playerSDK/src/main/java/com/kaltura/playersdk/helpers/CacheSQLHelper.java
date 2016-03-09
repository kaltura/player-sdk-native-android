package com.kaltura.playersdk.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by nissimpardo on 26/10/15.
 */
public class CacheSQLHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "KCache.db";
    private final String TABLE_NAME = "KCacheTable";
    private final String id = "_id";
    public static final String Encoding = "Encoding";
    public static final String MimeType = "MimeType";
    public static final String LastUsed = "LastUsed";
    public static final String Size = "Size";

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



    public void addFile(String fileName, String mimeType, String encoding) {
        ContentValues values = new ContentValues();
        values.put(id, fileName);
        values.put(MimeType, mimeType);
        values.put(Encoding, encoding);
        values.put(LastUsed, 0);
        values.put(Size, 0);
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLiteException e){
            e.printStackTrace();
        } finally {
            if(db.isOpen()){
                db.close();
            }
        }
    }

    public boolean removeFile(String fileId) {
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.delete(TABLE_NAME, id + "=?", new String[]{fileId});
            return true;
        } catch (SQLiteException e) {
            return false;
        } finally {
            if (db.isOpen()) {
                db.close();
            }
        }
    }

    public long cacheSize() {
        String query = "SELECT SUM(Size) FROM KCacheTable";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        long size = 0;
        try {
            if (cursor.moveToFirst()) {
                size = cursor.getInt(0);
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }
        return size;
    }

    public void updateDate(String fileId) {
        ContentValues data = new ContentValues();
        data.put(LastUsed, System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.update(TABLE_NAME, data, id + "=?", new String[]{fileId});
        } catch (SQLiteException e){
            e.printStackTrace();
        } finally {
            if(db.isOpen()){
                db.close();
            }
        }
    }

    public void updateFileSize(String fileId, long fileSize) {
        ContentValues data = new ContentValues();
        data.put(Size, fileSize);
        data.put(LastUsed, System.currentTimeMillis());
        SQLiteDatabase db = getWritableDatabase();
        try {
            db.update(TABLE_NAME, data, id + "=?", new String[]{fileId});
        } catch (SQLiteException e){
            e.printStackTrace();
        } finally {
            if(db.isOpen()){
                db.close();
            }
        }
    }

    public long sizeForId(String fileId) {
        String query = "Select Size FROM " + TABLE_NAME + " WHERE " + id + " =  \"" + fileId + "\"";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor.moveToFirst()) {
            int size = 0;
            try {
                size = cursor.getInt(0);
            } catch (SQLiteException e) {
                e.printStackTrace();
            } finally {
                if (!cursor.isClosed()) {
                    cursor.close();
                }
            }
            return (long)size;
        }
        return 0;
    }

    public void deleteLessUsedFiles(long sizeToDelete, KSQLHelperDeleteListener listener) {
        String query = "SELECT * FROM KCacheTable ORDER BY LastUsed Desc";
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        boolean stop = false;
        ArrayList<String> deletedIds = new ArrayList<>();
        while (!stop) {
            if (cursor.moveToFirst()) {
                int size = cursor.getInt(4);
                String fileId = cursor.getString(0);
                sizeToDelete -= size;
                stop = sizeToDelete <= 0;
                deletedIds.add(fileId);
            }
        }
        for (String fileId: deletedIds) {
            try {
                db.delete(TABLE_NAME, id + "=?", new String[]{fileId});
                listener.fileDeleted(fileId);
            } catch (SQLiteException e) {
                e.printStackTrace();
            } finally {
                if (db.isOpen()) {
                    db.close();
                }
            }

        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
    }

    public HashMap<String, Object> fetchParamsForFile(String fileName) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, new String[]{id, Encoding,MimeType}, id + "=?", new String[]{fileName}, null, null, null);

        HashMap<String , Object> params = null;
        try {
            if (cursor.moveToFirst()) {
                params = new HashMap<>();
                params.put(Encoding, cursor.getString(1));
                params.put(MimeType, cursor.getString(2));
            }
        } catch (SQLiteException e) {
            e.printStackTrace();
        } finally {
            if (!cursor.isClosed()) {
                cursor.close();
            }
        }
        return params;
    }
}
