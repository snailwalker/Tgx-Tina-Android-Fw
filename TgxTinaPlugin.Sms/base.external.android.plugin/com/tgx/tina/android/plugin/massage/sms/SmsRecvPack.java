package com.tgx.tina.android.plugin.massage.sms;

import base.tina.core.task.AbstractResult;


public class SmsRecvPack
        extends
        AbstractResult
{
	public String           address;
	public String           content;
	public int              msgOriginId;
	public int              msgId;
	public long             timeStamp;
	
	public final static int SerialNum = SmsMsgPack.SerialNum + 1;
	
	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
	
	@Override
	public void dispose() {
		address = null;
		content = null;
		super.dispose();
	}
}
