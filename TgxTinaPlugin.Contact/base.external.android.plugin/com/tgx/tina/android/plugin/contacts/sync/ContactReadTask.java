package com.tgx.tina.android.plugin.contacts.sync;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.RawContacts;


public class ContactReadTask
        extends
        ContactTask
{
	public final static int SerialNum = SerialDomain + 34;
	
	public ContactReadTask(Context context, int contactId) {
		super(context);
		this.contactId = contactId;
	}
	
	private final int     contactId;
	final static String[] projection = {
		                                 RawContacts._ID
	                                 
	                                 };
	final static String   selection  = RawContacts.CONTACT_ID + "=?";
	
	@Override
	public void run() throws Exception {
		int[] rawContactIds = getRawContactIds();
		scheduleService.requestService(new RawContactReadTask(context, rawContactIds), false, getListenSerial());
	}
	
	protected int[] getRawContactIds() {
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
