package android.csulb.edu.crisisconnect.database;

import android.content.Context;
import android.csulb.edu.crisisconnect.database.ChatHistoryContract.Messages;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by vaibhavjain on 5/8/2017
 */

public class MessageHistoryDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "ChatHistory.db";
    private static final String SQL_CREATE_MESSAGE_TABLE =
            "CREATE TABLE " + Messages.TABLE_NAME + " (" +
                    Messages._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    Messages.COLUMN_NAME_SENDER + " TEXT," +
                    Messages.COLUMN_NAME_RECEIVER + " TEXT," +
                    Messages.COLUMN_NAME_MESSAGE_TYPE + " INTEGER," +
                    Messages.COLUMN_NAME_MESSAGE + " TEXT, " +
                    Messages.COLUMN_NAME_LATITUDE + " TEXT," +
                    Messages.COLUMN_NAME_LONGITUDE + " TEXT)";
    private static final String SQL_DROP_MESSAGE_TABLE =
            "DROP TABLE IF EXISTS " + Messages.TABLE_NAME;
    private static SQLiteDatabase dbConn = null;

    public MessageHistoryDbHelper(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        dbConn = getWritableDatabase();
    }

    public static SQLiteDatabase getDBConnection() {
        if (dbConn != null) {
            return dbConn;
        } else {
            throw new NullPointerException("Database Connection not initialized");
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_MESSAGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP_MESSAGE_TABLE);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
}
