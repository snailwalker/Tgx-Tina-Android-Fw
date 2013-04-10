package com.tgx.tina.android.plugin.contacts.category;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;

import android.content.Context;
import android.provider.ContactsContract;


public class CategoryReadTask
        extends
        ContactTask
{
	public CategoryReadTask(Context context) {
		super(context);
	}
	
	public final static int SerialNum = SerialDomain + 40;
	
	@Override
	public void run() throws Exception {
		
	}
	
	@Override
	public int getSerialNum() {
		return SerialNum;
	}
	
	public final static String   GROUPMEM_SELECTION      = ContactsContract.Data.MIMETYPE + "=?";
	public final static String[] GROUPMEM_SELECTION_ARGS = {
		                                                     ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
	                                                     };
	public final static String[] GROUPMEM_PROJECTION     = {
	        ContactsContract.CommonDataKinds.GroupMembership.RAW_CONTACT_ID,
	        ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID
	                                                     };
}
