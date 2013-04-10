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
package com.tgx.tina.android.plugin.downloader;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import java.io.File;

/**
 * Receives system broadcasts (boot, network connectivity)
 */
public class DownloadReceiver
				extends
				BroadcastReceiver
{

	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
		{
			//#debug verbose
			base.tina.core.log.LogPrinter.v(Constants.TAG, "Receiver onBoot");
			context.startService(new Intent(context, DownloadService.class));
		}
		else if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION))
		{
			//#debug verbose
			base.tina.core.log.LogPrinter.v(Constants.TAG, "Receiver onConnectivity");
			NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
			if (info != null && info.isConnected())
			{
				//#ifdef debug
				//#if debug<=2
				if (Helpers.isNetworkAvailable(context))
				{
					base.tina.core.log.LogPrinter.i(Constants.TAG, "Broadcast: Network Up");
				}
				else
				{
					base.tina.core.log.LogPrinter.i(Constants.TAG, "Broadcast: Network Up, Actually Down");
				}
				//#endif
				//#endif
				context.startService(new Intent(context, DownloadService.class));
			}
			else
			{
				//#ifdef debug
				//#if debug<=2
				if (Helpers.isNetworkAvailable(context))
				{
					base.tina.core.log.LogPrinter.i(Constants.TAG, "Broadcast: Network Down, Actually Up");
				}
				else
				{
					base.tina.core.log.LogPrinter.i(Constants.TAG, "Broadcast: Network Down");
				}
				//#endif
				//#endif
			}
		}
		else if (intent.getAction().equals(Constants.ACTION_RETRY))
		{
			//#debug verbose
			base.tina.core.log.LogPrinter.v(Constants.TAG, "Receiver retry");
			context.startService(new Intent(context, DownloadService.class));
		}
		else if (intent.getAction().equals(Constants.ACTION_OPEN) || intent.getAction().equals(Constants.ACTION_LIST))
		{
			//#ifdef debug
			//#if debug<1
			if (intent.getAction().equals(Constants.ACTION_OPEN))
			{
				base.tina.core.log.LogPrinter.v(Constants.TAG, "Receiver open for " + intent.getData());
			}
			else
			{
				base.tina.core.log.LogPrinter.v(Constants.TAG, "Receiver list for " + intent.getData());
			}
			//#endif
			//#endif
			Cursor cursor = context.getContentResolver().query(intent.getData(), null, null, null, null);
			if (cursor != null)
			{
				if (cursor.moveToFirst())
				{
					int statusColumn = cursor.getColumnIndexOrThrow(GlobalDownload.COLUMN_STATUS);
					int status = cursor.getInt(statusColumn);
					int visibilityColumn = cursor.getColumnIndexOrThrow(GlobalDownload.COLUMN_VISIBILITY);
					int visibility = cursor.getInt(visibilityColumn);
					if (GlobalDownload.isStatusCompleted(status) && visibility == GlobalDownload.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
					{
						ContentValues values = new ContentValues();
						values.put(GlobalDownload.COLUMN_VISIBILITY, GlobalDownload.VISIBILITY_VISIBLE);
						context.getContentResolver().update(intent.getData(), values, null, null);
					}

					if (intent.getAction().equals(Constants.ACTION_OPEN))
					{
						int filenameColumn = cursor.getColumnIndexOrThrow(GlobalDownload._DATA);
						int mimetypeColumn = cursor.getColumnIndexOrThrow(GlobalDownload.COLUMN_MIME_TYPE);
						String filename = cursor.getString(filenameColumn);
						String mimetype = cursor.getString(mimetypeColumn);
						Uri path = Uri.parse(filename);
						// If there is no scheme, then it must be a file
						if (path.getScheme() == null)
						{
							path = Uri.fromFile(new File(filename));
						}
						Intent activityIntent = new Intent(Intent.ACTION_VIEW);
						activityIntent.setDataAndType(path, mimetype);
						activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						try
						{
							context.startActivity(activityIntent);
						}
						catch (ActivityNotFoundException ex)
						{
							//#debug
							base.tina.core.log.LogPrinter.d(Constants.TAG, "no activity for " + mimetype, ex);
							// nothing anyone can do about this, but we're in a clean state,
							//     swallow the exception entirely
						}
					}
					else
					{
						int packageColumn = cursor.getColumnIndexOrThrow(GlobalDownload.COLUMN_NOTIFICATION_PACKAGE);
						int classColumn = cursor.getColumnIndexOrThrow(GlobalDownload.COLUMN_NOTIFICATION_CLASS);
						String pckg = cursor.getString(packageColumn);
						String clazz = cursor.getString(classColumn);
						if (pckg != null && clazz != null)
						{
							Intent appIntent = new Intent(GlobalDownload.ACTION_NOTIFICATION_CLICKED);
							appIntent.setClassName(pckg, clazz);
							if (intent.getBooleanExtra("multiple", true))
							{
								appIntent.setData(GlobalDownload.CONTENT_URI);
							}
							else
							{
								appIntent.setData(intent.getData());
							}
							context.sendBroadcast(appIntent);
						}
					}
				}
				cursor.close();
			}
			NotificationManager notMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			if (notMgr != null)
			{
				notMgr.cancel((int) ContentUris.parseId(intent.getData()));
			}
		}
		else if (intent.getAction().equals(Constants.ACTION_HIDE))
		{
			//#debug verbose
			base.tina.core.log.LogPrinter.v(Constants.TAG, "Receiver hide for " + intent.getData());
			Cursor cursor = context.getContentResolver().query(intent.getData(), null, null, null, null);
			if (cursor != null)
			{
				if (cursor.moveToFirst())
				{
					int statusColumn = cursor.getColumnIndexOrThrow(GlobalDownload.COLUMN_STATUS);
					int status = cursor.getInt(statusColumn);
					int visibilityColumn = cursor.getColumnIndexOrThrow(GlobalDownload.COLUMN_VISIBILITY);
					int visibility = cursor.getInt(visibilityColumn);
					if (GlobalDownload.isStatusCompleted(status) && visibility == GlobalDownload.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
					{
						ContentValues values = new ContentValues();
						values.put(GlobalDownload.COLUMN_VISIBILITY, GlobalDownload.VISIBILITY_VISIBLE);
						context.getContentResolver().update(intent.getData(), values, null, null);
					}
				}
				cursor.close();
			}
		}
	}
}
