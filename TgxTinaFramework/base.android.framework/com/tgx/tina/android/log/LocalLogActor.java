package com.tgx.tina.android.log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import base.tina.core.log.BufferedLogIoActor;


public class LocalLogActor
        extends
        BufferedLogIoActor
        implements
        Runnable
{
	
	final InputStream is;
	
	public LocalLogActor(String fileName, InputStream in) throws IOException {
		super(fileName);
		is = in;
		new Thread(this).start();
	}
	
	@Override
	public int write(String tag, int priority, String msg, Throwable throwable) throws IOException {
		
		return 0;
	}
	
	@Override
	public void run() {
		DataInputStream dis = null;
		try
		{
			dis = new DataInputStream(is);
			String str;
			while ((str = dis.readLine()) != null)
			{
				rf.writeChars(str);
				rf.writeChars("\r\n");
			}
			close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (dis != null) dis.close();
				if (is != null) is.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
}
