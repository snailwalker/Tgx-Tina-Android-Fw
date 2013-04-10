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
 package base.tina.external.encrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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

	public String getMD5Str(byte[] in) {
		try
		{
			MessageDigest MD5Digest = MessageDigest.getInstance("MD5");
			byte[] strB = MD5Digest.digest(in);
			String x = "";
			for (byte b : strB)
			{
				String c = Integer.toHexString(b & 0xFF);
				if (c.length() < 2) x += '0';
				x += c;
			}
			return x;
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
		return null;
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

	private static int[]	crc_t;	// CRC table

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
