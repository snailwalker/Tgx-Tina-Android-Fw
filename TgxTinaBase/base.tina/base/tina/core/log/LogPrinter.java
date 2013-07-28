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

import base.tina.core.log.ILogPrinter.Level;


public class LogPrinter
{
	private static ILogPrinter  iLogPrinter;
	private static ILogSetting  iLogSetting;
	private static final String TGX_TAG = "TGX";
	
	public final static void setIPrinter(ILogPrinter logPrinter) {
		iLogPrinter = logPrinter;
		Thread.setDefaultUncaughtExceptionHandler(iLogPrinter);
	}
	
	public final static boolean isEnable() {
		return iLogPrinter != null;
	}
	
	public final static void v(String tag, String msg) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.v(tag, msg);
		println(tag, Level.VERBOSE, msg, null);
	}
	
	public final static void v(String tag, String msg, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.v(tag, msg, throwable);
		println(tag, Level.VERBOSE, msg, throwable);
	}
	
	public final static void d(String tag, String msg) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.d(tag, msg);
		println(tag, Level.DEBUG, msg, null);
	}
	
	public final static void d(String tag, String msg, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.d(tag, msg, throwable);
		println(tag, Level.DEBUG, msg, throwable);
	}
	
	public final static void i(String tag, String msg) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.i(tag, msg);
		println(tag, Level.INFO, msg, null);
	}
	
	public final static void i(String tag, String msg, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.i(tag, msg, throwable);
		println(tag, Level.INFO, msg, throwable);
	}
	
	public final static void w(String tag, String msg) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.w(tag, msg);
		println(tag, Level.WARN, msg, null);
	}
	
	public final static void w(String tag, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.w(tag, throwable);
		println(tag, Level.WARN, null, throwable);
	}
	
	public final static void w(String tag, String msg, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.w(tag, msg, throwable);
		println(tag, Level.WARN, msg, throwable);
	}
	
	public final static void e(String tag, String msg) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.e(tag, msg);
		println(tag, Level.ERROR, msg, null);
	}
	
	public final static void e(String tag, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.e(tag, throwable);
		println(tag, Level.ERROR, null, throwable);
	}
	
	public final static void e(String tag, String msg, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogPrinter != null) iLogPrinter.e(tag, msg, throwable);
		println(tag, Level.ERROR, msg, throwable);
	}
	
	/**
	 * @param tag
	 * @param level
	 *            debug level
	 * @return byte to write
	 */
	
	public final static int println(String tag, Level level, String msg, Throwable throwable) {
		if (tag == null || "".equals(tag.trim())) tag = TGX_TAG;
		if (iLogSetting != null && !iLogSetting.isRemotePrint()) return 0;
		int printNum = 0;
		if (_instance != null && _instance.logIoActor != null) try
		{
			synchronized (_instance)
			{
				printNum = _instance.logIoActor.write(tag, level, msg, throwable);
			}
		}
		catch (Exception e)
		{
			//#debug warn
			e.printStackTrace();
		}
		return printNum;
	}
	
	private final static void setIoActor(ILogIoActor logIoActor) {
		_instance.logIoActor = logIoActor;
	}
	
	private static LogPrinter _instance;
	private ILogIoActor       logIoActor;
	
	private LogPrinter(ILogIoActor logIoActor) {
		_instance = this;
		LogPrinter.setIoActor(logIoActor);
	}
	
	public static LogPrinter getLogPrinter(ILogIoActor iLogIoActor) {
		if (iLogIoActor == null) throw new NullPointerException();
		else if (iLogSetting != null) iLogSetting.enableRPrint();
		if (_instance == null) return new LogPrinter(iLogIoActor);
		else setIoActor(iLogIoActor);
		return _instance;
	}
	
	public static void actorClose() {
		if (_instance != null && _instance.logIoActor != null) try
		{
			_instance.logIoActor.close();
		}
		catch (IOException e)
		{
			//#debug warn
			e.printStackTrace();
		}
	}
	
	//#ifdef LOG_TAG
	//$public final static String LOG_TAG ="/*$LOG_TAG$*/";
	//#else
	public final static String LOG_TAG = "TINA_LOG";
	//#endif
}
