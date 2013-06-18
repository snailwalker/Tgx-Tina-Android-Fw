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
package base.tina.external.encrypt;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;


public class TinaCrypt
{
	static
	{
		System.loadLibrary("tina_crypt");
	}
	
	public native byte[] pubKey(String passwd);
	
	/*
	 * 尚未单独实现 native byte[] eccEncrypt(byte[] publicKey, byte[] contents);
	 */
	/**
	 * @param passwd
	 *            解码密钥
	 * @param chiper
	 *            接收到的密文
	 * @return 解密后的结果
	 */
	public native byte[] eccDecrypt(String passwd, byte[] chiper);
	
	public native byte[] sha1(byte[] contents);
	
	public native byte[] sha256(byte[] contents);
	
	/**
	 * @param seed
	 *            生成时的随机seed
	 * @param publicKey
	 *            公钥
	 * @param chiper
	 *            将要传递到解密方的密文存储buf
	 * @return rc4对称密钥(明文)
	 */
	public native byte[] getRc4Key(String seed, byte[] publicKey, byte[] chiper);
	
	/**
	 * @return 密文buf的空间容量
	 */
	native int getVlsize();
	
	public byte[] getChiperBuf() {
		return new byte[getVlsize()];
	}
	
	private static final char HEX_DIGITS[] = {
	        '0',
	        '1',
	        '2',
	        '3',
	        '4',
	        '5',
	        '6',
	        '7',
	        '8',
	        '9',
	        'A',
	        'B',
	        'C',
	        'D',
	        'E',
	        'F'
	                                       };
	
	private static String toHexString(byte[] b) {
		StringBuilder sb = new StringBuilder(b.length * 2);
		for (int i = 0; i < b.length; i++)
		{
			sb.append(HEX_DIGITS[(b[i] & 0xf0) >>> 4]);
			sb.append(HEX_DIGITS[b[i] & 0x0f]);
		}
		return sb.toString();
	}
	
	public static String md5(File file) {
		InputStream fis;
		byte[] buffer = new byte[1024];
		int numRead = 0;
		MessageDigest md5;
		try
		{
			fis = new FileInputStream(file);
			md5 = MessageDigest.getInstance("MD5");
			while ((numRead = fis.read(buffer)) > 0)
			{
				md5.update(buffer, 0, numRead);
			}
			fis.close();
			return toHexString(md5.digest());
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static String md5(byte[] in) {
		if (in == null) throw new NullPointerException();
		MessageDigest md5;
		try
		{
			md5 = MessageDigest.getInstance("MD5");
			md5.update(in, 0, in.length);
			return toHexString(md5.digest());
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public final static int adler32(byte[] buf, int off, int len) {
		int s1 = 1 & 0x0000FFFF;
		int s2 = (1 >> 16) & 0x0000FFFF;
		len += off;
		for (int j = off; j < len; j++)
		{
			s1 += (buf[j] & 0x000000FF);
			s2 += s1;
		}
		s1 = s1 % 0xFFF1;
		s2 = s2 % 0xFFF1;
		return (int) ((s2 << 16) & 0xFFFF0000) | (int) (s1 & 0x0000FFFF);
	}
	
	private static int[] crc_t;	// CRC table
	                            
	private final static void mk() {
		int c, k;
		if (crc_t == null) crc_t = new int[256];
		for (int n = 0; n < 256; n++)
		{
			c = n;
			for (k = 0; k < 8; k++)
				c = (c & 1) == 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
			crc_t[n] = c;
		}
	}
	
	private final static int update(byte[] buf, int off, int len) {
		int c = 0xFFFFFFFF;
		int n;
		if (crc_t == null) mk();
		for (n = off; n < len + off; n++)
		{
			c = crc_t[(c ^ buf[n]) & 0xFF] ^ (c >>> 8);
		}
		return c;
	}
	
	public final static int checksum(byte[] buf, int off, int len) {
		return update(buf, off, len) ^ 0xFFFFFFFF;
	}
	
	public final static void releaseTable() {
		if (crc_t != null) crc_t = null;
	}
	
}
