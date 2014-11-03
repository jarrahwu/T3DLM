package com.stkj.dlm;

import java.io.File;

import android.Manifest.permission;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat.Builder;


public class DownloadManager {
	private static final DownloadManager INSTANCE = new DownloadManager();

	public enum Status {
		UNKNOWN, PENDING, DOWNLOADING, FAILED, FINISHED, CANCELED, INSTALLED, WAITING, PAUSED, DELETED,
	};

	static final ContentValues EMPTY = new ContentValues();

	private final Observable mDataSetObservable;
	private final Receiver mReceiver;
	private Context mContext;
	protected Db mDb;
	private String[] mSubDir;
	private boolean mOpenOnFinished;
	private NotificationManager mNotificationManager;
	private Builder mSummaryBuilder;
	private Builder mBuilder;
	private PendingIntent mPendingIntentDownloadManager;

	private DownloadManager() {
		mDataSetObservable = new Observable();
		mReceiver = new Receiver();
		mSubDir = new String[0];
		mOpenOnFinished = true;
	}

	public static final DownloadManager getInstance() {
		return INSTANCE;
	}

	public static final boolean isEmpty(ContentValues cv) {
		return cv == EMPTY;
	}

	public void registerDataSetObserver(Observer observer) {
		try {
			mDataSetObservable.registerObserver(observer);
		} catch (IllegalStateException e) {
			if (Log.DEBUG) {
				Log.w(e);
			}
		}

	}

	public void unregisterDataSetObserver(Observer observer) {
		try {
			mDataSetObservable.unregisterObserver(observer);
		} catch (IllegalStateException e) {
			if (Log.DEBUG) {
				Log.w(e);
			}
		}

	}

	void notifyDataSetChanged(State state) {
		mDataSetObservable.notifyChanged(state);
		if (state.id >= 0) {
			switch (state.status) {
			case DOWNLOADING:
				if (state.installOnFinished) {
					int progress = (int) ((float) state.currentSize
							/ (float) state.totalSize * 100);
					mBuilder.setContentTitle(state.label)
							.setContentText(progress + "%")
							.setProgress(100, progress, false)
							.setContentIntent(mPendingIntentDownloadManager)
							.setTicker(
									mContext.getString(R.string.start_download,
											state.label));
					mNotificationManager.notify(state.pkgName.hashCode(),
							mBuilder.build());
				}
				break;
			case FINISHED:
				if (state.installOnFinished) {
					mBuilder.setContentTitle(state.label)
							.setContentText(
									mContext.getString(R.string.downloaded))
							.setProgress(100, 100, false)
							.setSmallIcon(
									android.R.drawable.stat_sys_download_done)
							.setContentIntent(
									buildInstallPendingIntent(Uri
											.fromFile(state.getFile())));
					mNotificationManager.notify(state.pkgName.hashCode(),
							mBuilder.build());
				}
				break;
			default:
				mNotificationManager.cancel(state.pkgName.hashCode());
			}
		}
		ContentValues cv = getDb().unfinished();
		if (!isEmpty(cv)) {
			mSummaryBuilder
					.setContentText(cv.getAsString(DbHelper.DESCRIPTION));
			mNotificationManager.notify(1, mSummaryBuilder.build());
		} else {
			mNotificationManager.cancel(1);
		}
	}

	public DownloadManager init(Context context) {
		if (null == context) {
			throw new NullPointerException("context can not be null");
		}
		boolean isInitialized = true;
		synchronized (this) {
			if (null == mContext) {
				isInitialized = false;
				mContext = context.getApplicationContext();
				mDb = new Db(mContext);
			}
		}
		if (!isInitialized) {
			IntentFilter filter = new IntentFilter();
			filter.addAction(Receiver.ACTION_PROGRESS);
			mContext.registerReceiver(mReceiver, filter);
			filter = new IntentFilter();
			filter.addAction(Intent.ACTION_PACKAGE_ADDED);
			filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
			filter.addDataScheme("package");
			mContext.registerReceiver(mReceiver, filter);
			Intent service = new Intent(context, Service.class);
			service.setAction(Receiver.ACTION_ALARM);
			context.startService(service);
			mNotificationManager = (NotificationManager) mContext
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Intent intent = new Intent(mContext, ActivityDownloadManager.class);
			intent.putExtra("tab", 0);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			mPendingIntentDownloadManager = PendingIntent.getActivity(mContext,
					1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
			mSummaryBuilder = new Builder(mContext)
					.setSmallIcon(android.R.drawable.stat_sys_download_done)
					.setContentTitle(
							mContext.getText(R.string.download_manager))
					.setWhen(System.currentTimeMillis())
					.setContentIntent(mPendingIntentDownloadManager)
					.setAutoCancel(true);
			mBuilder = new Builder(mContext)
					.setSmallIcon(android.R.drawable.stat_sys_download)
					.setContentText("%0").setWhen(System.currentTimeMillis())
					.setContentIntent(mPendingIntentDownloadManager)
					.setAutoCancel(true);
		}
		return this;
	}

	public void deinit() {

	}

	public DownloadManager setSubDir(String... subDir) {
		if (null != subDir) {
			mSubDir = subDir;
		}
		return this;
	}

	public DownloadManager setOpenOnFinished(boolean b) {
		this.mOpenOnFinished = b;
		return this;
	}

	protected void onFinished(State state) {
		if (state.installOnFinished && state.getFile().exists()) {
			install(Uri.fromFile(state.getFile()));
		}
	}

	private void install(Uri uri) {
		Intent install = new Intent(Intent.ACTION_VIEW);
		install.setDataAndType(uri, "application/vnd.android.package-archive");
		install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		getContext().startActivity(install);
	}

	private PendingIntent buildInstallPendingIntent(Uri uri) {
		Intent install = new Intent(Intent.ACTION_VIEW);
		install.setDataAndType(uri, "application/vnd.android.package-archive");
		install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		return PendingIntent.getActivity(mContext, 1, install,
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	public long enqueue(String pkgName, Uri uri, String label, String iconUri,
			String apkSize) {
		return enqueue(pkgName, uri, label, iconUri, apkSize, false);
	}

	public long enqueue(String pkgName, Uri uri, String label, String iconUri,
			String apkSize, boolean force) {
		return enqueue(pkgName, uri, label, iconUri, apkSize, force, false);
	}

	public long enqueue(String pkgName, Uri uri, String label, String iconUri,
			String apkSize, boolean force, boolean paused) {
		return enqueue(pkgName, uri, label, iconUri, apkSize, force, false,
				mOpenOnFinished);
	}

	public long enqueue(String pkgName, Uri uri, String label, String iconUri,
			String apkSize, boolean force, boolean paused,
			boolean installOnFinished) {
		State state = new State();
		state.installOnFinished = installOnFinished;
		File destDir;
		if (hasSdCard(getContext())) {
			destDir = buildExternalPath(mSubDir);
		} else {
			destDir = buildInternalPath(getContext(), mSubDir);
		}
		state.path = destDir.getAbsolutePath();
		state.pkgName = pkgName;
		state.label = label;

		boolean isFileExists = state.getFile().exists();
		if (Log.DEBUG) {
			Log.d("DownloadManager enqueue: pkgName=" + pkgName + " uri=" + uri
					+ " label=" + label + " isFileExists=" + isFileExists);
		}
		if (force && isFileExists) {
			state.getFile().delete();
			isFileExists = false;
		}
		ContentValues cv = getDb().query(pkgName);
		if (!isEmpty(cv)) {
			state.id = cv.getAsLong(DbHelper.ID);
			if (isFileExists) {
				state.status = Status.FINISHED;
				state.currentSize = state.getFile().length();
				state.totalSize = state.currentSize;
				mDb.setFileSize(state.id, state.currentSize, state.totalSize);
			} else {
				state.status = Status.values()[cv.getAsInteger(DbHelper.STATUS)];
				state.currentSize = cv.getAsLong(DbHelper.CURRENT_SIZE);
				state.totalSize = cv.getAsLong(DbHelper.TOTAL_SIZE);
				if (Status.FAILED == state.status) {
					mDb.setResume(pkgName, paused);
					state.status = paused ? Status.PAUSED : Status.PENDING;
					state.currentSize = cv.getAsLong(DbHelper.CURRENT_SIZE);
					state.totalSize = cv.getAsLong(DbHelper.TOTAL_SIZE);
				} else if (Status.FINISHED == state.status) {
					getDb().setRedownload(state.id, pkgName, uri, destDir,
							label, paused);
					state.status = paused ? Status.PAUSED : Status.PENDING;
					state.currentSize = 0;
					state.totalSize = -1;
				}
			}
		} else {
			if (isFileExists) {
				state.status = Status.FINISHED;
				state.currentSize = state.getFile().length();
				state.totalSize = state.currentSize;
			} else {
				state.status = paused ? Status.PAUSED : Status.PENDING;
				state.currentSize = 0;
				state.totalSize = -1;
			}
			long id = getDb().insert(pkgName, uri, destDir, label,
					state.status, state.currentSize, state.totalSize, iconUri,
					apkSize, state.installOnFinished);
			state.id = id;
		}
		this.notifyDataSetChanged(state);
		if (Status.FINISHED == state.status) {
			onFinished(state);
		}
		startDownloadService();
		if (Log.DEBUG) {
			Log.d("enqueue ret=" + state);
		}
		return state.id;
	}

	private void startDownloadService() {
		Intent service = new Intent(getContext(), Service.class);
		getContext().startService(service);
	}

	public State query(String pkgName) {
		State ret = new State();
		Context ctx = getContext();
		ContentValues cv = getDb().query(pkgName);
		PackageManager pm = ctx.getPackageManager();
		try {
			pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA);
			if (!isEmpty(cv)
					&& Status.FINISHED == Status.values()[cv
							.getAsInteger(DbHelper.STATUS)]) {
				ret.id = cv.getAsLong(DbHelper.ID);
				ret.status = Status.INSTALLED;
				ret.currentSize = cv.getAsLong(DbHelper.CURRENT_SIZE);
				ret.totalSize = cv.getAsLong(DbHelper.TOTAL_SIZE);
				ret.path = cv.getAsString(DbHelper.DESTINATION);
				ret.pkgName = cv.getAsString(DbHelper.PACKAGE_NAME);
				ret.label = cv.getAsString(DbHelper.DESCRIPTION);
				ret.uriString = cv.getAsString(DbHelper.URI);
				ret.token = cv.getAsInteger(DbHelper.TOKEN);
				ret.installOnFinished = cv.getAsInteger(DbHelper.INSTALL) == 1;
				if (ret.getFile().exists()) {
					return ret;
				}
			}
		} catch (NameNotFoundException e) {
		}
		if (!isEmpty(cv)) {
			ret.id = cv.getAsLong(DbHelper.ID);
			ret.status = Status.values()[cv.getAsInteger(DbHelper.STATUS)];
			ret.currentSize = cv.getAsLong(DbHelper.CURRENT_SIZE);
			ret.totalSize = cv.getAsLong(DbHelper.TOTAL_SIZE);
			ret.path = cv.getAsString(DbHelper.DESTINATION);
			ret.pkgName = cv.getAsString(DbHelper.PACKAGE_NAME);
			ret.label = cv.getAsString(DbHelper.DESCRIPTION);
			ret.uriString = cv.getAsString(DbHelper.URI);
			ret.token = cv.getAsInteger(DbHelper.TOKEN);
			ret.installOnFinished = cv.getAsInteger(DbHelper.INSTALL) == 1;
		}
		return ret;
	}

	public State query(long id) {
		State ret = new State();
		ContentValues cv = getDb().query(id);
		if (!isEmpty(cv)) {
			ret.id = id;
			ret.status = Status.values()[cv.getAsInteger(DbHelper.STATUS)];
			ret.currentSize = cv.getAsLong(DbHelper.CURRENT_SIZE);
			ret.totalSize = cv.getAsLong(DbHelper.TOTAL_SIZE);
			ret.path = cv.getAsString(DbHelper.DESTINATION);
			ret.pkgName = cv.getAsString(DbHelper.PACKAGE_NAME);
			ret.label = cv.getAsString(DbHelper.DESCRIPTION);
			ret.uriString = cv.getAsString(DbHelper.URI);
			ret.token = cv.getAsInteger(DbHelper.TOKEN);
			ret.installOnFinished = cv.getAsInteger(DbHelper.INSTALL) == 1;
		}
		return ret;
	}

	private void doNotify() {
		State state = new State();
		notifyDataSetChanged(state);
	}

	public boolean pause(String pkgName) {
		int n = getDb().setStatus(pkgName, Status.PAUSED);
		doNotify();
		return n == 1;
	}

	public void pauseAll() {
		getDb().setAllPaused();
		doNotify();
	}

	public boolean resume(String pkgName) {
		int n = getDb().setResume(pkgName);
		doNotify();
		startDownloadService();
		return n == 1;
	}

	public Cursor openDownloadedCursor() {
		return mDb.mDbHelper.getReadableDatabase().query(
				DbHelper.TABLE_NAME,
				new String[] { DbHelper.ID, DbHelper.PACKAGE_NAME,
						DbHelper.DESCRIPTION, DbHelper.ICON_URI, DbHelper.URI,
						DbHelper.APK_SIZE },
				DbHelper.STATUS + "=?",
				new String[] { String.valueOf(DownloadManager.Status.FINISHED
						.ordinal()) }, null, null, null);
	}

	public Cursor openDownloadingCursor() {
		return mDb.mDbHelper.getReadableDatabase().query(
				DbHelper.TABLE_NAME,
				new String[] { DbHelper.ID, DbHelper.PACKAGE_NAME,
						DbHelper.DESCRIPTION, DbHelper.ICON_URI, DbHelper.URI,
						DbHelper.APK_SIZE },
				DbHelper.STATUS + "<>?",
				new String[] { String.valueOf(DownloadManager.Status.FINISHED
						.ordinal()) }, null, null, null);
	}

	public void delete(String pkgName) {
		State state = query(pkgName);
		if (state.id >= 0) {
			mDb.delete(pkgName);
			state.getFile().delete();
			state.status = Status.DELETED;
			notifyDataSetChanged(state);
		}
	}

	private Context getContext() {
		checkInit();
		synchronized (this) {
			return mContext;
		}
	}

	private Db getDb() {
		checkInit();
		synchronized (this) {
			return mDb;
		}
	}

	private void checkInit() {
		synchronized (this) {
			if (null == mContext || null == mDb) {
				throw new IllegalStateException("invoke setContext first!");
			}
		}
	}

	protected static boolean hasSdCard(Context context) {
		return Environment.getExternalStorageState().equals(
				Environment.MEDIA_MOUNTED)
				&& PackageManager.PERMISSION_GRANTED == context
						.checkCallingOrSelfPermission(permission.WRITE_EXTERNAL_STORAGE);
	}

	protected static File buildExternalPath(String... subDir) {
		File dir = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		for (String s : subDir) {
			dir = new File(dir, s);
		}
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	protected static File buildInternalPath(Context context, String... subDir) {
		File dir = context.getCacheDir();
		for (String s : subDir) {
			dir = new File(dir, s);
		}
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}

	protected static String buildSubPath(String pkgName, String... subDir) {
		String ret = File.separator;
		for (String s : subDir) {
			ret += s + File.separator;
		}
		ret += "." + pkgName;
		return ret;
	}

}
