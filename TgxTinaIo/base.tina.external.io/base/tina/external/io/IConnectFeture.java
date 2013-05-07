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
package base.tina.external.io;

import base.tina.core.task.infc.ITaskListener;
import base.tina.core.task.timer.TimerTask;


public interface IConnectFeture<T extends TimerTask>
{
	public boolean isMySession(IoSession<?> ioSession);
	
	public void finishConnect(IoSession<?> ioSession);
	
	public void resetConnect(IoSession<?> ioSession);
	
	/**
	 * Connect to target address
	 * 
	 * @param ioFilter
	 * @param listener
	 * @return true:change state to connecting;false:network is not connected!
	 */
	public boolean connectTarAddr(IoFilter ioFilter, ITaskListener listener);
	
	/**
	 * invoke this function in the same thread with {@link connectTarAddr}
	 * 
	 * @return
	 */
	public boolean isConnectedOrConnecting();
	
	public boolean isConnected();
	
	public void connectByTimer(T timerTask, boolean networkChanged);
	
	public boolean isTiming(T timer);
	
	public String getTarAddr();
	
	public void onConnectFaild(Exception exception);
	
	public static enum State {
		CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED, RESET
	}
	
}
