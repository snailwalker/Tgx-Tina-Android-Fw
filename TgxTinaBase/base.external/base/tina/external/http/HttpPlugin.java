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
 package base.tina.external.http;

import java.io.InputStream;

import base.tina.core.task.AbstractResult;

public abstract class HttpPlugin
		extends
		AbstractResult
{
	protected String	url;
	byte[]				requestData;
	InputStream			requestDataInputStream;
	long				dataLength;

	public HttpPlugin(String url)
	{
		super();
		//#debug
		base.tina.core.log.LogPrinter.d(null, "http url:" + url);
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setPostData(byte[] data) {
		this.requestData = data;
	}

	public void setPostData(InputStream requestDataInputStream, long dataLength) {
		this.requestDataInputStream = requestDataInputStream;
		this.dataLength = dataLength;
	}

	public boolean	authOK;

	/**
	 * 解析网络返回的数据
	 * 
	 * @param responseData
	 * @return handled
	 */
	public boolean parseData(byte[] responseData) throws Exception {
		return false;
	}

	@Override
	public void dispose() {
		url = null;
		requestData = null;
		requestDataInputStream = null;
		super.dispose();
	}

	protected final static int	SerialDomain	= -HttpTask.SerialDomain;
}
