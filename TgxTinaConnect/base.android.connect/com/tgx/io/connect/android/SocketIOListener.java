package com.tgx.io.connect.android;

import android.util.SparseArray;
import base.tina.core.task.AbstractListener;
import base.tina.core.task.TaskService;
import base.tina.core.task.infc.IDisposable;
import base.tina.core.task.infc.ITaskResult;
import base.tina.external.io.IConnectFeture;
import base.tina.external.io.IoFilter;
import base.tina.external.io.IoSession;
import base.tina.external.io.net.socket.CSocketTask;
import base.tina.external.io.net.socket.LSocketTask;
import base.tina.external.io.net.socket.SocketTask;


public class SocketIOListener
        extends
        AbstractListener
        implements
        IDisposable
{
	final String                                      TAG = "Socket";
	/* 预期容量<=7 同时管理20以上链接的客户端不列入当前的设计范围 */
	private SparseArray<IConnectFeture<ConnectTimer>> conFetures;
	
	public SocketIOListener() {
		super();
		conFetures = new SparseArray<IConnectFeture<ConnectTimer>>(3);
	}
	
	public void addConnectFeture(IConnectFeture<ConnectTimer> feture) {
		if (conFetures.indexOfValue(feture) < 0) conFetures.append(conFetures.size(), feture);
	}
	
	public void iterateConnect(IoFilter filter, boolean networkChanged) {
		for (int i = 0, size = conFetures.size(); i < size; i++)
		{
			IConnectFeture<ConnectTimer> feture = conFetures.valueAt(i);
			feture.connectByTimer(new ConnectTimer(feture, filter, this), networkChanged);
		}
	}
	
	@Override
	public boolean ioHandle(ITaskResult message, TaskService service) {
		boolean isHandled = true;
		switch (message.getSerialNum()) {
			case ConnectTimer.SerialNum:
				break;
			case IoSession.SerialNum:
				IoSession<?> session = (IoSession<?>) message;
				//#debug 
				base.tina.core.log.LogPrinter.d(TAG, session.url + " OK");
				for (int i = 0, size = conFetures.size(); i < size; i++)
				{
					IConnectFeture<ConnectTimer> feture = conFetures.valueAt(i);
					if (!feture.isConnected() && session.url.equals(feture.getTarAddr()))
					{
						feture.finishConnect(session);
						break;
					}
				}
				break;
			case SocketTask.SerialNum:
			case CSocketTask.SerialNum:
				break;
			default:
				isHandled = false;
				break;
		}
		return isHandled;
	}
	
	@Override
	public boolean exceptionCaught(ITaskResult taskResult, TaskService service) {
		boolean isHandled = true;
		Exception e = taskResult.getError();
		switch (taskResult.getSerialNum()) {
			case ConnectTimer.SerialNum:
				//这个地方应该永远跑不进来
				break;
			case IoSession.SerialNum:
				IoSession<?> session = (IoSession<?>) taskResult;
				//#debug error
				base.tina.core.log.LogPrinter.e(TAG, session.url, e);
				for (int i = 0, size = conFetures.size(); i < size; i++)
				{
					IConnectFeture<ConnectTimer> feture = conFetures.valueAt(i);
					if (feture.isMySession(session))
					{
						feture.resetConnect(session);
						break;
					}
				}
				session.reset();
				break;
			case SocketTask.SerialNum:
				//#debug warn
				base.tina.core.log.LogPrinter.w(TAG, e);
				break;
			case CSocketTask.SerialNum:
				CSocketTask task = (CSocketTask) taskResult;
				for (int i = 0, size = conFetures.size(); i < size; i++)
				{
					IConnectFeture<ConnectTimer> feture = conFetures.valueAt(i);
					if (task.url.equals(feture.getTarAddr()))
					{
						feture.onConnectFaild(e);
						break;
					}
				}
				break;
			case LSocketTask.SerialNum:
				//#debug error
				base.tina.core.log.LogPrinter.e(TAG, "LSocketTask!,Check!~~~", e);
				break;
			default:
				isHandled = false;
				break;
		}
		return isHandled;
	}
	
	@Override
	public void dispose() {
		conFetures.clear();
		conFetures = null;
	}
	
	@Override
	public boolean isDisposable() {
		return true;
	}
	
}
