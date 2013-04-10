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

import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import base.tina.core.task.Task;

public class SendSmsTask
				extends
				Task
{
	public final static String	SMS_SENT_ACTION		= "TaskService.SmsTask.SENT.IntentAction";
	public final static String	SMS_DELIVERY_ACTION	= "TaskService.SmsTask.DELIVERY.IntentAction";
	public final static String	MSG_ID				= "msgID";
	public final static String	MSG_PARTS			= "msgParts";
	public final static String	MSG_PART_INDEX		= "msgPartIndex";
	Context						context;
	Handler						sendHandler;

	private SendSmsTask(Context context, Handler sendHandler)
	{
		super(SerialDomain);
		this.context = context;
		this.sendHandler = sendHandler;
	}

	@Override
	public void dispose() {
		content = null;
		address = null;
		context = null;
		sendHandler = null;
		super.dispose();
	}

	@Override
	public void initTask() {
		isBloker = true;
		super.initTask();
	}

	protected final static int	SerialDomain	= -0x6000;
	public final static int		SerialNum		= SerialDomain + 1;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public void run() throws Exception {
		if (sendHandler != null)
		{
			SmsMsgPack messagePack = new SmsMsgPack();
			messagePack.address = address;
			messagePack.msgID = msgID;
			messagePack.state = SmsMsgPack.SENT;
			commitResult(messagePack, CommitAction.WAKE_UP);
			Bundle bundle = new Bundle();
			bundle.putInt("msgID", msgID);
			bundle.putInt("port", port);
			bundle.putString("address", address);
			bundle.putString("content", content);
			Message msg = new Message();
			msg.setData(bundle);
			sendHandler.sendMessage(msg);
		}
		else
		{
			//#debug warn
			base.tina.core.log.LogPrinter.w("TGX_TINA_SMS", "service没起来，发短信，肿么发？？？ 不发了！！！");
		}
	}

	private int			msgID;
	private String		content;
	private int			port;
	private String		address;

	public boolean		userCancel	= false;

	static long			timeStamp;				// 记录上个短信任务应该执行的时间点
	final static long	minTimeDeta	= 20000L;

	/**
	 * port 为-1的时候代表本短信时已普通短信执行 msgID 为全局使用的唯一消息标识
	 */
	public static SendSmsTask smsRequestFactory(Context context, Handler sendHandler, int msgID, String address, String content, int port) {
		SendSmsTask smsTask = new SendSmsTask(context, sendHandler);
		if (address == null || address.equals("") || content == null || content.equals("")) smsTask.cancel();
		else
		{
			smsTask.msgID = msgID;
			smsTask.content = content;
			smsTask.port = port;
			smsTask.address = address;
		}
		long currentTimeMillis = System.currentTimeMillis();
		if (currentTimeMillis - timeStamp < minTimeDeta)
		{
			timeStamp = timeStamp + minTimeDeta;
			smsTask.setDelay(timeStamp - currentTimeMillis, TimeUnit.MILLISECONDS);
		}
		else timeStamp = currentTimeMillis;
		return smsTask;
	}

	public int getMsgID() {
		return msgID;
	}

	public String getPhone() {
		return address;
	}
}
