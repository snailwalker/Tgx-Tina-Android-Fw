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
