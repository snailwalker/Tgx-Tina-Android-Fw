/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.tgx.tina.android.plugin.contacts.calllog;

import java.util.LinkedList;

import com.tgx.tina.android.plugin.contacts.base.Profile;

public class CallLogProfile
				extends
				Profile
{

	public final static int	SerialNum	= CallLogProfileSN;

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

	public CallEntry addEntry(long duration, long date, int type) {
		CallEntry entry = new CallEntry(duration, date, type);
		entries.add(entry);
		return entry;
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
		entries.clear();//由于CallEntry内部只有基本数据类型，所以不再执行profile.dispose()
		entries = null;
		phoneNum = null;
		displayName = null;
		super.dispose();
	}

	@Override
	public Profile clone() {
		CallLogProfile callLogProfile = new CallLogProfile(phoneNum, displayName);
		for (CallEntry callEntry : entries)
			callLogProfile.addEntry(callEntry.duration, callEntry.date, callEntry.type);
		return callLogProfile;
	}

}
