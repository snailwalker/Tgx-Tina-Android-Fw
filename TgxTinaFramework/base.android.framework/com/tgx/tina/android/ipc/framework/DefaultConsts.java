/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tgx.tina.android.ipc.framework;

public interface DefaultConsts
{

	public final static String	serviceAction					= ".service.ui.action.";

	public final static int		SERVERACTION_CLIENT_PUSH		= -4;
	public final static int		SERVERACTION_CLIENT_LOG			= -3;
	public final static int		SERVERACTION_CLIENT_STOP		= -2;
	public final static int		SERVERACTION_CLIENT_START		= -1;

	public final static int		BaseService_BroadcastReceived	= -1;
	public final static int		BaseService_ServiceStatus		= BaseService_BroadcastReceived + 1;
	public final static int		BaseService_LogOnline			= BaseService_ServiceStatus + 1;
	public final static int		BaseService_LogOffine			= BaseService_LogOnline + 1;
	public final static int		BaseSerice_LogToggleOn			= BaseService_LogOffine + 1;
	public final static int		BaseSerice_LogToggleOff			= BaseSerice_LogToggleOn + 1;
	public final static int		BaseSerice_LogComplate			= BaseSerice_LogToggleOff + 1;

	public final static int		NO_DATA_ARG						= 0;
	public final static int		HAS_DATA_ARG					= NO_DATA_ARG + 1;

	public final static int		INIT_RELOGIN_GAP				= 2;

}
