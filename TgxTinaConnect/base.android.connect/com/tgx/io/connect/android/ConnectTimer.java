package com.tgx.io.connect.android;

import base.tina.core.task.timer.TimerTask;
import base.tina.external.io.IoFilter;
import base.tina.external.io.net.socket.SocketFeture;


public class ConnectTimer
        extends
        TimerTask
{
	
	private SocketFeture feture;
	private IoFilter     filter;
	
	@Override
	public void dispose() {
		filter = null;
		feture = null;
		super.dispose();
	}
	
	public ConnectTimer(SocketFeture feture, IoFilter filter) {
		super(feture.getNextDelay());//隐含了feture不为null；
		this.feture = feture;
		this.filter = filter;
	}
	
	@Override
	public int getSerialNum() {
		return SerialNum;
	}
	
	public final static int SerialNum = SerialDomain - 5;
	
	@Override
	protected boolean doTimeMethod() {
		//#debug
		base.tina.core.log.LogPrinter.d(ConnectionService.TAG, "feture: " + feture.getClass().getSimpleName() + "@" + Integer.toHexString(feture.hashCode()) + " |" + (feture.isEnable() ? "connect: " + feture.isConnectedOrConnecting() : "feture closed") + " network ok: " + ConnectionService.networkOk);
		if (!feture.isEnable() || feture.isConnectedOrConnecting() || !ConnectionService.networkOk || feture.single()) return true;//网路确认为失败或者已经开始连接将取消当前过程
		feture.onTimer(this);//当此次定时器生效时，取消由于递增效应而滞后历史定时器
		feture.connect(filter, feture);
		return false;
	}
}
