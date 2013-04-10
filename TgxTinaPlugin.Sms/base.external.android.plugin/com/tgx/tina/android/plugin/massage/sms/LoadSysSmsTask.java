package com.tgx.tina.android.plugin.massage.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import base.tina.core.task.Task;
import base.tina.core.task.infc.ITaskProgress;


public class LoadSysSmsTask
        extends
        Task
{
	private Context context;
	private int     alreadyLoad;
	
	public LoadSysSmsTask(int threadId, ITaskProgress progress, Context context, int alreadyLoad) {
		super(threadId, progress);
		this.context = context;
		setAlreadyLoad(alreadyLoad);
	}
	
	@Override
	public void dispose() {
		context = null;
		super.dispose();
	}
	
	public final void setAlreadyLoad(int load) {
		alreadyLoad = load;
	}
	
	@Override
	public final void initTask() {
		isBloker = true;
		super.initTask();
	}
	
	@Override
	public final void run() throws Exception {
		Cursor sysSmsCursor = context.getContentResolver().query(URI, new String[] {
		        ID,
		        THREAD_ID,
		        ADDRESS,
		        DATE,
		        BODY,
		        TYPE,
		        SUBJECT
		}, "_id>?", new String[] {
			String.valueOf(alreadyLoad)
		}, "_id desc");
		if (sysSmsCursor != null) try
		{
			
		}
		finally
		{
			sysSmsCursor.close();
		}
		
	}
	
	public final static int SerialNum = SendSmsTask.SerialNum + 1;
	
	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
	
	public final static Uri    URI       = Uri.parse("content://sms");
	public final static String ADDRESS   = "address";
	public final static String DATE      = "date";
	public final static String READ      = "read";
	public final static String STATUS    = "status";
	public final static String TYPE      = "type";
	public final static String BODY      = "body";
	public final static String THREAD_ID = "thread_id";
	public final static String ID        = "_id";
	public final static String SUBJECT   = "subject";
}
