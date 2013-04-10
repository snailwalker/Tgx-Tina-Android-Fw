package com.tgx.tina.android.plugin.contacts.base;

import base.tina.core.task.AbstractResult;


public abstract class Profile
        extends
        AbstractResult
{
	final protected static int SerialDomain = -ContactTask.SerialDomain;
	protected int              primaryKey;                              //_ID
	protected int              foreignKey;
	protected int              externalKey;
	
	public abstract int getContactID();
	
	public abstract int getRawContactID();
	
	public int getPrimaryKey() {
		return primaryKey;
	}
}
