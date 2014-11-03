package com.stkj.dlm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.View;


/**
 * @author jarrah
 * 需要绑定一个textview 用来提示当前网络状态 
 * 并且注册了一个网络监听器
 */


public  abstract class NetworkReceiverFragment extends BaseFragment{
	
	
	public static final int TIPS_TEXTVIEW_ID = R.id.networkTips;
	
	protected boolean mIsWifi;

	// net work change receiver
	public BroadcastReceiver mBrocast = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager manager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			// NetworkInfo activeInfo = manager.getActiveNetworkInfo();
			// NetworkInfo mobileInfo = manager
			// .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			NetworkInfo wifiInfo = manager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			boolean isWifi = wifiInfo == null ? false : wifiInfo
					.isConnectedOrConnecting();
			mIsWifi = isWifi;
			onNetworkStateChanged(isWifi);
		}

	};

	@Override
	public void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter(
				"android.net.conn.CONNECTIVITY_CHANGE");
		getActivity().registerReceiver(mBrocast, filter);
	}

	/**
	 * 当网络状态改变的时候触发
	 * @param isWifi 是否为wifi网络
	 */
	protected void onNetworkStateChanged(boolean isWifi) {
		if(findViewById(TIPS_TEXTVIEW_ID) == null) {
			throw new IllegalArgumentException("CAN NOT FIND TIPS TEXTVIEW ID");
		}
		
		if (isWifi) {
			findViewById(TIPS_TEXTVIEW_ID).setVisibility(View.GONE);
		} else {
			findViewById(TIPS_TEXTVIEW_ID).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		getActivity().unregisterReceiver(mBrocast);
	}

}
