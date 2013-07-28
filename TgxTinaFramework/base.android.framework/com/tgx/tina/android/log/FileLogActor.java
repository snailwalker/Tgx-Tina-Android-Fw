package com.tgx.tina.android.log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import base.tina.core.log.ILogIoActor;
import base.tina.core.log.ILogPrinter.Level;


public class FileLogActor
        implements
        ILogIoActor

{
	
	final static boolean append = false;
	private PrintStream  ps;
	private boolean      logOpen;
	
	public FileLogActor(String fileName, Level level, String pid) throws IOException {
		lv = level;
		dateFormat.setTimeZone(TimeZone.getDefault());// getTimeZone("UTC"));
		ps = new PrintStream(new FileOutputStream(fileName, append), true, "UTF-8");
		logOpen = true;
		this.pid = pid;
	}
	
	@Override
	public void close() throws IOException {
		if (ps != null) ps.close();
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
	private String     formatStr  = "%s: %s/%s(%s): ";
	
	@Override
	public synchronized int write(String tag, Level level, String msg, Throwable throwable) throws IOException {
		if (level.compareTo(lv) < 0 || !logOpen) return 0;
		if (msg == null && throwable != null) msg = throwable.getMessage();
		if (msg != null) ps.printf(formatStr, formatTime(System.currentTimeMillis()), level.name().charAt(0), tag, pid).println(msg);
		if (throwable != null)
		{
			StackTraceElement[] stackTrace = throwable.getStackTrace();
			for (int i = 0; i < stackTrace.length; i++)
				ps.printf(formatStr, formatTime(System.currentTimeMillis()), level.name().charAt(0), tag, pid).println("\tat " + stackTrace[i]);
			Throwable ourCause = throwable.getCause();
			if (ourCause != null) printStackTraceAsCause(ps, throwable, stackTrace, level, tag);
		}
		return 0;
	}
	
	private void printStackTraceAsCause(PrintStream s, Throwable throwable, StackTraceElement[] causedTrace, Level level, String tag) {
		// assert Thread.holdsLock(s);
		
		// Compute number of frames in common between this and caused
		StackTraceElement[] trace = throwable.getStackTrace();
		int m = trace.length - 1, n = causedTrace.length - 1;
		while (m >= 0 && n >= 0 && trace[m].equals(causedTrace[n]))
		{
			m--;
			n--;
		}
		int framesInCommon = trace.length - 1 - m;
		
		s.printf(formatStr, formatTime(System.currentTimeMillis()), level.name().charAt(0), tag, pid).println("Caused by: " + this);
		for (int i = 0; i <= m; i++)
			s.printf(formatStr, formatTime(System.currentTimeMillis()), level.name().charAt(0), tag, pid).println("\tat " + trace[i]);
		if (framesInCommon != 0) s.println("\t... " + framesInCommon + " more");
	}
}
