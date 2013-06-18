/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com). Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 *******************************************************************************/
package com.tgx.tina.android.log;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import base.tina.core.log.ILogPrinter;
import base.tina.core.log.LogPrinter;


public class AndroidPrinter
        implements
        ILogPrinter
{
	@Override
	public void v(String tag, String msg) {
		android.util.Log.v(tag, msg);
	}
	
	@Override
	public void v(String tag, String msg, Throwable throwable) {
		android.util.Log.v(tag, msg, throwable);
	}
	
	@Override
	public void d(String tag, String msg) {
		android.util.Log.d(tag, msg);
	}
	
	@Override
	public void d(String tag, String msg, Throwable throwable) {
		android.util.Log.d(tag, msg, throwable);
	}
	
	@Override
	public void i(String tag, String msg) {
		android.util.Log.i(tag, msg);
	}
	
	@Override
	public void i(String tag, String msg, Throwable throwable) {
		android.util.Log.i(tag, msg, throwable);
	}
	
	@Override
	public void w(String tag, String msg) {
		android.util.Log.w(tag, msg);
	}
	
	@Override
	public void w(String tag, Throwable throwable) {
		android.util.Log.w(tag, throwable);
	}
	
	@Override
	public void w(String tag, String msg, Throwable throwable) {
		android.util.Log.w(tag, msg, throwable);
	}
	
	@Override
	public void e(String tag, String msg) {
		android.util.Log.e(tag, msg);
	}
	
	@Override
	public void e(String tag, Throwable throwable) {
		android.util.Log.e(tag, throwable.getMessage(), throwable);
	}
	
	@Override
	public void e(String tag, String msg, Throwable throwable) {
		android.util.Log.e(tag, msg, throwable);
	}
	
	//#ifdef LOG_DIR
	//$final static String			LOG_DIR			= android.os.Environment.getExternalStorageDirectory().getPath() + "//*$LOG_DIR$*//" + LogPrinter.LOG_TAG;
	//#else
	final static String        LOG_DIR         = android.os.Environment.getExternalStorageDirectory().getPath() + "/TgxTina/" + LogPrinter.LOG_TAG;
	//#endif
	//#ifdef LOG_NAME
	//$	final static String        LOG_NAME        = "/*$LOG_NAME$*/";
	//#else
	final static String        LOG_NAME        = "TgxTina_Log_";
	//#endif
	public final static String LOG_SP_NAME     = "LogOnlineData";
	public final static String LOG_SP_NAME_CMD = "Cmd";
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		LogPrinter.e(null, ex);
		try
		{
			LogPrinter.i(null, "wait");
			
			Thread.sleep(3000);
			LogPrinter.actorClose();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		for (int pid : includes)
		{
			if (pid == selfPid) continue;
			android.os.Process.killProcess(pid);
		}
		android.os.Process.killProcess(selfPid);
	}
	
	private int    selfPid;
	private String logFileName;
	
	private AndroidPrinter(Context context) {
		//#debug info
		i(LogPrinter.LOG_TAG, "AndroidPrinter: ---- begin ----");
		selfPid = android.os.Process.myPid();
		_instance = this;
		if (!dumpFile) return;
		java.util.Calendar calendar = java.util.Calendar.getInstance();
		String fileName = LOG_NAME + java.text.DateFormat.getDateInstance().format(calendar.getTime()).replace(' ', '_').replace('-', '_').replace(':', '_').replace('/', '_').replace(',', '_') + "_" + selfPid + ".log";
		//#debug info
		i(LogPrinter.LOG_TAG, "log-file: " + fileName);
		String sdStatus = android.os.Environment.getExternalStorageState();
		if (sdStatus.equals(android.os.Environment.MEDIA_MOUNTED))
		{
			try
			{
				java.io.File logDir = new File(LOG_DIR);
				if (!logDir.exists()) logDir.mkdirs();
				
				java.io.File logFile = new File(LOG_DIR + "/" + fileName);
				if (!logFile.exists()) logFile.createNewFile();
				logFileName = logFile.getAbsolutePath();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		else
		{
			try
			{
				java.io.File logFile = context.getFileStreamPath(fileName);
				if (!logFile.exists()) logFile.createNewFile();
				logFileName = logFile.getAbsolutePath();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		//#ifdef debug
		Level lv = Level.DEBUG;
		//#else
		//$LEVEL lv = LEVEL.WARN;
		//#endif
		FileLogActor actor;
		try
		{
			actor = new FileLogActor(getCurFile(), lv, Integer.toString(selfPid));
			LogPrinter.getLogPrinter(actor);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	public static String getCurFile() {
		return _instance == null ? null : _instance.logFileName;
	}
	
	public static AndroidPrinter getIPrinter(Context context) {
		if (context == null) throw new NullPointerException();
		if (_instance == null) return new AndroidPrinter(context.getApplicationContext());
		return _instance;
	}
	
	private static AndroidPrinter _instance;
	final Set<Integer>            includes = new HashSet<Integer>(4);
	
	public void includeKill(int pid) {
		includes.add(pid);
	}
	
	private final static boolean dumpFile = true;
	
	public final static void createByApp(Context appContext) {
		AndroidPrinter androidPrinter = AndroidPrinter.getIPrinter(appContext);
		LogPrinter.setIPrinter(androidPrinter);
		//#debug info
		LogPrinter.i(null, "Application pid: " + android.os.Process.myPid() + " /TID: " + android.os.Process.myTid());
	}
	
	public final static void createByActivity(Activity activity) {
		createByApp(activity);
		_instance.includeKill(android.os.Process.myPid());
		//#debug info
		LogPrinter.i(null, " Activity pid: " + android.os.Process.myPid());
	}
	
	public final static void createByService(Service service, boolean includeKill) {
		createByApp(service);
		if (includeKill) _instance.includeKill(android.os.Process.myPid());
		//#debug info
		LogPrinter.i(null, "Service pid: " + android.os.Process.myPid());
	}
	
}
