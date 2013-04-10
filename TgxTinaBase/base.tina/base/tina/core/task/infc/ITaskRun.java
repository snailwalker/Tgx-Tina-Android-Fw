package base.tina.core.task.infc;

public interface ITaskRun
{
	public enum CommitAction {
		WAKE_UP, NOWAKE_UP
	}
	
	public void initTask();
	
	public void run() throws Exception;
	
	public void finishTask();
	
	public void interrupt();
	
	public void wakeUp();
	
	public boolean needAlarm();
	
	public void commitResult(ITaskResult result, CommitAction action, int listenerBind);
}
