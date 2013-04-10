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
import base.tina.core.task.infc.ITaskProgress;

public class LoadSysSmsTask
				extends
				Task
{
	private Context	context;
	private int		alreadyLoad;

	public LoadSysSmsTask(int threadId, ITaskProgress progress, Context context, int alreadyLoad)
	{
		super(threadId, progress);
		this.context = context;
		setAlreadyLoad(alreadyLoad);
	}

	@Override
	public void dispose() {
		context = null;
		super.dispose();
	}

	public final void setAlreadyLoad(int load) {
		alreadyLoad = load;
	}

	@Override
	public final void initTask() {
		isBloker = true;
		super.initTask();
	}

	@Override
	public final void run() throws Exception {
		Cursor sysSmsCursor = context.getContentResolver().query(URI, new String[] {
						ID ,
						THREAD_ID ,
						ADDRESS ,
						DATE ,
						BODY ,
						TYPE ,
						SUBJECT
		}, "_id>?", new String[] {
						String.valueOf(alreadyLoad)
		}, "_id desc");
		if (sysSmsCursor != null) try
		{

		}
		finally
		{
			sysSmsCursor.close();
		}

	}

	public final static int	SerialNum	= SendSmsTask.SerialNum + 1;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	public final static Uri		URI			= Uri.parse("content://sms");
	public final static String	ADDRESS		= "address";
	public final static String	DATE		= "date";
	public final static String	READ		= "read";
	public final static String	STATUS		= "status";
	public final static String	TYPE		= "type";
	public final static String	BODY		= "body";
	public final static String	THREAD_ID	= "thread_id";
	public final static String	ID			= "_id";
	public final static String	SUBJECT		= "subject";
}
