package com.stkj.dlm;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.PowerManager;

public class WakeLocker {
	private static volatile PowerManager.WakeLock WAKE_LOCK = null;
	private static final String WAKE_LOCK_NAME = "com.stkj.android.dlm.LOCKER";

	private static final synchronized PowerManager.WakeLock getWakeLock(
			Context context) {
		if (!hasPermission(context, permission.WAKE_LOCK)) {
			return null;
		}
		if (WAKE_LOCK == null) {
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			WAKE_LOCK = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
					WAKE_LOCK_NAME);
			WAKE_LOCK.setReferenceCounted(true);
		}

		return (WAKE_LOCK);
	}

	/**
	 * 获取CPU锁
	 * 
	 * @param context
	 */
	public static void lock(Context context) {
		if (hasPermission(context, permission.WAKE_LOCK)) {
			PowerManager.WakeLock wakeLock = getWakeLock(context);
			if (!wakeLock.isHeld()) {
				wakeLock.acquire();
			}
			if (Log.DEBUG) {
				Log.d("lock, wake lock is held " + wakeLock.isHeld());
			}
		} else {
			Log.w("no wake lock!");
		}
	}

	/**
	 * 释放CPU锁
	 * 
	 * @param context
	 */
	public static void unlock(Context context) {
		if (hasPermission(context, permission.WAKE_LOCK)) {
			PowerManager.WakeLock wakeLock = getWakeLock(context);
			if (wakeLock.isHeld()) {
				wakeLock.release();
			}
			if (Log.DEBUG) {
				Log.d("unlock, wake lock is held " + wakeLock.isHeld());
			}
		} else {
			Log.w("no wake lock!");
		}
	}

	private static boolean hasPermission(Context context, String perm) {
		return PackageManager.PERMISSION_GRANTED == context
				.checkCallingOrSelfPermission(perm);
	}

}
