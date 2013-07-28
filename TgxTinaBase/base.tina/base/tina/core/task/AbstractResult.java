/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package base.tina.core.task;

import java.util.HashMap;
import java.util.Map;

import base.tina.core.task.infc.ITaskResult;


public abstract class AbstractResult
        implements
        ITaskResult
{
	private volatile boolean   isInResponseQueue;
	private int                bindSerial;
	private volatile Exception exception;
	protected volatile boolean disposable = true;
	
	/**
	 * 任何获得isDisposable()接口许可 或者 在ResponseQueue中未经处理的Result都将默认执行此操作
	 */
	@Override
	public void dispose() {
		exception = null;
		if (attributes != null) attributes.clear();
		attributes = null;
		
	}
	
	@Override
	public final void lockResponsed() {
		isInResponseQueue = true;
	}
	
	@Override
	public final void unlockResponsed() {
		isInResponseQueue = false;
	}
	
	@Override
	public final boolean isResponsed() {
		return isInResponseQueue;
	}
	
	@Override
	public final void setListenSerial(int bindSerial) {
		this.bindSerial = bindSerial;
	}
	
	@Override
	public final int getListenSerial() {
		return bindSerial;
	}
	
	@Override
	public boolean isDisposable() {
		return disposable;
	}
	
	@Override
	public final boolean hasError() {
		return exception != null;
	}
	
	@Override
	public final void setError(Exception ex) {
		exception = ex;
	}
	
	@Override
	public final Exception getError() {
		return exception;
	}
	
	private Map<String, Object> attributes;
	
	@Override
	public final void setAttributes(Map<String, Object> map) {
		attributes = map;
	}
	
	@Override
	public final Object getAttribute(String key) {
		if (attributes == null || attributes.isEmpty()) return null;
		return attributes.get(key);
	}
	
	@Override
	public final void setAttribute(String key, Object value) {
		if (attributes == null) attributes = new HashMap<String, Object>(2, 0.5f);
		attributes.put(key, value);
	}
	
	@Override
	public boolean canOtherHandle() {
		return true;
	}
}
