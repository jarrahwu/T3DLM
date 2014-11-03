package com.stkj.dlm;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class SeekableOutputFile implements SeekableOutput {
	private final RandomAccessFile mFile;

	public SeekableOutputFile(File f)
			throws FileNotFoundException {
		mFile = new RandomAccessFile(f, "rw");
	}

	public void close() throws IOException {
		mFile.close();
	}

	public void seek(long offset) throws IOException {
		mFile.seek(offset);
	}

	@Override
	public void write(byte[] buf, int offset, int length) throws IOException {
		mFile.write(buf, offset, length);
	}

}
