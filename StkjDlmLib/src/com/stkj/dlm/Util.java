package com.stkj.dlm;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

public class Util {

	private Util() {
	}


	public static void openByPackageName(Context context, String pkgName) {
		PackageManager pm = context.getPackageManager();
		Intent intent = pm.getLaunchIntentForPackage(pkgName);

		if (intent == null) {
			Toast.makeText(context, R.string.app_cannot_open,
					Toast.LENGTH_SHORT).show();
			return;
		}

		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		context.startActivity(intent);
	}
	
	public static void install(Context context, Uri uri) {
		Intent install = new Intent(Intent.ACTION_VIEW);
		install.setDataAndType(uri, "application/vnd.android.package-archive");
		install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(install);
	}
}
