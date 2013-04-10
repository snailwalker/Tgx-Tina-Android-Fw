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

import base.tina.core.task.infc.ITaskFactory;
import base.tina.external.io.net.socket.DisSocketTask;
import base.tina.external.io.net.socket.NioSocketICon;
import base.tina.external.io.net.socket.SocketTask;

/**
 * @version 1.0
 * @author ZhangZhuo 1.0 版本提供schema:socket://支持 1.1 版本提供schema:file://支持 1.2
 *         版本提供schema:http://支持
 * @param <E>
 */
public class IoTaskFactory
		implements
		ITaskFactory<String, IoSession<?>, IoFilter, IoTask<?>>
{
	@Override
	public IoTask<?> createTask(String url, int mode, IoFilter ioFilter) {
		if (url.startsWith("socket"))
		{
			SocketTask socketTask = new SocketTask(url, ioFilter);
			return socketTask;
		}

		//#debug info
		base.tina.core.log.LogPrinter.i("IoTaskFactory", "no create task!");
		return null;
	}

	@Override
	public IoTask<?> createTask(IoSession<?> ioSession, IoFilter ioFilter) {
		if (ioSession.status.equals(IoSession.Status.PREFETCHED))
		{
			if (ioSession.url.startsWith("socket"))
			{
				@SuppressWarnings("unchecked")
				SocketTask socketTask = new SocketTask((IoSession<NioSocketICon>) ioSession, ioFilter);
				return socketTask;
			}
		}
		return null;
	}

	public IoTask<?> createTask(IoSession<?> ioSession) {
		if (ioSession.url.startsWith("socket"))
		{
			@SuppressWarnings("unchecked")
			SocketTask socketTask = new SocketTask((IoSession<NioSocketICon>) ioSession, ioSession.getFilterChain());
			return socketTask;
		}
		return null;
	}

	@Override
	public IoTask<?> createTask(IoSession<?> ioSession, int mode) {
		if (ioSession.url.startsWith("socket") && mode == IConnection.CLOSE)
		{
			@SuppressWarnings("unchecked")
			DisSocketTask disSocketTask = new DisSocketTask((IoSession<NioSocketICon>) ioSession);
			return disSocketTask;
		}
		return null;
	}

	@Override
	public boolean isSurport(IoTask<?> task) {
		if (task.url.startsWith("socket") || task.url.startsWith("http") || task.ioSession != null) return true;
		return false;
	}

	@Override
	public byte getType(IoTask<?> task) {
		String[] urlSplit = IoUtil.splitURL(task.url);
		if (urlSplit[IoUtil.PROTOCOL].equals("socket")) return socketType;
		else if (urlSplit[IoUtil.PROTOCOL].equals("http")) return httpType;
		return 0;
	}

	final static byte	rmsType	= 1, httpType = rmsType + 1, socketType = httpType + 1, fileType = socketType + 1, smsType = fileType + 1, mmsType = smsType + 1,
			shttpType = mmsType + 1, contactType = shttpType + 1;

}
