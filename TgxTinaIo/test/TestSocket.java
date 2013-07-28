import java.net.InetSocketAddress;

import base.tina.external.io.IoUtil;


public class TestSocket
{
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String toUrl = "socket://www.baidu.com:80/";
		String[] split = IoUtil.splitURL(toUrl);
		InetSocketAddress address = new InetSocketAddress(split[IoUtil.HOST], Integer.parseInt(split[IoUtil.PORT]));
		byte[] rawAddr = address.getAddress().getAddress();
		for (byte b : rawAddr)
			System.out.println(b & 0xFF);
	}
	
}
