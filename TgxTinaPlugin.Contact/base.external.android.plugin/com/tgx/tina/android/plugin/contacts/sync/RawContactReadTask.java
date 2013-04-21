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

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.util.SparseArray;
import base.tina.core.task.infc.ITaskProgress.TaskProgressType;
import base.tina.external.io.IoUtil;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

/**
 * 用以给需要获取特定RawContactID时返回RawContactProfile
 * 
 * @author Zhangzhuo
 */

public class RawContactReadTask
				extends
				ContactTask
{
	protected int[]				rawContactIDs;
	private TaskProgressType	progressType	= TaskProgressType.horizontal;

	public RawContactReadTask(Context context, int[] rawContactIDs)
	{
		super(context);
		this.rawContactIDs = rawContactIDs;
		profileMap = new SparseArray<RawContactProfile>();
	}

	@Override
	public void dispose() {
		profileMap.clear();
		rawContactIDs = null;
		progressType = null;
		super.dispose();
	}

	public final static int	SerialNum	= RawContactReadTaskSN;

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

	public void setTaskProgressType(TaskProgressType type) {
		progressType = type;
	}

	protected SparseArray<RawContactProfile>	profileMap;

	protected void initProfileMap() {
		for (int i = 0; i < rawContactIDs.length; i++)
		{
			RawContactProfile profile = new RawContactProfile(rawContactIDs[i]);
			profileMap.put(rawContactIDs[i], profile);
		}
	}

	protected RawContactPack getPack() {
		RawContactPack profilePack = new RawContactPack();
		final String[] SELECTION_ARGS = new String[rawContactIDs.length];
		for (int i = 0; i < rawContactIDs.length; i++)
			SELECTION_ARGS[i] = String.valueOf(rawContactIDs[i]);
		initProfileMap();
		int rawContactID, contactID, lastRawContatcID = -1;
		RawContactProfile profile = null;
		StringBuffer sqlWhere = new StringBuffer();
		sqlWhere.append(Data.RAW_CONTACT_ID + " IN (");
		sqlWhere.append("?");
		for (int i = 1; i < rawContactIDs.length; i++)
		{
			sqlWhere.append(",?");
		}
		sqlWhere.append(")");
		Cursor cursor = context.getContentResolver().query(Data.CONTENT_URI, RawContactReadAllTask.DATA_PROJECTION_STRINGS, sqlWhere.toString(), SELECTION_ARGS, Data.RAW_CONTACT_ID);
		if (cursor != null) try
		{
			String mimeType;
			while (cursor.moveToNext() && !disable)
			{
				profile = null;
				rawContactID = cursor.getInt(RawContactReadAllTask.DATA_PROJECTIONMAP.get(Data.RAW_CONTACT_ID));
				profile = profileMap.get(rawContactID);
				if (profile == null) continue;
				if (rawContactID != lastRawContatcID)// 由于检索过程以Data.RAW_CONTACT_ID排序,所以递增过程是有序可查的
				{
					if (lastRawContatcID >= 0)
					{
						RawContactProfile lastProfile = profileMap.get(lastRawContatcID);
						if (lastProfile != null)
						{
							profilePack.addProfile(lastProfile);
						}
					}
					lastRawContatcID = rawContactID;
					if (progress != null) progress.updateProgress(progressType, 1);
				}

				contactID = cursor.getInt(RawContactReadAllTask.DATA_PROJECTIONMAP.get(Data.CONTACT_ID));
				profile.setContactID(contactID);
				mimeType = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(Data.MIMETYPE));
				if (ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mimeType))
				{
					profile.name = new String[7];
					profile.displayName = profile.name[RawContactProfile.DISPLAY_NAME] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
					profile.name[RawContactProfile.PREFIX_NAME] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.PREFIX));
					profile.name[RawContactProfile.GIVEN_NAME] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
					profile.name[RawContactProfile.MIDDLE_NAME] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME));
					profile.name[RawContactProfile.FAMILY_NAME] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));
					profile.name[RawContactProfile.SUFFIX_NAME] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredName.SUFFIX));
				}
				else if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType))
				{
					if (profile.phones == null) profile.phones = new String[1][2];// 存在多个电话的情况
					else
					{
						String[][] tmp = profile.phones;
						profile.phones = new String[tmp.length + 1][2];
						System.arraycopy(tmp, 0, profile.phones, 0, tmp.length);
					}
					int phone_index = profile.phones.length - 1;
					profile.phones[phone_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.NUMBER));
					profile.phones[phone_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Phone.TYPE));
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
					profile.addresses[address_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS));
					profile.addresses[address_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
					profile.addresses[address_index][RawContactProfile.ADDRESS_LABEL] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.LABEL));
					profile.addresses[address_index][RawContactProfile.ADDRESS_STREET] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
					profile.addresses[address_index][RawContactProfile.ADDRESS_POBOX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.POBOX));
					profile.addresses[address_index][RawContactProfile.ADDRESS_NEIGHBORHOOD] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP
									.get(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD));
					profile.addresses[address_index][RawContactProfile.ADDRESS_CITY] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
					profile.addresses[address_index][RawContactProfile.ADDRESS_REGION] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
					profile.addresses[address_index][RawContactProfile.ADDRESS_POSTCODE] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
					profile.addresses[address_index][RawContactProfile.ADDRESS_COUNTRY] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
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
					profile.orgs[org_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.COMPANY));
					profile.orgs[org_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.TYPE));
					profile.orgs[org_index][RawContactProfile.ORG_TITEL] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.TITLE));
					profile.orgs[org_index][RawContactProfile.SUB_CONTENT_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Organization.LABEL));
				}
				else if (ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType))
				{
					if (profile.emails == null) profile.emails = new String[1][2];
					else
					{
						String[][] tmp = profile.emails;
						profile.emails = new String[tmp.length + 1][2];
						System.arraycopy(tmp, 0, profile.emails, 0, tmp.length);
					}
					int email_index = profile.emails.length - 1;
					profile.emails[email_index][RawContactProfile.CONTENT_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Email.DATA));
					profile.emails[email_index][RawContactProfile.MIMETYPE_INDEX] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Email.TYPE));
				}
				else if (ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE.equals(mimeType))
				{
					if (cursor.getInt(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Event.TYPE)) == ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
					{
						profile.birthday = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Event.START_DATE));
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
					profile.webUrls[webUrl_index] = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Website.URL));
				}
				else if (ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE.equals(mimeType)) profile.note = cursor
								.getString(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Note.NOTE));
				else if (ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE.equals(mimeType)) profile.nickName = cursor.getString(RawContactReadAllTask.DATA_PROJECTIONMAP
								.get(ContactsContract.CommonDataKinds.Nickname.NAME));
				else if (ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE.equals(mimeType))
				{
					byte[] photoData = cursor.getBlob(RawContactReadAllTask.DATA_PROJECTIONMAP.get(ContactsContract.CommonDataKinds.Photo.PHOTO));
					profile.photoEncoded = IoUtil.base64Encoder(photoData, 0, 76);
				}
			}
			RawContactProfile lastProfile = profileMap.get(lastRawContatcID);
			if (lastProfile != null) profilePack.addProfile(lastProfile);
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
		if (profileMap != null) profileMap.clear();
		if (progress != null) progress.finishProgress(disable ? TaskProgressType.cancel : TaskProgressType.complete);

		return profilePack;
	}

	@Override
	public void run() throws Exception {
		if (rawContactIDs == null) return;
		RawContactPack profilePack = getPack();
		commitResult(profilePack, CommitAction.WAKE_UP);
	}
}
