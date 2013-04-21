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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;

public class DownloadManager
{

	private String									Tag				= "TGX_TINA_DOWNLOAD";
	private HashMap<String, DownloadItem>			mDownloadItems	= new HashMap<String, DownloadItem>();
	private HashMap<String, DownloadViewListener>	mViews			= new HashMap<String, DownloadViewListener>();
	private Context									mContext;
	private BroadcastReceiver						br;
	private String									where;
	Comparator<DownloadItem>						comparator;

	public DownloadManager(Context context,String authority)
	{
		mContext = context;
		comparator = new Comparator<DownloadItem>()
		{

			@Override
			public int compare(DownloadItem lhs, DownloadItem rhs) {
				return lhs.mId - rhs.mId;
			}
		};
		br = new BroadcastReceiver()
		{

			@Override
			public void onReceive(Context context, Intent intent) {
				if (intent.getAction().equals(Constants.ACTION_LIST_REFRESH))
				{
					//#debug verbose
					base.tina.core.log.LogPrinter.v(Tag, "Receiver ACTION_LIST_REFRESH");
					if (mViews.size() < 1)
					{
						if (!mDownloadItems.isEmpty()) mDownloadItems.clear();
						return;
					}
					Cursor c = context.getContentResolver().query(GlobalDownload.CONTENT_URI, new String[] {
									GlobalDownload._ID ,
									GlobalDownload.COLUMN_TITLE ,
									GlobalDownload.COLUMN_CURRENT_BYTES ,
									GlobalDownload.COLUMN_TOTAL_BYTES ,
									GlobalDownload.COLUMN_STATUS ,
									GlobalDownload._DATA ,
									GlobalDownload.COLUMN_LAST_MODIFICATION
					}, where, null, GlobalDownload._ID);

					if (c == null) { return; }

					final int idColumn = 0;
					final int titleColumn = 1;
					final int currentBytesColumn = 2;
					final int totalBytesColumn = 3;
					final int statusColumn = 4;
					final int filenameColumnId = 5;
					final int lastModColumnId = 6;

					for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
					{

						int id = c.getInt(idColumn);
						int max = c.getInt(totalBytesColumn);
						int progress = c.getInt(currentBytesColumn);
						int status = c.getInt(statusColumn);
						String filename = c.getString(filenameColumnId);
						long lastMod = c.getLong(lastModColumnId);

						String title = c.getString(titleColumn);
						if (title == null || title.length() == 0)
						{
							title = "<unknown>";
						}
						String key = String.valueOf(id);
						if (mDownloadItems.containsKey(key))
						{
							mDownloadItems.get(key).setTotalCurrent(progress).setTotalTotal(max).setlastMod(lastMod).setFilename(filename).setStatus(status);
						}
						else
						{
							DownloadItem item = new DownloadItem();
							item.mId = id;
							item.setTitle(title).setTotalCurrent(progress).setTotalTotal(max).setFilename(filename).setStatus(status);
							mDownloadItems.put(key, item);
						}
					}
					c.close();
					for (DownloadViewListener listener : mViews.values())
					{
						ArrayList<DownloadItem> data = new ArrayList<DownloadItem>(mDownloadItems.values());
						Collections.sort(data, comparator);
						listener.updataDownloadView(data);
					}
				}
			}

		};
		IntentFilter intf = new IntentFilter();
		intf.addAction(Constants.ACTION_LIST_REFRESH);
		context.registerReceiver(br, intf);
		GlobalDownload.setAutority(authority);
	}

	public void setWhereParam(ArrayList<String> params) {
		where = getWhere(params);
	}

	public void registerView(String className, DownloadViewListener listener) {
		if (!mDownloadItems.containsKey(className))
		{
			mViews.put(className, listener);
		}
	}

	public void unRegisterView(String className) {
		if (mDownloadItems.containsKey(className))
		{
			mViews.remove(className);
		}
	}

	private String getWhere(ArrayList<String> params) {
		if (params != null)
		{
			StringBuilder buffer = new StringBuilder();
			for (int index = 0; index < params.size(); index++)
			{
				String param = params.get(index);
				if (index > 0) buffer.append(" OR ");
				buffer.append(GlobalDownload._ID);
				buffer.append("=");
				buffer.append("'").append(param).append("'");
			}
			return buffer.toString();
		}
		return null;
	}

	public void deleteItemHistory(String title) {
		mContext.getContentResolver().delete(GlobalDownload.CONTENT_URI, GlobalDownload.COLUMN_TITLE + "=?", new String[] {
						title
		});
	}

	public ArrayList<DownloadItem> getDownloadList(ArrayList<String> params) {
		String where = getWhere(params);
		ArrayList<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
		Cursor c = mContext.getContentResolver().query(GlobalDownload.CONTENT_URI, new String[] {
						GlobalDownload._ID ,
						GlobalDownload.COLUMN_TITLE ,
						GlobalDownload.COLUMN_CURRENT_BYTES ,
						GlobalDownload.COLUMN_TOTAL_BYTES ,
						GlobalDownload.COLUMN_STATUS ,
						GlobalDownload._DATA ,
						GlobalDownload.COLUMN_LAST_MODIFICATION
		}, where, null, GlobalDownload._ID);

		if (c == null) { return null; }

		final int idColumn = 0;
		final int titleColumn = 1;
		final int currentBytesColumn = 2;
		final int totalBytesColumn = 3;
		final int statusColumn = 4;
		final int filenameColumnId = 5;
		final int lastModColumnId = 6;

		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext())
		{

			int id = c.getInt(idColumn);
			int max = c.getInt(totalBytesColumn);
			int progress = c.getInt(currentBytesColumn);
			int status = c.getInt(statusColumn);
			String filename = c.getString(filenameColumnId);
			long lastMod = c.getLong(lastModColumnId);

			String title = c.getString(titleColumn);
			if (title == null || title.length() == 0)
			{
				title = "<未命名>";
			}
			DownloadItem item = new DownloadItem();
			item.mId = id;
			item.setTitle(title).setTotalCurrent(progress).setTotalTotal(max).setFilename(filename).setStatus(status).setlastMod(lastMod);
			downloadItems.add(item);
		}
		c.close();
		Collections.sort(downloadItems, comparator);
		return downloadItems;
	}

	public int download(String url, String title) {
		ContentValues values = new ContentValues();
		values.put(GlobalDownload.COLUMN_URI, url);
		values.put(GlobalDownload.COLUMN_TITLE, title);
		Uri uri = mContext.getContentResolver().insert(GlobalDownload.CONTENT_URI, values);
		int id = (int) ContentUris.parseId(uri);
		return id;
	}

	public void destroy() {
		mContext.unregisterReceiver(br);
	}

	public class DownloadItem
	{
		int		mId;					// This first db _id for the download for the app
		int		mTotalCurrent	= 0;
		int		mTotalTotal		= 0;
		int		mStatus;
		long	mlastMod;
		String	mTitle;
		String	mFilename;

		public int getStatus() {
			return mStatus;
		}

		public DownloadItem setStatus(int mStatus) {
			this.mStatus = mStatus;
			return this;
		}

		public long getlastMod() {
			return mlastMod;
		}

		public DownloadItem setlastMod(long mlastMod) {
			this.mlastMod = mlastMod;
			return this;
		}

		public String getFilename() {
			return mFilename;
		}

		public DownloadItem setFilename(String mFilename) {
			this.mFilename = mFilename;
			return this;
		}

		public int getId() {
			return mId;
		}

		public DownloadItem setId(int mId) {
			this.mId = mId;
			return this;
		}

		public int getTotalCurrent() {
			return mTotalCurrent;
		}

		public DownloadItem setTotalCurrent(int mTotalCurrent) {
			this.mTotalCurrent = mTotalCurrent;
			return this;
		}

		public int getTotalTotal() {
			return mTotalTotal;
		}

		public DownloadItem setTotalTotal(int mTotalTotal) {
			this.mTotalTotal = mTotalTotal;
			return this;
		}

		public String getTitle() {
			return mTitle;
		}

		public DownloadItem setTitle(String mTitle) {
			this.mTitle = mTitle;
			return this;
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("[");
			buffer.append("<DownloadItem>");
			buffer.append(":mId=" + mId);
			buffer.append(",mTotalCurrent=" + mTotalCurrent);
			buffer.append(",mTotalTotal=" + mTotalTotal);
			buffer.append(",mStatus=" + mStatus);
			buffer.append(",mlastMod=" + mlastMod);
			buffer.append(",mTitle=" + mTitle);
			buffer.append(",mFilename=" + mFilename);
			buffer.append("]");
			return buffer.toString();
		}

	}
}
