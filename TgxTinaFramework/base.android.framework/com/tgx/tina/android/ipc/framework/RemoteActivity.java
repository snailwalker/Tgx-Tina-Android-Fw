package com.tgx.tina.android.ipc.framework;

import android.os.Bundle;
import android.os.RemoteException;
import base.tina.core.task.android.ATaskService;

public abstract class RemoteActivity
		extends
		BaseActivity
		implements
		IBridge
{
	protected BaseBridge	mBridge;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mBridge = bindBridge(this);
		if (mBridge == null) throw new NullPointerException();
		if (isServiceRemote())
		{
			ioAService = new ATaskService();
			ioAService.startAService(this);
		}
		if (isInCreateBind()) mBridge.startBind(remoteBootAction());
	}
	
	/**
	 *  设置是否在OnCreate方法中就开始进行Bridge.startBind(remoteAction);
	 * @return
	 */
	public abstract boolean isInCreateBind();

	@Override
	protected void onDestroy() {
		RemoteService rServiceStub = mBridge.getRemoteService();
		if (rServiceStub != null)
		{
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

	public final void sendCMD(Bundle bundle) {
		mBridge.sendCMD(bundle);
	}
}
