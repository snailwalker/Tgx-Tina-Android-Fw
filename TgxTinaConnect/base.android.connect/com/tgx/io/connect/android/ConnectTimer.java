package com.tgx.io.connect.android;

import java.util.concurrent.TimeUnit;

import base.tina.core.task.infc.ITaskListener;
import base.tina.core.task.timer.TimerTask;
import base.tina.external.io.IConnectFeture;
import base.tina.external.io.IoFilter;


public class ConnectTimer
        extends
        TimerTask
{
	
	private int                          pF0         = 1;
	private int                          pF1         = 2;
	private int                          nextConTime = pF0 + pF1;
	private IConnectFeture<ConnectTimer> feture;
	private IoFilter                     filter;
	private ITaskListener                listener;
	
	@Override
	public void dispose() {
		feture = null;
		filter = null;
		feture = null;
		super.dispose();
	}
	
	public ConnectTimer(IConnectFeture<ConnectTimer> feture, IoFilter filter, ITaskListener listener) {
		super(3);
		if (feture == null) throw new NullPointerException();
		this.feture = feture;
		this.filter = filter;
		this.listener = listener;
	}
	
	@Override
	public int getSerialNum() {
		return SerialNum;
	}
	
	public final static int SerialNum = SerialDomain - 5;
	
	@Override
	protected boolean doTimeMethod() {
		if (feture.isTiming(this)) return true;
		if (feture.connectTarAddr(filter, listener)) return true;
		if (feture.isConnectedOrConnecting()) return false;
		if (nextConTime > 900)
		{
			pF0 = 1;
			pF1 = 2;
			nextConTime = 901;
		}
		else
		{
			nextConTime = pF0 + pF1;
			pF0 = pF1;
			pF1 = nextConTime;
		}
		setWaitTime(TimeUnit.SECONDS.toMillis(nextConTime));
		//#debug
		base.tina.core.log.LogPrinter.d(null, "Connect Next:" + nextConTime + "|Sec|");
		return false;
	}
}
