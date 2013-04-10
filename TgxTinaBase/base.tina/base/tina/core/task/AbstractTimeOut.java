package base.tina.core.task;

import java.util.concurrent.TimeUnit;

import base.tina.core.task.infc.ITaskTimeout;

public abstract class AbstractTimeOut<E extends Task>
		implements
		ITaskTimeout<E>
{
	protected boolean	enabled	= true;

	@Override
	public void cancel() {
		enabled = false;
	}

	@Override
	public boolean isTimeout(long curTime, E task) {
		return TimeUnit.SECONDS.toMillis(task.timeOut) < curTime - task.getStartTime();
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public long toWait(long curTime, E task) {
		return TimeUnit.SECONDS.toMillis(task.timeOut) + task.getStartTime() - curTime;
	}
}
