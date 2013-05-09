/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package base.tina.core.log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;


public abstract class BufferedLogIoActor
        implements
        ILogIoActor
{
	
	protected final String           logFileName;
	protected final FileChannel      fileChannel;
	protected final ByteBuffer       readBuffer;
	protected final RandomAccessFile rf;
	protected int                    fSeekBegin, fSeekEnd;
	
	public String getFileName() {
		return logFileName;
	}
	
	public BufferedLogIoActor(String fileName) throws IOException {
		logFileName = fileName;
		readBuffer = ByteBuffer.allocate(0x1000);
		fileChannel = (rf = new RandomAccessFile(fileName, "rw")).getChannel();
		//#debug
		LogPrinter.d("BufActor", "logFile length->" + rf.length());
		if (rf.length() > 8)
		{
			fSeekBegin = rf.readInt();
			fSeekEnd = rf.readInt();
		}
		else
		{
			fSeekBegin = 0;
			fSeekEnd = 0;
		}
	}
	
	public final void readLast4k() throws IOException {
		if (fSeekBegin < fSeekEnd)
		{
			int length = fSeekEnd - fSeekBegin;
			if (length > 0x1000) length = 0x1000;
			fileChannel.position(fSeekEnd - length);
			readBuffer.limit(length);
			fileChannel.read(readBuffer);
		}
		else
		{
			if (fSeekEnd > 0x1008)
			{
				fileChannel.position(fSeekEnd - 0x1000);//fSeekEnd超越0x1008定位到4K以前的位置上
				fileChannel.read(readBuffer);
			}
			else
			{
				int len1 = fSeekEnd - 0x8;//重用部分的长度
				int len0 = 0x400008 - fSeekBegin;
				if (len0 + len1 > 0x1000) len0 = 0x1000 - len1;
				fileChannel.position(0x400008 - len0);
				readBuffer.limit(len0);
				fileChannel.read(readBuffer);
				readBuffer.limit(len1 + len0);
				fileChannel.read(readBuffer);
			}
		}
		readBuffer.flip();
	}
	
	public final byte[] read() throws IOException {
		byte[] dst = null;
		readLast4k();
		if (readBuffer.hasRemaining())
		{
			dst = new byte[readBuffer.remaining()];
			readBuffer.get(dst);
			readBuffer.clear();
		}
		return dst;
	}
	
	@Override
	public void close() throws IOException {
		fileChannel.force(true);
		fileChannel.close();
		rf.close();
	}
	
}
