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
