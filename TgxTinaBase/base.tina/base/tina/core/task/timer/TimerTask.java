package base.tina.core.task.timer;

import java.util.concurrent.TimeUnit;

import base.tina.core.task.Task;


public abstract class TimerTask
        extends
        Task
{
	private long waitMilliSecond;
	
	public final void setWaitTime(long waitMilliSecond) {
		this.waitMilliSecond = waitMilliSecond;
	}
	
	public TimerTask(int delaySecond) {
		this(delaySecond, TimeUnit.SECONDS);
	}
	
	public TimerTask(long duration, TimeUnit timeUnit) {
		super(0);
		setDelay(duration, timeUnit);
		setWaitTime(timeUnit.toMillis(duration));
	}
	
	@Override
	public final void initTask() {
		isCycle = true;
		super.initTask();
	}
	
	@Override
	public final void run() throws Exception {
		if (doTimeMethod()) setDone();
		else setDelay(waitMilliSecond, TimeUnit.MILLISECONDS);
	}
	
	public final void refresh(int delaySecond) {
		invalid();
		setWaitTime(TimeUnit.SECONDS.toMillis(delaySecond));
	}
	
	/**
	 * @return 任务已处理完毕
	 */
	protected abstract boolean doTimeMethod();
	
	protected final static int SerialDomain = -0x2000;
	
}
