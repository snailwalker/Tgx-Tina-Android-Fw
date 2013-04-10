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

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

import android.content.Context;
import android.database.Cursor;
import android.provider.CallLog.Calls;
import base.tina.core.task.infc.ITaskProgress.TaskProgressType;

public class CallLogReadTask
				extends
				ContactTask
{

	public CallLogReadTask(Context context)
	{
		super(context);
	}

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

	public final static int	SerialNum			= SerialDomain + 10;

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
	final static int		MAX_READ			= 500;

	@Override
	public void run() throws Exception {
		CallLogProfile profile = null;
		Cursor callsCursor = context.getContentResolver().query(Calls.CONTENT_URI, PROJECTION_STRINGS, null, null, Calls.DEFAULT_SORT_ORDER);
		if (callsCursor != null) try
		{
			CallPack profilePack = new CallPack();
			int count = 0;
			while (callsCursor.moveToNext() && count < MAX_READ)
			{
				String phoneNum = callsCursor.getString(0);
				String cachedName = callsCursor.getString(6);
				if (profile == null || !profile.phoneNum.equals(phoneNum))
				{
					profile = new CallLogProfile(phoneNum, cachedName);
					profilePack.addProfile(profile);
				}
				profile.addEntry(callsCursor.getLong(2), callsCursor.getLong(1), callsCursor.getInt(3));
				count++;
			}
			commitResult(profilePack, CommitAction.WAKE_UP);
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
}
