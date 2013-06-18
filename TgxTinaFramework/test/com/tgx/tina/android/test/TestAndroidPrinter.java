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
				for (;;)
					try
					{
						Thread.sleep(1100);
						System.out.println("-!-");
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
			}
		}).start();
	}
}
