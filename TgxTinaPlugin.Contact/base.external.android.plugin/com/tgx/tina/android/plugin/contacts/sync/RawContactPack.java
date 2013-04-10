package com.tgx.tina.android.plugin.contacts.sync;

import com.tgx.tina.android.plugin.contacts.base.ProfilePack;


public class RawContactPack
        extends
        ProfilePack<RawContactProfile>
{
	public final static int SerialNum = ProfilePack.SerialNum + 4;
	
	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
}
