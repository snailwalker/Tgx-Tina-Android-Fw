package base.tina.core.task.infc;

import base.tina.core.task.Task;


public interface ITaskFactory<S, I, K, T extends Task>
{
	public T createTask(S arg1, int mode, K arg3);
	
	public T createTask(I arg1, K arg3);
	
	public T createTask(I arg1, int mode);
	
	public boolean isSurport(T task);
	
	public byte getType(T task);
}
