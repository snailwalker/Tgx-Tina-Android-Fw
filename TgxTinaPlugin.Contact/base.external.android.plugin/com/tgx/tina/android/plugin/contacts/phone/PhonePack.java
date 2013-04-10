package com.tgx.tina.android.plugin.contacts.phone;

import com.tgx.tina.android.plugin.contacts.base.ProfilePack;


public class PhonePack
        extends
        ProfilePack<PhoneProfile>
{
	public final static int SerialNum = ProfilePack.SerialNum + 3;
	
	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
}
