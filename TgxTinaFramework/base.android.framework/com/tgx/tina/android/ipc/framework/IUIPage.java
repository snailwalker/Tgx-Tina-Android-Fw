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

import android.os.Bundle;
import android.view.View;
import base.tina.core.task.infc.IDisposable;
import base.tina.core.task.infc.ITaskResult;

public interface IUIPage<Activity>
		extends
		IDisposable
{
	/**
	 * @param <T>
	 * @param context
	 * @param initializers
	 *            初始状态
	 * @return View 创建好的View
	 */
	public <T extends Activity> View createView(T t, int initializers);

	/**
	 * @param toStatus
	 *            目标状态
	 * @return View 更新后View
	 */
	public View updateView(int toStatus, ITaskResult data);

	/**
	 * @param cmd
	 *            投递的命令
	 * @param bundle
	 *            命令携带的数据
	 */
	public void notifyView(int cmd, Bundle bundle);

	/**
	 * @return View 当前的View
	 */
	public View getView();

	/**
	 * 进入Page之前要调用
	 * 
	 * @param prePage
	 *            前一个Page
	 * @return int 更新后的状态值
	 */
	public int enter(IUIPage<?> prePage);

	/**
	 * 离开Page之前调用
	 * 
	 * @param nextPage
	 *            下一个Page
	 * @return int 更新后的状态值
	 */
	public int leave(IUIPage<?> nextPage);

	/**
	 * 处理backpress
	 * 
	 * @return
	 */
	public boolean handleBack();

	public void setStatus(int status);

	/**
	 * 当前页是否需要进入back键的历史栈中
	 * 
	 * @return
	 */
	public boolean isHistoryInclude();

	public final static int	KEEP_LAST_STATUS	= -1;
	public final static int	DEFAULT_STATUS		= 0;
	public final static int	UPDATE_STATUS		= 1;
	public final static int	BINDEND_STATUS		= 2;
	public final static int	BINDOK_STATUS		= 3;

}
