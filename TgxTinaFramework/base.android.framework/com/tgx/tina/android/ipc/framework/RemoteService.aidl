 package com.tgx.tina.android.ipc.framework;
 
 interface RemoteService {
	//security base interface
	void register(String account,String password);
 	void login(String account,String password,String salt);
 	void bindOthers(String jsonArg);
	//connect
 	String sActionStr(String cActionStr);
 	boolean onNoAction(String cActionStr);
 	//pid 
 	int remotePID();
 	String remoteClazz();
 }