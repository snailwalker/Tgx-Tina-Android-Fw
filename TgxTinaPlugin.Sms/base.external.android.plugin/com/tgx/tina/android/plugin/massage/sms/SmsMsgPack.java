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
package com.tgx.tina.android.plugin.massage.sms;

import java.util.Date;

import base.tina.core.task.AbstractResult;

public class SmsMsgPack
				extends
				AbstractResult
{
	public int				msgID;
	public int				msgServerID;
	public int				msgPartIndex;
	public int				msgParts;
	public String			type;
	public String			address;
	public String			content;
	public Date				timeStamp;
	public int				state;
	public boolean			isServerRequest;
	public boolean			userCancel			= false;

	public final static int	SENT				= 1;						//已发送
	public final static int	WAIT_TO_SEND		= SENT + 1;				//等待发送
	public final static int	LOCAL_SEND			= WAIT_TO_SEND + 1;		//本地机制发送的消息
	public final static int	SERVER_ORDER_SEND	= LOCAL_SEND + 1;			//服务器要求发送的消息
	public final static int	LOCAL_RECEIVE		= SERVER_ORDER_SEND + 1;	//短信接收或者接收到彩信时的状态标记
	public final static int	LOCAL_PORT_RECEIVE	= LOCAL_RECEIVE + 1;		//短信接收或者接收到彩信时的状态标记
	public final static int	DELIVERED			= LOCAL_PORT_RECEIVE + 1;	//收到短信回执，对方已收到本条短信
	public final static int	SENT_FAILD			= DELIVERED + 1;			//短信发送失败，原因肯能有很多，debug的时候再深究
	public final static int	DELIVERED_FAILD		= SENT_FAILD + 1;			//短线投递失败，基于回执的失败提示

	public void dispose() {
		type = null;
		address = null;
		content = null;
		timeStamp = null;
		state = 0;
		msgID = 0;
		super.dispose();
	}

	public final static int	SerialNum	= -SendSmsTask.SerialDomain;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
}
