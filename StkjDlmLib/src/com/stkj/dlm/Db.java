package com.stkj.dlm;

import java.io.File;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.stkj.dlm.DownloadManager.Status;


public final class Db {
	private static final int MAX_RETRY = 10;
	private final Context mContext;
	protected final DbHelper mDbHelper;

	protected Db(Context context) {
		mContext = context;
		mDbHelper = DbHelper.getInstance(context);
	}

	protected int delete(String pkgName) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.delete(DbHelper.TABLE_NAME, DbHelper.PACKAGE_NAME + "=?",
				new String[] { pkgName, });
	}

	protected long insert(String pkgName, Uri uri, File targetDir,
			String description, Status status, long current, long total,
			String iconUri, String apkSize, boolean installOnFinished) {
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.PACKAGE_NAME, pkgName);
		cv.put(DbHelper.URI, uri.toString());
		cv.put(DbHelper.DESTINATION, targetDir.getAbsolutePath());
		cv.put(DbHelper.DESCRIPTION, description);
		cv.put(DbHelper.STATUS, status.ordinal());
		cv.put(DbHelper.CURRENT_SIZE, current);
		cv.put(DbHelper.TOTAL_SIZE, total);
		cv.put(DbHelper.ICON_URI, iconUri);
		cv.put(DbHelper.APK_SIZE, apkSize);
		cv.put(DbHelper.INSTALL, installOnFinished ? 1 : 0);
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.insert(DbHelper.TABLE_NAME, DbHelper.ID, cv);
	}

	protected ContentValues query(String pkgName) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return query(db, DbHelper.PACKAGE_NAME + "=?",
				new String[] { pkgName, }, "1");
	}

	protected ContentValues query(long id) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return query(db, DbHelper.ID + "=?",
				new String[] { Long.toString(id) }, null);
	}

	protected ContentValues unfinished() {
		ContentValues ret = DownloadManager.EMPTY;
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		Cursor c = db.query(
				DbHelper.TABLE_NAME,
				new String[] { DbHelper.ID, DbHelper.PACKAGE_NAME,
						DbHelper.URI, DbHelper.DESCRIPTION, DbHelper.STATUS,
						DbHelper.CURRENT_SIZE, DbHelper.TOTAL_SIZE,
						DbHelper.RETRY, DbHelper.DESTINATION,
						DbHelper.DESCRIPTION },
				DbHelper.STATUS + " in (?,?,?,?)",
				new String[] { Integer.toString(Status.PENDING.ordinal()),
						Integer.toString(Status.WAITING.ordinal()),
						Integer.toString(Status.DOWNLOADING.ordinal()),
						Integer.toString(Status.PAUSED.ordinal()), }, null,
				null, null);
		if (null != c) {
			try {
				int n = 0;
				String desc = "";
				while (c.moveToNext()) {
					if (n < 2)
						desc += (n == 0 ? "" : ",") + c.getString(9);
					n++;
				}
				if (n > 0) {
					ret = new ContentValues();
					ret.put(DbHelper.DESCRIPTION,
							mContext.getString(R.string.unfinished, desc, n));
				}
			} finally {
				c.close();
			}
		}

		return ret;
	}

	protected int setStatus(long id, Status status) {
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.STATUS, status.ordinal());
		return update(cv, id);
	}

	protected int setStatus(String pkgName, Status status) {
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.STATUS, status.ordinal());
		return update(cv, pkgName);
	}

	protected int setResume(String pkgName) {
		return setResume(pkgName, false);
	}

	protected int setResume(String pkgName, boolean paused) {
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.STATUS, paused ? Status.PAUSED.ordinal()
				: Status.PENDING.ordinal());
		cv.put(DbHelper.RETRY, 0);
		return update(cv, pkgName);
	}

	protected int setRedownload(long id, String pkgName, Uri uri, File destDir,
			String description, boolean paused) {
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.STATUS, paused ? Status.PAUSED.ordinal()
				: Status.PENDING.ordinal());
		cv.put(DbHelper.RETRY, 0);
		cv.put(DbHelper.CURRENT_SIZE, 0);
		cv.put(DbHelper.TOTAL_SIZE, -1);
		cv.put(DbHelper.PACKAGE_NAME, pkgName);
		cv.put(DbHelper.URI, uri.toString());
		cv.put(DbHelper.DESTINATION, destDir.getAbsolutePath());
		cv.put(DbHelper.DESCRIPTION, description);
		return update(cv, id);
	}

	protected void setLastStatus(long id, Status status, long now) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		String sql = "UPDATE " + DbHelper.TABLE_NAME + " SET "
				+ DbHelper.STATUS + "=?," + DbHelper.LAST_TIMESTAMP
				+ "=datetime(?, 'unixepoch') WHERE " + DbHelper.ID + "=?";
		db.execSQL(sql, new Object[] { status.ordinal(), now, id });
	}

	protected void setFileSize(long id, long current) {
		setFileSize(id, current, -1);
	}

	protected void setFileSize(long id, long current, long total) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		long now = System.currentTimeMillis() / 1000;
		String sql;
		Object[] objs;
		if (current != total) {
			sql = "UPDATE " + DbHelper.TABLE_NAME + " SET "
					+ DbHelper.CURRENT_SIZE + "=?," + DbHelper.TOTAL_SIZE
					+ "=?," + DbHelper.LAST_TIMESTAMP
					+ "=datetime(?, 'unixepoch') WHERE " + DbHelper.ID + "=?";
			objs = new Object[] { current, total, now, id };
			db.execSQL(sql, objs);
		} else {
			sql = "UPDATE " + DbHelper.TABLE_NAME + " SET "
					+ DbHelper.CURRENT_SIZE + "=?," + DbHelper.TOTAL_SIZE
					+ "=?," + DbHelper.STATUS + "=?," + DbHelper.LAST_TIMESTAMP
					+ "=datetime(?, 'unixepoch') WHERE " + DbHelper.ID + "=?";
			objs = new Object[] { current, total, Status.FINISHED.ordinal(),
					now, id };
			db.execSQL(sql, objs);
		}
	}

	protected int update(ContentValues cv, long id) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.update(DbHelper.TABLE_NAME, cv, DbHelper.ID + "=?",
				new String[] { Long.toString(id), });
	}

	private int update(ContentValues cv, String pkgName) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.update(DbHelper.TABLE_NAME, cv, DbHelper.PACKAGE_NAME + "=?",
				new String[] { pkgName, });
	}

	protected ContentValues takePending(Integer token) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();

		ContentValues ret = query(db, DbHelper.STATUS + "=? and "
				+ DbHelper.TOKEN + "=?",
				new String[] { Integer.toString(Status.DOWNLOADING.ordinal()),
						token.toString() }, "1");
		if (DownloadManager.EMPTY != ret) {
			return ret;
		}
		ret = query(db, DbHelper.STATUS + "=?",
				new String[] { Integer.toString(Status.PENDING.ordinal()) },
				"1");
		if (DownloadManager.EMPTY == ret) {
			return ret;
		}
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.STATUS, Status.DOWNLOADING.ordinal());
		cv.put(DbHelper.TOKEN, token);
		db.update(DbHelper.TABLE_NAME, cv, DbHelper.ID + "=?",
				new String[] { Long.toString(ret.getAsLong(DbHelper.ID)), });
		ret.put(DbHelper.STATUS, Status.DOWNLOADING.ordinal());
		return ret;
	}

	static ContentValues query(SQLiteDatabase db, String whereClause,
			String[] whereArgs, String limit) {
		ContentValues ret = DownloadManager.EMPTY;
		Cursor c = db.query(DbHelper.TABLE_NAME, new String[] { DbHelper.ID,
				DbHelper.PACKAGE_NAME, DbHelper.URI, DbHelper.DESCRIPTION,
				DbHelper.STATUS, DbHelper.CURRENT_SIZE, DbHelper.TOTAL_SIZE,
				DbHelper.RETRY, DbHelper.DESTINATION, DbHelper.DESCRIPTION,
				DbHelper.TOKEN, DbHelper.INSTALL }, whereClause, whereArgs,
				null, null, null, limit);
		if (null != c) {
			try {
				if (c.moveToFirst()) {
					ret = new ContentValues();
					ret.put(DbHelper.ID, c.getInt(0));
					ret.put(DbHelper.PACKAGE_NAME, c.getString(1));
					ret.put(DbHelper.URI, c.getString(2));
					ret.put(DbHelper.DESCRIPTION, c.getString(3));
					ret.put(DbHelper.STATUS, c.getInt(4));
					ret.put(DbHelper.CURRENT_SIZE, c.getInt(5));
					ret.put(DbHelper.TOTAL_SIZE, c.getInt(6));
					ret.put(DbHelper.RETRY, c.getInt(7));
					ret.put(DbHelper.DESTINATION, c.getString(8));
					ret.put(DbHelper.DESCRIPTION, c.getString(9));
					ret.put(DbHelper.TOKEN, c.getInt(10));
					ret.put(DbHelper.INSTALL, c.getInt(11));
				}
			} finally {
				c.close();
			}
		}
		return ret;
	}

	protected int deleteAll() {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.delete(DbHelper.TABLE_NAME, "1", null);
	}

	public void houseKeeping() {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.STATUS, Status.FAILED.ordinal());
		db.update(DbHelper.TABLE_NAME, cv, DbHelper.STATUS + "=? and "
				+ DbHelper.RETRY + ">=?",
				new String[] { Integer.toString(Status.WAITING.ordinal()),
						Integer.toString(MAX_RETRY) });
		String sql = "UPDATE " + DbHelper.TABLE_NAME + " SET "
				+ DbHelper.STATUS + "=?," + DbHelper.RETRY + "= "
				+ DbHelper.RETRY + "+ 1 WHERE " + DbHelper.STATUS
				+ "=? and (strftime('%s', 'now') - strftime('%s', "
				+ DbHelper.LAST_TIMESTAMP + ") >= 30) and " + DbHelper.RETRY
				+ " < ?";
		db.execSQL(
				sql,
				new Object[] { Status.PENDING.ordinal(),
						Status.WAITING.ordinal(), MAX_RETRY });
	}

	public int setAllPaused() {
		ContentValues cv = new ContentValues();
		cv.put(DbHelper.STATUS, Status.PAUSED.ordinal());
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		return db.update(
				DbHelper.TABLE_NAME,
				cv,
				DbHelper.STATUS + " in (?,?,?)",
				new String[] { Integer.toString(Status.PENDING.ordinal()),
						Integer.toString(Status.WAITING.ordinal()),
						Integer.toString(Status.DOWNLOADING.ordinal()), });
	}
}
