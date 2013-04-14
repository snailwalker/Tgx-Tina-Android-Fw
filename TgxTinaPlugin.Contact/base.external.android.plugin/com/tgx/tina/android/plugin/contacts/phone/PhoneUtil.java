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
package com.tgx.tina.android.plugin.contacts.phone;

public class PhoneUtil
{
	static final int[]	iKmpBuf	= new int[32];

	/**
	 * @param key
	 * @param src
	 * @return 匹配的结果集合
	 */
	public static int matchString(String key, String src) {
		char[] keyText = key.toCharArray();
		char[] srcText = src.toCharArray();
		int i, j;
		int len1 = 0;
		int len2 = 0;
		int p1 = 0;
		int p2 = 0;
		int k = 0;
		int phoneMatchPos;
		iKmpBuf[0] = 0;
		j = 0;
		len1 = key.length();
		for (i = 1; i < len1; i++)
		{
			while (keyText[j] != keyText[i] && j > 0)
				j = iKmpBuf[j - 1];
			if (keyText[j] == keyText[i]) j++;
			iKmpBuf[i] = j;
		}

		j = 0;
		len2 = src.length();
		for (i = 0; i < len2; i++)
		{
			while (keyText[j] != srcText[i] && j > 0)
			{
				j = iKmpBuf[j - 1];
			}

			if (keyText[j] == srcText[i])
			{
				j++;
			}

			if (j >= len1)
			{

				p1 = i - j + 1;
				p2 = p1 + j;
				phoneMatchPos = 0;
				for (k = p1; k < p2; k++)
				{
					phoneMatchPos |= 1 << k;
				}
				return phoneMatchPos; //i - j +1; 匹配定位
			}
		}
		return 0;
	}

	public static String formatPhone(String phoneSrc) {
		StringBuffer phoneDes = new StringBuffer();
		for (char c : phoneSrc.toCharArray())
		{
			if ((c >= '0' && c <= '9') || c == '+' || c == '*' || c == '#') phoneDes.append(c);
		}
		return phoneDes.toString();
	}

	public static String reversePhone(String phone) {
		return new StringBuffer(phone).reverse().toString();
	}

	public static String getMinMatchKey(String reversePhone) {
		int len = reversePhone.length();
		int end = len > 10 ? 10 : len;//中国的高端用户啊！！！！189xxx和139xxx屌丝啊 屌丝
		return reversePhone.substring(0, end);
	}

	public static boolean isMatch(String key, String phone, boolean needFormat) {
		String reversePhone = reversePhone(needFormat ? formatPhone(phone) : phone);
		String reverseKey = reversePhone(needFormat ? formatPhone(key) : key);
		String toMatch = getMinMatchKey(reversePhone);
		String matchKey = getMinMatchKey(reverseKey);
		return 0 != matchString(matchKey, toMatch);
	}

	public static boolean isMinSame(String phone1, String phone2, boolean needFormate) {
		String reversePhone = reversePhone(needFormate ? formatPhone(phone2) : phone2);
		String reverseKey = reversePhone(needFormate ? formatPhone(phone1) : phone1);
		String toMatch = getMinMatchKey(reversePhone);
		String matchKey = getMinMatchKey(reverseKey);
		return 0xFF == matchString(matchKey, toMatch);
	}

	public static boolean isMinSame(String lookupKey1, String lookupKey2) {
		int len = Math.min(lookupKey1.length(), lookupKey2.length());
		return (0x3FF >> (10 - len)) == matchString(lookupKey1, lookupKey2);
	}
}
