package com.stkj.dlm;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.SystemClock;

public class DispatcherTask extends AsyncTask<Intent, State, Intent> {
	private static final int HTTP_TIMEOUT = 55000;
	private static final int MAX_DOWNLOADING = 3;
	private static final int WAIT_TOKEN_IN_SECONDS = 1;
	private static final ThreadFactory sThreadFactory = new ThreadFactory() {
		private final AtomicInteger mCount = new AtomicInteger(1);

		public Thread newThread(Runnable r) {
			return new Thread(r, "DownloadManager #" + mCount.getAndIncrement());
		}
	};
	private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(
			128);
	protected static final Executor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
			4, 10, 1, TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);
	private final Service mSvc;
	private final Db mDb;
	private final BlockingQueue<Integer> mTokenQueue;

	protected DispatcherTask(Service svc) {
		mSvc = svc;
		mDb = new Db(mSvc);
		mTokenQueue = new LinkedBlockingQueue<Integer>(MAX_DOWNLOADING + 1);
		for (int i = 0; i < MAX_DOWNLOADING; i++) {
			try {
				putToken(i);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	private boolean hasNetwork() {
		ConnectivityManager cm = (ConnectivityManager) mSvc
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		return null != cm.getActiveNetworkInfo();
	}

	protected Integer getToken() throws InterruptedException {
		return mTokenQueue.poll(WAIT_TOKEN_IN_SECONDS, TimeUnit.SECONDS);
	}

	protected void putToken(Integer token) throws InterruptedException {
		mTokenQueue.put(token);
	}

	@Override
	protected Intent doInBackground(Intent... params) {
		if (Log.DEBUG) {
			Log.d("DownloadManager.DispatcherTask.doInBackground enter");
		}
		if (!hasNetwork()) {
			if (Log.DEBUG) {
				Log.d("DownloadManager.DispatcherTask.doInBackground no network, so exit");
			}
			return params[0];
		}
		boolean priorEmpty = false;
		long emptyTimestamp = 0;
		while (!isCancelled()) {
			mDb.houseKeeping();
			try {
				Integer token = getToken();
				if (null == token) {
					continue;
				}
				ContentValues cv = mDb.takePending(token);
				if (DownloadManager.isEmpty(cv)) {
					putToken(token);
					if (maxIdle(priorEmpty, emptyTimestamp)
							&& mTokenQueue.size() == MAX_DOWNLOADING) {
						if (Log.DEBUG) {
							Log.d("DownloadManager.DispatcherTask.doInBackground too max idle");
						}
						break;
					}
					if (!priorEmpty) {
						priorEmpty = true;
						emptyTimestamp = SystemClock.elapsedRealtime();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						break;
					}
					continue;
				}
				priorEmpty = false;
				State state = new State();
				state.token = token;
				state.id = cv.getAsLong(DbHelper.ID);
				state.status = DownloadManager.Status.values()[cv
						.getAsInteger(DbHelper.STATUS)];
				state.currentSize = cv.getAsLong(DbHelper.CURRENT_SIZE);
				state.totalSize = cv.getAsLong(DbHelper.TOTAL_SIZE);
				state.path = cv.getAsString(DbHelper.DESTINATION);
				state.pkgName = cv.getAsString(DbHelper.PACKAGE_NAME);
				state.label = cv.getAsString(DbHelper.DESCRIPTION);
				state.uriString = cv.getAsString(DbHelper.URI);
				state.installOnFinished = cv.getAsInteger(DbHelper.INSTALL) == 1;
				(new WorkerTask(this)).exec(state);
				if (Log.DEBUG) {
					Log.d("DownloadManager.DispatcherTask.doInBackground start WorkerTask with state="
							+ state);
				}

			} catch (InterruptedException e) {
				Log.w(e);
				break;
			}
		}
		if (Log.DEBUG) {
			Log.d("DownloadManager.DispatcherTask.doInBackground exit");
		}
		return params[0];
	}

	@Override
	protected void onPostExecute(Intent result) {
		super.onPostExecute(result);
		mSvc.stopSelf();
	}

	@Override
	protected void onCancelled() {
		super.onCancelled();
		mSvc.stopSelf();
	}

	private boolean maxIdle(boolean priorEmpty, long emptyTimestamp) {
		return priorEmpty
				&& (SystemClock.elapsedRealtime() - emptyTimestamp > 60000);
	}

	public void exec(Intent intent) {
		executeOnExecutor(DispatcherTask.THREAD_POOL_EXECUTOR, intent);
	}

	private static final class WorkerTask extends
			AsyncTask<State, State, State> {
		private final DispatcherTask mDispatcherTask;

		protected WorkerTask(DispatcherTask dt) {
			mDispatcherTask = dt;
		}

		@Override
		protected State doInBackground(State... params) {
			for (State state : params) {
				if (Log.DEBUG) {
					Log.d("DispatcherTask.WorkerTask.doInBackground: start process state="
							+ state);
				}
				try {
					if (state.currentSize == state.totalSize) {
						mDispatcherTask.mDb.setFileSize(state.id,
								state.currentSize, state.totalSize);
						publishProgress(state);
						continue;
					}
					SeekableOutputFile f = new SeekableOutputFile(
							state.getTmpFile());
					try {
						download(state, f);
					} finally {
						f.close();
					}
				} catch (Exception e) {
					if (Log.DEBUG) {
						Log.w(e);
					}
					mDispatcherTask.mDb.setLastStatus(state.id,
							DownloadManager.Status.WAITING,
							System.currentTimeMillis() / 1000);
					state.status = DownloadManager.Status.WAITING;
					publishProgress(state);
				} finally {
					try {
						mDispatcherTask.putToken(state.token);
						if (Log.DEBUG) {
							Log.d("DispatcherTask.WorkerTask.doInBackground: release token state="
									+ state);
						}
					} catch (InterruptedException e) {
					}
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(State... values) {
			if (null == values) {
				return;
			}
			for (State state : values) {
				Intent intent = new Intent(Receiver.ACTION_PROGRESS);
				intent.putExtra(Receiver.EXTRA_ID, state.id);
				mDispatcherTask.mSvc.sendBroadcast(intent);
				if (DownloadManager.Status.FINISHED == state.status) {
					DownloadManager.getInstance().onFinished(state);
				}
			}
		}

		protected void download(State state, SeekableOutput so)
				throws IOException {
			if (Log.DEBUG) {
				Log.d("DownloadManager.download: state=" + state);
			}
			state.totalSize = -1;
			URL url = new URL(state.uriString);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			try {
				conn.setConnectTimeout(HTTP_TIMEOUT);
				conn.setReadTimeout(HTTP_TIMEOUT);
				conn.addRequestProperty("Range", "bytes=" + state.currentSize
						+ "-");
				int code = conn.getResponseCode();
				if (Log.DEBUG) {
					Log.d("DownloadManager.download: conn response headers="
							+ conn.getHeaderFields());
				}
				if (HttpURLConnection.HTTP_PARTIAL == code) {
					so.seek(state.currentSize);
					state.totalSize = parseLength(conn
							.getHeaderField("Content-Range"));
				} else if (200 <= code && code < 300) {
					so.seek(0);
					state.currentSize = 0;
					state.totalSize = parseLength(conn
							.getHeaderField("Content-Length"));
				} else {
					state.totalSize = parseLength(conn
							.getHeaderField("Content-Length"));
					throw new IOException("http error: " + code + " headers: "
							+ conn.getHeaderFields());
				}
				InputStream is = conn.getInputStream();
				try {
					copy(state, is, so);
				} finally {
					is.close();
				}
			} finally {
				conn.disconnect();
			}
			if (Log.DEBUG) {
				Log.d("DownloadManager.download: done");
			}
		}

		private static long parseLength(String headerField) {
			if (null == headerField) {
				return -1;
			}
			int pos = headerField.lastIndexOf('/');
			if (pos >= 0) {
				return Long.parseLong(headerField.substring(pos + 1));
			} else {
				return Long.parseLong(headerField);
			}
		}

		private static final int BUFFER_SIZE = 1024 * 8;

		private void copy(State state, InputStream is, SeekableOutput so)
				throws IOException {
			if (Log.DEBUG) {
				Log.d("DownloadManager.copy: id=" + state.id + " offset="
						+ state.currentSize + " length=" + state.totalSize);
			}
			mDispatcherTask.mDb.setFileSize(state.id, state.currentSize,
					state.totalSize);
			publishProgress(state);
			byte[] buffer = new byte[BUFFER_SIZE];
			long now = SystemClock.elapsedRealtime();
			long start = now;
			int n;
			while (state.currentSize < state.totalSize && !this.isCancelled()) {
				n = is.read(buffer);
				if (-1 == n) {
					break;
				}
				state.currentSize += n;
				so.write(buffer, 0, n);
				now = SystemClock.elapsedRealtime();
				if (now - start > 999) {
					start = now;
					mDispatcherTask.mDb.setFileSize(state.id,
							state.currentSize, state.totalSize);
					publishProgress(state);
					ContentValues v = mDispatcherTask.mDb.query(state.id);
					if (DownloadManager.isEmpty(v)
							|| DownloadManager.Status.DOWNLOADING != DownloadManager.Status
									.values()[v.getAsInteger(DbHelper.STATUS)]) {
						break;
					}
				}
			}
			mDispatcherTask.mDb.setFileSize(state.id, state.currentSize,
					state.totalSize);
			if (state.currentSize == state.totalSize) {
				File s = state.getTmpFile();
				state.status = DownloadManager.Status.FINISHED;
				File d = state.getFile();
				if (Log.DEBUG) {
					Log.d("rename " + s.getAbsolutePath() + " to "
							+ d.getAbsolutePath());
				}
				d.delete();
				s.renameTo(d);
				d.setReadable(true, false);
				publishProgress(state);
			} else {
				publishProgress(state);
			}
		}

		public void exec(State state) {
			executeOnExecutor(DispatcherTask.THREAD_POOL_EXECUTOR, state);
		}

	}
}
