package com.tgx.io.connect.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


public class ConnectReceiver
        extends
        BroadcastReceiver
{
	final static String                TAG = "CONNECT_TGX";
	protected static ConnectionService service;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		ConnectionService.checkNetwork(context);
		if (service != null)
		{
			if (ConnectionService.networkChType)
			{
				//#debug 
				base.tina.core.log.LogPrinter.d(TAG, "Change!");
				service.onNetworkChange();
			}
		}
	}
}
