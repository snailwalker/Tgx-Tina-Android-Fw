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
package base.tina.external.android.db.api.provider;

import java.util.ArrayList;
import java.util.Collection;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public abstract class BaseProvider
				extends
				ContentProvider
{
	//#debug fatal
	public final static String		TAG			= "TGX_PROVIDER";

	private static final UriMatcher	sURIMatcher	= new UriMatcher(UriMatcher.NO_MATCH);

	BaseProviderConfig				config;

	/** The database that lies underneath this content provider */
	private SQLiteOpenHelper		mOpenHelper	= null;

	/**
	 * Creates and updated database on demand when opening it. Helper class to
	 * create database the first time the provider is initialized and upgrade it
	 * when a new version of the provider needs an updated version of the
	 * database.
	 */
	private final class DatabaseHelper
					extends
					SQLiteOpenHelper
	{

		public DatabaseHelper(final Context context, final String db_name, final int db_version)
		{
			super(context, db_name, null, db_version);
		}

		@Override
		public void onCreate(final SQLiteDatabase db) {
			//#debug verbose
			base.tina.core.log.LogPrinter.v(TAG, "populating new database");
			BaseProvider.this.db = db;
			Collection<BaseTable> tables = config.getTables();
			if (tables != null) for (BaseTable table : tables)
			{
				table.createTable(db, getContext());
				table.addIndex(db, getContext());
				table.addTrigger(db, getContext());
				table.addData(db, getContext());
			}
		}

		@Override
		public void onUpgrade(final SQLiteDatabase db, int oldV, final int newV) {
			//#debug verbose
			base.tina.core.log.LogPrinter.v(TAG, "upgrad database");
			Collection<BaseTable> tables = config.getTables();
			if (tables != null) for (BaseTable table : tables)
			{
				table.alterTable(db, getContext());
			}
		}
	}

	private SQLiteDatabase	db;

	public void dynamicTable(BaseTable table) {
		if (table != null) table.createTable(db, getContext());
	}

	/**
	 * Initializes the content provider when it is created.
	 */
	@Override
	public boolean onCreate() {
		Context context = getContext();
		// init config
		String AUTHORITY = getAuthority();
		config = new BaseProviderConfig(AUTHORITY, sURIMatcher);
		customsTable(config);
		// init databasehelper
		mOpenHelper = new DatabaseHelper(context, config.DB_NAME, config.DB_VERSION);
		return true;
	}

	/**
	 * Returns the content-provider-style MIME types of the various types
	 * accessible through this content provider.
	 */
	@Override
	public String getType(final Uri uri) {
		int code = sURIMatcher.match(uri);
		BaseTable table = config.getTable(code);
		if (table != null)
		{
			return table.getType(code - table.mPathIndex);
		}
		else
		{
			//#debug verbose
			base.tina.core.log.LogPrinter.v(TAG, "calling getType on an unknown URI: " + uri);
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int code = sURIMatcher.match(uri);
		BaseTable table = config.getTable(code);
		if (table != null)
		{
			return table.insert(db, getContext(), code - table.mPathIndex, uri, values);// 这个还没定，先不用。
		}
		else
		{
			//#debug
			base.tina.core.log.LogPrinter.d(TAG, "calling insert on an unknown/invalid URI: " + uri);
			throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
		}
	}

	@Override
	public Cursor query(final Uri uri, String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		int code = sURIMatcher.match(uri);
		BaseTable table = config.getTable(code);
		if (table != null)
		{
			return table.query(db, getContext(), code - table.mPathIndex, uri, projection, selection, selectionArgs, sortOrder);
		}
		else
		{
			//#debug
			base.tina.core.log.LogPrinter.d(TAG, "calling query on an unknown/invalid URI: " + uri);
			throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
		}
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		int code = sURIMatcher.match(uri);
		BaseTable table = config.getTable(code);
		if (table != null)
		{
			return table.update(db, getContext(), code - table.mPathIndex, uri, values, selection, selectionArgs);
		}
		else
		{
			//#debug
			base.tina.core.log.LogPrinter.d(TAG, "calling query on an unknown/invalid URI: " + uri);
			throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
		}
	}

	@Override
	public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		int code = sURIMatcher.match(uri);
		BaseTable table = config.getTable(code);
		if (table != null)
		{
			return table.delete(db, getContext(), code - table.mPathIndex, uri, selection, selectionArgs);
		}
		else
		{
			//#debug
			base.tina.core.log.LogPrinter.d(TAG, "calling query on an unknown/invalid URI: " + uri);
			throw new IllegalArgumentException("Unknown/Invalid URI " + uri);
		}
	}

	private volatile boolean	initialize;

	public boolean isInitialized() {
		return initialize;
	}

	protected abstract void customsTable(BaseProviderConfig config);

	protected abstract String getAuthority();

	@Override
	public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> operations) throws OperationApplicationException {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();

		try
		{
			db.beginTransaction();
			ContentProviderResult[] results = super.applyBatch(operations);
			db.setTransactionSuccessful();
			return results;
		}
		finally
		{
			db.endTransaction();
		}
	}

}
