package com.stkj.dlm;

import android.content.Intent;
import android.os.IBinder;

public class Service extends android.app.Service {
	private DispatcherTask task;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		task = new DispatcherTask(this);
	}

	@Override
	public void onDestroy() {
		if (null != task && task.getStatus() == AsyncTask.Status.RUNNING) {
			task.cancel(true);
		}
		task = null;
		WakeLocker.unlock(this);
		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (Log.DEBUG) {
			Log.d("Service onStartCommand");
		}
		if (null != task && task.getStatus() == AsyncTask.Status.PENDING) {
			WakeLocker.lock(this);
			task.exec(intent);
		}
		return START_FLAG_REDELIVERY;
	}

}
