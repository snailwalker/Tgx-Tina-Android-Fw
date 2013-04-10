package com.tgx.tina.android.plugin.contacts.base;

public class TContactPack
        extends
        ProfilePack<TContactProfile>
{
	public final static int SerialNum = ProfilePack.SerialNum + 5;
	
	@Override
	public int getSerialNum() {
		return SerialNum;
	}
}
