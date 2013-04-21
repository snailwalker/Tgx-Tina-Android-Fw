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
package com.tgx.tina.android.ipc.framework;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

public abstract class BaseBridge
				implements
				ServiceConnection
{
	protected final Context	mContext;
	private final IBridge	mIBridge;
	private Handler			recvHandler;
	private boolean			isBridgeOn;

	protected BaseBridge(Context context, IBridge iBridge, Handler handler)
	{
		if (iBridge == null || context == null) throw new NullPointerException();
		mContext = context;
		mIBridge = iBridge;
		recvHandler = handler;
		service_action = context.getPackageName() + DefaultConsts.serviceAction + hashCode();
	}

	private RemoteService	rServiceStub;
	private ClientReceiver	clientReceiver;

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		rServiceStub = RemoteService.Stub.asInterface(service);
		try
		{
			if (clientReceiver != null) throw new IllegalStateException("no connected ,has receiver!");
			String sActionStr = rServiceStub.sActionStr(service_action);
			IntentFilter serviceFilter = new IntentFilter(sActionStr);
			mContext.registerReceiver(clientReceiver = new ClientReceiver(), serviceFilter, mIBridge.actionCPermission(), recvHandler);
			sendCMD(DefaultConsts.SERVERACTION_CLIENT_START, null);
			isBridgeOn = true;
			onRemoteConnected();
			//#debug verbose
			base.tina.core.log.LogPrinter.v(null, "Bridge Connected");
		}
		catch (RemoteException e)
		{
			e.printStackTrace();
			onRemoteConnectedEx();
		}
	}

	protected abstract void onRemoteConnected();

	protected abstract void onRemoteConnectedEx();

	public void startBind(String action) {
		if (isBridgeOn()) return;
		if (action == null || "".equals(action.trim())) throw new IllegalArgumentException("remote_action is invaild!");
		mContext.startService(new Intent(action));
		mContext.bindService(new Intent(action), this, Context.BIND_AUTO_CREATE);
	}

	public void startBind() {
		startBind(mIBridge.remoteBootAction());
	}

	public void stopBind() {
		mContext.unbindService(this);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		if (clientReceiver != null)
		{
			sendCMD(DefaultConsts.SERVERACTION_CLIENT_STOP, null);
			mContext.unregisterReceiver(clientReceiver);
		}
		clientReceiver = null;
		rServiceStub = null;
		isBridgeOn = false;
		//#debug verbose
		base.tina.core.log.LogPrinter.v(null, "Bridge Disconnected");
	}

	public final boolean isBridgeOn() {
		return isBridgeOn;
	}

	public final RemoteService getRemoteService() {
		return rServiceStub;
	}

	public final String tarAction() {
		return service_action;
	}

	/**
	 * 仅能在Activity的UI-Thread中进行实现
	 * 
	 * @param handler
	 */
	public final void changeCurHandler(Handler handler) {
		recvHandler = handler;
	}

	private final class ClientReceiver
					extends
					BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent) {
			ArrayList<Bundle> bundleList = intent.getParcelableArrayListExtra("bundle");
			LoopList:
			for (Bundle bundle : bundleList)
			{
				Message msg = Message.obtain();
				int cmd = bundle.getInt("cmd", -2);
				boolean hasExternal = bundle.getBoolean("has_external", false);
				msg.what = cmd;
				msg.arg1 = DefaultConsts.NO_DATA_ARG;
				switch (cmd) {
					case DefaultConsts.BaseService_BroadcastReceived:
						retry:
						for (;;)
						{
							boolean sent = sentCondition.get();
							if (!sent || sentCondition.compareAndSet(true, false)) break retry;
						}
						sendCMD(null);// 清理下一批未发送的广播流
						//#debug verbose
						base.tina.core.log.LogPrinter.v(null, "Bridge clean buffered commands!");
						continue LoopList;
					case DefaultConsts.BaseService_ServiceStatus:
						msg.arg1 = DefaultConsts.HAS_DATA_ARG;
						//#debug verbose
						base.tina.core.log.LogPrinter.v(null, "Bridge check service status ,received!");
						break;
				}
				onReceiveUpdate(cmd, bundle);
				if (hasExternal) msg.setData(bundle);
				if (recvHandler != null) recvHandler.sendMessage(msg);
				else System.err.println("recevierHandler is null!");
			}
		}
	}

	// 广播队列
	private final Queue<Bundle>	broadcastQueue	= new ConcurrentLinkedQueue<Bundle>();
	private final AtomicBoolean	sentCondition	= new AtomicBoolean();

	/**
	 * onStart()方法中不可调用此方法，由于动态广播系统尚未完成对接过程。必须在onRemoteConnected()之后使用
	 * 
	 * @param cmd
	 * @param bundle
	 */
	public final void sendCMD(int cmd, Bundle bundle) {
		//#debug info
		base.tina.core.log.LogPrinter.i(this.getClass().getSimpleName(), "Send CMD:" + cmd);
		if (bundle == null) bundle = new Bundle();
		bundle.putInt("cmd", cmd);
		sendCMD(bundle);
	}

	public final void sendCMD(Bundle bundle) {
		if (bundle != null) broadcastQueue.add(bundle);
		if (broadcastQueue.isEmpty() || sentCondition.get()) return;
		for (;;)
		{
			boolean sent = sentCondition.get();
			if (sent) break;
			if (sentCondition.compareAndSet(false, true))
			{
				ArrayList<Bundle> bundleList = new ArrayList<Bundle>(broadcastQueue);
				broadcastQueue.removeAll(bundleList);
				Intent intent = new Intent(service_action);
				intent.putParcelableArrayListExtra("bundle", bundleList);
				mContext.sendBroadcast(intent, mIBridge.actionSPermission());
				break;
			}
		}
	}

	private String	service_action	= "com.android.tina" + DefaultConsts.serviceAction + hashCode();

	protected abstract void onReceiveUpdate(int cmd, Bundle bundle);
}
