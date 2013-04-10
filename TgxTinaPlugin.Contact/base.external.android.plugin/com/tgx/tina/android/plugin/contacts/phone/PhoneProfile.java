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
	public String  displayName;
	public String  phone;
	public String  label;
	public String  phoneMinMatch;
	public boolean isFavorite;
	public int     photoID = -1;
	public long    lastContactTime;
	public int     local_raw_version;
	public long    weightvalue;
	public int     type;
	
	public PhoneProfile(int _ID, int contactID, int rawContactID) {
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
	
	public final static int SerialNum = SerialDomain + 3;
	
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
