/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.tgx.tina.android.ipc.framework;

import com.tgx.tina.android.task.ATaskService;

import android.os.Bundle;
import android.os.RemoteException;


public abstract class RemoteActivity<T extends BaseBridge>
        extends
        BaseActivity
        implements
        IBridge<T>
{
	protected T mBridge;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBridge = bindBridge(this);
		if (mBridge == null) throw new NullPointerException();
		if (isServiceRemote())
		{
			mAService = new ATaskService();
			mAService.startAService(this);
		}
		if (isInCreateBind()) mBridge.startBind(remoteBootAction());
	}
	
	/**
	 *  设置是否在OnCreate方法中就开始进行Bridge.startBind(remoteAction);
	 * 
	 * @return
	 */
	public abstract boolean isInCreateBind();
	
	@Override
	protected void onDestroy() {
		RemoteService rServiceStub = mBridge.getRemoteService();
		if (rServiceStub != null)
		{
			//#debug verbose
			base.tina.core.log.LogPrinter.v(null, "Bridge remote onNoAction-");
			try
			{
				rServiceStub.onNoAction(mBridge.tarAction());
			}
			catch (RemoteException e)
			{
				e.printStackTrace();
			}
			mBridge.stopBind();
		}
		super.onDestroy();
	}
	
	public final void sendCMD(int cmd, Bundle bundle) {
		mBridge.sendCMD(cmd, bundle);
	}
}
