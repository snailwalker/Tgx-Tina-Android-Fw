package com.tgx.tina.android.plugin.contacts.category;

import com.tgx.tina.android.plugin.contacts.base.Profile;


public class CategoryProfile
        extends
        Profile
{
	public final static int SerialNum = SerialDomain + 2;
	
	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
	
	@Override
	public int getContactID() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int getRawContactID() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
