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
 package base.tina.core.log;

import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

import base.tina.core.log.AbstractLogSetting.LogLevel;

public class SimplePrinter
		implements
		ILogPrinter
{
	String	TAG	= getClass().getSimpleName();

	public SimplePrinter()
	{
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));// getTimeZone("GMT+8:00"));
	}

	public volatile boolean	formatTime	= true;

	public final String formatTime(long time) {
		if (!formatTime) Long.toString(time);
		date.setTime(time);
		return dateFormat.format(date);
	}

	private DateFormat	dateFormat	= new java.text.SimpleDateFormat("MM-dd HH:mm:ss.SSS");
	private Date		date		= new Date();
	private String		PID			= "unknown";

	@Override
	public void v(String tag, String msg) {
		v(tag, msg, null);
	}

	@Override
	public void v(String tag, String msg, Throwable throwable) {
		System.out.println(build(tag, msg, LogLevel.VERBOSE));
		if (throwable != null) throwable.printStackTrace(System.out);
	}

	@Override
	public void d(String tag, String msg) {
		d(tag, msg, null);
	}

	@Override
	public void d(String tag, String msg, Throwable throwable) {
		System.out.println(build(tag, msg, LogLevel.DEBUG));
		if (throwable != null) throwable.printStackTrace(System.out);
	}

	@Override
	public void i(String tag, String msg) {
		i(tag, msg, null);
	}

	@Override
	public void i(String tag, String msg, Throwable throwable) {
		System.out.println(build(tag, msg, LogLevel.INFO));
		if (throwable != null) throwable.printStackTrace(System.out);
	}

	@Override
	public void w(String tag, String msg) {
		w(tag, tag, null);
	}

	@Override
	public void w(String tag, Throwable throwable) {
		w(tag, null, throwable);
	}

	@Override
	public void w(String tag, String msg, Throwable throwable) {
		System.err.println(build(tag, msg, LogLevel.WARN));
		if (throwable != null) throwable.printStackTrace(System.err);
	}

	@Override
	public void e(String tag, String msg) {
		e(tag, msg, null);
	}

	@Override
	public void e(String tag, Throwable throwable) {
		e(tag, null, throwable);
	}

	@Override
	public void e(String tag, String msg, Throwable throwable) {
		System.err.println(build(tag, msg, LogLevel.ERROR));
		if (throwable != null) throwable.printStackTrace(System.err);
	}

	private String build(String tag, String msg, LogLevel level) {
		synchronized (builder)
		{
			builder.setLength(0);
			builder.append(formatTime(System.currentTimeMillis())).append(": ");
			builder.append(level.name().charAt(0)).append('/').append(tag).append('(').append(PID).append(')').append(": ");
			if (msg != null) builder.append(msg);
			return builder.toString();
		}
	}

	private StringBuilder	builder	= new StringBuilder(64);

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		e(LogPrinter.LOG_TAG, "FATAL/Uncaught", ex);
	}
}
