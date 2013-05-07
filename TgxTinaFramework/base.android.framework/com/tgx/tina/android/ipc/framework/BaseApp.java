package com.tgx.tina.android.ipc.framework;

import base.tina.core.log.ILogPrinter.LEVEL;

import com.tgx.tina.android.log.AndroidPrinter;
import com.tgx.tina.android.task.ATaskService;

import android.app.Application;


public abstract class BaseApp<T extends BaseBridge>
        extends
        Application
        implements
        IBridge<T>
{
	//#ifdef debug
	LEVEL                lv = LEVEL.VERBOSE;
	//#else
	//$LEVEL lv = LEVEL.WARN;
	//#endif
	private T            bridge;
	private ATaskService mAService;
	
	@Override
	public void onCreate() {
		super.onCreate();
		AndroidPrinter.createByApp(getApplicationContext(), lv);
		bridge = bindBridge(getApplicationContext());
		mAService = isServiceRemote() ? new ATaskService() : BaseService.getInstance().mAService;
	}
	
	public ATaskService getMainScheduler() {
		return mAService;
	}
	
	public final void startSchedule() {
		mAService.startAService(getApplicationContext());
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		base.tina.core.log.LogPrinter.i(null, "-Terminate-");
	}
	
	public final T getBridge() {
		return bridge;
	}
	
}
