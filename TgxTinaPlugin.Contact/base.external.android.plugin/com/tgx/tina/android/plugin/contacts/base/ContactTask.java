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
package com.tgx.tina.android.plugin.contacts.base;

import android.content.Context;
import base.tina.core.task.Task;

public abstract class ContactTask
				extends
				Task
{
	protected final static int	SerialDomain				= -0x4000;
	final static int			SerialNum					= SerialDomain;
	protected final static int	CallLogReadTaskSN			= SerialNum + 1;
	protected final static int	CategoryReadTaskSN			= CallLogReadTaskSN + 1;
	protected final static int	PhonesReadTaskSN			= CategoryReadTaskSN + 1;
	protected final static int	ContactReadTaskSN			= PhonesReadTaskSN + 1;
	protected final static int	RawContactReadTaskSN		= ContactReadTaskSN + 1;
	protected final static int	RawContactReadAllTaskSN		= RawContactReadTaskSN + 1;
	protected final static int	RawContactWriteTaskSN		= RawContactReadAllTaskSN + 1;
	protected final static int	RawContactBatchWriteTaskSN	= RawContactWriteTaskSN + 1;

	protected Context			context;

	public ContactTask(Context context)
	{
		super(ContactTask.SerialNum);
		this.context = context;
	}

	@Override
	public void initTask() {
		isBloker = true;
		super.initTask();
	}

	@Override
	public void interrupt() {
		disable = true;
	}

	@Override
	protected void finishThreadTask() {

	}

	public final static String	TMP_FILE_FOR_READ	= "contacts.buf";
	public final static String	TMP_FILE_FOR_WRITE	= "download.buf";

}
