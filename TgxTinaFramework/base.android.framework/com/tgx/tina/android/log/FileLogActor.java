package com.tgx.tina.android.log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import base.tina.core.log.ILogIoActor;
import base.tina.core.log.ILogPrinter.Level;


public class FileLogActor
        implements
        ILogIoActor

{
	private final class TinaPrinterStream
	        extends
	        PrintStream
	{
		
		public TinaPrinterStream(OutputStream out, boolean autoFlush, String charsetName) throws UnsupportedEncodingException {
			super(out, autoFlush, charsetName);
		}
		
		@Override
		public synchronized void println(String str) {
			printf(formatStr, fArgs);
			System.err.println("$$$$" + str);
			super.println(str);
		}
		
		@Override
		public synchronized void println(Object o) {
			printf(formatStr, fArgs);
			System.err.println("----" + o.toString());
			super.println(o);
		}
		
		 
		private String   formatStr = "%s: %s/%s(%s): ";
		
		private Object[] fArgs;
		
		public synchronized void setFormatArgs(Object... args) {
			fArgs = args;
		}
	}
	
	final static boolean      append = false;
	private TinaPrinterStream tps;
	private boolean           logOpen;
	
	public FileLogActor(String fileName, Level level, String pid) throws IOException {
		lv = level;
		dateFormat.setTimeZone(TimeZone.getDefault());// getTimeZone("UTC"));
		tps = new TinaPrinterStream(new FileOutputStream(fileName, append), true, "UTF-8");
		logOpen = true;
		this.pid = pid;
	}
	
	@Override
	public void close() throws IOException {
		if (tps != null) tps.close();
	}
	
	private Level  lv;
	public boolean formatTime = true;
	
	public final String formatTime(long time) {
		if (!formatTime) Long.toString(time);
		date.setTime(time);
		return dateFormat.format(date);
	}
	
	private DateFormat dateFormat = new java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS");
	private Date       date       = new Date();
	private String     pid        = "unknown";
	
	@Override
	public int write(String tag, Level level, String msg, Throwable throwable) throws IOException {
		if (level.compareTo(lv) < 0 || !logOpen) return 0;
		tps.setFormatArgs(formatTime(System.currentTimeMillis()), level.name().charAt(0), tag, pid);
		if (msg != null) tps.println(msg);
		if (throwable != null) throwable.printStackTrace(tps);
		tps.flush();
		return 0;
	}
}
