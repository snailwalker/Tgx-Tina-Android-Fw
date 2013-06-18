/*******************************************************************************
 * Copyright 2013 fmz
 * 
 *******************************************************************************/
package com.tgx.tina.android.plugin.massage.sms.intercept;

import java.lang.reflect.Method;


public class TgxSmsMessage
{
	private String address;
	private String content;
	private long   timeStamp;
	private byte[] userData;
	
	public static TgxSmsMessage createFromPdu(byte[] pdu, String from) {
		TgxSmsMessage smsMessage = new TgxSmsMessage();
		Object wrappedMessage = null;
		Class<?> smsManagerClass = null;
		Class[] createFromPduPamas = {
			new byte[0].getClass()
		};
		Method createFromPdu = null;
		String messageClassName = "com.android.internal.telephony." + from.toLowerCase() + ".SmsMessage";
		try
		{
			smsManagerClass = Class.forName(messageClassName);
			createFromPdu = smsManagerClass.getMethod("createFromPdu", createFromPduPamas);
			wrappedMessage = createFromPdu.invoke(null, pdu);
			
			Class<?> c = Class.forName("com.android.internal.telephony.SmsMessageBase");
			Method method = c.getDeclaredMethod("getDisplayOriginatingAddress", null);
			method.setAccessible(true);
			smsMessage.address = (String) method.invoke(wrappedMessage, null);
			
			method = c.getDeclaredMethod("getDisplayMessageBody", null);
			method.setAccessible(true);
			smsMessage.content = (String) method.invoke(wrappedMessage, null);
			
			method = c.getDeclaredMethod("getTimestampMillis", null);
			method.setAccessible(true);
			smsMessage.timeStamp = (Long) method.invoke(wrappedMessage, null);
			
			method = c.getDeclaredMethod("getUserData", null);
			method.setAccessible(true);
			smsMessage.userData = (byte[]) method.invoke(wrappedMessage, null);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return smsMessage;
	}
	
	public String getDisplayOriginatingAddress() {
		return address;
	}
	
	public String getDisplayMessageBody() {
		return content;
	}
	
	public long getTimestampMillis() {
		return timeStamp;
	}
	
	public byte[] getUserData() {
		return userData;
		
	}
}
