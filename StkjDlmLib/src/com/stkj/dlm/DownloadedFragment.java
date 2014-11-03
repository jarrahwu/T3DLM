package com.stkj.dlm;

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
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.stkj.dlm.DownloadManager.Status;


public class DownloadedFragment extends BaseFragment {

	private ListView mListView;
	private DownloadedAdapter mAdapter;
	private Observer mDownloadObserver;

	@Override
	public void onResume() {
		super.onResume();
		mDownloadObserver = new Observer() {
			@Override
			public void onChanged(State state) {
				if (null != mAdapter) {
					mAdapter.notifyDataSetChanged();
					if (state.status == Status.DELETED) {
						Cursor c = mAdapter.swapCursor(getDownloadedCursor());
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

	public void delete(String pkgName) {
		DownloadManager.getInstance().delete(pkgName);
	}

	@Override
	public void onViewDidLoad(Bundle savedInstanceState) {
		mListView = (ListView) findViewById(R.id.listView);
		mAdapter = new DownloadedAdapter(getActivity(), getDownloadedCursor(),
				1);
		mListView.setAdapter(mAdapter);
	}

	private Cursor getDownloadedCursor() {
		return DownloadManager.getInstance().openDownloadedCursor();

	}

	@Override
	public int onLoadView() {
		return R.layout.fragment_downloaded;
	}

	public class DownloadedAdapter extends CursorAdapter {

		public DownloadedAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context arg1, Cursor cursor) {
			Apk apk = parse(cursor);
			DownloadedItemView itemView = (DownloadedItemView) view;
			itemView.setData(apk);
			updateUI(itemView, apk);
		}

		private void updateUI(DownloadedItemView appItemView, Apk apk) {
			State state = DownloadManager.getInstance().query(apk.pkgName);
			// FileDescriptor<Apk> fd = new FileDescriptor<Apk>(
			// Util.getFileType(state.getFile()), state.getFile(), apk);
			boolean isFileExist = state.getFile().exists();

			switch (state.status) {
			case INSTALLED:
				if (isFileExist) {
					appItemView.showOpen();
				} else {
					DownloadManager.getInstance().delete(state.pkgName);
				}
				break;
			case FINISHED:
				if (isFileExist) {
					appItemView.showInstall();
				} else {
					DownloadManager.getInstance().delete(state.pkgName);
				}
				break;
			default:
				appItemView.showOpen();
				break;
			}
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

		@Override
		public View newView(Context arg0, Cursor arg1, ViewGroup arg2) {
			return new DownloadedItemView(arg0);
		}

	}

	public class DownloadedItemView extends FrameLayout implements
			OnClickListener {
		private NetworkImageView mAppIconImageView;
		private Button mOpenButton;
		private TextView mAppNameTextView;
		private TextView mAppSizeTextView;
		private Button mAppInstallButton;
		private ViewGroup mAppBox;
		private TextProgressBar mDownloadProgress;
		private ViewGroup mItemContainer;

		public DownloadedItemView(Context context) {
			super(context);
			loadContent();
		}

		private void loadContent() {
			LayoutInflater layoutInflater = (LayoutInflater) getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layoutInflater.inflate(R.layout.item_app_downloaded, this, true);
			onViewDidLoad();
		}

		public DownloadedItemView(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		private void onViewDidLoad() {
			mAppIconImageView = (NetworkImageView) findViewById(R.id.appIcon);
			mAppIconImageView = (NetworkImageView) findViewById(R.id.appIcon);
			mAppIconImageView.setDefaultImageResId(R.drawable.ic_launcher);
			mAppIconImageView.setErrorImageResId(R.drawable.ic_launcher);
			mOpenButton = (Button) findViewById(R.id.btnOpen);
			mAppNameTextView = (TextView) findViewById(R.id.appName);
			mAppSizeTextView = (TextView) findViewById(R.id.appSize);
			mAppInstallButton = (Button) findViewById(R.id.btnInstall);
			mAppBox = (ViewGroup) findViewById(R.id.appBox);
			mDownloadProgress = (TextProgressBar) findViewById(R.id.progressDownload);
			mItemContainer = (ViewGroup) findViewById(R.id.itemContainer);

			mOpenButton.setOnClickListener(this);
			mAppInstallButton.setOnClickListener(this);
			mAppIconImageView.setOnClickListener(this);
			// 调用删除
			mAppBox.setOnClickListener(this);
			mAppNameTextView.setOnClickListener(this);
			mAppSizeTextView.setOnClickListener(this);
			mDownloadProgress.setOnClickListener(this);
			mItemContainer.setOnClickListener(this);
		}

		public void setData(Apk apk) {
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
			mAppInstallButton.setVisibility(View.VISIBLE);
			mOpenButton.setVisibility(View.GONE);
		}

		public void showOpen() {
			mAppInstallButton.setVisibility(View.GONE);
			mOpenButton.setVisibility(View.VISIBLE);
		}

		@Override
		public void onClick(View v) { // open
			final Apk apk = (Apk) getTag();
			if (v == mOpenButton) {
				if (apk.pkgName != null)
					Util.openByPackageName(getContext(), apk.pkgName);
				else
					Toast.makeText(getActivity(), "打开文件异常", Toast.LENGTH_SHORT).show();
			}
			if (v == mAppInstallButton) {
				State state = DownloadManager.getInstance().query(apk.pkgName);
				Util.install(getContext(), Uri.fromFile(state.getFile()));
			}
			if (v == mAppIconImageView || v == mAppSizeTextView
					|| v == mAppNameTextView || v == mAppBox
					|| v == mDownloadProgress || v == mItemContainer) {
				OptionPopupMenu om = new OptionPopupMenu(getContext(), v) {
					@Override
					public void onMenuDelete() {
						delete(apk.pkgName);
					}
				};
				om.show();
			}
		}
	}

	public ImageLoader getImageLoader() {
		Application application = getActivity().getApplication();
		if (application instanceof ILoaderApplication) {
			ILoaderApplication loaderApplication = (ILoaderApplication) application;
			return loaderApplication.getImageLoader();
		}
		Log.w("can not find application imageloader");
		return null;
	}
}
