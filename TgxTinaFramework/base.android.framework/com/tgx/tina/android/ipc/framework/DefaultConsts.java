package com.tgx.tina.android.ipc.framework;

public interface DefaultConsts
{
	
	public final static String serviceAction                 = ".service.ui.action.";
	
	public final static int    SERVERACTION_CLIENT_PUSH       = -4;
	public final static int    SERVERACTION_CLIENT_LOG       = -3;
	public final static int    SERVERACTION_CLIENT_STOP      = -2;
	public final static int    SERVERACTION_CLIENT_START     = -1;
	
	public final static int    BaseService_BroadcastReceived = -1;
	public final static int    BaseService_ServiceStatus     = BaseService_BroadcastReceived + 1;
	public final static int    BaseService_LogOnline         = BaseService_ServiceStatus + 1;
	public final static int    BaseService_LogOffine         = BaseService_LogOnline + 1;
	public final static int    BaseSerice_LogToggleOn        = BaseService_LogOffine + 1;
	public final static int    BaseSerice_LogToggleOff       = BaseSerice_LogToggleOn + 1;
	public final static int    BaseSerice_LogComplate        = BaseSerice_LogToggleOff + 1;
	
	public final static int    NO_DATA_ARG                   = 0;
	public final static int    HAS_DATA_ARG                  = NO_DATA_ARG + 1;
	
	public final static int    INIT_RELOGIN_GAP              = 2;
	
}
