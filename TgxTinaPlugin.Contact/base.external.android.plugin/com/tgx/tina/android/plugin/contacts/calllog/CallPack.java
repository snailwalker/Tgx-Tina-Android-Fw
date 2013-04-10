package com.tgx.tina.android.plugin.contacts.calllog;

import com.tgx.tina.android.plugin.contacts.base.ProfilePack;

public class CallPack
		extends
		ProfilePack<CallLogProfile>
{
	public final static int SerialNum = ProfilePack.SerialNum + 1;

	@Override
	public final int getSerialNum()
	{
		return SerialNum;
	}
}
