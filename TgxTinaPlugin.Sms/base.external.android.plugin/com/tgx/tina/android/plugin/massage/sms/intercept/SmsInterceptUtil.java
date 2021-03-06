/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.tgx.tina.android.plugin.massage.sms.intercept;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;

import com.tgx.tina.android.plugin.massage.sms.SmsContentObserver;
import com.tgx.tina.android.plugin.massage.sms.SmsMsgPack;


public class SmsInterceptUtil
{
	
	final static String tag = "sms";
	
	public final static void onReceive(Context context, Intent intent, IMsgGather msgGather) {
		onReceiveSMS(context, intent, msgGather);
	}
	
	public final static void getSmsRecvObserver(Context context, SmsContentObserver observer) {
		context.getContentResolver().registerContentObserver(Uri.parse("content://sms/inbox"), false, SmsInterceptUtil.observer = observer);
	}
	
	public final static void rmSmsRecvObserver(Context context) {
		if (observer != null) context.getContentResolver().unregisterContentObserver(observer);
	}
	
	private static SmsContentObserver observer;
	
	public static void setObserverEnable(boolean enabled) {
		if (observer != null) observer.setEnable(enabled);
	}
	
	static class SMS_MULTI_PARTS
	
	{
		String phone, content;
		long   timeStamp;
		
		public void dispose() {
			phone = null;
			content = null;
		}
		
	}
	
	static class SMS_MULTI_MSG
	{
		public SMS_MULTI_MSG(int sign) {
			this.sign = sign;
		}
		
		SMS_MULTI_PARTS[] parts;
		int               sign;
		long              putStamp;
		
		public void dispose() {
			if (parts != null) for (SMS_MULTI_PARTS part : parts)
			{
				part.dispose();
			}
			parts = null;
		}
	}
	
	static void onReceiveSMS(Context context, Intent intent, IMsgGather msgGather) {
		//#debug
		base.tina.core.log.LogPrinter.d(tag, "~~~SMS~~~");
		Object[] objects = (Object[]) (intent.getExtras().get("pdus"));
		String from = intent.getExtras().getString("from");
		if (TextUtils.isEmpty(from) && intent.getExtras().containsKey("format"))
		{
			String format = intent.getExtras().getString("format");
			if (format.equalsIgnoreCase("3gpp"))
			{
				from = "GSM";
			}
			else if (format.equalsIgnoreCase("3gpp2"))
			{
				from = "CDMA";
			}
		}
		
		if (TextUtils.isEmpty(from))
		{
			TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			from = tm.getPhoneType() == TelephonyManager.PHONE_TYPE_CDMA ? "CDMA" : "GSM";
		}
		//#debug
		base.tina.core.log.LogPrinter.d(tag, "~~~from~~~" + from);
		byte[] pdu, userData;
		int tp_udl, tp_sign;
		SparseArray<SMS_MULTI_MSG> toPackMsgs = new SparseArray<SMS_MULTI_MSG>();
		PDUS:
		for (Object obj : objects)
		{
			pdu = (byte[]) obj;
			TgxSmsMessage smsMessage = TgxSmsMessage.createFromPdu(pdu, from);
			String address = smsMessage.getDisplayOriginatingAddress();
			String content = smsMessage.getDisplayMessageBody();
			long timeStamp = smsMessage.getTimestampMillis();
			userData = smsMessage.getUserData();
			//CDMA
			if (from.equalsIgnoreCase("CDMA"))
			{
				
				if ((userData[0] & 0xFF) == 0x05 && (userData[1] & 0xFF) == 0x00)
				{
					//#debug
					base.tina.core.log.LogPrinter.d(tag, "cdma长短信需要拼接~~~~~~");
					SMS_MULTI_MSG multiMsg;
					
					tp_sign = userData[3];
					
					if (toPackMsgs.indexOfKey(tp_sign) < 0)
					{
						int size = userData[4];
						multiMsg = new SMS_MULTI_MSG(tp_sign);
						multiMsg.parts = new SMS_MULTI_PARTS[size];
						toPackMsgs.put(tp_sign, multiMsg);
						multiMsg.putStamp = System.currentTimeMillis();
					}
					else
					{
						multiMsg = toPackMsgs.get(tp_sign);
					}
					int index = userData[5] & 0xFF;
					
					//#debug
					base.tina.core.log.LogPrinter.d(tag, "[cdma long SMS] part index: " + index + " O:" + userData[5]);
					
					if (index < 0 || index > multiMsg.parts.length)
					{
						//#debug warn
						base.tina.core.log.LogPrinter.w(tag, "[cdma long SMS] part error:index");
						continue PDUS;
					}
					index--;
					multiMsg.parts[index] = new SMS_MULTI_PARTS();
					multiMsg.parts[index].phone = address;
					multiMsg.parts[index].content = content;
					multiMsg.parts[index].timeStamp = timeStamp;
					
					boolean receiveAll = true;
					for (SMS_MULTI_PARTS sms_part : multiMsg.parts)
						if (sms_part == null)
						{
							receiveAll = false;
							break;
						}
					if (receiveAll)
					{
						SmsMsgPack messagePack = new SmsMsgPack();
						messagePack.state = SmsMsgPack.LOCAL_RECEIVE;
						messagePack.address = address;
						StringBuffer buffer = new StringBuffer();
						for (SMS_MULTI_PARTS sms_part : multiMsg.parts)
						{
							buffer.append(sms_part.content);
							sms_part.dispose();
						}
						messagePack.content = buffer.toString();
						if (timeStamp > 0) messagePack.timeStamp = new Date(timeStamp);
						toPackMsgs.remove(tp_sign);
						// ~插入服务的队列
						msgGather.gatherMsg(messagePack);
					}
					
				}
				else
				{
					
					SmsMsgPack messagePack = new SmsMsgPack();
					messagePack.state = SmsMsgPack.LOCAL_RECEIVE;
					messagePack.address = address;
					messagePack.content = content;
					
					//#debug
					base.tina.core.log.LogPrinter.d(tag, "cdma smsMessage.getIndexOnIcc : ");
					
					if (timeStamp > 0) messagePack.timeStamp = new Date(timeStamp);
					
					// ~插入服务的队列
					msgGather.gatherMsg(messagePack);
				}
			}
			else
			{
				//GSM  
				tp_udl = pdu[pdu.length - 1 - userData.length] & 0xFF;
				//#debug
				base.tina.core.log.LogPrinter.d(tag, "tp_udl: " + tp_udl + " userData.length:" + userData.length);
				if (tp_udl == userData.length || (tp_udl > userData.length && (tp_udl * 7) >>> 3 == userData.length))
				{
					SmsMsgPack messagePack = new SmsMsgPack();
					messagePack.state = SmsMsgPack.LOCAL_RECEIVE;
					messagePack.address = address;
					messagePack.content = content;
					
					//#debug 
					base.tina.core.log.LogPrinter.d(tag, "<%%%%%%%> smsMessage.getIndexOnIcc : ");
					
					if (timeStamp > 0) messagePack.timeStamp = new Date(timeStamp);
					
					// ~插入服务的队列
					msgGather.gatherMsg(messagePack);
				}
				else
				{
					//#debug
					base.tina.core.log.LogPrinter.d(tag, "长短信需要拼接~~~~~~");
					SMS_MULTI_MSG multiMsg;
					
					tp_udl = pdu[pdu.length - 1 - userData.length - 6] & 0xFF;
					if (tp_udl == userData.length + 6 && pdu[pdu.length - 1 - userData.length - 5] == 0x05) tp_sign = pdu[pdu.length - 1 - userData.length - 2];
					else
					{
						tp_udl = pdu[pdu.length - 1 - userData.length - 7] & 0xFF;
						tp_sign = (pdu[pdu.length - 1 - userData.length - 3] & 0xFF) << 8 | pdu[pdu.length - 1 - userData.length - 2] & 0xFF;
					}
					if (toPackMsgs.indexOfKey(tp_sign) < 0)
					{
						int size = pdu[pdu.length - 1 - userData.length - 1];
						multiMsg = new SMS_MULTI_MSG(tp_sign);
						multiMsg.parts = new SMS_MULTI_PARTS[size];
						toPackMsgs.put(tp_sign, multiMsg);
						multiMsg.putStamp = System.currentTimeMillis();
					}
					else multiMsg = toPackMsgs.get(tp_sign);
					int index = pdu[pdu.length - 1 - userData.length] - 1;
					//#debug
					base.tina.core.log.LogPrinter.d(tag, "[long SMS] part index: " + index);
					if (index < 0 || index >= multiMsg.parts.length)
					{
						//#debug warn
						base.tina.core.log.LogPrinter.w(tag, "[long SMS] part error:index");
						continue PDUS;
					}
					multiMsg.parts[index] = new SMS_MULTI_PARTS();
					multiMsg.parts[index].phone = address;
					multiMsg.parts[index].content = content;
					multiMsg.parts[index].timeStamp = timeStamp;
					
					boolean receiveAll = true;
					for (SMS_MULTI_PARTS sms_part : multiMsg.parts)
						if (sms_part == null)
						{
							receiveAll = false;
							break;
						}
					if (receiveAll)
					{
						SmsMsgPack messagePack = new SmsMsgPack();
						messagePack.state = SmsMsgPack.LOCAL_RECEIVE;
						messagePack.address = address;
						StringBuffer buffer = new StringBuffer();
						for (SMS_MULTI_PARTS sms_part : multiMsg.parts)
						{
							buffer.append(sms_part.content);
							sms_part.dispose();
						}
						messagePack.content = buffer.toString();
						if (timeStamp > 0) messagePack.timeStamp = new Date(timeStamp);
						toPackMsgs.remove(tp_sign);
						// ~插入服务的队列
						msgGather.gatherMsg(messagePack);
					}
				}
			}
		}
		
		//清理未收全的消息，按单条存入
		int len = toPackMsgs.size();
		if (len > 0)
		{
			long curTime = System.currentTimeMillis();
			for (int i = 0; i < len; i++)
			{
				SMS_MULTI_MSG multiMsg = toPackMsgs.valueAt(i);
				if (TimeUnit.MILLISECONDS.toSeconds(multiMsg.putStamp - curTime) > 300)
				{
					
					for (SMS_MULTI_PARTS part : multiMsg.parts)
					{
						if (part != null)
						{
							SmsMsgPack messagePack = new SmsMsgPack();
							messagePack.state = SmsMsgPack.LOCAL_RECEIVE;
							messagePack.address = part.phone;
							messagePack.content = part.content;
							if (part.timeStamp > 0) messagePack.timeStamp = new Date(part.timeStamp);
							part.dispose();
							// ~插入服务的队列
							msgGather.gatherMsg(messagePack);
						}
					}
					multiMsg.dispose();
					toPackMsgs.remove(toPackMsgs.keyAt(i));
				}
			}
		}
	}
}
