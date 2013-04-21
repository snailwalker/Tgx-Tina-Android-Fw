 /*******************************************************************************
  * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  *******************************************************************************/
 package base.tina.core.task.infc;

import base.tina.core.task.Task;



/**
 * 某一类型的Task存在超时过程时所需要执行的行为规范
 * 
 * @author Zhangzhuo
 * @see #doTimeout(Task)
 * @see #onInvalid(Task)
 * @see #cancel()
 * @see #isTimeout(long, Task)
 * @see #isEnabled()
 */
public interface ITaskTimeout<T extends Task>
{
	/**
	 * 发生timeout后的行为
	 * 
	 * @param task
	 *            当前发生timeout的任务
	 * @author Zhangzhuo
	 */
	public void doTimeout(T task);
	
	/**
	 * 规范了当前超时过程已失效时的操作,包括超时计算已无使用必要时，以及任务已经被关闭时
	 * 
	 * @param task
	 *            失效过程所影响的任务
	 * @author Zhangzhuo
	 */
	public void onInvalid(T task);
	
	/**
	 * 注销当前的超时回调
	 */
	public void cancel();
	
	/**
	 * 当前时间是否已符合超时条件
	 * 
	 * @param curTime
	 *            当前时间,由时间暂存提供以提高性能,并不需要绝对校准当前时间.
	 * @param task
	 *            包含了所关注的任务.内部提供了{@code startTime}和{@code timeOut}两项用于判定的时间戳
	 * @return true 已出发超时<br>
	 *         false 尚未触发超时<br>
	 *         回调处于disable时恒返回false
	 * @see Task#startTime
	 * @see Task#timeOut
	 * @see Task#setStartTime(long)
	 * @see Task#timeOut(int, ITaskTimeout)
	 */
	public boolean isTimeout(long curTime, T task);
	
	/**
	 * 返回当前时间距离超时触发
	 * 尚需等待的时间
	 * 
	 * @param curTime
	 *            当前时间,由时间暂存提供以提高性能,并不需要绝对校准当前时间.
	 * @param task
	 *            包含了所关注的任务.内部提供了{@code startTime}和{@code timeOut}两项用于判定的时间戳
	 * @return 还需等待多久将会触发超时
	 * @see Task#startTime
	 * @see Task#timeOut
	 * @see Task#setStartTime(long)
	 * @see Task#timeOut(int, ITaskTimeout)
	 */
	public long toWait(long curTime, T task);
	
	/**
	 * @author Zhangzhuo
	 * @return 回调是否可用
	 * @see ITaskTimeout#cancel()
	 */
	public boolean isEnabled();
	
}
