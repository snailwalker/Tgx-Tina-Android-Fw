package base.tina.core.log;

import java.lang.Thread.UncaughtExceptionHandler;

public interface ILogPrinter
		extends
		UncaughtExceptionHandler
{
	public void v(String tag, String msg);

	public void v(String tag, String msg, Throwable throwable);

	public void d(String tag, String msg);

	public void d(String tag, String msg, Throwable throwable);

	public void i(String tag, String msg);

	public void i(String tag, String msg, Throwable throwable);

	public void w(String tag, String msg);

	public void w(String tag, Throwable throwable);

	public void w(String tag, String msg, Throwable throwable);

	public void e(String tag, String msg);

	public void e(String tag, Throwable throwable);

	public void e(String tag, String msg, Throwable throwable);

}
