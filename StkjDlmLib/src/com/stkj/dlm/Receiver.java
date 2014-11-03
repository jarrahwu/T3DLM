package com.stkj.dlm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

public class Receiver extends BroadcastReceiver {
	public static final String ACTION_ALARM = "com.stkj.android.dlm.ACTION_ALARM";
	public static final String ACTION_PROGRESS = "com.stkj.android.dlm.ACTION_PROGRESS";
	public static final String EXTRA_ID = "extra_id";

	@Override
	public void onReceive(Context context, Intent intent) {

		if (null == intent) {
			return;
		}
		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())
				|| ACTION_ALARM.equals(intent.getAction())) {
			Intent service = new Intent(context, Service.class);
			service.setAction(intent == null ? ConnectivityManager.CONNECTIVITY_ACTION
					: intent.getAction());
			service.putExtras(intent);
			context.startService(service);
		} else if (ACTION_PROGRESS.equals(intent.getAction())
				|| Intent.ACTION_PACKAGE_ADDED.equals(intent.getAction())
				|| Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
			State state;
			if (ACTION_PROGRESS.equals(intent.getAction())) {
				long id = intent.getLongExtra(EXTRA_ID, -1);
				state = DownloadManager.getInstance().query(id);
			} else {
				state = new State();
			}
			DownloadManager.getInstance().init(context)
					.notifyDataSetChanged(state);
		}
	}

}
