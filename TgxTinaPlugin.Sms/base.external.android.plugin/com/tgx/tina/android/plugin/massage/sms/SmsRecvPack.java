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
package com.tgx.tina.android.plugin.massage.sms;

import base.tina.core.task.AbstractResult;

public class SmsRecvPack
				extends
				AbstractResult
{
	public String			address;
	public String			content;
	public int				msgOriginId;
	public int				msgId;
	public long				timeStamp;

	public final static int	SerialNum	= SmsMsgPack.SerialNum + 1;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public void dispose() {
		address = null;
		content = null;
		super.dispose();
	}
}
