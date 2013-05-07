package com.tgx.test;

import android.os.Bundle;
import base.tina.external.io.IoFilter;

import com.tgx.io.connect.android.ConnectionService;
import com.tgx.io.connect.android.SocketIOListener;
import com.tgx.io.connect.android.test.R;


public class TestConnectService
        extends
        ConnectionService
{
	
	SocketIOListener socketListener;
	
	@Override
	public void onCreate() {
		super.onCreate();
		socketListener = new SocketIOListener();
	}
	
	@Override
	protected void register(String account, String password) {
		
	}
	
	@Override
	protected void login(String account, String password, String salt) {
		
	}
	
	@Override
	protected void bindOthers(String jsonArg) {
		
	}
	
	@Override
	protected Bundle getServiceStatus() {
		return null;
	}
	
	@Override
	protected String actionSPermission() {
		return getString(R.string.permission_service);
	}
	
	@Override
	protected String actionCPermission() {
		return getString(R.string.permission_broadcast);
		
	}
	
	@Override
	protected void onActionReceive(int cmd, Bundle bundle) {
		
	}
	
	@Override
	public SocketIOListener getSocketListener() {
		return socketListener;
	}
	
	@Override
	public IoFilter getSocketFilter() {
		return null;
	}
	
}
