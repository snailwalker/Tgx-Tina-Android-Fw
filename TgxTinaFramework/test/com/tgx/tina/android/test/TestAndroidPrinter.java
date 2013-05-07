package com.tgx.tina.android.test;

import com.tgx.tina.android.log.AndroidPrinter;

import android.app.Activity;
import android.os.Bundle;


public class TestAndroidPrinter
        extends
        Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidPrinter.createByActivity(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		new Thread(new Runnable()
		{
			
			@Override
			public void run() {
				try
				{
					Thread.sleep(1100);
					throw new RuntimeException("~~~~~");
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}).start();
//		
	}
}
