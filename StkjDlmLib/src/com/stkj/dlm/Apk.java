package com.stkj.dlm;

public class Apk {
	public String iconUri;
	public String label;
	public String apkSize;
	public String description;
	public String apkUri;
	public String pkgName;
	public String stars;
	public String softId;

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Apk)) {
			return false;
		}
		Apk lhs = (Apk) o;
		return lhs.pkgName.equals(pkgName);
	}

	@Override
	public int hashCode() {
		return pkgName.hashCode();
	}

	public String toString() {
		return String
				.format("id=%s, label=%s, description=%s, iconUri=%s, size=%s, apkUri=%s, stars=%s",
						softId, label, description, iconUri, apkSize, apkUri,
						stars);
	}
}