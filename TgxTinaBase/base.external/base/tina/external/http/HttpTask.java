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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import base.tina.core.task.Task;


public class HttpTask
        extends
        Task
{
	
	public HttpTask(HttpPlugin httpPlugin) {
		super(0);
		this.httpPlugin = httpPlugin;
	}
	
	public HttpPlugin      httpPlugin;
	public HttpRequestBase request;
	public HttpClient      httpClient;
	
	@Override
	public final void run() throws Exception {
		if (httpPlugin == null || httpPlugin.url == null) return;
		//#debug 
		base.tina.core.log.LogPrinter.d(null, "-----------" + httpPlugin.url + "-----------");
		httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 60000);
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 60000);
		HttpResponse response;
		
		if (httpPlugin.requestData == null && httpPlugin.requestDataInputStream == null)
		{
			HttpGet getRequest = new HttpGet(URI.create(httpPlugin.url));
			request = getRequest;
			response = httpClient.execute(getRequest);
		}
		else
		{
			HttpPost requestPost = new HttpPost(URI.create(httpPlugin.url));
			
			request = requestPost;
			
			// request.setHeader("Connention", "close");
			
			if (httpPlugin.requestDataInputStream != null)
			{
				InputStream instream = httpPlugin.requestDataInputStream;
				InputStreamEntity inputStreamEntity = new InputStreamEntity(instream, httpPlugin.dataLength);
				requestPost.setEntity(inputStreamEntity);
			}
			else
			{
				InputStream instream = new ByteArrayInputStream(httpPlugin.requestData);
				InputStreamEntity inputStreamEntity = new InputStreamEntity(instream, httpPlugin.requestData.length);
				requestPost.setEntity(inputStreamEntity);
			}
			response = httpClient.execute(requestPost);
		}
		if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
		{
			httpPlugin.parseData(EntityUtils.toByteArray(response.getEntity()));
			commitResult(httpPlugin, CommitAction.WAKE_UP);
		}
		else
		{
			//#debug error
			base.tina.core.log.LogPrinter.e(null,"Http error : " + new String(EntityUtils.toByteArray(response.getEntity())));
			throw new Exception("Http response code is : " + response.getStatusLine().getStatusCode());
		}
	}
	
	@Override
	public void dispose() {
		if (request != null) request.abort();
		if (httpClient != null) httpClient.getConnectionManager().shutdown();
		httpClient = null;
		httpPlugin = null;
		super.dispose();
	}
	
	@Override
	public void initTask() {
		isBloker = true;
		super.initTask();
	}
	
	@Override
	public void interrupt() {
		if (request != null) request.abort();
	}
	
	protected final static int SerialDomain = -0x3000;
	public final static int    SerialNum    = SerialDomain + 1;
	
	@Override
	public final int getSerialNum() {
		return SerialNum;
	}
}
