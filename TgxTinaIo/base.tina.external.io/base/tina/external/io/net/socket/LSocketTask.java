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
 package base.tina.external.io.net.socket;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import base.tina.core.task.Task;
import base.tina.core.task.infc.ITaskResult;
import base.tina.external.io.IoFilter;
import base.tina.external.io.IoSession;

public class LSocketTask
		extends
		Task
		implements
		ISelectorX
{
	static LSocketTask							singleInstance;
	public final static int						SerialNum	= CSocketTask.SerialNum + 1;

	private Selector							selector, selectorX, swap_selector;
	final ConcurrentLinkedQueue<CSocketTask>	toRegister;
	final LinkedList<SocketTask>				timeOutTasks;
	final ConcurrentLinkedQueue<SocketTask>		todoTasks;

	private LSocketTask()
	{
		super(SerialNum);
		toRegister = new ConcurrentLinkedQueue<CSocketTask>();
		todoTasks = new ConcurrentLinkedQueue<SocketTask>();
		timeOutTasks = new LinkedList<SocketTask>();
		if (singleInstance != null) throw new IllegalStateException("create more Lsocket");
		singleInstance = this;
	}

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

	@Override
	public void initTask() {
		isBloker = true;
		isCycle = true;
		// -Djava.nio.channels.spi.SelectorProvider=sun.nio.ch.EPollSelectorProvider
		super.initTask();
	}

	final AtomicBoolean	disConnectAll	= new AtomicBoolean(false);
	final AtomicBoolean	hasDisConnect	= new AtomicBoolean(false);
	volatile long		delayTime, waitStart, idleTime;
	final AtomicBoolean	wakenUp			= new AtomicBoolean(false);
	private int			selectorError;

	final void offerWrite(SocketTask socketTask) {
		if (todoTasks.offer(socketTask) && socketTask.ioSession != null) socketTask.ioSession.offerWrite();
	}

	final void exchangeSelector(CSocketTask cSocketTask) {
		toRegister.offer(cSocketTask);
	}

	@Override
	public final void run() throws Exception {
		if (selector == null) selectorX = selector = SelectorProvider.provider().openSelector();
		else if (swap_selector != null)
		{
			if (selector.isOpen()) selector.close();
			selector = swap_selector;
			swap_selector = null;
		}
		CSocketTask cSocketTask;
		do
		{
			cSocketTask = toRegister.poll();
			if (cSocketTask == null) continue;
			NioSocketICon connection = cSocketTask.ioSession.getConnection();
			if (cSocketTask.ioSession != null && !cSocketTask.ioSession.disconnect.get() && !cSocketTask.hasError() && connection != null)
			{
				connection.setSelectorX(this);
				connection.registerRead(cSocketTask.ioSession);
			}
			cSocketTask.setDone();
			commitResult(cSocketTask);
		}
		while (!toRegister.isEmpty());
		SocketTask socketTask = null;
		for (Iterator<SocketTask> iter = todoTasks.iterator(); iter.hasNext();)
		{
			socketTask = iter.next();
			if (socketTask.ioSession == null || socketTask.ioSession.disconnect.get() || socketTask.hasError())
			{
				iter.remove();
				socketTask.setDone();
				commitResult(socketTask);// 从这个节点跳出之后，任务回到CallBack系统。
				continue;
			}
			NioSocketICon connection = socketTask.ioSession.getConnection();
			connection.setInterestedInWrite(true);
		}

		waitStart = System.currentTimeMillis();
		if (wakenUp.getAndSet(false)) selector.wakeup();
		int selectedCount;
		if (delayTime > 0)
		{
			long absoluteTime = waitStart + delayTime;
			setAlarmTime(absoluteTime);
			selectedCount = selector.select(delayTime);
		}
		else selectedCount = selector.select();
		delayTime = -1;
		idleTime = System.currentTimeMillis() - waitStart;
		// ----------------------------------------------------------------------------------
		/**
		 * @author Zhangzhuo Fix selector(long time) no block bug
		 */
		if (idleTime < 50 && selectedCount == 0)
		{
			selectorError++;
			Thread.yield();
			if (selectorError > 10)
			{
				//#debug warn
				base.tina.core.log.LogPrinter.w(null, "Selector error:runtime error[ selector invalid ]");
				selectorX = SelectorProvider.provider().openSelector();
				for (SelectionKey key : selector.keys())
				{

					@SuppressWarnings("unchecked")
					final IoSession<NioSocketICon> ioSession = (IoSession<NioSocketICon>) key.attachment();
					if (key.isValid() && key.interestOps() != 0 && ioSession != null)
					{
						NioSocketICon iCon = ioSession.getConnection();
						iCon.setSelectorX(this);
						iCon.register(key.interestOps(), ioSession);
						continue;
					}
					key.cancel();
					if (ioSession != null)
					{
						ioSession.setError(new ClosedChannelException());
						for (;;)
						{
							boolean dis = ioSession.disconnect.get();
							if (dis || ioSession.disconnect.compareAndSet(false, true)) break;
						}
					}
					for (;;)
					{
						boolean hasDis = hasDisConnect.get();
						if (hasDis || hasDisConnect.compareAndSet(false, true)) break;
					}

				}
				swap_selector = selectorX;
				selectorError = 0;
			}
		}
		else selectorError = 0;
		// ----------------------------------------------------------------------------------
		if (selectedCount > 0)
		{
			Set<SelectionKey> keys = selector.selectedKeys();
			for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();)
			{
				SelectionKey key = iter.next();
				iter.remove();
				@SuppressWarnings("unchecked")
				IoSession<NioSocketICon> ioSession = (IoSession<NioSocketICon>) key.attachment();
				if (ioSession == null)
				{
					key.cancel();
					continue;
				}
				if (key.isValid() && key.isReadable())
				{
					toRead((SocketChannel) key.channel(), ioSession);
				}
				if (key.isValid() && key.isWritable())
				{
					toWrite((SocketChannel) key.channel(), ioSession);
				}
			}
		}

		if (!timeOutTasks.isEmpty())
		{
			long curTime = System.currentTimeMillis();
			// 此处对于超时的操作方法并不符合接口行为的原始定义
			socketTask = null;
			for (Iterator<SocketTask> iter = timeOutTasks.iterator(); iter.hasNext();)
			{
				socketTask = iter.next();
				if (socketTask.ioSession.disconnect.get())
				{
					socketTask.setError(new ClosedChannelException());
					hasDisConnect.compareAndSet(false, true);
				}
				if (socketTask.timeoutCall != null && !socketTask.hasError())
				{
					if (socketTask.timeoutCall.isEnabled())
					{
						if (socketTask.ioSession.isTimeOut || socketTask.ioSession.disconnect.get())
						{
							socketTask.setDone();
							socketTask.timeoutCall.onInvalid(socketTask);
						}
						else if (socketTask.timeoutCall.isTimeout(curTime, socketTask))
						{
							socketTask.setDone();
							socketTask.timeoutCall.doTimeout(socketTask);
							socketTask.ioSession.isTimeOut = true;
							socketTask.ioSession.setError(new SocketTimeoutException("iosession: -> do:" + socketTask.toWrite.toString()));
							socketTask.ioSession.disconnect.compareAndSet(false, true);
							socketTask.setError(socketTask.ioSession.getError());
							hasDisConnect.compareAndSet(false, true);
						}
						else continue;// 未触发超时逻辑
					}
					else socketTask.setDone();
				}
				else
				{
					// 虽然在add的时候已经加了限制,但是不排除在特殊情况下导致null的出现.此处作为保护性代码出现
					socketTask.setDone();
				}
				commitResult(socketTask);
				iter.remove();
			}
			scheduleService.commitNotify();
		}

		if (timeOutTasks.isEmpty())
		{
			needAlarm = false;
			delayTime = -1;
		}
		else
		{
			Collections.sort(timeOutTasks, toWaitComparator);
			delayTime = TimeUnit.SECONDS.toMillis(timeOutTasks.getFirst().timeOut);
		}

		if (hasDisConnect.get())
		{
			Set<SelectionKey> keys = selector.keys();
			if (keys.size() > 0)
			{
				IoSession<?> ioSession;
				for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();)
				{
					SelectionKey key = iter.next();
					ioSession = (IoSession<?>) key.attachment();
					if (ioSession == null) continue;
					else if (ioSession.disconnect.get()) try
					{
						ioSession.close(false);
					}
					catch (Exception e)
					{
						//#debug warn
						e.printStackTrace();
					}
					finally
					{
						if (!ioSession.hasError()) ioSession.setError(new ClosedChannelException());
						commitResult(ioSession);
					}
				}
				scheduleService.commitNotify();
			}
			hasDisConnect.set(false);
		}
	}

	public final static LSocketTask open() {
		if (singleInstance == null) singleInstance = new LSocketTask();
		return singleInstance;
	}

	@Override
	public final void wakeUp() {
		if (wakenUp.compareAndSet(false, true) && selector != null && selector.isOpen()) selector.wakeup();
	}

	@Override
	public final Selector getSelector() {
		return selectorX;
	}

	final Comparator<SocketTask>	toWaitComparator	= new Comparator<SocketTask>()
														{

															@Override
															public int compare(SocketTask lhs, SocketTask rhs) {
																if (lhs == null) return 1;
																if (rhs == null) return -1;
																long lhsST = lhs.getStartTime();
																long rhsST = rhs.getStartTime();
																return lhs.timeOut + lhsST > rhs.timeOut + rhsST ? 1 : lhs.timeOut + lhsST < rhs.timeOut + rhsST ? -1 : 0;
															}
														};

	@Override
	public final void dispose() {
		for (Iterator<CSocketTask> iter = toRegister.iterator(); iter.hasNext();)
		{
			CSocketTask cSocketTask = iter.next();
			cSocketTask.setError(getError());
			commitResult(cSocketTask);
			iter.remove();
		}
		for (Iterator<SocketTask> iter = todoTasks.iterator(); iter.hasNext();)
		{
			SocketTask socketTask = iter.next();
			socketTask.setError(getError());
			commitResult(socketTask);
			iter.remove();

		}
		for (Iterator<SocketTask> iter = timeOutTasks.iterator(); iter.hasNext();)
		{
			SocketTask socketTask = iter.next();
			socketTask.setError(getError());
			commitResult(socketTask);
			iter.remove();
		}
		toRegister.clear();
		todoTasks.clear();
		timeOutTasks.clear();
		if (selector != null) try
		{
			selector.close();
		}
		catch (Exception e)
		{
			//#debug warn
			e.printStackTrace();
		}
		selector = null;
		singleInstance = null;
		super.dispose();
	}

	final void toRead(SocketChannel socketChannel, IoSession<NioSocketICon> ioSession) throws Exception {
		try
		{
			IoFilter filter = ioSession.getFilterChain();
			ITaskResult result = null;
			int actuallyRead;
			do
			{
				actuallyRead = socketChannel.read(ioSession.readBuffer);
				if (actuallyRead < 0)
				{
					//#debug
					base.tina.core.log.LogPrinter.d(null, "read -- EOF");
					throw new EOFException("NioConnection Read EOF!");
				}
				else if (actuallyRead == 0)
				{
					//#debug
					base.tina.core.log.LogPrinter.d(null, "read 0 / nothing more");
					return;
				}
				ioSession.readBuffer.flip();

				// Version 0
				// while (ioSession.readBuffer.hasRemaining())
				// {
				// if (result != null) commitResult(result);
				// result = filter.filterChainDecode(this, ioSession,
				// ioSession.readBuffer);
				// }
				// if (result != null) commitResult(result,
				// CommitAction.WAKE_UP);
				// Version 1
				while (ioSession.readBuffer.hasRemaining())
				{
					result = filter.filterChainDecode(this, ioSession, ioSession.readBuffer);
					if (result != null)
					{
						ioSession.receiveOk();
						commitResult(result, CommitAction.WAKE_UP);
						if (!timeOutTasks.isEmpty())
						{
							SocketTask socketTask = null;
							for (Iterator<SocketTask> iter = timeOutTasks.iterator(); iter.hasNext();)
							{
								socketTask = iter.next();
								if (socketTask.ioSession == ioSession)
								{
									iter.remove();
									socketTask.setDone();
									if (socketTask.timeoutCall != null && socketTask.timeoutCall.isEnabled()) socketTask.timeoutCall.onInvalid(socketTask);
									commitResult(socketTask);
								}
							}
						}
					}
				}
				ioSession.readBuffer.clear();
			}
			while (actuallyRead > 0);

		}
		catch (IOException e)
		{
			//#debug warn
			e.printStackTrace();
			ioSession.setError(e);
			for (;;)
			{
				boolean dis = ioSession.disconnect.get();
				if (dis || ioSession.disconnect.compareAndSet(false, true))
				{
					//#debug info
					base.tina.core.log.LogPrinter.i(null, "IoSession read Ex disconnect:" + ioSession.url + "@" + ioSession.hashCode(), e);
					break;
				}
			}
			for (;;)
			{
				boolean hasDis = hasDisConnect.get();
				if (hasDis || hasDisConnect.compareAndSet(false, true)) break;
			}
		}
	}

	/**
	 * @param socketChannel
	 * @throws Exception
	 */
	final void toWrite(SocketChannel socketChannel, IoSession<NioSocketICon> ioSession) throws Exception {
		int write_Count = 0;
		ByteBuffer toWrite = null;
		int actuallyWrite = 0;
		long curTime = System.currentTimeMillis();

		NioSocketICon connection = ioSession.getConnection();
		IoFilter filter = ioSession.getFilterChain();
		SocketTask socketTask = null;
		if (!todoTasks.isEmpty()) SocketIterator:
		for (Iterator<SocketTask> iter = todoTasks.iterator(); iter.hasNext();)
		{
			socketTask = iter.next();
			if (socketTask == null)
			{
				iter.remove();
				continue SocketIterator;
			}
			if (socketTask.ioSession != ioSession) continue SocketIterator;
			iter.remove();
			SocketTask:
			{
				if (!ioSession.disconnect.get()) try
				{
					Object result = filter.filterChainEncode(socketTask, ioSession, socketTask.toWrite);
					if (result != null) toWrite = result instanceof byte[] ? ByteBuffer.wrap((byte[]) result) : result instanceof ByteBuffer ? (ByteBuffer) result : null;
					break SocketTask;
				}
				catch (Exception e)
				{
					//#debug warn
					e.printStackTrace();
					socketTask.setError(e);
				}
				else socketTask.setError(new ClosedChannelException());
				ioSession.pollWrite();
				commitResult(socketTask);
				connection.setInterestedInWrite(ioSession.notWriteOver());
				continue SocketIterator;
			}
			ioSession.pollWrite();
			WriteChannel:
			{
				if (toWrite != null && toWrite.hasRemaining())
				{
					//#debug info
					int need2write = toWrite.remaining();
					WriteCycle:
					while (toWrite.hasRemaining())
					{
						try
						{
							actuallyWrite = socketChannel.write(toWrite);
						}
						catch (Exception e)
						{
							//#debug error
							base.tina.core.log.LogPrinter.e(null, e);
							actuallyWrite = -1;
						}
						if (actuallyWrite > 0)
						{
							//#debug info 
							if (need2write > actuallyWrite) base.tina.core.log.LogPrinter.i(null, "socket write:" + actuallyWrite);
							continue WriteCycle;// 成功写出,继续检查toWrite容量是否全部写完
						}
						if (actuallyWrite == 0 && write_Count++ < 140)
						{
							//#debug
							base.tina.core.log.LogPrinter.d(null, "toWrite nothing has been written!");
							try
							{
								wait(200);// 有点问题 可能会引发电源管理问题
							}
							catch (IllegalMonitorStateException ie)
							{
								//#debug warn
								base.tina.core.log.LogPrinter.w(null, "toWrite 写不出去,而且不停的有请求进来,导致多线程并发操作LSocketTask");
							}
							continue WriteCycle;
						}
						else if (actuallyWrite < 0) ioSession.setError(new ClosedChannelException());
						else if (actuallyWrite == 0) ioSession.setError(new SocketTimeoutException("write data error!"));
						ioSession.disconnect.compareAndSet(false, true);
						socketTask.setError(ioSession.getError());
						hasDisConnect.compareAndSet(false, true);
						break WriteChannel;// 将由hasDisConnect启动,在后续的操作过程中完成ioSession的关闭以及提交回调
					}
					if (socketTask.timeOut > 0 && socketTask.timeoutCall != null && connection.isOpen())
					{
						socketTask.setStartTime(curTime);
						timeOutTasks.add(socketTask);
					}
					else
					{
						socketTask.setDone();
						commitResult(socketTask);
					}
					write_Count = 0;
					//#debug info
					base.tina.core.log.LogPrinter.i(null, "socket write success");
				}
				else
				{
					if (toWrite == null) socketTask.setError(new NullPointerException("toWrite is null"));
					if (!toWrite.hasRemaining()) socketTask.setError(new IllegalAccessException("toWrite has no data!"));
					commitResult(socketTask);
					//#debug warn
					base.tina.core.log.LogPrinter.w(null, "no data in socket task!");
				}
			}
			connection.setInterestedInWrite(ioSession.notWriteOver());
		}
	}
}
