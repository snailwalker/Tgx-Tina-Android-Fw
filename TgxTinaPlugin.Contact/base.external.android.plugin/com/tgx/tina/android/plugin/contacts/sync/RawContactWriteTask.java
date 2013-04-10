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

import java.util.ArrayList;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import base.tina.core.task.infc.ITaskProgress.TaskProgressType;
import base.tina.external.io.IoUtil;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

/**
 * 用以写入通讯录数据
 * 
 * @author Zhangzhuo
 */

public class RawContactWriteTask
				extends
				ContactTask
{
	public final static int	SerialNum	= SerialDomain + 31;

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

	RawContactProfile	rawContactProfile;
	Account				mAccount;
	TaskProgressType	progressType	= TaskProgressType.horizontal;

	@Override
	public void dispose() {
		if (rawContactProfile != null) rawContactProfile.dispose();
		mAccount = null;
		progressType = null;
		super.dispose();
	}

	public RawContactWriteTask(Context context, RawContactProfile rawContactProfile, Account account)
	{
		super(context);
		this.rawContactProfile = rawContactProfile;
		this.mAccount = account;
	}

	public RawContactWriteTask(Context context, String todoAndvCard, Account account)
	{
		super(context);
		RawContactProfile rawContactProfile = new RawContactProfile(-1);
		if (todoAndvCard.startsWith("add|")) rawContactProfile.toDo = RawContactProfile.toInsert;
		else if (todoAndvCard.startsWith("rep|")) rawContactProfile.toDo = RawContactProfile.toReplace;
		else if (todoAndvCard.startsWith("del|")) rawContactProfile.toDo = RawContactProfile.toDelete;
		if (rawContactProfile.toDo == 0) throw new IllegalStateException("not initlized what to do!");
		String vCard = todoAndvCard.substring(4);
		rawContactProfile.vCardDecode(vCard);
		this.mAccount = account;
		this.rawContactProfile = rawContactProfile;
	}

	public void setTaskProgressType(TaskProgressType type) {
		progressType = type;
	}

	@Override
	public void run() throws Exception {
		if (rawContactProfile == null) return;
		ContentResolver contentResolver = context.getContentResolver();
		String lookUpKey = rawContactProfile.lookUpKey;
		ArrayList<ContentProviderOperation> operations;
		long rawContactId = -1;
		boolean bInsert = true;
		switch (rawContactProfile.toDo) {
			case RawContactProfile.toReplace:
				operations = new ArrayList<ContentProviderOperation>(8);
				rawContactId = rawContactProfile.getRawContactID();
				if (rawContactId > 0)
				{
					operations.add(ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI).withSelection(ContactsContract.Data.RAW_CONTACT_ID + "=?", new String[] {
						String.valueOf(rawContactId)
					}).build());
					contentResolver.applyBatch(ContactsContract.AUTHORITY, operations);
					operations.clear();
					bInsert = false;
				}
			case RawContactProfile.toInsert:
				if (bInsert)
				{
					ContentValues values = new ContentValues();
					if (mAccount != null)
					{
						values.put(ContactsContract.RawContacts.ACCOUNT_NAME, mAccount.name);
						values.put(ContactsContract.RawContacts.ACCOUNT_TYPE, mAccount.type);
					}
					else
					{
						values.putNull(ContactsContract.RawContacts.ACCOUNT_TYPE);
						values.putNull(ContactsContract.RawContacts.ACCOUNT_NAME);
					}
					Uri rawContactUri = contentResolver.insert(ContactsContract.RawContacts.CONTENT_URI, values);
					rawContactId = ContentUris.parseId(rawContactUri);
					values.clear();
				}
				operations = new ArrayList<ContentProviderOperation>(8);
				if (rawContactProfile.name != null)
				{
					Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
									.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
					if (rawContactProfile.name[RawContactProfile.DISPLAY_NAME] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
									rawContactProfile.name[RawContactProfile.DISPLAY_NAME]);
					if (rawContactProfile.name[RawContactProfile.PREFIX_NAME] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredName.PREFIX,
									rawContactProfile.name[RawContactProfile.PREFIX_NAME]);
					if (rawContactProfile.name[RawContactProfile.GIVEN_NAME] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
									rawContactProfile.name[RawContactProfile.GIVEN_NAME]);
					if (rawContactProfile.name[RawContactProfile.MIDDLE_NAME] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
									rawContactProfile.name[RawContactProfile.MIDDLE_NAME]);
					if (rawContactProfile.name[RawContactProfile.FAMILY_NAME] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
									rawContactProfile.name[RawContactProfile.FAMILY_NAME]);
					if (rawContactProfile.name[RawContactProfile.SUFFIX_NAME] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredName.SUFFIX,
									rawContactProfile.name[RawContactProfile.SUFFIX_NAME]);
					operations.add(builder.build());
				}

				if (rawContactProfile.phones != null)
				{
					for (String[] phone : rawContactProfile.phones)
						operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
										.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
										.withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone[RawContactProfile.CONTENT_INDEX])
										.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phone[RawContactProfile.MIMETYPE_INDEX]).build());
				}
				if (rawContactProfile.addresses != null)
				{
					for (String[] address : rawContactProfile.addresses)
					{
						Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
										.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
										.withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, address[RawContactProfile.MIMETYPE_INDEX])
										.withValue(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS, address[RawContactProfile.CONTENT_INDEX]);
						if (address[RawContactProfile.ADDRESS_LABEL] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.LABEL, address[RawContactProfile.ADDRESS_LABEL]);
						if (address[RawContactProfile.ADDRESS_STREET] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.STREET, address[RawContactProfile.ADDRESS_STREET]);
						if (address[RawContactProfile.ADDRESS_POBOX] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POBOX, address[RawContactProfile.ADDRESS_POBOX]);
						if (address[RawContactProfile.ADDRESS_NEIGHBORHOOD] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD,
										address[RawContactProfile.ADDRESS_NEIGHBORHOOD]);
						if (address[RawContactProfile.ADDRESS_CITY] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.CITY, address[RawContactProfile.ADDRESS_CITY]);
						if (address[RawContactProfile.ADDRESS_REGION] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.REGION, address[RawContactProfile.ADDRESS_REGION]);
						if (address[RawContactProfile.ADDRESS_POSTCODE] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, address[RawContactProfile.ADDRESS_POSTCODE]);
						if (address[RawContactProfile.ADDRESS_COUNTRY] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, address[RawContactProfile.ADDRESS_COUNTRY]);
						operations.add(builder.build());
					}
				}
				//formatter:off
				if (rawContactProfile.orgs != null)
				{
					for (String[] org : rawContactProfile.orgs)
					{
						Builder builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						                                          .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
						                                          .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
						if (org[RawContactProfile.CONTENT_INDEX] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.Organization.COMPANY, org[RawContactProfile.CONTENT_INDEX]);
						if (org[RawContactProfile.MIMETYPE_INDEX] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.Organization.TYPE, org[RawContactProfile.MIMETYPE_INDEX]);
						if (org[RawContactProfile.SUB_CONTENT_INDEX] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.Organization.LABEL, org[RawContactProfile.SUB_CONTENT_INDEX]);
						if (org[RawContactProfile.ORG_TITEL] != null) builder = builder.withValue(ContactsContract.CommonDataKinds.Organization.TITLE, org[RawContactProfile.ORG_TITEL]);
						operations.add(builder.build());
					}
				}
				if (rawContactProfile.birthday != null)
				{
					operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
					                                       .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
					                                       .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE)
					                                       .withValue(ContactsContract.CommonDataKinds.Event.START_DATE, rawContactProfile.birthday)
					                                       .withValue(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY)
					                                       .build());
				}
				if (rawContactProfile.emails != null)
				{
					for (String[] email : rawContactProfile.emails)
						operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						                                       .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
						                                       .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
						                                       .withValue(ContactsContract.CommonDataKinds.Email.DATA, email[RawContactProfile.CONTENT_INDEX])
						                                       .withValue(ContactsContract.CommonDataKinds.Email.TYPE, email[RawContactProfile.MIMETYPE_INDEX])
						                                       .build());
				}
				if (rawContactProfile.webUrls != null)
				{
					//这里的website API 中有类型 ，这里先不管了，都other吧				
					for (String webUrl : rawContactProfile.webUrls)
						operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
						                                       .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
						                                       .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE)
						                                       .withValue(ContactsContract.CommonDataKinds.Website.URL, webUrl)
						                                       .withValue(ContactsContract.CommonDataKinds.Website.TYPE, ContactsContract.CommonDataKinds.Website.TYPE_OTHER)
						                                       .build());
				}
				if (rawContactProfile.note != null) 
					operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                           .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                                           .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE)
                                                           .withValue(ContactsContract.CommonDataKinds.Note.NOTE, rawContactProfile.note)
                                                           .build());
				if (rawContactProfile.nickName != null)
					operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                           .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                                           .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE)
                                                           .withValue(ContactsContract.CommonDataKinds.Nickname.NAME, rawContactProfile.nickName)
                                                           .build());
				if (rawContactProfile.photoEncoded != null) 
					operations.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                                           .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawContactId)
                                                           .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                                                           .withValue(
                                                                      ContactsContract.CommonDataKinds.Photo.PHOTO,
                                                                      IoUtil.base64Decoder(rawContactProfile.photoEncoded.toCharArray(), 0))
                                                           .build());
				//formatter:on
				contentResolver.applyBatch(ContactsContract.AUTHORITY, operations);
				// 未来将对rawContacts表使用 source_id来标示服务器使用的UID信息,以更好的对同步操作提供支持
				operations.clear();
				break;
			case RawContactProfile.toDelete:
				if (lookUpKey != null)
				{
					Uri contactUri = ContactsContract.Contacts.lookupContact(contentResolver, Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, Uri.encode(lookUpKey)));
					Uri deleteUri = contactUri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true").build();
					if (deleteUri != null) contentResolver.delete(deleteUri, null, null);
				}
				break;

		}
		if (progress != null) progress.updateProgress(progressType, 1);
	}

}
