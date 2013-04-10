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


public interface ITaskFactory<S, I, K, T extends Task>
{
	public T createTask(S arg1, int mode, K arg3);
	
	public T createTask(I arg1, K arg3);
	
	public T createTask(I arg1, int mode);
	
	public boolean isSurport(T task);
	
	public byte getType(T task);
}
