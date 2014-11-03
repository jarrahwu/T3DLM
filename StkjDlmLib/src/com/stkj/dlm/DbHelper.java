package com.stkj.dlm;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class DbHelper extends SQLiteOpenHelper {
	private static DbHelper INSTANCE;
	protected static final String DATABASE_NAME = "dlmgr";
	protected static final int DATABASE_VERSION = 4;
	public static final String TABLE_NAME = "download";
	public static final String ID = "_id";
	public static final String PACKAGE_NAME = "name";
	public static final String DESTINATION = "dest";
	public static final String URI = "uri";
	public static final String DESCRIPTION = "desc";
	public static final String STATUS = "status";
	public static final String CURRENT_SIZE = "current";
	public static final String TOTAL_SIZE = "total";
	protected static final String RETRY = "retry";
	protected static final String FIRST_TIMESTAMP = "first";
	protected static final String LAST_TIMESTAMP = "last";
	public static final String ICON_URI = "icon_uri";
	public static final String APK_SIZE = "apk_size";
	public static final String COUNT = "count";
	public static final String TOKEN = "token";
	public static final String INSTALL = "install";

	public static synchronized DbHelper getInstance(Context context) {
		if (null == INSTANCE) {
			INSTANCE = new DbHelper(context);
		}
		return INSTANCE;
	}

	private DbHelper(Context context) {
		this(context, DATABASE_NAME);
	}

	private DbHelper(Context context, String name) {
		super(context, name, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + ID
				+ " INTEGER PRIMARY KEY NOT NULL," + PACKAGE_NAME
				+ " TEXT NOT NULL," + URI + " TEXT NOT NULL," + ICON_URI
				+ " TEXT NULL," + APK_SIZE + " TEXT NULL," + DESTINATION
				+ " TEXT NOT NULL, " + DESCRIPTION
				+ " TEXT NOT NULL DEFAULT ''," + STATUS
				+ " INTEGER NOT NULL DEFAULT '0', " + CURRENT_SIZE
				+ " INTEGER NOT NULL DEFAULT '0', " + TOTAL_SIZE
				+ " INTEGER NOT NULL DEFAULT '-1', " + RETRY
				+ " INTEGER NOT NULL DEFAULT '0', " + INSTALL
				+ " INTEGER NOT NULL DEFAULT '1', " + TOKEN + " INTEGER NULL, "
				+ FIRST_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP, "
				+ LAST_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";
		db.execSQL(sql);
		sql = "CREATE UNIQUE INDEX IF NOT EXISTS idx_file_name on "
				+ TABLE_NAME + " (" + PACKAGE_NAME + ");";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 4) {
			String sqlString = "ALTER TABLE " + TABLE_NAME + " ADD " + INSTALL
					+ " INTEGER NOT NULL DEFAULT '1';";
			db.execSQL(sqlString);
		}
		if (oldVersion < 3) {
			String sqlString = "ALTER TABLE " + TABLE_NAME + " ADD " + TOKEN
					+ " INTEGER NULL;";
			db.execSQL(sqlString);
		}
		if (oldVersion < 2) {
			String sqlString = "ALTER TABLE " + TABLE_NAME + " ADD " + ICON_URI
					+ " TEXT NULL;";
			db.execSQL(sqlString);
			sqlString = "ALTER TABLE " + TABLE_NAME + " ADD " + APK_SIZE
					+ " TEXT NULL;";
			db.execSQL(sqlString);
		}
	}
}
