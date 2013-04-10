package com.tgx.tina.android.plugin.contacts.base;

import android.content.Context;
import base.tina.core.task.Task;


public abstract class ContactTask
        extends
        Task
{
	protected final static int SerialDomain = -0x4000;
	final static int           SerialNum    = SerialDomain;
	protected Context          context;
	
	public ContactTask(Context context) {
		super(ContactTask.SerialNum);
		this.context = context;
	}
	
	@Override
	public void initTask() {
		isBloker = true;
		super.initTask();
	}
	
	@Override
	public void interrupt() {
		disable = true;
	}
	
	@Override
	protected void finishThreadTask() {
		
	}
	
	public final static String TMP_FILE_FOR_READ  = "contacts.buf";
	public final static String TMP_FILE_FOR_WRITE = "download.buf";
	
}
