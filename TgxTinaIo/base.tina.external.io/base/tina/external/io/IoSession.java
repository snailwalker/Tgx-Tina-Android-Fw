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

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import base.tina.core.task.AbstractResult;
import base.tina.core.task.infc.ICloseable;
import base.tina.core.task.infc.IDisposable;

public class IoSession<E extends IConnection>
		extends
		AbstractResult
		implements
		ICloseable
{
	public final String			url;
	final int					resource;
	final static int			BUFFER_SIZE	= 0x10000;
	public final ByteBuffer		readBuffer;
	private boolean				receiveData;
	public boolean				isTimeOut;
	public final AtomicBoolean	disconnect	= new AtomicBoolean(true);

	public IoSession(String address, IoFilter filter)
	{
		super();
		readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
		url = address;
		setFilterChain(filter);
		resource = hashCode();
		status = Status.UNREALIZED;
	}

	private final AtomicInteger	writeCount	= new AtomicInteger(0);

	public final boolean notWriteOver() {
		return writeCount.get() > 0;
	}

	public final int offerWrite() {
		if (writeCount.get() < 0) writeCount.set(0);
		return writeCount.getAndIncrement();
	}

	public final int pollWrite() {
		receiveData = false;
		return writeCount.getAndDecrement();
	}

	public final void receiveOk() {
		receiveData = true;
	}

	public final boolean received() {
		return receiveData;
	}

	public final boolean isInvalid() {
		return status == Status.INVAILD || disconnect.get();
	}

	public Status	status;

	public static enum Status
	{
		UNREALIZED, REALIZED, PREFETCHED, INVAILD
	};

	public final Status getStatus() {
		return status;
	}

	/**
	 * 此方法将在processor线程中被NotifyObserved方法驱动
	 */
	@Override
	public final void dispose() {
		status = Status.INVAILD;
		if (iConnection != null) try
		{
			iConnection.close(false);
		}
		catch (Exception e)
		{
			//#debug
			e.printStackTrace();
		}
		iConnection = null;
		if (attach != null && attach.isDisposable()) attach.dispose();
		attach = null;
		super.dispose();
	}

	@Override
	public final void close(boolean await) throws Exception {
		if (iConnection != null) iConnection.close(await);
	}

	/**
	 * 仅会在CallBack 中使用。
	 */
	public final void reset() {
		status = Status.UNREALIZED;
		disconnect.set(true);
		writeCount.set(0);
		receiveData = false;
		disposable = true;
	}

	public final IoFilter getFilterChain() {
		return ioFilterChain;
	}

	public final E getConnection() {
		return iConnection;
	}

	private E			iConnection;
	private IoFilter	ioFilterChain;

	/**
	 * @param connection
	 */
	public final void setIConnection(E connection) {
		iConnection = connection;
		status = Status.REALIZED;
	}

	public final void setFilterChain(IoFilter filterChain) {
		ioFilterChain = filterChain;
	}

	public final void prefetch() {
		status = Status.PREFETCHED;
		disposable = false;
		disconnect.set(false);
	}

	private IDisposable	attach;

	public final void setTag(IDisposable tag) {
		attach = tag;
	}

	public final IDisposable getTag() {
		return attach;
	}

	public final static int	SerialNum	= -IoTask.SerialDomain + 1;

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

}
