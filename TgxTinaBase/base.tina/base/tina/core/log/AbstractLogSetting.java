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
