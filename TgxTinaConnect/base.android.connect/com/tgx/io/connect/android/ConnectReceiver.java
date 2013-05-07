package com.tgx.io.connect.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ConnectReceiver
        extends
        BroadcastReceiver
{
	final static String                TAG = "Connect";
	protected static ConnectionService service;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectionService.checkNetwork(context);
		if (service != null)
		{
			if (ConnectionService.networkOk && ConnectionService.networkChType && service.getSocketListener() != null)
			{
				//#debug 
				base.tina.core.log.LogPrinter.d(TAG, "iterate connect!");
				service.getSocketListener().iterateConnect(service.getSocketFilter(), ConnectionService.networkChType);
			}
		}
	}
	
}
