package com.example.finanzas.data.local;

import android.content.Context;

import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.finanzas.data.local.room.AppRoomDatabase;

public class LocalDatabase {

    public static final String DB_NAME = "finanzas_local.db";

    private static LocalDatabase instance;
    private final Context context;
    private final AppRoomDatabase room;

    public static synchronized LocalDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new LocalDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private LocalDatabase(Context context) {
        this.context = context;
        this.room = AppRoomDatabase.build(context);
    }

    public SupportSQLiteDatabase getReadableDatabase() {
        return room.getOpenHelper().getReadableDatabase();
    }

    public SupportSQLiteDatabase getWritableDatabase() {
        return room.getOpenHelper().getWritableDatabase();
    }

    public AppRoomDatabase getRoom() {
        return room;
    }

    public Context getContext() {
        return context;
    }
}
