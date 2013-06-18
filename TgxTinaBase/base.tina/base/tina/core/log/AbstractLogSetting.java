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
package base.tina.core.log;

import base.tina.core.log.ILogPrinter.Level;
import base.tina.core.task.AbstractListener;


public abstract class AbstractLogSetting
        extends
        AbstractListener
{
	
	public static Level    logLevel = Level.VERBOSE;
	public static int      priority = Level.VERBOSE.ordinal();
	/**
	 * 是否进行逐行输出到服务器的操作
	 */
	private static boolean RemotePrint;
	
	public final static void enableRPrint() {
		RemotePrint = true;
	}
	
	public final static boolean isRemotePrint() {
		return RemotePrint;
	}
}
