package base.tina.core.task.infc;

import base.tina.core.task.TaskService;



public interface ITaskListener
{
	/**
	 * @param message
	 * @param service
	 * @return true:将停止继续分发
	 */
	public boolean ioHandle(ITaskResult message, TaskService service);
	
	/**
	 * task 将携带错误信息，task内含的资源需要在此处进行手工释放
	 * 
	 * @param taskResult
	 * @param service
	 * @return true:将停止继续分发
	 */
	public boolean exceptionCaught(ITaskResult taskResult, TaskService service);
	
	/**
	 * 当前Listener是否处于可用状态,此状态的由具体实现决定
	 * 
	 * @return
	 */
	public boolean isEnable();
	
	/**
	 * 设置当前Listener的串号,为进行分发确定唯一标识
	 * 
	 * @param bindSerial
	 */
	public void setBindSerial(int bindSerial);
	
	public int getBindSerial();
}
