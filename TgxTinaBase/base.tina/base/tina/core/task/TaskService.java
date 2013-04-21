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

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import base.tina.core.task.TaskService.Executor.Worker;
import base.tina.core.task.infc.IDisposable;
import base.tina.core.task.infc.ITaskListener;
import base.tina.core.task.infc.ITaskProgress;
import base.tina.core.task.infc.ITaskResult;
import base.tina.core.task.infc.ITaskRun;
import base.tina.core.task.infc.ITaskRun.CommitAction;
import base.tina.core.task.infc.ITaskTimeout;
import base.tina.core.task.infc.ITaskWakeTimer;

/**
 * 此处所使用的线程池模型已于2013-2-11日升级为Doug Lea 撰写的新版本代码
 * 依然提供了对特定线程ID进行内部分发的特性,依然未提供shutdown/shutdownNow方法 虽然已经提供了对应的程序管理与控制函数,但未启用
 * 默认线程存活时间为30s 在多Task.threadID!=0条件下会出现线程池的并发线程过多的问题
 * 请自行在业务逻辑中谨慎使用Task.threadID用以避免业务流同步处理事宜
 * 
 * @since 2013-2-11
 * @author Zhangzhuo
 */

public class TaskService
				implements
				Comparator<Task>,
				IDisposable
{
	final static byte							SERVICE_TASK_INIT		= -1;
	final static byte							SERVICE_PROCESSING		= SERVICE_TASK_INIT + 1;
	final static byte							SERVICE_SCHEDULE		= SERVICE_PROCESSING + 1;
	final static byte							SERVICE_NOTIFYOBSERVER	= SERVICE_SCHEDULE + 1;
	final static byte							SERVICE_DOWN			= -128;

	final static byte							LISTENER_INITIAL_NUM	= 7;

	final Processor								processor;
	final HashMap<Integer, ITaskListener>		listeners;
	final ConcurrentLinkedQueue<ITaskResult>	responseQueue;
	protected final ScheduleQueue<Task>			mainQueue;

	final ReentrantLock							mainLock				= new ReentrantLock();
	final ReentrantLock							runLock					= new ReentrantLock();

	protected TaskService()
	{
		listeners = new HashMap<Integer, ITaskListener>(LISTENER_INITIAL_NUM);
		mainQueue = new ScheduleQueue<Task>(this, this);
		responseQueue = new ConcurrentLinkedQueue<ITaskResult>();
		processor = new Processor();
		_instance = this;
	}

	protected static TaskService	_instance;

	public static TaskService getInstance() {
		if (_instance == null) throw new NullPointerException("No create service!");
		return _instance;
	}

	/**
	 * TaskService 启动函数 在此之前需要将listener都加入到监听队列中
	 */
	public final void startService() {
		processor.start();
		Thread.yield();
	}

	public final void stopService() {
		processor.processing = false;
		processor.interrupt();
	}

	protected void setScheduleAlarmTime(long RTC_WakeTime) {
	}

	protected void noScheduleAlarmTime() {

	}

	protected void onProcessorStop() {

	}

	protected ITaskWakeTimer setTaskAlarmTime(long RTC_WakeTime, ITaskWakeTimer owner, ITaskRun task) {
		return null;
	}

	/*
	 * priority > 0时没有任何可能相等的情况 if (task1.priority > task2.priority) return -1;
	 * if (task1.priority < task2.priority) return 1; if (task1.inQueueIndex <
	 * task2.inQueueIndex) result = -1; if (task1.inQueueIndex >
	 * task2.inQueueIndex) result = 1; if (task1.doTime == task2.doTime) return
	 * result; if (task1.doTime < task2.doTime) return -1; if (task1.doTime >
	 * task2.doTime) return 1; return result;
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public final int compare(Task task1, Task task2) {
		int result = 0;
		long dT = task1.doTime - task2.doTime;
		result = dT < 0 ? -1 : dT > 0 ? 1 : 0;
		if (result != 0) return result;
		result = task1.priority > task2.priority ? -1 : task1.priority < task2.priority ? 1 : task1.inQueueIndex < task2.inQueueIndex ? -1 : task1.inQueueIndex > task2.inQueueIndex ? 1 : 0;
		if (result == 0) result = task1.hashCode() - task2.hashCode();
		return result;
	}

	public final boolean addListener(ITaskListener listener) {
		if (listener == null) throw new NullPointerException();
		ReentrantLock mainLock = this.mainLock;
		if (mainLock.tryLock()) try
		{
			//#debug
			base.tina.core.log.LogPrinter.d(null, "to add listener: " + listener);
			Set<Integer> keySet = listeners.keySet();
			if (listener.getBindSerial() == 0 || keySet.contains(listener.getBindSerial()))
			{
				//#debug warn
				base.tina.core.log.LogPrinter.w(null, listener.getClass().getSimpleName() + "@" + listener.hashCode() + " bindSerial error : " + listener.getBindSerial());
				return false;
			}
			listeners.put(listener.getBindSerial(), listener);
			return true;
		}
		finally
		{
			mainLock.unlock();
		}
		//#debug warn
		else base.tina.core.log.LogPrinter.w(null, "add listener" + listener.toString() + " faild!");
		return false;
	}

	private ITaskListener	recycleListener;

	public final void setRecycle(ITaskListener recycle) {
		this.recycleListener = recycle;
	}

	public final ITaskListener removeListener(int bindSerial) {
		if (bindSerial == 0) return null;
		ReentrantLock mainLock = this.mainLock;
		if (mainLock.tryLock()) try
		{
			return listeners.remove(bindSerial);
		}
		finally
		{
			mainLock.unlock();
		}
		return null;
	}

	public final boolean requestService(Task task, boolean schedule) {
		return requestService(task, schedule, 0);
	}

	public final boolean requestService(Task task, int bindSerial) {
		return requestService(task, false, bindSerial);
	}

	/**
	 * @param task
	 *            正在执行和已经完成的任务不能再次进入任务队列
	 * @param schedule
	 *            是否将任务装入队列头
	 * @param listenerSerial
	 *            绑定的处理器
	 * @return {@code true}成功托管到任务队列 {@code false}
	 *         任务已经执行或完成;或者由于队列中已存在相同任务导致加入队列失败
	 * @throws NullPointerException
	 *             task为{@code null}
	 */
	public final boolean requestService(Task task, boolean schedule, int listenerSerial) {
		if (task == null) throw new NullPointerException();
		if (task.isPending || task.isDone) return false;
		task.scheduleService = this;
		task.setListenSerial(listenerSerial);
		ScheduleQueue<Task> mainQueue = this.mainQueue;
		task.priority = schedule ? mainQueue.priorityIncrease.incrementAndGet() : 0;
		boolean success = mainQueue.offer(task);
		//#debug
		base.tina.core.log.LogPrinter.d(null, "offer: " + success);
		return success;
	}

	public final boolean requestService(Task task, boolean isSchedule, int timelimit, long delayTimeMills, 
					byte retryLimit, Object attachment, ITaskProgress progress, int timeOutSecound,
					ITaskTimeout<?> taskTimeout,
					int bindSerial) {
		if (task == null) throw new NullPointerException();
		task.timeLimit = timelimit;
		task.setRetryLimit(retryLimit);
		task.attachment = attachment;
		task.progress = progress;
		task.setDelay(delayTimeMills, TimeUnit.MILLISECONDS);
		task.timeOut(timeOutSecound, taskTimeout);
		return requestService(task, isSchedule, bindSerial);
	}

	public final boolean requestServiceRetry(Task task, int timeOutSecound) {
		if (task == null) throw new NullPointerException();
		task.timeOut(timeOutSecound, task.timeoutCall);
		return requestService(task, false, task.getListenSerial());
	}

	public final boolean requestService(Task task, long delayTime, int bindSerial) {
		return requestService(task, false, -1, delayTime, (byte) 0, null, null, 0, null, bindSerial);
	}

	/**
	 * @param threadID
	 * @param available
	 * @param schedule
	 * @return
	 */
	public final boolean cancelService(int threadID, int available, boolean schedule) {
		if (processor == null || processor.executor == null) return true;// 没有需要关闭的服务，恒返回true
		Worker worker = processor.executor.id2work2.get(threadID);
		if (worker != null)
		{

		}
		return false;
	}

	/**
	 * @since 2011-6-10 接口变更,取消存在性检查,迭代过程本身效能太低
	 * @author Zhangzhuo
	 * @param object
	 *            将要入队的运行结果 检验是否为TaskResult的实现
	 * @return 入队成功 结果
	 */
	final boolean responseTask(ITaskResult taskResult) {
		if (taskResult == null) return false;
		if (taskResult.isResponsed()) return false;
		taskResult.setResponse(false);
		return responseQueue.offer(taskResult);
	}

	public final void commitNotify() {
		wakeUp();
	}

	protected final void wakeUp() {
		if (processor != null && !processor.isInterrupted()) try
		{
			processor.interrupt();
		}
		catch (Exception e)
		{
			//#debug info
			base.tina.core.log.LogPrinter.i(null, "processorDo interrupt", e);
		}
	}

	private final void notifyObserver() {
		final ReentrantLock mainLock = this.mainLock;
		mainLock.lock();
		ITaskResult receive = null;
		boolean isHandled = false;
		try
		{
			while (!responseQueue.isEmpty())
			{
				receive = responseQueue.poll();
				receive.setResponse(true);
				//#debug
				base.tina.core.log.LogPrinter.d(null, "response notify: " + receive);
				isHandled = false;
				if (!listeners.isEmpty())
				{
					int listenSerial = receive.getListenSerial();
					if (listenSerial != 0)
					{
						ITaskListener listener = listeners.get(listenSerial);
						if (listener != null && listener.isEnable()) try
						{
							isHandled = isHandled(receive, listener);
						}
						catch (Exception e)
						{
							//#debug warn
							e.printStackTrace();
							// 防止回调出错,而导致Processor线程被销毁的风险,忽略任何在回调过程中产生的错误
						}
						finally
						{
							if (receive.isDisposable() && isHandled) receive.dispose();
						}
						else throw new RuntimeException(receive.getClass().getName() + ":No right bind listener!");
					}
					else
					{
						for (ITaskListener listener : listeners.values())
						{
							if (!listener.isEnable()) continue;
							try
							{
								isHandled = isHandled(receive, listener);
							}
							catch (Exception e)
							{
								//#debug warn
								e.printStackTrace();
								// 防止回调出错,而导致Processor线程被销毁的风险,忽略任何在回调过程中产生的错误
							}
							finally
							{
								if (receive.isDisposable() && isHandled) receive.dispose();
							}
							if (isHandled) break;
						}
					}
				}
				if (!isHandled && recycleListener != null) try
				{
					isHandled = isHandled(receive, recycleListener);
				}
				catch (Exception e)
				{
					//#debug warn
					e.printStackTrace();
					// 防止回调出错,而导致Processor线程被销毁的风险,忽略任何在回调过程中产生的错误
				}
				finally
				{
					if (receive.isDisposable() || !isHandled) receive.dispose();
				}
				//#debug warn
				if (!isHandled) base.tina.core.log.LogPrinter.w(null, "NoListener Handle!" + receive.toString());
				//#debug verbose
				if (!isHandled) throw new RuntimeException(receive.getClass().getName() + ":No listener handle it!");
			}
		}
		finally
		{
			mainLock.unlock();
		}
	}

	private final boolean isHandled(ITaskResult receive, ITaskListener listener) {
		int result = receive.getSerialNum();
		if (result == Integer.MIN_VALUE || result == Integer.MAX_VALUE) return false;
		//#debug info
		base.tina.core.log.LogPrinter.i(null, receive.hasError() ? "exCaught : " : "to handle : " + receive);
		return receive.hasError() ? listener.exceptionCaught(receive, this) : listener.ioHandle(receive, this);
	}

	private final class Processor
					extends
					Thread
					implements
					IDisposable
	{
		public Processor()
		{
			processing = true;
			setName("taskService-processor");
		}

		@Override
		public final void dispose() {
			executor = null;
		}

		@Override
		public final boolean isDisposable() {
			return true;
		}

		volatile boolean	processing;
		Executor			executor;

		public final void run() {
			final ScheduleQueue<Task> mainQueue = TaskService.this.mainQueue;
			Task curTask = null;
			byte serviceState = SERVICE_SCHEDULE;
			mainLoop:
			while (processing)
			{
				processor_switch:
				{
					switch (serviceState) {
						case SERVICE_TASK_INIT:
							curTask.initTask();
							if (curTask.isBlocker())
							{
								if (executor == null) executor = new Executor(mainQueue);
								executor.execute(curTask);
								curTask = null;
								serviceState = SERVICE_SCHEDULE;
								break processor_switch;
							}
							serviceState = SERVICE_PROCESSING;
						case SERVICE_PROCESSING:
							try
							{
								curTask.beforeRun();
								curTask.run();
								curTask.finish();
								curTask.afterRun();
							}
							catch (Exception e)
							{
								//#debug warn
								e.printStackTrace();
								curTask.setError(e);
								curTask.doAfterException();
								curTask.setDone();
								curTask.commitResult(curTask);
							}
							finally
							{
								/*
								 * 每次任务执行结束之前都检查一下responseQueue是否有内容需要处理
								 */
								notifyObserver();
								if (!curTask.hasError()) curTask.finishTask();// 如果curTask不携带错误信息将执行finishTask操作
								if (!curTask.isDone && !curTask.isPending)
								{
									curTask.priority = 0;
									mainQueue.offer(curTask);// 任务未完成重新进入队列执行,
								}
								curTask = null;
								serviceState = SERVICE_SCHEDULE;
							}
						case SERVICE_SCHEDULE:
							try
							{
								curTask = mainQueue.take();
							}
							catch (InterruptedException ie)
							{
								//#debug
								base.tina.core.log.LogPrinter.d(null, "Take task interrupted!");
							}
							catch (Exception e)
							{
								//#debug error
								base.tina.core.log.LogPrinter.e(null, "Error!", e);
							}
							if (curTask != null)
							{
								if (curTask.isDone || curTask.disable)
								{
									curTask = null;
									continue mainLoop;
								}
								serviceState = SERVICE_TASK_INIT;
								break processor_switch;
							}
						case SERVICE_NOTIFYOBSERVER:
							notifyObserver();
							serviceState = SERVICE_SCHEDULE;
							break processor_switch;
					}
				}
			}
			mainQueue.clear();
			onProcessorStop();
		}
	}

	private static final int	COUNT_BITS	= Integer.SIZE - 3;
	private static final int	CAPACITY	= (1 << COUNT_BITS) - 1;

	private static final int	RUNNING		= -1 << COUNT_BITS;
	private static final int	SHUTDOWN	= 0 << COUNT_BITS;
	private static final int	STOP		= 1 << COUNT_BITS;
	private static final int	TIDYING		= 2 << COUNT_BITS;
	private static final int	TERMINATED	= 3 << COUNT_BITS;

	private static int runStateOf(int c) {
		return c & ~CAPACITY;
	}

	private static int workerCountOf(int c) {
		return c & CAPACITY;
	}

	private static int ctlOf(int rs, int wc) {
		return rs | wc;
	}

	private static boolean runStateLessThan(int c, int s) {
		return c < s;
	}

	private static boolean runStateAtLeast(int c, int s) {
		return c >= s;
	}

	private static boolean isRunning(int c) {
		return c < SHUTDOWN;
	}

	final class Executor
	{
		final AtomicInteger				ctl			= new AtomicInteger(ctlOf(RUNNING, 0));
		final ReentrantLock				mainLock	= new ReentrantLock();
		final Condition					termination	= mainLock.newCondition();
		final BlockingQueue<Task>		workQueue;
		final ScheduleQueue<Task>		mainQueue;
		final HashSet<Worker>			workers		= new HashSet<Worker>();
		final HashMap<Integer, Worker>	id2work2	= new HashMap<Integer, Worker>();
		long							completedTaskCount;
		volatile boolean				allowCoreThreadTimeOut;
		volatile int					corePoolSize;
		volatile ThreadFactory			threadFactory;
		volatile int					maximumPoolSize;
		volatile long					keepAliveTime;
		int								largestPoolSize;
		static final boolean			ONLY_ONE	= true;

		public Executor(ScheduleQueue<Task> mainQueue)
		{
			threadFactory = new WorkerFactory();
			corePoolSize = 0;
			allowCoreThreadTimeOut = true;
			maximumPoolSize = 1024;
			keepAliveTime = TimeUnit.SECONDS.toNanos(1);
			workQueue = new SynchronousQueue<Task>();
			this.mainQueue = mainQueue;
		}

		final class WorkerFactory
						implements
						ThreadFactory
		{
			final AtomicInteger	id;

			public WorkerFactory()
			{
				id = new AtomicInteger(0);
			}

			@Override
			public Thread newThread(Runnable r) {
				if (id.get() == maximumPoolSize) id.set(0);
				return new Thread(r, "TaskService" + "-pool-" + id.getAndIncrement());
			}
		}

		private boolean compareAndIncrementWorkerCount(int expect) {
			return ctl.compareAndSet(expect, expect + 1);
		}

		private boolean compareAndDecrementWorkerCount(int expect) {
			return ctl.compareAndSet(expect, expect - 1);
		}

		private void decrementWorkerCount() {
			do
			{
				//#debug
				base.tina.core.log.LogPrinter.d(null, "decrementWorkerCount--:" + ctl.get());
			}
			while (!compareAndDecrementWorkerCount(ctl.get()));
		}

		public void execute(Task task) {
			if (task == null) throw new NullPointerException();
			if (task.threadId != 0)
			{
				final ReentrantLock lock = this.mainLock;
				lock.lock();
				try
				{
					Worker worker = id2work2.get(task.threadId);
					if (worker != null && worker.taskBlkQueue.offer(task))
					{
						worker.thread.interrupt();
						return;
					}
				}
				finally
				{
					lock.unlock();
				}
			}

			int c = ctl.get();
			if (workerCountOf(c) < corePoolSize)
			{
				if (addWorker(task, true)) return;
				c = ctl.get();
			}
			if (isRunning(c) && workQueue.offer(task))
			{
				int recheck = ctl.get();
				if (!isRunning(recheck) && remove(task)) reject(task);
				else if (workerCountOf(recheck) == 0) addWorker(null, false);
			}
			else if (!addWorker(task, false)) reject(task);
		}

		private void reject(Task task) {
			boolean success = mainQueue.offer(task);
			if (!success) System.err.println("reject task failed! " + task + " | " + mainQueue);
		}

		private Task getTask(Worker w) {
			boolean timedOut = false; // Did the last poll() time out?

			retry:
			for (;;)
			{
				int c = ctl.get();
				int rs = runStateOf(c);

				// Check if queue empty only if necessary.
				if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty()))
				{
					decrementWorkerCount();
					return null;
				}

				boolean timed; // Are workers subject to culling?

				for (;;)
				{
					int wc = workerCountOf(c);
					timed = allowCoreThreadTimeOut || wc > corePoolSize;
					if (wc <= maximumPoolSize && !(timedOut && timed)) break;
					if (compareAndDecrementWorkerCount(c)) return null;
					c = ctl.get(); // Re-read ctl
					if (runStateOf(c) != rs) continue retry;
					// else CAS failed due to workerCount change; retry inner
					// loop
				}

				try
				{
					Task t = null;
					if (w.taskThreadId != 0) t = w.taskBlkQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
					if (t == null) t = timed ? workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : workQueue.take();
					if (t != null) return t;
					timedOut = true;
				}
				catch (InterruptedException retry)
				{
					timedOut = false;
				}
			}
		}

		private void processWorkerExit(Worker w, boolean completedAbruptly) {
			if (completedAbruptly) // If abrupt, then workerCount wasn't
									// adjusted
			decrementWorkerCount();

			final ReentrantLock mainLock = this.mainLock;
			mainLock.lock();
			try
			{
				completedTaskCount += w.completedTasks;
				workers.remove(w);
			}
			finally
			{
				mainLock.unlock();
			}

			tryTerminate();

			int c = ctl.get();
			if (runStateLessThan(c, STOP))
			{
				if (!completedAbruptly)
				{
					int min = allowCoreThreadTimeOut ? 0 : corePoolSize;
					if (min == 0 && !workQueue.isEmpty()) min = 1;
					if (workerCountOf(c) >= min) return; // replacement not
															// needed
				}
				addWorker(null, false);
			}
		}

		final void tryTerminate() {
			for (;;)
			{
				int c = ctl.get();
				if (isRunning(c) || runStateAtLeast(c, TIDYING) || (runStateOf(c) == SHUTDOWN && !workQueue.isEmpty())) return;
				if (workerCountOf(c) != 0)
				{ // Eligible to terminate
					interruptIdleWorkers(ONLY_ONE);
					return;
				}

				final ReentrantLock mainLock = this.mainLock;
				mainLock.lock();
				try
				{
					if (ctl.compareAndSet(c, ctlOf(TIDYING, 0)))
					{
						try
						{
							// terminated();
						}
						finally
						{
							ctl.set(ctlOf(TERMINATED, 0));
							termination.signalAll();
						}
						return;
					}
				}
				finally
				{
					mainLock.unlock();
				}
				// else retry on failed CAS
			}
		}

		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			long nanos = unit.toNanos(timeout);
			final ReentrantLock mainLock = this.mainLock;
			mainLock.lock();
			try
			{
				for (;;)
				{
					if (runStateAtLeast(ctl.get(), TERMINATED)) return true;
					if (nanos <= 0) return false;
					nanos = termination.awaitNanos(nanos);
				}
			}
			finally
			{
				mainLock.unlock();
			}
		}

		public boolean remove(Task task) {
			boolean removed = workQueue.remove(task);
			tryTerminate(); // In case SHUTDOWN and now empty
			return removed;
		}

		private boolean addWorker(Task firstTask, boolean core) {
			retry:
			for (;;)
			{
				int c = ctl.get();
				int rs = runStateOf(c);

				// Check if queue empty only if necessary.
				if (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null && !workQueue.isEmpty())) return false;

				for (;;)
				{
					int wc = workerCountOf(c);
					if (wc >= CAPACITY || wc >= (core ? corePoolSize : maximumPoolSize)) return false;
					if (compareAndIncrementWorkerCount(c)) break retry;
					c = ctl.get(); // Re-read ctl
					if (runStateOf(c) != rs) continue retry;
					// else CAS failed due to workerCount change; retry inner
					// loop
				}
			}

			Worker w = new Worker(firstTask);
			Thread t = w.thread;

			final ReentrantLock mainLock = this.mainLock;
			mainLock.lock();
			try
			{
				// Recheck while holding lock.
				// Back out on ThreadFactory failure or if
				// shut down before lock acquired.
				int c = ctl.get();
				int rs = runStateOf(c);

				if (t == null || (rs >= SHUTDOWN && !(rs == SHUTDOWN && firstTask == null)))
				{
					decrementWorkerCount();
					tryTerminate();
					return false;
				}

				workers.add(w);
				int s = workers.size();
				if (s > largestPoolSize) largestPoolSize = s;
			}
			finally
			{
				mainLock.unlock();
			}

			t.start();
			// It is possible (but unlikely) for a thread to have been
			// added to workers, but not yet started, during transition to
			// STOP, which could result in a rare missed interrupt,
			// because Thread.interrupt is not guaranteed to have any effect
			// interrupt).
			if (runStateOf(ctl.get()) == STOP && !t.isInterrupted()) t.interrupt();

			return true;
		}

		private void interruptIdleWorkers(boolean onlyOne) {
			final ReentrantLock mainLock = this.mainLock;
			mainLock.lock();
			try
			{
				for (Worker w : workers)
				{
					Thread t = w.thread;
					if (!t.isInterrupted() && w.tryLock())
					{
						try
						{
							t.interrupt();
						}
						catch (SecurityException ignore)
						{
						}
						finally
						{
							w.unlock();
						}
					}
					if (onlyOne) break;
				}
			}
			finally
			{
				mainLock.unlock();
			}
		}

		void advanceRunState(int targetState) {
			while (true)
			{
				int c = ctl.get();
				if (runStateAtLeast(c, targetState) || ctl.compareAndSet(c, ctlOf(targetState, workerCountOf(c)))) break;
			}
		}

		public void shutdown() {
			final ReentrantLock mainLock = this.mainLock;
			mainLock.lock();
			try
			{
				advanceRunState(SHUTDOWN);
				interruptIdleWorkers();
			}
			finally
			{
				mainLock.unlock();
			}
			tryTerminate();
		}

		private void interruptIdleWorkers() {
			interruptIdleWorkers(false);
		}

		final class Worker
						extends
						AbstractQueuedSynchronizer
						implements
						Runnable
		{
			private static final long	serialVersionUID	= 3833487996901286223L;

			final BlockingQueue<Task>	taskBlkQueue;
			Task						firstTask;
			Task						lastTask;
			volatile int				taskThreadId;
			volatile long				completedTasks;
			IWakeLock					wakeLock;
			Thread						thread;

			public Worker(Task task)
			{
				firstTask = task;
				taskBlkQueue = new LinkedBlockingQueue<Task>();
				wakeLock = getWakeLock();
				thread = threadFactory.newThread(this);
				if (wakeLock != null && thread != null) wakeLock.initialize("worker#" + thread.getId());
			}

			protected boolean isHeldExclusively() {
				return getState() == 1;
			}

			protected boolean tryAcquire(int unused) {
				if (compareAndSetState(0, 1))
				{
					setExclusiveOwnerThread(Thread.currentThread());
					return true;
				}
				return false;
			}

			protected boolean tryRelease(int unused) {
				setExclusiveOwnerThread(null);
				setState(0);
				return true;
			}

			public void lock() {
				acquire(1);
			}

			public boolean tryLock() {
				return tryAcquire(1);
			}

			public void unlock() {
				release(1);
			}

			public boolean isLocked() {
				return isHeldExclusively();
			}

			@Override
			public void run() {
				runWorker(this);
			}

		}

		final void runWorker(Worker w) {
			Task task = w.firstTask;
			Task lastTask = task;
			w.firstTask = null;
			boolean completedAbruptly = true;
			try
			{
				workLoop:
				for (;;)
				{
					while (task != null || (task = getTask(w)) != null)
					{
						lastTask = task;
						w.lock();
						clearInterruptsForTaskRun();
						try
						{
							if (task.disable || task.isDone) continue;
							if (w.taskThreadId == 0 && task.threadId != 0)
							{
								final ReentrantLock lock = mainLock;
								lock.lock();
								try
								{
									w.taskThreadId = task.threadId;
									id2work2.put(w.taskThreadId, w);
								}
								finally
								{
									lock.unlock();
								}
							}
							if (task.wakeLock && w.wakeLock != null) w.wakeLock.acquire();// 并非所有的任务都强制要求cpu
																							// 唤醒状态保护.
							try
							{
								task.beforeRun();
								run:
								for (;;)
								{
									task.run();
									task.finish();
									if (!task.isDone && task.isCycle) continue run;// 任务还将继续执行
									else if (!task.isDone && task.isSegment)
									{
										if (!w.taskBlkQueue.isEmpty())// 判断当前工作线程的队列状态,如果非空将进行任务切换,为空则不进行任务切换
										{
											w.taskBlkQueue.offer(task);
											break run;
										}
										else if (w.taskThreadId != 0 && w.taskThreadId == task.threadId) continue run;
										else mainQueue.offer(task);// 回到主队列重新进行分发
									}
									else break run;
								}
								task.afterRun();
							}
							catch (Exception x)
							{
								task.setError(x);
								task.doAfterException();
								task.setDone();
								task.commitResult(task, CommitAction.WAKE_UP);
							}
							finally
							{
								if (task.wakeLock && w.wakeLock != null) w.wakeLock.release();
								if (!task.hasError()) task.finishTask();
							}
						}
						finally
						{
							task = null;
							w.completedTasks++;
							w.unlock();
						}
					}
					final ReentrantLock lock = this.mainLock;
					lock.lock();
					try
					{
						if (w.taskBlkQueue.isEmpty())
						{
							id2work2.remove(w.taskThreadId);
							w.taskThreadId = 0;
							break workLoop;
						}
						else
						{
							retry:
							for (;;)
							{
								int c = ctl.get();
								int rs = runStateOf(c);

								// Check if queue empty only if necessary.
								if (rs >= SHUTDOWN) break workLoop;

								for (;;)
								{
									if (compareAndIncrementWorkerCount(c)) break retry;
									c = ctl.get(); // Re-read ctl
									if (runStateOf(c) != rs) continue retry;
									// else CAS failed due to workerCount
									// change; retry inner loop
								}
							}
						}
					}
					finally
					{
						lock.unlock();
					}
				}
				if (lastTask != null)
				{
					lastTask.finishThreadTask();
					lastTask = null;
				}
				completedAbruptly = false;
			}
			finally
			{
				processWorkerExit(w, completedAbruptly);
			}
		}

		private void clearInterruptsForTaskRun() {
			if (runStateLessThan(ctl.get(), STOP) && Thread.interrupted() && runStateAtLeast(ctl.get(), STOP)) Thread.currentThread().interrupt();
		}
	}

	public static interface IWakeLock
	{
		public void acquire();

		public void release();

		public void initialize(String lockName);
	}

	protected IWakeLock getWakeLock() {
		return null;
	}

	@Override
	public void dispose() {
		_instance = null;
	}

	@Override
	public boolean isDisposable() {
		return true;
	}

}
