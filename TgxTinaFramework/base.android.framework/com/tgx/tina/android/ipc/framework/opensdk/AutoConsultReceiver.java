package com.tgx.tina.android.ipc.framework.opensdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


public abstract class AutoConsultReceiver
        extends
        BroadcastReceiver
{
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent == null) return;
		Context appContext = context.getApplicationContext();
		Intent brIntent = new Intent(getConsultAction(appContext));
		brIntent.setData(Uri.parse(getConsultData(appContext)));
		brIntent.putExtra("bootAction", getRemoteAction(appContext));
		appContext.sendBroadcast(brIntent, getConsultPermission(appContext));
		abortBroadcast();
	}
	
	protected abstract String getConsultAction(Context context);
	
	protected abstract String getConsultData(Context context);
	
	protected abstract String getConsultPermission(Context context);
	
	protected abstract String getRemoteAction(Context context);
	
}
