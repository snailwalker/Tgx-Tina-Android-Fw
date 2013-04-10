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
package com.tgx.tina.android.plugin.massage.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import base.tina.core.task.Task;

public class LoadSysSmsInboxTask
				extends
				Task
{
	private Context	context;
	private int		loadLimit, alreadyLoad;

	public LoadSysSmsInboxTask(int threadId, Context context, int loadLimit, int alreadyLoad)
	{
		super(threadId);
		this.context = context;
		this.loadLimit = loadLimit;
		this.alreadyLoad = alreadyLoad;
	}

	public void updateLimit(int limit) {
		loadLimit = limit;
	}

	@Override
	public void dispose() {
		context = null;
		super.dispose();
	}

	@Override
	public final void initTask() {
		isBloker = true;
		super.initTask();
	}

	@Override
	public void run() throws Exception {
		Cursor sysSmsCursor = context.getContentResolver().query(URI, new String[] {
						ID ,
						ADDRESS ,
						DATE ,
						BODY ,
						STATUS ,
						SUBJECT
		}, TYPE + "=?" + " AND " + READ + "=?" + " AND " + ID + ">?", new String[] {
						"1" ,
						"0" ,
						String.valueOf(alreadyLoad)
		}, DATE + " desc");
		if (sysSmsCursor != null) try
		{
			for (int i = 0; i < loadLimit && sysSmsCursor.moveToNext(); i++)
			{
				SmsRecvPack pack = new SmsRecvPack();
				pack.msgOriginId = sysSmsCursor.getInt(0);
				pack.address = sysSmsCursor.getString(1);
				pack.timeStamp = sysSmsCursor.getLong(2);
				pack.content = sysSmsCursor.getString(3);
				commitResult(pack);
			}
			scheduleService.commitNotify();
		}
		finally
		{
			sysSmsCursor.close();
		}

	}

	public final static int	SerialNum	= LoadSysSmsTask.SerialNum + 1;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	public final static Uri		URI		= Uri.parse("content://sms");
	public final static String	ADDRESS	= "address";
	public final static String	DATE	= "date";
	public final static String	READ	= "read";
	public final static String	STATUS	= "status";
	public final static String	TYPE	= "type";
	public final static String	BODY	= "body";
	public final static String	ID		= "_id";
	public final static String	SUBJECT	= "subject";
}
