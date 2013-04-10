package com.tgx.tina.android.plugin.contacts.category;

import com.tgx.tina.android.plugin.contacts.base.ProfilePack;


public class CategoryPack
        extends
        ProfilePack<CategoryProfile>
{
	public final static int SerialNum = ProfilePack.SerialNum + 2;
	
	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
}
