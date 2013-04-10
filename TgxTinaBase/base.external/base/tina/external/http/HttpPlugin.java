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
