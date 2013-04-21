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

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.UriMatcher;
import android.util.SparseArray;

public class BaseProviderConfig
{

	//需要修改的
	public String						DB_NAME			= "Base.db";
	public int							DB_VERSION		= 1;

	//不用动的
	public final AtomicInteger			atomi_PathIndex	= new AtomicInteger(1);

	final LinkedList<BaseTable>			tables			= new LinkedList<BaseTable>();

	public final SparseArray<BaseTable>	path2TableMap	= new SparseArray<BaseTable>();

	final UriMatcher					URIMatcher;
	final String						authority;

	public BaseProviderConfig(String authority, UriMatcher URIMatcher)
	{
		this.authority = authority;
		this.URIMatcher = URIMatcher;
	}

	public void onNewTable(BaseTable table) {
		if (table != null)
		{
			tables.add(table);
			if (table.paths != null) for (String path : table.paths)
			{
				path2TableMap.append(atomi_PathIndex.get(), table);
				setPathCode(authority, URIMatcher, path, atomi_PathIndex.getAndIncrement());
			}
			else
			{
				//#debug 
				base.tina.core.log.LogPrinter.d(BaseProvider.TAG, "table is : " + table);
				throw new IllegalArgumentException("paths is NULL");
			}
		}
		else
		{
			//#debug 
			base.tina.core.log.LogPrinter.d(BaseProvider.TAG, "table is : " + table);
			throw new IllegalArgumentException("table is NUll");
		}
	}

	public int getCurPathIndex() {
		return atomi_PathIndex.get();
	}

	public void setPathCode(String authority, UriMatcher URIMatcher, String path, int code) {
		URIMatcher.addURI(authority, path, code);
	}

	public BaseTable getTable(Integer code) {
		return path2TableMap.get(code);
	}

	public Collection<BaseTable> getTables() {
		return tables;
	}
}
