package com.tgx.tina.android.plugin.contacts.sync;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import base.tina.core.task.infc.ITaskProgress.TaskProgressType;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;
import com.tgx.tina.android.plugin.contacts.sync.VCardPack.Status;


public class ReadSimContactTask
        extends
        ContactTask
{
	
	public ReadSimContactTask(Context context) {
		super(context);
		try
		{
			File readFile = context.getFileStreamPath(TMP_FILE_FOR_READ);
			readFile.delete();
			bufferStream = context.openFileOutput(TMP_FILE_FOR_READ, Context.MODE_PRIVATE);
		}
		catch (FileNotFoundException e)
		{
			//#debug warn
			e.printStackTrace();
		}
	}
	
	int                     available;
	TaskProgressType        progressType;
	FileOutputStream        bufferStream;
	VCardPack               vCardPack;
	public final static int SerialNum = ReadSimContactTaskSN;
	
	@Override
	public int getSerialNum() {
		return SerialNum;
	}
	
	@Override
	public void dispose() {
		progressType = null;
		bufferStream = null;
		if (disable && vCardPack != null) vCardPack.dispose();
		vCardPack = null;
		super.dispose();
	}
	
	@Override
	public void run() throws Exception {
		RawContactProfile profile = null;
		TelephonyManager mTelephonyManager;
		mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int simState = mTelephonyManager.getSimState();
		available = 0;
		switch (simState) {
			case TelephonyManager.SIM_STATE_ABSENT:
			case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
			case TelephonyManager.SIM_STATE_PIN_REQUIRED:
			case TelephonyManager.SIM_STATE_PUK_REQUIRED:
				break;
			case TelephonyManager.SIM_STATE_READY:
				Uri uri = Uri.parse("content://icc/adn");
				Cursor cursor = context.getContentResolver().query(uri, new String[] {
				        "_id",
				        "name",
				        "number"
				}, null, null, null);
				Query:
				{
					if (cursor != null) try
					{
						available = cursor.getCount();
						if (progress != null) progress.createProgress(progressType, available);
						if (available <= 0) break Query;
						vCardPack = new VCardPack(false);
						vCardPack.setStatus(Status.TO_READ);
						int id_ci = cursor.getColumnIndex("_id");
						int name_ci = cursor.getColumnIndex("name");
						int num_ci = cursor.getColumnIndex("number");
						while (cursor.moveToNext() && !disable)
						{
							/** new profile */
							profile = new RawContactProfile();
							profile.name = new String[7];
							profile.phones = new String[1][2];
							profile.name[RawContactProfile.DISPLAY_NAME] = cursor.getString(name_ci);
							profile.phones[0][RawContactProfile.CONTENT_INDEX] = cursor.getString(num_ci);
							profile.phones[0][RawContactProfile.MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
							profile.setRawContactID(-Integer.parseInt(cursor.getString(id_ci)));
							vCardPack.addProfileVCard(profile.vCardEncode());
							profile.dispose();
							if (vCardPack.needDump()) vCardPack.dumpTofile(bufferStream);
							if (progress != null) progress.updateProgress(progressType, 1);
						}
						if (profile != null) profile.dispose();
						if (!disable)
						{
							vCardPack.dumpTofile(bufferStream);
							vCardPack.setStatus(Status.READ_COMPLETE);
							commitResult(vCardPack, CommitAction.WAKE_UP);
						}
						else
						{
							vCardPack.dispose();
						}
					}
					catch (Exception e)
					{
						if (progress != null) progress.finishProgress(TaskProgressType.error);
						throw e;
					}
					finally
					{
						cursor.close();
					}
				}
			default:
				if (progress != null) progress.finishProgress(disable ? TaskProgressType.cancel : TaskProgressType.complete);
				break;
		}
		
	}
}
