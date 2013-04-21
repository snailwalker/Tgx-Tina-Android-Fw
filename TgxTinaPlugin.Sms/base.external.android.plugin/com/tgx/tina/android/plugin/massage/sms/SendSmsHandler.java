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

import java.nio.charset.Charset;
import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;

public class SendSmsHandler
				extends
				Handler
{
	Context	context;

	public SendSmsHandler(Context context)
	{
		this.context = context;
	}

	public final static String			SMS_SENT_ACTION		= "TaskService.SmsTask.SENT.IntentAction";
	public final static String			SMS_DELIVERY_ACTION	= "TaskService.SmsTask.DELIVERY.IntentAction";
	private final static String			MSG_ID				= "msgID";
	private final static String			MSG_PARTS			= "msgParts";
	private final static String			MSG_PART_INDEX		= "msgPartIndex";

	private int							msgID;
	private int							port;
	private int							multiPartCount;
	private String						address;
	private String						content;
	private byte[]						contentPDU;
	private ArrayList<String>			contents;
	private ArrayList<byte[]>			contentPDUs;
	private ArrayList<PendingIntent>	sentIntents;
	private ArrayList<PendingIntent>	deliveryIntents;
	private PendingIntent				sentIntent;
	private PendingIntent				deliveryIntent;

	@Override
	public void handleMessage(android.os.Message msg) {
		//#debug info
		base.tina.core.log.LogPrinter.i(null, "使用handler发短信");

		Bundle bundle = msg.getData();
		address = bundle.getString("address");
		content = bundle.getString("content");
		msgID = bundle.getInt("msgID");
		port = bundle.getInt("port");

		SmsManager smsManager = SmsManager.getDefault();
		//from init():
		{
			contents = smsManager.divideMessage(content);
			multiPartCount = contents.size();
			if (multiPartCount > 1)
			{
				//#debug
				base.tina.core.log.LogPrinter.d(null, "//拆分为多条消息了~");
				sentIntents = new ArrayList<PendingIntent>(multiPartCount);
				deliveryIntents = new ArrayList<PendingIntent>(multiPartCount);
				int partIndex = 0;
				for (String content : contents)
				{
					Intent intent = new Intent(SMS_SENT_ACTION);
					intent.putExtra(MSG_ID, msgID);
					intent.putExtra(MSG_PART_INDEX, partIndex);
					intent.putExtra(MSG_PARTS, multiPartCount);
					if (port > 0) try
					{
						contentPDUs.add(content.getBytes(Charset.forName("UTF-8").name()));
					}
					catch (Exception e)
					{
						contentPDUs.add(content.getBytes());
					}
					sentIntents.add(PendingIntent.getBroadcast(context, msgID, intent, PendingIntent.FLAG_ONE_SHOT));
					Intent intent2 = new Intent(SMS_DELIVERY_ACTION);
					intent2.putExtra(MSG_ID, msgID);
					intent2.putExtra(MSG_PART_INDEX, partIndex);
					intent2.putExtra(MSG_PARTS, multiPartCount);
					deliveryIntents.add(PendingIntent.getBroadcast(context, msgID, intent2, PendingIntent.FLAG_ONE_SHOT));
					partIndex++;
				}
			}
			else
			{
				Intent intent = new Intent(SMS_SENT_ACTION);
				intent.putExtra(MSG_ID, msgID);
				intent.putExtra(MSG_PARTS, 1);
				sentIntent = PendingIntent.getBroadcast(context, msgID, intent, PendingIntent.FLAG_ONE_SHOT);
				Intent intent2 = new Intent(SMS_DELIVERY_ACTION);
				intent2.putExtra(MSG_ID, msgID);
				intent2.putExtra(MSG_PARTS, 1);
				deliveryIntent = PendingIntent.getBroadcast(context, msgID, intent2, PendingIntent.FLAG_ONE_SHOT);
				if (port > 0) try
				{
					contentPDU = content.getBytes(Charset.forName("UTF-8").name());
				}
				catch (Exception e)
				{
					contentPDU = content.getBytes();
				}
			}
		}

		//from run():
		{
			if (multiPartCount > 1)
			{
				if (port > 0) for (byte[] contentPDU : contentPDUs)
					smsManager.sendDataMessage(address, null, (short) port, contentPDU, sentIntent, deliveryIntent);
				else smsManager.sendMultipartTextMessage(address, null, contents, sentIntents, deliveryIntents);
			}
			else
			{
				if (port > 0) smsManager.sendDataMessage(address, null, (short) port, contentPDU, sentIntent, deliveryIntent);
				else smsManager.sendTextMessage(address, null, content, sentIntent, deliveryIntent);
			}

			//#debug info
			base.tina.core.log.LogPrinter.i(null, "~短信任务执行完毕~ 移交系统调度");
			//#debug 
			base.tina.core.log.LogPrinter.d(null, "SEND SMS msgID : " + msgID);
			//#debug 
			base.tina.core.log.LogPrinter.d(null, "SEND SMS address : " + address);
		}
	}
}
