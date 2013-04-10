package com.tgx.tina.android.plugin.massage.sms;

import android.database.ContentObserver;


public abstract class SmsContentObserver
        extends
        ContentObserver
{
	
	public SmsContentObserver() {
		super(null);
	}
	
	@Override
	public boolean deliverSelfNotifications() {
		return false;
	}
	
	@Override
	public void onChange(boolean selfChange) {
		if (enabled && !selfChange) readSms();
	}
	
	protected boolean enabled;
	
	public void setEnable(boolean enabled) {
		this.enabled = enabled;
	}
	
	protected abstract void readSms();
}
