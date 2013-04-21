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
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ScheduleQueue<E extends Task>
{
	transient final ReentrantLock	lock				= new ReentrantLock();
	transient final Condition		available			= lock.newCondition();
	final TreeSet<E>				tasksTree;
	int								offerIndex;
	TaskService						service;
	final AtomicInteger				priorityIncrease	= new AtomicInteger(0);

	public ScheduleQueue(Comparator<? super E> comparator, TaskService service)
	{
		tasksTree = new TreeSet<E>(comparator);
		this.service = service;
	}

	/**
	 * @author Zhangzhuo
	 * @param E
	 *            {@code ?super Task}
	 * @return <tt>True 成功插入 </tt> 由于此Queue使用Set特性,所以必须是!contain(<tt>param</tt>)
	 *         否则<tt>False</tt>
	 */
	public final boolean offer(E e) {
		if (e == null) return false;
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			E first = peek();
			e.inQueueIndex = ++offerIndex;
			if (!tasksTree.add(e))
			{
				e.inQueueIndex--;
				return false;
			}
			e.intoScheduleQueue();
			if (first == null || tasksTree.comparator().compare(e, first) < 0) available.signalAll();
			return true;
		}
		finally
		{
			lock.unlock();
		}
	}

	private E peek() {
		try
		{
			return tasksTree.first();
		}
		catch (NoSuchElementException e)
		{
			return null;
		}
	}

	private E pollFirst() {
		E first = peek();
		if (first == null) return null;
		else if (tasksTree.remove(first))
		{
			first.outScheduleQueue();
			return first;// remove操作应该会导致树的自平衡操作
		}
		return null;
	}

	/**
	 * 移除并返回队列的头部元素,队列为空或Task.isToSchedule < 0 时 忽略此操作
	 * 
	 * @return queue 的头部 , or <tt>null</tt> 如果队列为空的话
	 */
	public E poll() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			E first = peek();
			if (first == null || first.isToSchedule() < 0) return null;
			else
			{
				E x = pollFirst();
				assert x != null;
				if (!isEmpty()) available.signalAll();
				return x;
			}
		}
		finally
		{
			lock.unlock();
		}
	}

	public final boolean replace(E e) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			if (tasksTree.contains(e) && tasksTree.remove(e))
			{
				e.outScheduleQueue();
				boolean success = offer(e);
				return success;
			}
			return false;
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * @param e
	 * @param duration
	 * @param timeUnit
	 * @return {@code 1}:已经由队列进行更新<br>
	 *         {@code -1}:此任务不在队列中<br>
	 *         {@code -2}:任务入队失败<br>
	 */
	public final int setDelay(E e, long duration, TimeUnit timeUnit) {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			int result = 0;
			if (!tasksTree.contains(e)) result = -1;
			else if (tasksTree.remove(e)) e.outScheduleQueue();
			else result = -2;
			e.doTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(duration, timeUnit);
			if (result == 0) result = offer(e) ? 1 : -2;
			return result;
		}
		finally
		{
			lock.unlock();
		}
	}

	public final boolean updateDelay(E e, long duration, TimeUnit timeUnit) {
		final ReentrantLock lock = this.lock;
		if (lock.tryLock())
		{
			try
			{
				e.offTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(duration, timeUnit) - e.doTime;
				available.signalAll();
				return true;
			}
			finally
			{
				lock.unlock();
			}
		}
		return false;
	}

	final boolean isEmpty() {
		final ReentrantLock lock = this.lock;
		lock.lock();
		try
		{
			return tasksTree.isEmpty();
		}
		finally
		{
			lock.unlock();
		}
	}

	final int size() {
		return tasksTree.size();
	}

	public final void clear() {
		tasksTree.clear();
	}

	public final AtomicLong	toWakeUpAbsoluteTime	= new AtomicLong(-1);

	public final E take() throws InterruptedException {
		final ReentrantLock lock = this.lock;
		lock.lockInterruptibly();
		try
		{
			while (true)
			{
				E first = peek();
				if (first == null)
				{
					priorityIncrease.set(1);
					offerIndex = 0;
					service.noScheduleAlarmTime();
					available.await();
				}
				else
				{
					long delay = first.getDelay(TimeUnit.NANOSECONDS);
					boolean imReturn = first.isDone || first.disable || first.invalid;
					if (delay <= 0 || imReturn)
					{
						E x = pollFirst();
						assert x != null;
						if (x == null) throw new NullPointerException("Check TreeSet.remove -> Compare(t1,t2)!");
						if (!isEmpty()) // 当此Queue作为单例并未由多个消费者进行并发操作时 本块代码无价值~
						available.signalAll();
						toWakeUpAbsoluteTime.set(-1);
						// update delay time
						if (x != null && x.offTime > 0)
						{
							x.doTime += x.offTime;
							x.offTime = 0;
							offer(x);
						}
						else return x;
					}
					else
					{
						priorityIncrease.set(1);
						toWakeUpAbsoluteTime.set(first.doTime);
						service.setScheduleAlarmTime(first.doTime);
						available.awaitNanos(delay);
					}
				}
			}
		}
		finally
		{
			lock.unlock();
		}
	}
}
