package com.stkj.dlm;

public class Log {
	private Log() {
	}

	public static final String TAG = "dlm";
	public static final boolean DEBUG = true;

	public static void d(String msg) {
		if (DEBUG) {
			android.util.Log.d(TAG, msg);
		}
	}

	public static void w(String msg) {
		if (DEBUG) {
			android.util.Log.w(TAG, msg);
		}
	}

	public static void w(Throwable t) {
		if (DEBUG) {
			android.util.Log.w(TAG, t);
		}
	}
}
