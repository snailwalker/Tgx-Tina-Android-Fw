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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeoutException;

import base.tina.core.task.infc.IDisposable;
import base.tina.external.io.IConnection;
import base.tina.external.io.IoUtil;

/**
 * @author Zhangzhuo
 */

public final class NioSocketICon
		implements
		IConnection,
		IDisposable
{
	private SocketChannel		socketChannel;
	private volatile boolean	toClose;
	//#debug info
	public long					connectTime;
	SelectionKey				cwKey, rKey;
	Selector					selector;
	private ISelectorX			iSelectorX;

	public final ISelectorX getISelectorX() {
		return iSelectorX;
	}

	public NioSocketICon(ISelectorX iSelectorX)
	{
		setSelectorX(iSelectorX);
		System.setProperty("java.net.preferIPv6Addresses", "false");
	}

	@Override
	public final void close(boolean await) throws Exception {
		if (await) toClose = true;
		else
		{
			try
			{
				if (socketChannel != null) socketChannel.close();
			}
			finally
			{
				if (cwKey != null)
				{
					cwKey.cancel();
					cwKey.attach(null);
				}
				cwKey = null;
				if (rKey != null)
				{
					rKey.cancel();
					rKey.attach(null);
				}
				rKey = null;
			}
		}
	}

	@Override
	public final void dispose() {
		try
		{
			close(false);
		}
		catch (Exception e)
		{
			//#debug warn
			e.printStackTrace();
		}
		socketChannel = null;
		selector = null;
	}

	/**
	 * @return true 连接已请求过,无需再次请求 <br>
	 *         false 连接未Open或者尚未进行Connect
	 */
	@Override
	public final boolean isOpen() {
		if (socketChannel == null || toClose) return false;
		return socketChannel.isOpen() && (socketChannel.isConnected() || socketChannel.isConnectionPending());
	}

	public final boolean toClose() {
		return toClose;
	}

	/**
	 * @exception NullPointerException
	 *                socketChannel 为空的时候
	 */
	public final boolean isConnected() throws Exception {
		if (socketChannel == null) throw new NullPointerException();
		if (!socketChannel.isOpen()) throw new ClosedChannelException();
		return socketChannel.isConnected();
	}

	final SocketChannel getChannel() throws IOException {
		//即使处于toClose状态对已打开的socketChanel依然需要返回当前this.socketChannel
		if (socketChannel != null && socketChannel.isOpen()) return socketChannel;
		SocketChannel socketChannel = SocketChannel.open();
		socketChannel.configureBlocking(false);
		Socket socket = socketChannel.socket();
		socket.setTcpNoDelay(false);
		socket.setSoLinger(true, 1);
		//#debug 
		base.tina.core.log.LogPrinter.d(null, "sokcet otption:R-" + socket.getReceiveBufferSize() + " :S-" + socket.getSendBufferSize() + " :T-" + socket.getSoTimeout());
		return socketChannel;
	}

	public final void setSocketChannel(SocketChannel channel) {
		socketChannel = channel;
	}

	@Override
	public final boolean open(String toUrl, boolean await) throws IllegalStateException, IllegalArgumentException, TimeoutException, IOException {
		if (toClose) throw new IllegalStateException();
		String[] split = IoUtil.splitURL(toUrl);
		//#debug info
		base.tina.core.log.LogPrinter.i(null, "Actually to Connect: " + toUrl);
		if (await)
		{
			//Socket socket = new Socket(split[IoUtil.HOST], Integer.parseInt(split[IoUtil.PORT]));
		}
		else
		{
			SocketChannel socketChannel = getChannel();
			cwKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
			//#debug info
			connectTime = System.currentTimeMillis();
			InetSocketAddress address = new InetSocketAddress(split[IoUtil.HOST], Integer.parseInt(split[IoUtil.PORT]));
			//#debug info
			base.tina.core.log.LogPrinter.i(null, "DNS time mills:" + (System.currentTimeMillis() - connectTime));
			socketChannel.connect(address);
			this.socketChannel = socketChannel;
		}
		return true;
	}

	public final void finishConnect() {
		cwKey.cancel();
		cwKey.attach(null);
		cwKey = null;
	}

	public final SelectionKey registerRead(Object att) throws ClosedChannelException {
		if (selector == null || !selector.isOpen() || !isOpen()) return null;
		return rKey = socketChannel.register(selector, SelectionKey.OP_READ, att);
	}

	public final SelectionKey register(int interestedOps, Object att) throws ClosedChannelException {
		if (selector == null || !selector.isOpen() || !isOpen() || interestedOps == 0) return null;
		return rKey = socketChannel.register(selector, interestedOps, att);
	}

	final void setSelectorX(ISelectorX iSelectorX) {
		this.iSelectorX = iSelectorX;
		if (iSelectorX != null) this.selector = iSelectorX.getSelector();
	}

	public final void setInterestedInRead(boolean isInterested) throws CancelledKeyException {
		if (rKey == null || !rKey.isValid()) return;
		int oldInterestOps = rKey.interestOps();
		int newInterestOps = oldInterestOps;
		if (isInterested) newInterestOps |= SelectionKey.OP_READ;
		else newInterestOps &= ~SelectionKey.OP_READ;
		if (oldInterestOps != newInterestOps) rKey.interestOps(newInterestOps);
	}

	public final void setInterestedInWrite(boolean isInterested) throws CancelledKeyException {
		if (rKey == null || !rKey.isValid()) return;
		int newInterestOps = rKey.interestOps();
		if (isInterested) newInterestOps |= SelectionKey.OP_WRITE;
		else newInterestOps &= ~SelectionKey.OP_WRITE;
		//#ifdef debug
		String str = "";
		if ((newInterestOps & SelectionKey.OP_WRITE) != 0) str = "SelectionKey.OP_WRITE|";
		if ((newInterestOps & SelectionKey.OP_READ) != 0) str = str + "|SelectionKey.OP_READ|";
		if ((newInterestOps & SelectionKey.OP_CONNECT) != 0) str = str + "|SelectionKey.OP_CONNECT";
		if ((newInterestOps & SelectionKey.OP_ACCEPT) != 0) str = str + "|SelectionKey.OP_ACCEPT";
		//#debug
		base.tina.core.log.LogPrinter.d(null, "toWrite Key -> " + str);
		//#endif
		rKey.interestOps(newInterestOps);
	}

	@Override
	public final int available() {
		return -1;
	}

	@Override
	public final boolean isDisposable() {
		return true;
	}

}
