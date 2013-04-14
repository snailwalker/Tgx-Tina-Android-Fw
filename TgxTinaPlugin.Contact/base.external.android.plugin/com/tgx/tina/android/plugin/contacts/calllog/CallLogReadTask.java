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

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import base.tina.core.task.infc.ITaskProgress.TaskProgressType;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

public class CallLogReadTask
				extends
				ContactTask
{

	public CallLogReadTask(Context context, int limit, int lastID)
	{
		super(context);
		this.limit = limit;
		this.lastID = lastID;
	}

	private final int	limit;
	private final int	lastID;

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

	public final static int	SerialNum			= CallLogReadTaskSN;

	final String[]			PROJECTION_STRINGS	= {
					Calls.NUMBER ,//0
					Calls.DATE ,//1
					Calls.DURATION ,//2
					Calls.TYPE ,//3
					Calls.NEW ,//4
					Calls._ID ,//5
					Calls.CACHED_NAME
													//6
												};
	public final static int	MAX_READ			= 500;

	@Override
	public void run() throws Exception {
		CallLogProfile profile = null;
		Cursor callsCursor = context.getContentResolver().query(Calls.CONTENT_URI, PROJECTION_STRINGS, null, null, Calls.DEFAULT_SORT_ORDER);
		if (callsCursor != null) try
		{

			if (callsCursor.moveToFirst())
			{
				int lastID = callsCursor.getInt(5);
				int limit = this.limit;
				if (this.lastID >= 0 && this.lastID - lastID < 0) limit = Math.max(limit, lastID - this.lastID);
				CallPack profilePack = new CallPack(lastID);
				int count = 0;
				do
				{
					String phoneNum = callsCursor.getString(0);
					String cachedName = callsCursor.getString(6);
					int type = callsCursor.getInt(3);
					if (excludes(phoneNum, type)) continue;
					if (profile == null || !profile.phoneNum.equals(phoneNum))
					{
						profile = new CallLogProfile(phoneNum, cachedName);
						profilePack.addProfile(profile);
					}
					profile.addEntry(callsCursor.getLong(2), callsCursor.getLong(1), type);
					count++;
				}
				while (callsCursor.moveToNext() && count < limit);
				commitResult(profilePack, CommitAction.WAKE_UP);
			}
		}
		catch (Exception e)
		{
			//#debug warn
			e.printStackTrace();
			if (progress != null) progress.finishProgress(TaskProgressType.error);
		}
		finally
		{
			callsCursor.close();
		}
	}

	public boolean excludes(String phone, int type) {
		return false;
	}

}
