/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
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
	 * @author Zhangzhuo 锁定当前的状态等待真正的入队结果，如果成功，将保持这一锁定状态直至离开ResponseQueue；
	 */
	public void lockResponsed();
	
	public void unlockResponsed();
	
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
	
	public boolean canOtherHandle();
}
