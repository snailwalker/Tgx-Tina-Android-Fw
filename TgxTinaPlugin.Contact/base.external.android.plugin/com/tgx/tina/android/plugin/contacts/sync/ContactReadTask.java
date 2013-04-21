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
package com.tgx.tina.android.plugin.contacts.sync;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.RawContacts;

public class ContactReadTask
				extends
				ContactTask
{
	public final static int	SerialNum	= ContactReadTaskSN;

	public ContactReadTask(Context context, int contactId)
	{
		super(context);
		this.contactId = contactId;
	}

	protected final int		contactId;
	final static String[]	projection	= {
											RawContacts._ID

										};
	final static String		selection	= RawContacts.CONTACT_ID + "=?";

	@Override
	public void run() throws Exception {
		int[] rawContactIds = getRawContactIds(contactId);
		scheduleService.requestService(new RawContactReadTask(context, rawContactIds), false, getListenSerial());
	}

	protected int[] getRawContactIds(int contactId) {
		Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, projection, selection, new String[] {
						String.valueOf(contactId)
		}, null);
		if (cursor != null) try
		{
			if (cursor.getCount() < 1) return null;
			int[] rawContactIds = new int[1];
			while (cursor.moveToNext())
			{
				int rawContactId = cursor.getInt(0);
				if (rawContactIds[rawContactIds.length - 1] == 0) rawContactIds[rawContactIds.length - 1] = rawContactId;
				else
				{
					int[] tmp = new int[rawContactIds.length + 1];
					System.arraycopy(rawContactIds, 0, tmp, 0, rawContactIds.length);
					rawContactIds = tmp;
					rawContactIds[rawContactIds.length - 1] = rawContactId;
				}
			}
			return rawContactIds;
		}
		finally
		{
			cursor.close();
		}
		return null;
	}

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

}
