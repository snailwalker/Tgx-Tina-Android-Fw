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
package com.tgx.tina.android.plugin.contacts.phone;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

/**
 * 读取以Phone为独立元素的非重合通讯录结构
 * 
 * @author Zhangzhuo
 * @see ContactsContract.CommonDataKinds.Phone
 */
public class PhonesReadTask
				extends
				ContactTask
{

	public PhonesReadTask(Context context)
	{
		super(context);
		profilePack = new PhonePack();
	}

	public PhonePack		profilePack;					// 为分段读取准备，使用任务内全局变量，而未使用局部变量

	public final static int	SerialNum	= PhonesReadTaskSN;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public final void dispose() {
		profilePack = null;
		super.dispose();
	}

	@Override
	public void run() throws Exception {
		Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PHONES_PROJECTION, PHONES_SELECTION, PHONES_SELECTION_ARGS, ContactsContract.CommonDataKinds.Phone._ID);
		if (cursor != null) try
		{

			int _ID;
			int rawContactID;
			int contactID;
			String displayName, phone;
			while (cursor.moveToNext())
			{
				_ID = cursor.getInt(column_ID);
				rawContactID = cursor.getInt(column_RAW_CONTACT_ID);
				contactID = cursor.getInt(column_CONTACT_ID);
				displayName = cursor.getString(column_DISPLAY_NAME);
				phone = cursor.getString(column_NUMBER);
				if (phone != null) phone = PhoneUtil.formatPhone(phone);
				if (phone == null || "".equals(phone) || displayName == null || "".equals(displayName)) continue;// 匿名号码过滤掉
				PhoneProfile profile = new PhoneProfile(_ID, contactID, rawContactID);
				profile.isFavorite = cursor.getInt(column_STARRED) == 0 ? false : true;
				profile.lastContactTime = cursor.getLong(column_LAST_TIME_CONTACTED);
				profile.photoID = cursor.isNull(column_PHOTO_ID) ? -1 : cursor.getInt(column_PHOTO_ID);
				profile.type = cursor.getInt(column_TYPE);
				profile.label = cursor.isNull(column_LABEL) ? null : cursor.getString(column_LABEL);
				profile.local_raw_version = cursor.getInt(column_DATA_VERSION);
				profile.displayName = displayName;
				profile.phone = phone;
				profilePack.addProfile(profile);
			}
			commitResult(profilePack, CommitAction.WAKE_UP);
		}
		finally
		{
			cursor.close();
		}

	}

	// 某些系统或者某些状况下PHONES_SELECTION不能 是 <>0，其他通用的方法待查
	public final static String		PHONES_SELECTION		= null;
	public final static String[]	PHONES_SELECTION_ARGS	= null;
	public final static String[]	PHONES_PROJECTION		= {
					ContactsContract.CommonDataKinds.Phone._ID , // -Data._ID
					ContactsContract.CommonDataKinds.Phone.CONTACT_ID ,
					ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID ,
					ContactsContract.CommonDataKinds.Phone.NUMBER ,
					ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME ,
					ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED ,
					ContactsContract.CommonDataKinds.Phone.STARRED ,
					ContactsContract.CommonDataKinds.Phone.PHOTO_ID ,
					ContactsContract.CommonDataKinds.Phone.DATA_VERSION ,
					ContactsContract.CommonDataKinds.Phone.TYPE ,
					ContactsContract.CommonDataKinds.Phone.LABEL
															};

	static HashMap<String, Integer>	PHONES_PROJECTIONMAP	= new HashMap<String, Integer>(20);

	static int						column_ID;
	static int						column_RAW_CONTACT_ID;
	static int						column_CONTACT_ID;
	static int						column_DISPLAY_NAME;
	static int						column_STARRED;
	static int						column_PHOTO_ID;
	static int						column_LAST_TIME_CONTACTED;
	static int						column_NUMBER;
	static int						column_DATA_VERSION;
	static int						column_TYPE;
	static int						column_LABEL;

	static
	{
		for (int i = 0; i < PHONES_PROJECTION.length; i++)
			PHONES_PROJECTIONMAP.put(PHONES_PROJECTION[i], i);
		column_ID = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone._ID);
		column_RAW_CONTACT_ID = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID);
		column_CONTACT_ID = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
		column_DISPLAY_NAME = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
		column_STARRED = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.STARRED);
		column_PHOTO_ID = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.PHOTO_ID);
		column_LAST_TIME_CONTACTED = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.LAST_TIME_CONTACTED);
		column_NUMBER = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.NUMBER);
		column_DATA_VERSION = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.DATA_VERSION);
		column_TYPE = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.TYPE);
		column_LABEL = PHONES_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.LABEL);
	}

}
