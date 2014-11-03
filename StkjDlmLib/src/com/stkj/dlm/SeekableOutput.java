package com.stkj.dlm;

import java.io.IOException;

public interface SeekableOutput {
	public void seek(long offset) throws IOException;

	public void write(byte[] buf, int offset, int length) throws IOException;

	public void close() throws IOException;
}
