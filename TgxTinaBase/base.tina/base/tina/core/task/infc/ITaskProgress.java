package base.tina.core.task.infc;

public interface ITaskProgress
{
	public void createProgress(TaskProgressType type, int max);
	
	public void updateProgress(TaskProgressType type, int increase);
	
	public void finishProgress(TaskProgressType type);
	
	public enum TaskProgressType {
		error, cancel, percent, horizontal,vertical, cycle, complete
	};
}
