package com.tgx.tina.android.plugin.contacts.calllog;

import java.util.LinkedList;

import com.tgx.tina.android.plugin.contacts.base.Profile;

public class CallLogProfile
		extends
		Profile
{

	public final static int	SerialNum	= SerialDomain + 1;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public int getContactID() {
		return externalKey;
	}

	@Override
	public int getRawContactID() {
		return foreignKey;
	}

	public String	phoneNum;
	public String	displayName;

	public CallLogProfile(String phoneNum, String displayName)
	{
		this.phoneNum = phoneNum;
		this.displayName = displayName;
	}

	public LinkedList<CallEntry>	entries	= new LinkedList<CallLogProfile.CallEntry>();

	public void addEntry(long duration, long date, int type) {
		entries.add(new CallEntry(duration, date, type));
	}

	public static class CallEntry
	{
		public long	duration;
		public byte	type;
		public long	date;

		public CallEntry(long duration, long date, int type)
		{
			this.duration = duration;
			this.type = (byte) type;
			this.date = date;
		}
	}

	@Override
	public void dispose() {
		entries.clear();
		entries = null;
		phoneNum = null;
		displayName = null;
		super.dispose();
	}

}
