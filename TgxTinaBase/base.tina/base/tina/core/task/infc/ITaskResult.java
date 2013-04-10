package base.tina.core.task.infc;

import java.util.Map;


/**
 * @author Zhangzhuo
 * @see {@link #getSerialNum()}
 * @see {@link #setResponse(boolean)}
 * @see {@link #isResponsed()}
 * @see {@link #setListenSerial(long)}
 * @see {@link #getListenSerial()}
 * @see base.tina.core.task.AbstractResult
 */
public interface ITaskResult
        extends
        IDisposable
{
	/**
	 * 任务编号 0x80000001-0xFFFFFFFF <br>
	 * 结果类型 0x00 - 0x7FFFFFFF <br>
	 * 
	 * @author Zhangzhuo
	 * @return
	 */
	public int getSerialNum();
	
	/**
	 * reset设为进入队列
	 * 此处需要 volatile变量来作为标记
	 * 
	 * @author Zhangzhuo
	 * @param reset
	 *            true 重置这一变量 false 为默认值将设定对象已进入notify队列
	 */
	public void setResponse(boolean reset);
	
	/**
	 * 当前存在队列中的状态
	 * 
	 * @author Zhangzhuo
	 * @return true 已进入响应队列
	 */
	public boolean isResponsed();
	
	/**
	 * @author Zhangzhuo
	 * @param bindSerial
	 *            当前任务结果绑定的Listener SerialNum <br>
	 *            <code> 0</code> 将顺序提交到所有监听者
	 */
	public void setListenSerial(int bindSerial);
	
	/**
	 * @author Zhangzhuo
	 * @return 订阅此任务的Listener SerialNum <br>
	 *         <code> 0</code> 将顺序提交到所有监听者
	 */
	public int getListenSerial();
	
	public boolean hasError();
	
	public void setError(Exception ex);
	
	public Exception getError();
	
	public void setAttributes(Map<String, Object> map);
	
	public void setAttribute(String key, Object value);
	
	public Object getAttribute(String key);
}
