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

import java.util.HashMap;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

/**
 * This class handles the updating of the Notification Manager for the cases
 * where there is an ongoing download. Once the download is complete (be it
 * successful or unsuccessful) it is no longer the responsibility of this
 * component to show the download in the notification manager.
 * 
 */
class DownloadNotification
{

	Context								mContext;
	public NotificationManager			mNotificationMgr;
	HashMap<String, NotificationItem>	mNotifications;

	static final String					LOGTAG			= "DownloadNotification";
	static final String					WHERE_RUNNING	=
															"(" + GlobalDownload.COLUMN_STATUS + " >= '100') AND (" +
																			GlobalDownload.COLUMN_STATUS + " <= '199') AND (" +
																			GlobalDownload.COLUMN_VISIBILITY + " IS NULL OR " +
																			GlobalDownload.COLUMN_VISIBILITY + " == '" + GlobalDownload.VISIBILITY_VISIBLE + "' OR " +
																			GlobalDownload.COLUMN_VISIBILITY +
																			" == '" + GlobalDownload.VISIBILITY_VISIBLE_NOTIFY_COMPLETED + "')";
	static final String					WHERE_COMPLETED	=
															GlobalDownload.COLUMN_STATUS + " >= '200' AND " +
																			GlobalDownload.COLUMN_VISIBILITY + " == '" + GlobalDownload.VISIBILITY_VISIBLE_NOTIFY_COMPLETED + "'";

	/**
	 * This inner class is used to collate downloads that are owned by the same
	 * application. This is so that only one notification line item is used for
	 * all downloads of a given application.
	 * 
	 */
	static class NotificationItem
	{
		int			mId;								// This first db _id for the download for the app
		int			mTotalCurrent	= 0;
		int			mTotalTotal		= 0;
		int			mTitleCount		= 0;
		String		mPackageName;						// App package name
		String		mDescription;
		String[]	mTitles			= new String[2];	// download titles.

		/*
		 * Add a second download to this notification item.
		 */
		void addItem(String title, int currentBytes, int totalBytes) {
			mTotalCurrent += currentBytes;
			if (totalBytes <= 0 || mTotalTotal == -1)
			{
				mTotalTotal = -1;
			}
			else
			{
				mTotalTotal += totalBytes;
			}
			if (mTitleCount < 2)
			{
				mTitles[mTitleCount] = title;
			}
			mTitleCount++;
		}
	}

	/**
	 * Constructor
	 * 
	 * @param ctx
	 *            The context to use to obtain access to the Notification
	 *            Service
	 */
	DownloadNotification(Context ctx)
	{
		mContext = ctx;
		mNotificationMgr = (NotificationManager) mContext
						.getSystemService(Context.NOTIFICATION_SERVICE);
		mNotifications = new HashMap<String, NotificationItem>();
	}

	/*
	 * Update the notification ui.
	 */
	public void updateNotification() {
		//#debug 
		base.tina.core.log.LogPrinter.d(Constants.TAG, "updateNotification");
		Intent intent = new Intent(Constants.ACTION_LIST_REFRESH);
		mContext.sendBroadcast(intent);
		//        updateActiveNotification();
		//        updateCompletedNotification();
	}

	//    private void updateActiveNotification() {
	//        // Active downloads
	//        Cursor c = mContext.getContentResolver().query(
	//                Downloads.CONTENT_URI, new String [] {
	//                        Downloads._ID,
	//                        Downloads.COLUMN_TITLE,
	//                        Downloads.COLUMN_DESCRIPTION,
	//                        Downloads.COLUMN_NOTIFICATION_PACKAGE,
	//                        Downloads.COLUMN_NOTIFICATION_CLASS,
	//                        Downloads.COLUMN_CURRENT_BYTES,
	//                        Downloads.COLUMN_TOTAL_BYTES,
	//                        Downloads.COLUMN_STATUS, Downloads._DATA
	//                },
	//                WHERE_RUNNING, null, Downloads._ID);
	//        
	//        if (c == null) {
	//            return;
	//        }
	//        
	//        // Columns match projection in query above
	//        final int idColumn = 0;
	//        final int titleColumn = 1;
	//        final int descColumn = 2;
	//        final int ownerColumn = 3;
	//        final int classOwnerColumn = 4;
	//        final int currentBytesColumn = 5;
	//        final int totalBytesColumn = 6;
	//        final int statusColumn = 7;
	//        final int filenameColumnId = 8;
	//
	//        // Collate the notifications
	//        mNotifications.clear();
	//        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
	//            String packageName = c.getString(ownerColumn);
	//            int max = c.getInt(totalBytesColumn);
	//            int progress = c.getInt(currentBytesColumn);
	//            String title = c.getString(titleColumn);
	//            if (title == null || title.length() == 0) {
	//                title = mContext.getResources().getString(
	//                        R.string.download_unknown_title);
	//            }
	//            if (mNotifications.containsKey(packageName)) {
	//                mNotifications.get(packageName).addItem(title, progress, max);
	//            } else {
	//                NotificationItem item = new NotificationItem();
	//                item.mId = c.getInt(idColumn);
	//                item.mPackageName = packageName;
	//                item.mDescription = c.getString(descColumn);
	//                String className = c.getString(classOwnerColumn);
	//                item.addItem(title, progress, max);
	//                mNotifications.put(packageName, item);
	//            }
	//            
	//        }
	//        c.close();
	//        
	//        // Add the notifications
	//        for (NotificationItem item : mNotifications.values()) {
	//            // Build the notification object
	//            Notification n = new Notification();
	//            n.icon = android.R.drawable.stat_sys_download;
	//
	//            n.flags |= Notification.FLAG_ONGOING_EVENT;
	//            
	//            StringBuilder title = new StringBuilder(item.mTitles[0]);
	//            if (item.mTitleCount > 1) {
	//                title.append(mContext.getString(R.string.notification_filename_separator));
	//                title.append(item.mTitles[1]);
	//                n.number = item.mTitleCount;
	//                if (item.mTitleCount > 2) {
	//                    title.append(mContext.getString(R.string.notification_filename_extras,
	//                            new Object[] { Integer.valueOf(item.mTitleCount - 2) }));
	//                }
	//            } else {
	//            }
	//        
	//            String caption = "已下载0%";
	//            caption = "已下载"+(item.mTotalCurrent/item.mTotalTotal)*100;
	//            Intent intent = new Intent(Constants.ACTION_LIST);
	//            intent.setClassName(mContext.getPackageName(),
	//                    DownloadReceiver.class.getName());
	//            intent.setData(Uri.parse(Downloads.CONTENT_URI + "/" + item.mId));
	//            intent.putExtra("multiple", item.mTitleCount > 1);
	//
	//            n.contentIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
	//            n.setLatestEventInfo(mContext, title, caption,
	//                    PendingIntent.getBroadcast(mContext, 0, intent, 0));
	//            mNotificationMgr.notify(item.mId, n);
	//            
	//        }
	//    }

	//    private void updateCompletedNotification() {
	//        // Completed downloads
	//        Cursor c = mContext.getContentResolver().query(
	//                Downloads.CONTENT_URI, new String [] {
	//                        Downloads._ID,
	//                        Downloads.COLUMN_TITLE,
	//                        Downloads.COLUMN_DESCRIPTION,
	//                        Downloads.COLUMN_NOTIFICATION_PACKAGE,
	//                        Downloads.COLUMN_NOTIFICATION_CLASS,
	//                        Downloads.COLUMN_CURRENT_BYTES,
	//                        Downloads.COLUMN_TOTAL_BYTES,
	//                        Downloads.COLUMN_STATUS,
	//                        Downloads._DATA,
	//                        Downloads.COLUMN_LAST_MODIFICATION,
	//                        Downloads.COLUMN_DESTINATION
	//                },
	//                WHERE_COMPLETED, null, Downloads._ID);
	//        
	//        if (c == null) {
	//            return;
	//        }
	//        
	//        // Columns match projection in query above
	//        final int idColumn = 0;
	//        final int titleColumn = 1;
	//        final int descColumn = 2;
	//        final int ownerColumn = 3;
	//        final int classOwnerColumn = 4;
	//        final int currentBytesColumn = 5;
	//        final int totalBytesColumn = 6;
	//        final int statusColumn = 7;
	//        final int filenameColumnId = 8;
	//        final int lastModColumnId = 9;
	//        final int destinationColumnId = 10;
	//
	//        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
	//            // Add the notifications
	//            Notification n = new Notification();
	//            n.icon = android.R.drawable.stat_sys_download_done;
	//
	//            String title = c.getString(titleColumn);
	//            if (title == null || title.length() == 0) {
	//                title = mContext.getResources().getString(
	//                        R.string.download_unknown_title);
	//            }
	//            Uri contentUri = Uri.parse(Downloads.CONTENT_URI + "/" + c.getInt(idColumn));
	//            String caption;
	//            Intent intent;
	//            if (Downloads.isStatusError(c.getInt(statusColumn))) {
	//                caption = mContext.getResources()
	//                        .getString(R.string.notification_download_failed);
	//                intent = new Intent(Constants.ACTION_LIST);
	//            } else {
	//                caption = mContext.getResources()
	//                        .getString(R.string.notification_download_complete);
	//                if (c.getInt(destinationColumnId) == Downloads.DESTINATION_EXTERNAL) {
	//                    intent = new Intent(Constants.ACTION_OPEN);
	//                } else {
	//                    intent = new Intent(Constants.ACTION_LIST);
	//                }
	//            }
	//            intent.setClassName("com.android.providers.downloads2",
	//                    DownloadReceiver.class.getName());
	//            intent.setData(contentUri);
	//            n.setLatestEventInfo(mContext, title, caption,
	//                    PendingIntent.getBroadcast(mContext, 0, intent, 0));
	//
	//            intent = new Intent(Constants.ACTION_HIDE);
	//            intent.setClassName(mContext.getPackageName(),
	//                    DownloadReceiver.class.getName());
	//            intent.setData(contentUri);
	//            n.deleteIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);
	//
	//            n.when = c.getLong(lastModColumnId);
	//
	//            mNotificationMgr.notify(c.getInt(idColumn), n);
	//        }
	//        c.close();
	//    }

	/*
	 * Helper function to build the downloading text.
	 */
	//    private String getDownloadingText(long totalBytes, long currentBytes) {
	//        if (totalBytes <= 0) {
	//            return "";
	//        }
	//        long progress = currentBytes * 100 / totalBytes;
	//        StringBuilder sb = new StringBuilder();
	//        sb.append(progress);
	//        sb.append('%');
	//        return sb.toString();
	//    }

}
