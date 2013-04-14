 /*******************************************************************************
  * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *******************************************************************************/
 package base.tina.external.android.log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

	final static String			LOG_DIR			= "//*$LOG_DIR$*//" + LogPrinter.LOG_TAG;
	final static String			LOG_NAME		= "/*$LOG_NAME$*/";

	public final static String	LOG_SP_NAME		= "LogOnlineData";
	public final static String	LOG_SP_NAME_CMD	= "Cmd";

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		android.util.Log.e("FATAL/UNCAUGHT", "uncaught!", ex);

		java.util.Calendar calendar = java.util.Calendar.getInstance();
		String fileName = LOG_NAME + java.text.DateFormat.getDateInstance().format(calendar.getTime()).replace(' ', '_').replace('-', '_').replace(':', '_') + ".log";
		String sdStatus = android.os.Environment.getExternalStorageState();
		if (sdStatus.equals(android.os.Environment.MEDIA_MOUNTED))
		{
			try
			{
				java.io.File logDir = new File(LOG_DIR);
				if (!logDir.exists()) logDir.mkdirs();
				java.io.File logFile = new File(LOG_DIR + "/" + fileName);
				if (!logFile.exists()) logFile.createNewFile();
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
			}
			catch (IOException e)
			{
				e.printStackTrace();
				return;
			}
		}
		ArrayList<String> commandLine = new ArrayList<String>();
		commandLine.add("logcat");
		commandLine.add("-d");
		commandLine.add("-f");
		commandLine.add(fileName);
		commandLine.add("-v");
		commandLine.add("time");
		commandLine.add("-s");
		commandLine.add("*:*");
		try
		{
			Runtime.getRuntime().exec(commandLine.toArray(new String[commandLine.size()]));
		}
		catch (IOException e)
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

	private Context	context;
	private int		selfPid;

	private AndroidPrinter(Context context)
	{
		this.context = context;
		//#debug info
		i(LogPrinter.LOG_TAG, context.toString());
		selfPid = android.os.Process.myPid();
		_instance = this;
	}

	public static AndroidPrinter getIPrinter(Context context) {
		if (context == null) throw new NullPointerException();
		if (_instance == null) return new AndroidPrinter(context.getApplicationContext());
		return _instance;
	}

	private static AndroidPrinter	_instance;
	final Set<Integer>				includes	= new HashSet<Integer>(4);

	public void includeKill(int pid) {
		includes.add(pid);
	}

	public final static void createByApp(Context appContext) {
		AndroidPrinter androidPrinter = AndroidPrinter.getIPrinter(appContext);
		LogPrinter.setIPrinter(androidPrinter);
		//#debug info
		LogPrinter.i(null, androidPrinter.toString() + " /Application pid: " + android.os.Process.myPid() + " /TID: " + android.os.Process.myTid());
	}

	public final static void createByActivity(Activity activity) {
		AndroidPrinter androidPrinter = AndroidPrinter.getIPrinter(activity);
		androidPrinter.includeKill(android.os.Process.myPid());
		LogPrinter.setIPrinter(androidPrinter);
		//#debug info
		LogPrinter.i(null, androidPrinter.toString() + " /Activity pid: " + android.os.Process.myPid());
	}

	public final static void createByService(Service service, boolean includeKill) {
		AndroidPrinter androidPrinter = AndroidPrinter.getIPrinter(service);
		if (includeKill) androidPrinter.includeKill(android.os.Process.myPid());
		LogPrinter.setIPrinter(androidPrinter);
		//#debug info
		LogPrinter.i(null, androidPrinter.toString() + " /Service pid: " + android.os.Process.myPid());
	}

}