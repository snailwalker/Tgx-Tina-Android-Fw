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

import java.net.SocketTimeoutException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import base.tina.external.io.IoFilter;
import base.tina.external.io.IoSession;
import base.tina.external.io.IoTask;

public class CSocketTask
		extends
		IoTask<NioSocketICon>
		implements
		ISelectorX
{
	public final static int	SerialNum	= SocketTask.SerialNum + 1;
	Selector				selector;
	NioSocketICon			connection;
	volatile boolean		disConnect;
	final long				cTimeout	= TimeUnit.SECONDS.toMillis(60);
	volatile boolean		finishConnect;

	public CSocketTask(String url, IoFilter ioFilter)
	{
		super(0, url, ioFilter);
	}

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public final void initTask() {
		super.initTask();
		isBloker = true;
		isProxy = true;
	}

	@Override
	public final void run() throws Exception {
		selector = Selector.open();
		connection = new NioSocketICon(this);
		connection.open(url, false);
		setAlarmTime(System.currentTimeMillis() + cTimeout);
		int selectedCount = selector.select(cTimeout);
		// Key都是只有一个的，关于连接时使用的key
		if (selectedCount > 0)
		{
			Set<SelectionKey> keys = selector.selectedKeys();
			for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext();)
			{// 此处for循环只会访问到一个数据项，并不会出现多个
				SelectionKey key = iter.next();
				iter.remove();
				if (key.isValid() && key.isConnectable())
				{
					SocketChannel channel = (SocketChannel) key.channel();
					if (channel.finishConnect())
					{
						finishConnect = true;
						ioSession = new IoSession<NioSocketICon>(url, ioFilterChain);
						ioSession.setIConnection(connection);
						ioSession.prefetch();
						connection.finishConnect();
						LSocketTask lSocketTask = LSocketTask.open();
						lSocketTask.exchangeSelector(this);
						if (!lSocketTask.isInit() && !scheduleService.requestService(lSocketTask, true, getListenSerial())) throw new IllegalStateException(
								"LSocketTask is invalid");
						commitResult(ioSession, CommitAction.WAKE_UP);
					}
				}
			}
		}
		else throw new SocketTimeoutException();
	}

	@Override
	public final void dispose() {
		// 由于Exception 跳出时未生成IoSession 所以只需要清理IoConnection即可
		if (connection != null && hasError()) try
		{
			connection.close(false);
		}
		catch (Exception e)
		{
			//#debug
			e.printStackTrace();
		}
		if (selector != null) try
		{
			selector.close();
		}
		catch (Exception e)
		{
			//#debug
			e.printStackTrace();
		}
		selector = null;
		super.dispose();
	}

	@Override
	public final Selector getSelector() {
		return selector;
	}

	@Override
	public final void wakeUp() {
		if (selector != null && selector.isOpen()) selector.wakeup();
	}
}
