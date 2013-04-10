package com.tgx.tina.android.ipc.framework;

import android.content.Context;


public interface IBridge
{
	
	public boolean isServiceRemote();
	
	public String remoteBootAction();
	
	public String actionCPermission();
	
	public String actionSPermission();
	
	public BaseBridge bindBridge(Context context);
}
