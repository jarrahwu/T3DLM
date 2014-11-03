package com.stkj.dlm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;

import com.stkj.dlm.SwitchTab.OnTabClickListener;


public class ActivityDownloadManager extends ActionBarActivity implements
		OnTabClickListener {

	protected Fragment mDownloadingFragment;
	protected Fragment mDwonloadedFragment;
	private Fragment mShowingFragment;
	private SwitchTab mSwitchTab;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download_manager);
		setTitle(R.string.download_manager);
		
		mDownloadingFragment = initDownloadingFragment();
		mDwonloadedFragment = initDownloadedFragment();
		if (0 == getIntent().getIntExtra("tab", 0))
			switchFragment(mDownloadingFragment);
		else
			switchFragment(mDwonloadedFragment);
		mSwitchTab = (SwitchTab) findViewById(R.id.downloadSwitch);
		mSwitchTab.setOnTabClickListener(this);
	}

	public Fragment initDownloadingFragment() {
		return new DownloadingFragment();
	}
	
	public Fragment initDownloadedFragment() {
		return new DownloadedFragment();
	}

	private void switchFragment(Fragment f) {
		if (mShowingFragment != f) {
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragment_container, f)
					.commitAllowingStateLoss();
			mShowingFragment = f;
		}
	}

	public static void start(Activity activity) {
		Intent intent = new Intent(activity, ActivityDownloadManager.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
		activity.startActivity(intent);
	}

	@Override
	public void onLeftTabClick(View v) {
		switchFragment(mDownloadingFragment);
	}

	@Override
	public void onRightTabClick(View v) {
		switchFragment(mDwonloadedFragment);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

//	@Override
//	public void onBackPressed() {
//		super.onBackPressed();
//		Intent intent = NavUtils.getParentActivityIntent(this);
//		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
//				| Intent.FLAG_ACTIVITY_SINGLE_TOP);
//		intent.putExtra("tab", 1);
//		NavUtils.navigateUpTo(this, intent);
//	}
}
