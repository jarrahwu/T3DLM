package com.stkj.dlm;

import java.io.File;

import com.stkj.dlm.DownloadManager.Status;


public class State {
	public long id;
	public Status status;
	public long currentSize;
	public long totalSize;
	public String path;
	public String pkgName;
	public String label;
	protected String uriString;
	protected Integer token;
	public boolean installOnFinished;

	public State() {
		id = -1;
		status = Status.UNKNOWN;
		totalSize = -1;
	}

	public File getFile() {
		return new File(path, pkgName + ".apk");
	}

	public File getTmpFile() {
		return new File(path, "." + pkgName);
	}

	@Override
	public String toString() {
		return "{id=" + id + " status=" + status + " currentSize="
				+ currentSize + " totalSize=" + totalSize + " path=" + path
				+ " name=" + pkgName + " label=" + label + " token=" + token
				+ " uri=" + uriString + "}";
	}

}
