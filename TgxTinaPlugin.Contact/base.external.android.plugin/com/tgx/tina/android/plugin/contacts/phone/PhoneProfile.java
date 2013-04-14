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

import com.tgx.tina.android.plugin.contacts.base.Profile;

import android.content.res.Resources;
import android.provider.ContactsContract;

public final class PhoneProfile
				extends
				Profile
				implements
				Comparable<PhoneProfile>
{
	public String	displayName;
	public String	phone;
	public String	label;
	public String	phoneMinMatch;
	public boolean	isFavorite;
	public int		photoID	= -1;
	public long		lastContactTime;
	public int		local_raw_version;
	public long		weightvalue;
	public int		type;

	@Override
	public Profile clone() {
		PhoneProfile phoneProfile = new PhoneProfile(primaryKey, getContactID(), getRawContactID());
		phoneProfile.displayName = displayName;
		phoneProfile.phone = phone;
		phoneProfile.label = label;
		phoneProfile.phoneMinMatch = phoneMinMatch;
		phoneProfile.isFavorite = isFavorite;
		phoneProfile.photoID = photoID;
		phoneProfile.lastContactTime = lastContactTime;
		phoneProfile.local_raw_version = local_raw_version;
		phoneProfile.weightvalue = weightvalue;
		phoneProfile.type = type;
		return phoneProfile;
	}

	public PhoneProfile(int _ID, int contactID, int rawContactID)
	{
		primaryKey = _ID;
		foreignKey = rawContactID;
		externalKey = contactID;
	}

	@Override
	public void dispose() {
		displayName = null;
		phone = null;
		label = null;
		phoneMinMatch = null;
		super.dispose();
	}

	public final static int	SerialNum	= PhoneProfileSN;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public final int getContactID() {
		return externalKey;
	}

	@Override
	public final int getRawContactID() {
		return foreignKey;
	}

	@Override
	public final int compareTo(PhoneProfile another) {
		if (another == null) return -1;
		return externalKey < another.externalKey ? -1 : externalKey > another.externalKey ? 1 : foreignKey < another.foreignKey ? -1 : foreignKey > another.foreignKey ? 1 : 0;
	}

	public final String getType(Resources res) {
		if (type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) return label;
		return res.getString(ContactsContract.CommonDataKinds.Phone.getTypeLabelResource(type));
	}

}
