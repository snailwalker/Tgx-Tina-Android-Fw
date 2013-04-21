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

import java.util.Random;


public class Rc4
{
	
	public byte[] getKey(String seed) {
		long curTime = System.currentTimeMillis();
		long code = hashCode();
		long tick = curTime ^ (code << 31);
		Random rd = new Random(tick);
		byte[] xc;
		if (seed == null || "".equals(seed.trim())) seed = "Tgx.Tina.Rc4";
		xc = seed.getBytes();
		byte[] key = new byte[20];
		for (int i = 0, j = 1; i < key.length; i++)
		{
			for (byte b : xc)
			{
				long dx = System.currentTimeMillis() ^ tick ^ rd.nextLong() ^ b;
				key[i] ^= dx >> j++;
				if (j > 40) j = 1;
			}
		}
		return key;
	}
	
	public static byte[] decrypt(byte[] data, byte[] key) {
		return rc4(data, key);
	}
	
	public static byte[] encrypt(byte[] data, byte[] key) {
		return rc4(data, key);
	}
	
	private static byte[] rc4(byte[] data, byte[] key) {
		if (!isKeyValid(key)) throw new IllegalArgumentException("key is fail!");
		if (data.length < 1 || data.length > 256) throw new IllegalArgumentException("data is fail!");
		int[] S = new int[256];
		
		// KSA
		for (int i = 0; i < S.length; i++)
			S[i] = i;
		int j = 0;
		for (int i = 0; i < S.length; i++)
		{
			j = (j + S[i] + (key[i % key.length] & 0xFF)) % 256;
			swap(S, i, j);
		}
		
		// PRGA
		int i = 0;
		j = 0;
		
		byte[] encodeData = new byte[data.length];
		
		for (int x = 0; x < encodeData.length; x++)
		{
			i = (i + 1) % 256;
			j = (j + S[i]) % 256;
			swap(S, i, j);
			int k = S[(S[i] + S[j]) % 256];
			int K = (int) k;
			encodeData[x] = (byte) (data[x] ^ K);
		}
		return encodeData;
	}
	
	public static boolean isKeyValid(byte[] key) {
		byte[] bKey = key;
		int len = bKey.length;
		int num = 0;// 0x0E计数
		if (len > 0 && len <= 256)
		{
			for (int i = 0; i < len; i++)
			{
				if ((bKey[i] & 0xFF) == 0x0E)
				{
					num++;
					if (num > 3) return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public static void swap(int[] source, int a, int b) {
		int tmp = source[a];
		source[a] = source[b];
		source[b] = tmp;
	}
}
