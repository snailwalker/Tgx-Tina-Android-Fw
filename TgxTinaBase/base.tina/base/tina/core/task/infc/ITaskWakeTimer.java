package base.tina.core.task.infc;

public interface ITaskWakeTimer
		extends
		IDisposable
{
	public void setAlarmTime(long absoluteTime);

	public void cancel();

	public void wakeUpTask();

	public void setTask(ITaskRun myTask);
}
