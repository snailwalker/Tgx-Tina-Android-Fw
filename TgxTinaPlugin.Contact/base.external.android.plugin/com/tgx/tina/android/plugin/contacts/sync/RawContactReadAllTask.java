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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.util.SparseArray;
import base.tina.core.task.infc.ITaskProgress.TaskProgressType;
import base.tina.external.io.IoUtil;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

/**
 * 当使用分段模式执行时,未执行完毕时 需要对数据变更做出特定处理,如果需要强数据一致性就需要重读
 * 
 * @author Zhangzhuo
 */

public class RawContactReadAllTask
				extends
				ContactTask
{
	public final static int			SerialNum	= RawContactReadAllTaskSN;
	boolean							profileMapInit;
	int								lastCursorIndex;
	int								available;
	SparseArray<RawContactProfile>	profileMap;
	TaskProgressType				progressType;
	VCardPack						vCardPack	= new VCardPack();
	FileOutputStream				bufferStream;

	public RawContactReadAllTask(Context context)
	{
		super(context);
		try
		{
			bufferStream = context.openFileOutput(TMP_FILE_FOR_READ, Context.MODE_PRIVATE);
		}
		catch (FileNotFoundException e)
		{
			//#debug warn
			e.printStackTrace();
		}
	}

	public void setTaskProgressType(TaskProgressType type) {
		progressType = type;
	}

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

	@Override
	public void run() throws Exception {
		int rawContactID, contactID, lastRawContatcID = -1;
		RawContactProfile profile = null;

		TaskRunner:
		{
			if (!profileMapInit)
			{
				Cursor cursor = context.getContentResolver().query(RawContacts.CONTENT_URI, RAW_CONTACT_PROJECTION_STRINGS, RAW_SELECTION_STRING, null, RawContacts._ID);
				available = 0;
				if (cursor != null) try
				{
					available = cursor.getCount();
					if (progress != null) progress.createProgress(progressType, available);
					if (profileMap != null)
					{
						for (int i = 0, size = profileMap.size(); i < size; i++)
						{
							RawContactProfile rawContactProfile = profileMap.valueAt(i);
							rawContactProfile.dispose();
						}
						profileMap.clear();
					}
					else profileMap = new SparseArray<RawContactProfile>(available);
					while (cursor.moveToNext() && !disable)
					{
						profile = null;
						rawContactID = cursor.getInt(0);
						profile = profileMap.get(rawContactID);
						if (profile == null)
						{
							profile = new RawContactProfile(rawContactID);
							profileMap.put(rawContactID, profile);
						}
					}
					if (disable) break TaskRunner;
				}
				catch (Exception e)
				{
					//#debug warn
					e.printStackTrace();
					if (progress != null) progress.finishProgress(TaskProgressType.error);
				}
				finally
				{
					cursor.close();
				}
				lastCursorIndex = 0;
				profileMapInit = true;
			}
			rawContactID = 0;
			Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, DATA_PROJECTION_STRINGS, null, null, Data.RAW_CONTACT_ID);
			if (cursor != null) try
			{
				if (profileMap == null) return;
				String mimeType;
				if (cursor.moveToPosition(lastCursorIndex))
				{
					do
					{
						profile = null;
						rawContactID = cursor.getInt(DATA_PROJECTIONMAP.get(Data.RAW_CONTACT_ID));
						profile = profileMap.get(rawContactID);
						if (profile == null) continue;
						if (rawContactID != lastRawContatcID)
						{
							RawContactProfile lastProfile = profileMap.get(lastRawContatcID);
							if (lastRawContatcID >= 0)
							{
								vCardPack.addProfileVCard(lastProfile.vCardEncode());
								lastProfile.dispose();
							}

							lastRawContatcID = rawContactID;
							if (progress != null) progress.updateProgress(progressType, 1);

							if (vCardPack.needDump()) vCardPack.dumpTofile(bufferStream);
						}

						contactID = cursor.getInt(DATA_PROJECTIONMAP.get(Data.CONTACT_ID));
						profile.setContactID(contactID);
						mimeType = cursor.getString(DATA_PROJECTIONMAP.get(Data.MIMETYPE));
						if (ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							profile.name = new String[7];
							profile.displayName = profile.name[RawContactProfile.DISPLAY_NAME] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
							profile.name[RawContactProfile.PREFIX_NAME] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.PREFIX));
							profile.name[RawContactProfile.GIVEN_NAME] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
							profile.name[RawContactProfile.MIDDLE_NAME] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
							profile.name[RawContactProfile.FAMILY_NAME] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
							profile.name[RawContactProfile.SUFFIX_NAME] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.SUFFIX));
						}
						else if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							if (profile.phones == null) profile.phones = new String[1][4];// 存在多个电话的情况
							else
							{
								String[][] tmp = profile.phones;
								profile.phones = new String[tmp.length + 1][4];
								System.arraycopy(tmp, 0, profile.phones, 0, tmp.length);
							}
							int phone_index = profile.phones.length - 1;
							profile.phones[phone_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.NUMBER));
							profile.phones[phone_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.TYPE));
							profile.phones[phone_index][RawContactProfile.SUB_CONTENT_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.LABEL));
						}
						else if (ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							if (profile.addresses == null) profile.addresses = new String[1][10];
							else
							{
								String[][] tmp = profile.addresses;
								profile.addresses = new String[tmp.length + 1][10];
								System.arraycopy(tmp, 0, profile.addresses, 0, tmp.length);
							}
							int address_index = profile.addresses.length - 1;
							profile.addresses[address_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
							profile.addresses[address_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
							profile.addresses[address_index][RawContactProfile.ADDRESS_LABEL] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.LABEL));
							profile.addresses[address_index][RawContactProfile.ADDRESS_STREET] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
							profile.addresses[address_index][RawContactProfile.ADDRESS_POBOX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.POBOX));
							profile.addresses[address_index][RawContactProfile.ADDRESS_NEIGHBORHOOD] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD));
							profile.addresses[address_index][RawContactProfile.ADDRESS_CITY] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
							profile.addresses[address_index][RawContactProfile.ADDRESS_REGION] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
							profile.addresses[address_index][RawContactProfile.ADDRESS_POSTCODE] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
							profile.addresses[address_index][RawContactProfile.ADDRESS_COUNTRY] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
						}
						else if (ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							if (profile.orgs == null) profile.orgs = new String[1][5];
							else
							{
								String[][] tmp = profile.orgs;
								profile.orgs = new String[tmp.length + 1][5];
								System.arraycopy(tmp, 0, profile.orgs, 0, tmp.length);
							}
							int org_index = profile.orgs.length - 1;
							profile.orgs[org_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.COMPANY));
							profile.orgs[org_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.TYPE));
							profile.orgs[org_index][RawContactProfile.ORG_TITEL] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.TITLE));
							profile.orgs[org_index][RawContactProfile.SUB_CONTENT_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.LABEL));
						}
						else if (ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							if (profile.emails == null) profile.emails = new String[1][3];
							else
							{
								String[][] tmp = profile.emails;
								profile.emails = new String[tmp.length + 1][3];
								System.arraycopy(tmp, 0, profile.emails, 0, tmp.length);
							}
							int email_index = profile.emails.length - 1;
							profile.emails[email_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Email.DATA));
							profile.emails[email_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Email.TYPE));
							profile.emails[email_index][RawContactProfile.SUB_CONTENT_INDEX] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Email.LABEL));
						}
						else if (ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							if (cursor.getInt(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Event.TYPE)) == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
							{
								profile.birthday = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Event.START_DATE));
							}
						}
						else if (ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							// 这里的website API 中有类型 ，这里先不管了，都other吧
							if (profile.webUrls == null) profile.webUrls = new String[1];
							else
							{
								String[] tmp = profile.webUrls;
								profile.webUrls = new String[tmp.length + 1];
								System.arraycopy(tmp, 0, profile.webUrls, 0, tmp.length);
							}
							int webUrl_index = profile.webUrls.length - 1;
							profile.webUrls[webUrl_index] = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Website.URL));
						}
						else if (ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE.equals(mimeType)) profile.note = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Note.NOTE));
						else if (ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) profile.nickName = cursor.getString(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Nickname.NAME));
						else if (ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE.equals(mimeType))
						{
							byte[] photoData = cursor.getBlob(DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Photo.PHOTO));
							profile.photoEncoded = IoUtil.base64Encoder(photoData, 0, 76);
						}
						lastCursorIndex++;
						if (segmentBreak()) return;
					}
					while (cursor.moveToNext() && !disable);
					RawContactProfile lastProfile = profileMap.get(lastRawContatcID);
					vCardPack.addProfileVCard(lastProfile.vCardEncode());
					lastProfile.dispose();
					vCardPack.dumpTofile(bufferStream);
					bufferStream.close();
					setDone();
				}
				commitResult(vCardPack, CommitAction.WAKE_UP);
			}
			catch (Exception e)
			{
				//#debug warn
				e.printStackTrace();
				if (progress != null) progress.finishProgress(TaskProgressType.error);
			}
			finally
			{
				cursor.close();
			}
		}
		if (profileMap != null)
		{
			for (int i = 0, size = profileMap.size(); i < size; i++)
			{
				RawContactProfile rawContactProfile = profileMap.valueAt(i);
				rawContactProfile.dispose();
			}
			profileMap.clear();
		}
		if (progress != null) progress.finishProgress(disable ? TaskProgressType.cancel : TaskProgressType.complete);
	}

	@Override
	public void initTask() {
		isSegment = true;
		super.initTask();
	}

	@Override
	protected boolean segmentBreak() {
		return vCardPack.indexNum > 150;
	}

	static HashMap<String, Integer>	DATA_PROJECTIONMAP				= new HashMap<String, Integer>(20);

	final static String[]			DATA_PROJECTION_STRINGS			= {
					Data.RAW_CONTACT_ID ,
					Data.CONTACT_ID ,
					Data.MIMETYPE ,
					Data.DISPLAY_NAME ,
					Data.LOOKUP_KEY ,
					Data.IS_PRIMARY ,
					Data.IS_SUPER_PRIMARY ,
					Data.PHOTO_ID ,
					Data.DATA1 ,
					Data.DATA2 ,
					Data.DATA3 ,
					Data.DATA4 ,
					Data.DATA5 ,
					Data.DATA6 ,
					Data.DATA7 ,
					Data.DATA8 ,
					Data.DATA9 ,
					Data.DATA10 ,
					Data.DATA15 ,
					Data.DATA_VERSION
																	};
	static
	{
		for (int i = 0; i < DATA_PROJECTION_STRINGS.length; i++)
			DATA_PROJECTIONMAP.put(DATA_PROJECTION_STRINGS[i], i);
	}
	final static String				RAW_SELECTION_STRING			= Data.DELETED + "=0";
	final static String[]			SELECTIONARGS_STRINGS			= {
																		"0" ,
																	};
	final static String[]			RAW_CONTACT_PROJECTION_STRINGS	= {
																		RawContacts._ID
																	};

}
