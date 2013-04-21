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

import base.tina.core.task.AbstractListener;

public abstract class AbstractLogSetting
		extends
		AbstractListener
{
	public static enum LogLevel
	{
		VERBOSE, DEBUG, INFO, WARN, ERROR, FATAL, UI, SILENT
	}

	public final static int	verbose		= 0;
	public final static int	debug		= verbose + 1;
	public final static int	info		= debug + 1;
	public final static int	warn		= info + 1;
	public final static int	error		= warn + 1;
	public final static int	fatal		= error + 1;
	public final static int	ui			= fatal + 1;
	public final static int	silent		= ui + 1;
	public static LogLevel	logLevel	= LogLevel.VERBOSE;
	public static int		priority	= verbose;
	/**
	 * 是否进行逐行输出到服务器的操作
	 */
	public static boolean	RemotePrint;
}
