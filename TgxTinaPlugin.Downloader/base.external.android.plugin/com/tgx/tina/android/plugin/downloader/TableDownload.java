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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Binder;
import android.os.Process;
import base.tina.external.android.db.api.provider.BaseProviderConfig;
import base.tina.external.android.db.api.provider.BaseTable;
import base.tina.external.android.db.api.provider.Utils;

public class TableDownload
				extends
				BaseTable
{

	public TableDownload(BaseProviderConfig config)
	{
		super(config);
		table_name = "downloads";
		paths = new String[] {
						"download" ,
						"download/#"
		};
		//set readableColumns
		mReadableColumnsSet.add(GlobalDownload._ID);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_APP_DATA);
		mReadableColumnsSet.add(GlobalDownload._DATA);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_MIME_TYPE);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_VISIBILITY);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_DESTINATION);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_CONTROL);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_STATUS);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_LAST_MODIFICATION);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_NOTIFICATION_PACKAGE);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_NOTIFICATION_CLASS);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_TOTAL_BYTES);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_CURRENT_BYTES);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_TITLE);
		mReadableColumnsSet.add(GlobalDownload.COLUMN_DESCRIPTION);
	}

	@Override
	public boolean createTable(SQLiteDatabase db, Context context) {
		try
		{
			//formatter:off
			db.execSQL("CREATE TABLE " + table_name + 
			           "(" + 
					   GlobalDownload._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + 
			           GlobalDownload.COLUMN_URI + " TEXT, " + 
					   Constants.RETRY_AFTER_X_REDIRECT_COUNT + " INTEGER, " + 
			           GlobalDownload.COLUMN_APP_DATA + " TEXT, " + 
					   GlobalDownload.COLUMN_NO_INTEGRITY + " BOOLEAN, " + 
			           GlobalDownload.COLUMN_FILE_NAME_HINT + " TEXT, " + 
					   Constants.OTA_UPDATE + " BOOLEAN, " + 
			           GlobalDownload._DATA + " TEXT, " + 
					   GlobalDownload.COLUMN_MIME_TYPE + " TEXT, " +
			           GlobalDownload.COLUMN_DESTINATION + " INTEGER, " +
					   Constants.NO_SYSTEM_FILES + " BOOLEAN, " +
			           GlobalDownload.COLUMN_VISIBILITY + " INTEGER, " +
					   GlobalDownload.COLUMN_CONTROL + " INTEGER, " + 
			           GlobalDownload.COLUMN_STATUS + " INTEGER, " +
					   Constants.FAILED_CONNECTIONS + " INTEGER, " +
			           GlobalDownload.COLUMN_LAST_MODIFICATION + " BIGINT, " + 
					   GlobalDownload.COLUMN_NOTIFICATION_PACKAGE + " TEXT, " +
			           GlobalDownload.COLUMN_NOTIFICATION_CLASS + " TEXT, " +
					   GlobalDownload.COLUMN_NOTIFICATION_EXTRAS + " TEXT, " + 
			           GlobalDownload.COLUMN_COOKIE_DATA + " TEXT, " +
					   GlobalDownload.COLUMN_USER_AGENT + " TEXT, " + 
			           GlobalDownload.COLUMN_REFERER + " TEXT, " + 
					   GlobalDownload.COLUMN_TOTAL_BYTES + " INTEGER, " + 
			           GlobalDownload.COLUMN_CURRENT_BYTES + " INTEGER, " + 
					   Constants.ETAG + " TEXT, " + 
			           Constants.UID + " INTEGER, " + 
					   GlobalDownload.COLUMN_OTHER_UID + " INTEGER, " +
			           GlobalDownload.COLUMN_TITLE + " TEXT, " + 
					   GlobalDownload.COLUMN_DESCRIPTION + " TEXT, " +
			           Constants.MEDIA_SCANNED + " BOOLEAN);"
			           );
			//formatter:on
		}
		catch (SQLException ex)
		{
			base.tina.core.log.LogPrinter.e(Constants.TAG, "couldn't create table in downloads database");
			throw ex;
		}
		return true;
	}

	@Override
	public boolean dropTable(SQLiteDatabase db, Context context) {
		try
		{
			db.execSQL("DROP TABLE IF EXISTS " + table_name);
		}
		catch (SQLException ex)
		{
			base.tina.core.log.LogPrinter.e(Constants.TAG, "couldn't drop table in downloads database");
			throw ex;
		}
		return true;
	}

	@Override
	public int delete(SQLiteDatabase db, Context context, int pathIndex, Uri uri, String selection, String[] selectionArgs) {
		Utils.validateSelection(selection, mReadableColumnsSet);
		int count = 0;
		switch (pathIndex) {
			case 0:
			case 1: {
				String myWhere;
				if (selection != null)
				{
					if (pathIndex == 0)
					{
						myWhere = "( " + selection + " )";
					}
					else
					{
						myWhere = "( " + selection + " ) AND ";
					}
				}
				else
				{
					myWhere = "";
				}
				if (pathIndex == 1)
				{
					String segment = uri.getPathSegments().get(1);
					long rowId = Long.parseLong(segment);
					myWhere += " ( " + GlobalDownload._ID + " = " + rowId + " ) ";
				}
				count = db.delete(table_name, myWhere, selectionArgs);
				break;
			}
		}
		context.getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(SQLiteDatabase db, Context context, int pathIndex, Uri uri, ContentValues values) {
		ContentValues filteredValues = new ContentValues();

		Utils.copyString(GlobalDownload.COLUMN_URI, values, filteredValues);
		Utils.copyString(GlobalDownload.COLUMN_APP_DATA, values, filteredValues);
		Utils.copyBoolean(GlobalDownload.COLUMN_NO_INTEGRITY, values, filteredValues);
		Utils.copyString(GlobalDownload.COLUMN_FILE_NAME_HINT, values, filteredValues);
		Utils.copyString(GlobalDownload.COLUMN_MIME_TYPE, values, filteredValues);
		Integer dest = values.getAsInteger(GlobalDownload.COLUMN_DESTINATION);
		if (dest != null)
		{
			filteredValues.put(GlobalDownload.COLUMN_DESTINATION, dest);
		}
		else
		{
			dest = GlobalDownload.DESTINATION_EXTERNAL;
			filteredValues.put(GlobalDownload.COLUMN_DESTINATION, dest);
		}
		Integer vis = values.getAsInteger(GlobalDownload.COLUMN_VISIBILITY);
		if (vis == null)
		{
			if (dest == GlobalDownload.DESTINATION_EXTERNAL)
			{
				filteredValues.put(GlobalDownload.COLUMN_VISIBILITY, GlobalDownload.VISIBILITY_HIDDEN);
			}
			else
			{
				filteredValues.put(GlobalDownload.COLUMN_VISIBILITY, GlobalDownload.VISIBILITY_HIDDEN);
			}
		}
		else
		{
			filteredValues.put(GlobalDownload.COLUMN_VISIBILITY, vis);
		}
		Utils.copyInteger(GlobalDownload.COLUMN_CONTROL, values, filteredValues);
		filteredValues.put(GlobalDownload.COLUMN_STATUS, GlobalDownload.STATUS_PENDING);
		filteredValues.put(GlobalDownload.COLUMN_LAST_MODIFICATION, System.currentTimeMillis());
		String pckg = values.getAsString(GlobalDownload.COLUMN_NOTIFICATION_PACKAGE);
		String clazz = values.getAsString(GlobalDownload.COLUMN_NOTIFICATION_CLASS);
		if (pckg != null && clazz != null)
		{
			int uid = Binder.getCallingUid();
			try
			{
				if (uid == 0 || context.getPackageManager().getApplicationInfo(pckg, 0).uid == uid)
				{
					filteredValues.put(GlobalDownload.COLUMN_NOTIFICATION_PACKAGE, pckg);
					filteredValues.put(GlobalDownload.COLUMN_NOTIFICATION_CLASS, clazz);
				}
			}
			catch (PackageManager.NameNotFoundException ex)
			{
				/* ignored for now */
			}
		}
		Utils.copyString(GlobalDownload.COLUMN_NOTIFICATION_EXTRAS, values, filteredValues);
		Utils.copyString(GlobalDownload.COLUMN_COOKIE_DATA, values, filteredValues);
		Utils.copyString(GlobalDownload.COLUMN_USER_AGENT, values, filteredValues);
		Utils.copyString(GlobalDownload.COLUMN_REFERER, values, filteredValues);
		filteredValues.put(Constants.UID, Binder.getCallingUid());
		if (Binder.getCallingUid() == 0)
		{
			Utils.copyInteger(Constants.UID, values, filteredValues);
		}
		Utils.copyString(GlobalDownload.COLUMN_TITLE, values, filteredValues);
		Utils.copyString(GlobalDownload.COLUMN_DESCRIPTION, values, filteredValues);

		//#ifdef debug
		//#if debug<1
		base.tina.core.log.LogPrinter.v(Constants.TAG, "initiating download with UID " + filteredValues.getAsInteger(Constants.UID));
		if (filteredValues.containsKey(GlobalDownload.COLUMN_OTHER_UID))
		{
			base.tina.core.log.LogPrinter.v(Constants.TAG, "other UID " + filteredValues.getAsInteger(GlobalDownload.COLUMN_OTHER_UID));
		}
		//#endif
		//#endif

		context.startService(new Intent(context, DownloadService.class));

		long rowID = db.insert(table_name, null, filteredValues);

		Uri ret = null;

		if (rowID != -1)
		{
			context.startService(new Intent(context, DownloadService.class));
			ret = Uri.parse(GlobalDownload.CONTENT_URI + "/" + rowID);
			context.getContentResolver().notifyChange(uri, null);
		}
		else
		{
			//#debug
			base.tina.core.log.LogPrinter.d(Constants.TAG, "couldn't insert into downloads database");
		}

		return ret;
	}

	@Override
	public Cursor query(SQLiteDatabase db, Context context, int pathIndex, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		Utils.validateSelection(selection, mReadableColumnsSet);

		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (pathIndex) {
			case 0: {
				qb.setTables(table_name);
				break;
			}
			case 1: {
				qb.setTables(table_name);
				qb.appendWhere(GlobalDownload._ID + "=");
				qb.appendWhere(uri.getPathSegments().get(1));
				break;
			}
		}

		Cursor ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		if (ret != null)
		{
			ret = new Utils.ReadOnlyCursorWrapper(ret);
		}

		if (ret != null)
		{
			ret.setNotificationUri(context.getContentResolver(), uri);
			//#debug verbose
			base.tina.core.log.LogPrinter.v(Constants.TAG, "created cursor " + ret + " on behalf of " + Binder.getCallingPid());
		}
		else
		{
			//#debug verbose
			base.tina.core.log.LogPrinter.v(Constants.TAG, "query failed in database");
		}
		return ret;
	}

	@Override
	public int update(SQLiteDatabase db, Context context, int pathIndex, Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		Utils.validateSelection(selection, mReadableColumnsSet);

		int count;
		long rowId = 0;
		boolean startService = false;

		ContentValues filteredValues;
		if (Binder.getCallingPid() != Process.myPid())
		{
			filteredValues = new ContentValues();
			Utils.copyString(GlobalDownload.COLUMN_APP_DATA, values, filteredValues);
			Utils.copyInteger(GlobalDownload.COLUMN_VISIBILITY, values, filteredValues);
			Integer i = values.getAsInteger(GlobalDownload.COLUMN_CONTROL);
			if (i != null)
			{
				filteredValues.put(GlobalDownload.COLUMN_CONTROL, i);
				startService = true;
			}
			Utils.copyInteger(GlobalDownload.COLUMN_CONTROL, values, filteredValues);
			Utils.copyString(GlobalDownload.COLUMN_TITLE, values, filteredValues);
			Utils.copyString(GlobalDownload.COLUMN_DESCRIPTION, values, filteredValues);
		}
		else
		{
			filteredValues = values;
		}
		switch (pathIndex) {
			case 0:
			case 1: {
				String myWhere;
				if (selection != null)
				{
					if (pathIndex == 0)
					{
						myWhere = "( " + selection + " )";
					}
					else
					{
						myWhere = "( " + selection + " ) AND ";
					}
				}
				else
				{
					myWhere = "";
				}
				if (pathIndex == 1)
				{
					String segment = uri.getPathSegments().get(1);
					rowId = Long.parseLong(segment);
					myWhere += " ( " + GlobalDownload._ID + " = " + rowId + " ) ";
				}
				if (filteredValues.size() > 0)
				{
					count = db.update(table_name, filteredValues, myWhere, selectionArgs);
				}
				else
				{
					count = 0;
				}
				break;
			}
			default: {
				//#debug
				base.tina.core.log.LogPrinter.d(Constants.TAG, "updating unknown/invalid URI: " + uri);
				throw new UnsupportedOperationException("Cannot update URI: " + uri);
			}
		}
		context.getContentResolver().notifyChange(uri, null);
		if (startService)
		{
			context.startService(new Intent(context, DownloadService.class));
		}
		return count;
	}

	@Override
	public String getType(int pathIndex) {
		switch (pathIndex) {
			case 0:
				return "vnd.android.cursor.dir/download";
			case 1:
				return "vnd.android.cursor.item/download";
			default:
				return null;
		}
	}

	@Override
	public boolean addIndex(SQLiteDatabase db, Context context) {
		return true;
	}

	@Override
	public boolean addTrigger(SQLiteDatabase db, Context context) {
		return true;
	}

	@Override
	public boolean addData(SQLiteDatabase db, Context context) {
		return true;
	}

}
