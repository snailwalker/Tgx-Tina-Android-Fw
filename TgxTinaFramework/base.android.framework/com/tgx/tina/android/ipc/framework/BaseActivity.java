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
package com.tgx.tina.android.ipc.framework;

import java.util.LinkedList;

import android.app.Activity;
import android.os.Bundle;
import base.tina.core.task.android.ATaskService;

public abstract class BaseActivity
				extends
				Activity
{
	public ATaskService	ioAService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//#ifdef debug
		//#ifndef app_log 
		base.tina.external.android.log.AndroidPrinter.createByActivity(this);
		//#endif 
		//#endif 
	}

	@Override
	protected void onDestroy() {
		history.clear();
		if (ioAService != null) ioAService.stopAService();
		super.onDestroy();
		//#ifdef buf_activity
		//#else
		System.exit(0);
		//#endif
	}

	// ------------------------UI---------------------------------------
	protected final void onBackPressed0() {
		IUIPage<Activity> page = history.isEmpty() ? null : history.removeLast();
		if (page == null) super.onBackPressed();
		changePage(page, IUIPage.KEEP_LAST_STATUS);
	}

	public final IUIPage<Activity> changePage(IUIPage<Activity> nextPage, int initializers) {
		if (curPage == nextPage || nextPage == null) return curPage;
		if (curPage != null) curPage.setStatus(curPage.leave(nextPage));
		nextPage.createView(this, initializers);
		setContentView(nextPage.getView());
		nextPage.setStatus(nextPage.enter(curPage));
		if (initializers != IUIPage.KEEP_LAST_STATUS && curPage != null && curPage.isHistoryInclude()) history.add(curPage);
		curPage = nextPage;
		return nextPage;
	}

	public IUIPage<Activity>						curPage;
	protected final LinkedList<IUIPage<Activity>>	history	= new LinkedList<IUIPage<Activity>>();
}
