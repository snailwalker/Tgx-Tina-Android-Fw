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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import base.tina.core.task.android.ATaskService;
import base.tina.core.task.timer.TimerTask;

import com.tgx.tina.android.ipc.framework.RemoteService.Stub;

public abstract class BaseService
				extends
				Service
{
	public ATaskService				mAService;
	public final RemoteService.Stub	remoteBinder	= new Stub()
													{

														@Override
														public final void register(String account, String password) throws RemoteException {
															BaseService.this.register(account, password);
														}

														@Override
														public final void login(String account, String password, String salt) throws RemoteException {
															BaseService.this.login(account, password, salt);
														}

														@Override
														public final void bindOthers(String jsonArg) throws RemoteException {
															BaseService.this.bindOthers(jsonArg);
														}

														@Override
														public final String sActionStr(String cActionStr) throws RemoteException {
															ActionReceiver receiver = new ActionReceiver(BaseService.this);
															IntentFilter filter = new IntentFilter(cActionStr);
															registerReceiver(receiver, filter, actionSPermission(), null);
															actionReceivers.put(cActionStr, receiver);
															return service_update_ui;
														}

														@Override
														public boolean onNoAction(String cActionStr) throws RemoteException {
															ActionReceiver receiver = actionReceivers.remove(cActionStr);
															if (receiver != null) unregisterReceiver(receiver);
															return false;
														}

														@Override
														public int remotePID() throws RemoteException {
															return Process.myPid();
														}

														@Override
														public String remoteClazz() throws RemoteException {
															return BaseService.this.getClass().getName();
														}
													};

	protected abstract void register(String account, String password);

	protected abstract void login(String account, String password, String salt);

	protected abstract void bindOthers(String jsonArg);

	@Override
	public final IBinder onBind(Intent intent) {
		if (mAService == null) throw new RuntimeException("ATaskService isn't start!");
		return remoteBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		return true;
	}

	@Override
	public void onRebind(Intent intent) {
		super.onRebind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		//#ifdef debug
		base.tina.external.android.log.AndroidPrinter.createByService(this, false);
		//#endif 
		service_update_ui = getPackageName() + DefaultConsts.serviceAction + hashCode();
		_instance = this;
	}

	private static BaseService	_instance;

	public static BaseService getInstance() {
		return _instance;
	}

	public final class ActionReceiver
					extends
					BroadcastReceiver
	{
		BaseService	service;

		public ActionReceiver(BaseService service)
		{
			this.service = service;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			ArrayList<Bundle> bundleList = intent.getParcelableArrayListExtra("bundle");
			if (bundleList == null) { return; }
			int cmd;
			for (Bundle bundle : bundleList)
			{

				cmd = bundle.getInt("cmd", -9999);
				switch (cmd) {
					case DefaultConsts.SERVERACTION_CLIENT_START:
						//#debug verbose
						base.tina.core.log.LogPrinter.v(null, "Bridge Start");
						Bundle serviceStatusBundle = getServiceStatus();
						service.updateClient(DefaultConsts.BaseService_ServiceStatus, serviceStatusBundle);
						break;
					case DefaultConsts.SERVERACTION_CLIENT_STOP:
						break;
					case DefaultConsts.SERVERACTION_CLIENT_LOG:
						break;
					default:
						onActionReceive(cmd, bundle);
						break;
				}
			}
			service.updateClient(DefaultConsts.BaseService_BroadcastReceived, null);
		}
	}

	protected abstract Bundle getServiceStatus();

	protected abstract String actionSPermission();

	protected abstract String actionCPermission();

	protected abstract void onActionReceive(int cmd, Bundle bundle);

	final HashMap<String, ActionReceiver>		actionReceivers				= new HashMap<String, BaseService.ActionReceiver>(4);
	private final LinkedList<BroadcastReceiver>	dynamicRegisters			= new LinkedList<BroadcastReceiver>();
	private volatile long						lastSendBroadcastTime;																			// 上次发送广播时间
	private final AtomicBoolean					broadcastDirectIntoQueue	= new AtomicBoolean(false);										// 广播直接进待发队列
	private Queue<Bundle>						broadcastQueue				= new ConcurrentLinkedQueue<Bundle>();
	private static final long					SEND_BROADCAST_TIMESPACE	= 200;																// 发送广播最小时间间隔
	private String								service_update_ui			= "com.android.tina" + DefaultConsts.serviceAction + hashCode();

	public void updateClient(int broadcastType, Bundle bundle) {
		if (bundle == null) bundle = new Bundle();
		else bundle.putBoolean("has_external", true);
		bundle.putInt("cmd", broadcastType);
		boolean intoQueue = broadcastDirectIntoQueue.get();
		if (intoQueue || actionReceivers.isEmpty()) broadcastQueue.add(bundle);
		else
		{
			if (System.currentTimeMillis() - lastSendBroadcastTime > SEND_BROADCAST_TIMESPACE)
			{
				ArrayList<Bundle> bundleList = new ArrayList<Bundle>(broadcastQueue.size() + 1);
				bundleList.addAll(broadcastQueue);
				broadcastQueue.removeAll(bundleList);
				bundleList.add(bundle);
				Intent i = new Intent(service_update_ui);
				i.putParcelableArrayListExtra("bundle", bundleList);
				sendBroadcast(i, actionCPermission());
				lastSendBroadcastTime = System.currentTimeMillis();
			}
			else
			{
				retry:
				for (;;)
				{
					intoQueue = broadcastDirectIntoQueue.get();
					if (intoQueue || broadcastDirectIntoQueue.compareAndSet(false, true)) break retry;
				}
				broadcastQueue.add(bundle);
				mAService.requestService(new BroadCastTimer(SEND_BROADCAST_TIMESPACE, TimeUnit.MILLISECONDS), false);
			}
		}
	}

	public final class BroadCastTimer
					extends
					TimerTask
	{
		public BroadCastTimer(long duration, TimeUnit timeUnit)
		{
			super(duration, timeUnit);
		}

		public final static int	SerialNum	= SerialDomain - 1;

		@Override
		public int getSerialNum() {
			return SerialNum;
		}

		@Override
		protected boolean doTimeMethod() {
			retry:
			for (;;)
			{
				boolean intoQueue = broadcastDirectIntoQueue.get();
				if (!intoQueue || broadcastDirectIntoQueue.compareAndSet(true, false)) break retry;
			}
			ArrayList<Bundle> bundleList = new ArrayList<Bundle>(broadcastQueue);
			broadcastQueue.removeAll(bundleList);
			Intent i = new Intent(service_update_ui);
			i.putParcelableArrayListExtra("bundle", bundleList);
			sendBroadcast(i, actionCPermission());
			lastSendBroadcastTime = System.currentTimeMillis();
			return true;
		}
	}

	public void registerDynamic(BroadcastReceiver receiver, IntentFilter filter) {
		registerReceiver(receiver, filter);
		dynamicRegisters.add(receiver);
	}

	@Override
	public void onDestroy() {
		mAService.stopAService();
		if (!actionReceivers.isEmpty())
		{
			for (ActionReceiver receiver : actionReceivers.values())
				unregisterReceiver(receiver);
		}
		actionReceivers.clear();
		if (!dynamicRegisters.isEmpty())
		{
			for (BroadcastReceiver receiver : dynamicRegisters)
				unregisterReceiver(receiver);
		}
		dynamicRegisters.clear();
		super.onDestroy();
	}
}
