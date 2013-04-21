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
package base.tina.external.io;

import base.tina.core.task.TaskService;
import base.tina.core.task.infc.ITaskFactory;
import base.tina.core.task.infc.ITaskProgress;
import base.tina.core.task.infc.ITaskTimeout;

public class IoService
{
	public IoService(IoTaskFactory factory, TaskService service)
	{
		defaultFactory = factory;
		//#debug warn
		if (defaultFactory == null) base.tina.core.log.LogPrinter.w("IoServic", "No Default IoTaskFactory~");
		if (service == null) throw new NullPointerException();
		taskService = service;
	}

	private final TaskService	taskService;
	private IoTaskFactory		defaultFactory;

	public IoTask<?> requestService(String url, int mode, IoFilter filter, Object ioBuffer, boolean isShedule, int timelimit, long delayTime, byte retryLimit, Object attachment, ITaskProgress progress) {
		return requestService(null, url, mode, filter, ioBuffer, isShedule, timelimit, delayTime, retryLimit, attachment, progress, 0, null, 0);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, String url, int mode, IoFilter filter, Object toWrite, boolean isShedule, int timelimit, long delayTime,
					byte retryLimit, Object attachment, ITaskProgress progress, int timeOutSecound, ITaskTimeout<?> taskTimeOut, int bindSerial) {
		if (taskFactory == null) taskFactory = defaultFactory;
		if (taskFactory == null) return null;
		IoTask<?> ioTask = taskFactory.createTask(url, mode, filter);
		if (ioTask != null && !ioTask.isDisable())
		{
			ioTask.toWrite = toWrite;
			if (taskService.requestService(ioTask, isShedule, timelimit, delayTime, retryLimit, attachment, progress, timeOutSecound, taskTimeOut, bindSerial)) return ioTask;
			else
			{
				//#debug warn
				base.tina.core.log.LogPrinter.w(null, "request task: " + ioTask.getClass().getSimpleName() + "@" + ioTask.hashCode() + " failed!");
				return null;
			}
		}
		else return null;
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, IoSession<?> ioSession, IoFilter filter, Object toWrite, boolean isShedule, int timeLimit, long delayTime,
					byte retryLimit, Object attachment, ITaskProgress progress, int timeOutSecound, ITaskTimeout<?> taskTimeOut, int bindSerial) {
		if (taskFactory == null) taskFactory = defaultFactory;
		if (taskFactory == null) return null;
		IoTask<?> ioTask = taskFactory.createTask(ioSession, filter);
		if (ioTask != null && !ioTask.isDisable())
		{
			ioTask.toWrite = toWrite;
			if (taskService.requestService(ioTask, isShedule, timeLimit, delayTime, retryLimit, attachment, progress, timeOutSecound, taskTimeOut, bindSerial)) return ioTask;
			else
			{
				//#debug warn
				base.tina.core.log.LogPrinter.w(null, "request task: " + ioTask.getClass().getSimpleName() + "@" + ioTask.hashCode() + " failed!");
				return null;
			}
		}
		else return null;
	}

	/**
	 * @param ioSession
	 *            current linked IoSession
	 * @param mode
	 *            IConnection.CLOSE
	 * @param isShedule
	 * @param delayTime
	 * @return
	 */
	public IoTask<?> requestService(IoSession<?> ioSession, int mode, boolean isShedule, long delayTime) {
		if (defaultFactory == null || ioSession == null) return null;
		IoTask<?> ioTask = defaultFactory.createTask(ioSession, mode);
		if (ioTask != null && !ioTask.isDisable())
		{
			if (taskService.requestService(ioTask, isShedule, -1, delayTime, (byte) 0, null, null, 0, null, 0)) return ioTask;
			else
			{
				//#debug warn
				base.tina.core.log.LogPrinter.w(null, "request task: " + ioTask.getClass().getSimpleName() + "@" + ioTask.hashCode() + " failed!");
				return null;
			}
		}
		else return null;
	}

	public IoTask<?> requestService(IoSession<?> ioSession, IoFilter filter, Object ioBuffer) {
		return requestService(null, ioSession, filter, ioBuffer, false, -1, -1, (byte) 0, null, null, 0, null, 0);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, IoSession<?> ioSession, IoFilter filter, Object ioBuffer) {

		return requestService(taskFactory, ioSession, filter, ioBuffer, false, -1, -1, (byte) 0, null, null, 0, null, 0);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, IoSession<?> ioSession, IoFilter filter, Object ioBuffer, int bindSerial) {
		return requestService(taskFactory, ioSession, filter, ioBuffer, false, -1, -1, (byte) 0, null, null, 0, null, bindSerial);
	}

	public IoTask<?> requestService(String url, int mode, IoFilter filter) {
		return requestService(url, mode, filter, null, false, -1, -1, (byte) 0, null, null);
	}

	public IoTask<?> requestService(String url, int mode, IoFilter filter, int bindSerial) {
		return requestService(null, url, mode, filter, null, false, -1, -1, (byte) 0, null, null, 0, null, bindSerial);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, String url, int mode, IoFilter filter, int bindSerial) {
		return requestService(taskFactory, url, mode, filter, null, false, -1, -1, (byte) 0, null, null, 0, null, bindSerial);
	}

	public IoTask<?> requestService(String url, int mode, IoFilter filter, Object ioBuffer, boolean schedule, int timeOutSecound, ITaskTimeout<?> taskTimeOut) {
		return requestService(null, url, mode, filter, ioBuffer, schedule, -1, -1, (byte) 0, null, null, timeOutSecound, taskTimeOut, 0);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, String url, int mode, IoFilter filter, Object ioBuffer, boolean schedule, int timeOutSecound,
					ITaskTimeout<?> taskTimeOut) {
		return requestService(taskFactory, url, mode, filter, ioBuffer, schedule, -1, -1, (byte) 0, null, null, timeOutSecound, taskTimeOut, 0);
	}

	public IoTask<?> requestService(String url, int mode, IoFilter filter, Object ioBuffer, boolean schedule, int timeOutSecound, ITaskTimeout<?> taskTimeOut, int bindSerial) {
		return requestService(null, url, mode, filter, ioBuffer, schedule, -1, -1, (byte) 0, null, null, timeOutSecound, taskTimeOut, bindSerial);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, String url, int mode, IoFilter filter, Object ioBuffer, boolean schedule, int timeOutSecound,
					ITaskTimeout<?> taskTimeOut, int bindSerial) {
		return requestService(taskFactory, url, mode, filter, ioBuffer, schedule, -1, -1, (byte) 0, null, null, timeOutSecound, taskTimeOut, bindSerial);
	}

	public IoTask<?> requestService(IoSession<?> ioSession, IoFilter filter, Object ioBuffer, boolean schedule, int timeOutSecound, ITaskTimeout<?> taskTimeOut) {
		return requestService(null, ioSession, filter, ioBuffer, schedule, -1, -1, (byte) 0, null, null, timeOutSecound, taskTimeOut, 0);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, IoSession<?> ioSession, IoFilter filter, Object ioBuffer, boolean schedule, int timeOutSecound,
					ITaskTimeout<?> taskTimeOut) {
		return requestService(taskFactory, ioSession, filter, ioBuffer, schedule, -1, -1, (byte) 0, null, null, timeOutSecound, taskTimeOut, 0);
	}

	public IoTask<?> requestService(IoSession<?> ioSession, IoFilter filter, Object ioBuffer, long delay, int timeOutSecound, ITaskTimeout<?> taskTimeOut, byte retryLimit) {
		return requestService(null, ioSession, filter, ioBuffer, false, -1, delay, retryLimit, null, null, timeOutSecound, taskTimeOut, 0);
	}

	public IoTask<?> requestService(IoSession<?> ioSession, IoFilter filter, Object ioBuffer, long delay, int timeOutSecound, ITaskTimeout<?> taskTimeOut) {
		return requestService(null, ioSession, filter, ioBuffer, false, -1, delay, (byte) 0, null, null, timeOutSecound, taskTimeOut, 0);
	}

	public IoTask<?> requestService(IoSession<?> ioSession, IoFilter filter, Object ioBuffer, long delay) {
		return requestService(null, ioSession, filter, ioBuffer, false, -1, delay, (byte) 0, null, null, 0, null, 0);
	}

	public IoTask<?> requestService(ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>> taskFactory, IoSession<?> ioSession, IoFilter filter, Object ioBuffer, boolean schedule, int timeOutSecound,
					ITaskTimeout<?> taskTimeOut, int bindSerial) {
		return requestService(taskFactory, ioSession, filter, ioBuffer, schedule, -1, -1, (byte) 0, null, null, timeOutSecound, taskTimeOut, bindSerial);
	}
}
