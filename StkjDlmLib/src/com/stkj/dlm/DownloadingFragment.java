package com.stkj.dlm;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.CursorAdapter;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.stkj.dlm.DownloadManager.Status;

public class DownloadingFragment extends NetworkReceiverFragment {

	private ListView mListView;
	private DownloadAdapter mAdapter;
	private Observer mDownloadObserver;

	public static final int IDD = R.id.btnContinue;

	@Override
	protected void onNetworkStateChanged(boolean isWifi) {
		super.onNetworkStateChanged(isWifi);
		if (!isWifi)
			DownloadManager.getInstance().pauseAll();
	}

	@Override
	public void onResume() {
		super.onResume();
		mDownloadObserver = new Observer() {
			@Override
			public void onChanged(State state) {
				if (null != mAdapter) {
					mAdapter.notifyDataSetChanged();
					if (state.status == Status.DELETED) {
						Cursor c = mAdapter.swapCursor(getDownloadingCursor());
						c.close();
					}
				}
			}

		};
		DownloadManager.getInstance()
				.registerDataSetObserver(mDownloadObserver);
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onPause() {
		super.onPause();
		DownloadManager.getInstance().unregisterDataSetObserver(
				mDownloadObserver);
	}

	@Override
	public void onViewDidLoad(Bundle savedInstanceState) {
		mListView = (ListView) findViewById(R.id.listView);
		Cursor cursor = getDownloadingCursor();
		mAdapter = new DownloadAdapter(getActivity(), cursor, 1);
		mListView.setAdapter(mAdapter);
	}

	private Cursor getDownloadingCursor() {
		return DownloadManager.getInstance().openDownloadingCursor();
	}

	@Override
	public int onLoadView() {
		return R.layout.fragment_downloading;
	}

	public class DownloadAdapter extends CursorAdapter {

		private Context mContext;

		@SuppressLint("NewApi")
		public DownloadAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
			mContext = context;
		}

		public Context getContext() {
			return mContext;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			Apk apk = parse(cursor);
			DownloadItemView itemView = (DownloadItemView) view;
			itemView.setData(apk);
			updateUI(itemView, parse(cursor));
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			return new DownloadItemView(context);
		}

		private Apk parse(Cursor cursor) {
			Apk apk = new Apk();
			apk.apkUri = getCursorValue(cursor, DbHelper.URI);
			apk.label = getCursorValue(cursor, DbHelper.DESCRIPTION);
			apk.pkgName = getCursorValue(cursor, DbHelper.PACKAGE_NAME);
			apk.iconUri = getCursorValue(cursor, DbHelper.ICON_URI);
			apk.apkSize = getCursorValue(cursor, DbHelper.APK_SIZE);
			return apk;
		}

		private String getCursorValue(Cursor cursor, String columnName) {
			return cursor.getString(cursor.getColumnIndex(columnName));
		}

		private void updateUI(DownloadItemView appItemView, Apk apk) {
			State state = DownloadManager.getInstance().query(apk.pkgName);
			boolean isFileExist = state.getFile().exists();

			// set progress
			int progress = state.totalSize == -1 ? 0
					: (int) ((float) state.currentSize
							/ (float) state.totalSize * 100f);
			appItemView.setProgress(progress);
			switch (state.status) {
			case DOWNLOADING:
				appItemView.showPause();
				break;
			case INSTALLED:
				if (isFileExist) {
					appItemView.showOpen();
				} else {
					appItemView.showDownload();
				}
				break;
			case FINISHED:
				appItemView.setProgress(100);
				if (isFileExist) {
					appItemView.showInstall();
				} else {
					appItemView.showDownload();
				}
				break;

			case WAITING:
			case PENDING:
				appItemView.showPause();
				break;

			case FAILED:
				appItemView.showRetry();
				break;
			case PAUSED:
				appItemView.showDownload();
				break;

			default:
				appItemView.showDownload();
				break;
			}
		}

	}

	public class DownloadItemView extends FrameLayout implements
			OnClickListener, OnCheckedChangeListener {
		private NetworkImageView mAppIconImageView;
		private Button mInstallButton;
		private Button mOpenButton;
		private Button mContinueButton;
		private TextView mAppNameTextView;
		private TextView mAppSizeTextView;
		private TextProgressBar mDownloadProgressBar;

		public DownloadItemView(Context context) {
			super(context);
			loadContent();
		}

		public void setProgress(int progress) {
			mDownloadProgressBar.setProgress(progress);
		}

		private void loadContent() {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layoutInflater.inflate(R.layout.item_app_downloading, this, true);
			onViewDidLoad();
		}

		public DownloadItemView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		private void onViewDidLoad() {
			mAppIconImageView = (NetworkImageView) findViewById(R.id.appIcon);
			mAppIconImageView.setDefaultImageResId(R.drawable.ic_launcher);
			mAppIconImageView.setErrorImageResId(R.drawable.ic_launcher);

			mContinueButton = (Button) findViewById(R.id.btnContinue);
			mDownloadProgressBar = (TextProgressBar) findViewById(R.id.progressDownload);

			mInstallButton = (Button) findViewById(R.id.btnInstall);
			mOpenButton = (Button) findViewById(R.id.btnOpen);
			mAppNameTextView = (TextView) findViewById(R.id.appName);
			mAppSizeTextView = (TextView) findViewById(R.id.appSize);

			mContinueButton.setOnClickListener(this);
			mOpenButton.setOnClickListener(this);
			mInstallButton.setOnClickListener(this);
			mDownloadProgressBar.setOnClickListener(this);
			mDownloadProgressBar.setOnClickListener(this);
			findViewById(R.id.appBox).setOnClickListener(this);
			findViewById(R.id.btnRetry).setOnClickListener(this);
			findViewById(R.id.btnPause).setOnClickListener(this);
			findViewById(R.id.downloadingItemContainer)
					.setOnClickListener(this);
			findViewById(R.id.appName).setOnClickListener(this);
			findViewById(R.id.appSize).setOnClickListener(this);

		}

		private void setData(Apk apk) {
			setTag(apk);
			mAppNameTextView.setText(apk.label);
			mAppSizeTextView.setText(apk.apkSize);

			if (apk.iconUri == null) {
				mAppIconImageView.setImageResource(R.drawable.ic_launcher);
			} else {
				 mAppIconImageView.setImageUrl(apk.iconUri, getImageLoader());
			}

		}

		public void showInstall() {
			visible(R.id.btnInstall);
			gone(R.id.btnContinue, R.id.btnOpen, R.id.btnRetry, R.id.btnPause);
		}

		public void showDownload() {
			visible(R.id.btnContinue);
			gone(R.id.btnInstall, R.id.btnOpen, R.id.btnRetry, R.id.btnPause);
		}

		public void showOpen() {
			visible(R.id.btnOpen);
			gone(R.id.btnInstall, R.id.btnContinue, R.id.btnRetry,
					R.id.btnPause);
		}

		public void showRetry() {
			visible(R.id.btnRetry);
			gone(R.id.btnInstall, R.id.btnContinue, R.id.btnOpen, R.id.btnPause);
		}

		public void showPause() {
			visible(R.id.btnPause);
			gone(R.id.btnInstall, R.id.btnContinue, R.id.btnOpen, R.id.btnRetry);
		}

		public void gone(int... id) {
			for (int vid : id) {
				findViewById(vid).setVisibility(View.GONE);
			}
		}

		public void visible(int... id) {
			for (int vid : id) {
				findViewById(vid).setVisibility(View.VISIBLE);
			}
		}

		@Override
		public void onClick(View v) {

			final Apk apk = (Apk) getTag();
			int id = v.getId();
			if (id == R.id.btnContinue)
				resumeIfNecessary(apk);

			if (id == R.id.btnInstall) {
				State state = DownloadManager.getInstance().query(apk.pkgName);
				Util.install(getContext(), Uri.fromFile(state.getFile()));
			}
			if (id == R.id.btnOpen) {
				Util.openByPackageName(getContext(), apk.pkgName);
			}
			if (id == R.id.btnRetry) {
				DownloadManager.getInstance().resume(apk.pkgName);
			}
			if (id == R.id.btnPause) {
				DownloadManager.getInstance().pause(apk.pkgName);
				showDownload();
			}
			// 取消下载
			if (id == R.id.downloadingItemContainer || id == R.id.appBox
					|| id == R.id.progressDownload || id == R.id.appSize
					|| id == R.id.appName) {
				OptionPopupMenu om = new OptionPopupMenu(getContext(), v,
						"取消下载") {
					@Override
					public void onMenuDelete() {
						delete(apk.pkgName);
					}
				};
				om.show();
			}
		}

		public void resumeIfNecessary(final Apk apk) {
			if (DownloadManager.getInstance().query(apk.pkgName).status == Status.PAUSED) {
				DownloadManager.getInstance().resume(apk.pkgName);
			} else {
				setProgress(0);
				DownloadManager.getInstance().enqueue(apk.pkgName,
						Uri.parse(apk.apkUri), apk.label, apk.iconUri,
						apk.apkSize);
			}
		}

		private void delete(String pkgName) {
			DownloadManager.getInstance().delete(pkgName);
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
		}
	}

	public ImageLoader getImageLoader() {
		Application app = getActivity().getApplication();
		return app instanceof ILoaderApplication ? ((ILoaderApplication) app)
				.getImageLoader() : null;
	}
}
