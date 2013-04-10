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
 package base.tina.external.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;


public final class IoUtil
{
	
	public final static byte[] variableLength(int length) {
		int resLength = 0;
		int result = 0;
		do
		{
			result |= (length & 0x7F) << 24;
			length >>>= 7;
			resLength++;
			if (length > 0)
			{
				result >>>= 8;
				result |= 0x80000000;
			}
		}
		while (length > 0);
		byte[] res = new byte[resLength];
		for (int i = 0, move = 24; i < resLength; i++)
		{
			res[i] = (byte) (result >>> move);
			move -= 8;
		}
		return res;
	}
	
	public final static int readVariableLength(InputStream is) {
		int length = 0;
		int cur;
		try
		{
			do
			{
				cur = is.read();
				length |= (cur & 0x7F);
				if ((cur & 0x80) != 0) length <<= 7;
			}
			while ((cur & 0x80) != 0);
			return length;
		}
		catch (Exception e)
		{
			return 0;
		}
	}
	
	public final static int readVariableLength(ByteBuffer buf) {
		int length = 0;
		int cur;
		if (buf.hasRemaining()) do
		{
			cur = buf.get();
			length |= (cur & 0x7F);
			if ((cur & 0x80) != 0) length <<= 7;
		}
		while ((cur & 0x80) != 0 && buf.hasRemaining());
		return length;
	}
	
	public final static int writeLong(long v, byte[] b, int off) {
		b[off] = (byte) (0xFF & (v >> 56));
		b[off + 1] = (byte) (0xFF & (v >> 48));
		b[off + 2] = (byte) (0xFF & (v >> 40));
		b[off + 3] = (byte) (0xFF & (v >> 32));
		b[off + 4] = (byte) (0xFF & (v >> 24));
		b[off + 5] = (byte) (0xFF & (v >> 16));
		b[off + 6] = (byte) (0xFF & (v >> 8));
		b[off + 7] = (byte) (0xFF & v);
		
		return 8;
	}
	
	public final static int writeMac(long v, byte[] b, int off) {
		b[off] = (byte) (0xFF & (v >> 40));
		b[off + 1] = (byte) (0xFF & (v >> 32));
		b[off + 2] = (byte) (0xFF & (v >> 24));
		b[off + 3] = (byte) (0xFF & (v >> 16));
		b[off + 4] = (byte) (0xFF & (v >> 8));
		b[off + 5] = (byte) (0xFF & v);
		return 6;
	}
	
	public final static int writeInt(int v, byte[] b, int off) {
		b[off] = (byte) (0xFF & (v >> 24));
		b[off + 1] = (byte) (0xFF & (v >> 16));
		b[off + 2] = (byte) (0xFF & (v >> 8));
		b[off + 3] = (byte) (0xFF & v);
		return 4;
	}
	
	public final static int writeShort(int v, byte[] b, int off) {
		b[off] = (byte) (0xFF & (v >> 8));
		b[off + 1] = (byte) (0xFF & v);
		return 2;
	}
	
	public final static int writeByte(int v, byte[] b, int off) {
		b[off] = (byte) v;
		return 1;
	}
	
	public final static int write(byte[] v, int src_off, byte[] b, int off, int len) {
		if (v == null || v.length == 0) return 0;
		System.arraycopy(v, src_off, b, off, len);
		return len;
	}
	
	public final static short readShort(byte[] src, int off) {
		return (short) ((src[off] & 0xFF) << 8 | (src[off + 1] & 0xFF));
	}
	
	public final static int readUnsignedShort(byte[] src, int off) {
		return ((src[off] & 0xFF) << 8 | (src[off + 1] & 0xFF));
	}
	
	public final static int readInt(byte[] src, int off) {
		return (src[off] & 0xFF) << 24 | (src[off + 1] & 0xFF) << 16 | (src[off + 2] & 0xFF) << 8 | (src[off + 3] & 0xFF);
	}
	
	public final static long readLong(byte[] src, int off) {
		return (src[off] & 0xFFL) << 56 | (src[off + 1] & 0xFFL) << 48 | (src[off + 2] & 0xFFL) << 40 | (src[off + 3] & 0xFFL) << 32 | (src[off + 4] & 0xFFL) << 24 | (src[off + 5] & 0xFFL) << 16 | (src[off + 6] & 0xFFL) << 8 | (src[off + 7] & 0xFFL);
	}
	
	public final static long readMac(byte[] src, int off) {
		return (src[off] & 0xFFL) << 40 | (src[off + 1] & 0xFFL) << 32 | (src[off + 2] & 0xFFL) << 24 | (src[off + 3] & 0xFFL) << 16 | (src[off + 4] & 0xFFL) << 8 | (src[off + 5] & 0xFFL);
	}
	
	public final static String replace(String from, String to, String source) {
		if (source == null || from == null || to == null) return null;
		if (source.indexOf(from) < 0) return source;
		StringBuffer bf = new StringBuffer();
		int index = -1;
		while ((index = source.indexOf(from)) != -1)
		{
			bf.append(source.substring(0, index) + to);
			source = source.substring(index + from.length());
			index = -1;
		}
		bf.append(source);
		return bf.toString();
	}
	
	/**
	 * xor ^ <0-127>
	 * 
	 * @param src
	 * @param xor
	 *            key
	 * @param xor_s
	 *            key_1
	 * @param xor_e
	 *            key_2
	 */
	public final static byte xorArrays(byte[] src, byte xor, byte xor_s, byte xor_e) {
		if (src == null || src.length == 0) return xor;
		if ((xor_s & 0xFF) == 0xFF && (xor_e & 0xFF) == 0xFF) return xor;// test
		else
		{
			int length = src.length;
			for (int i = 0; i < length; i++)
			{
				writeByte((src[i] & 0xFF) ^ xor, src, i);
				xor = (byte) (xor < xor_e ? xor + 1 : xor_s);
			}
			return xor;
		}
	}
	
	private static int[] crc_t; // CRC table
	                            
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
		Thread.yield();
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
	
	public final static String[] splitString(String src, String regular, int limit) {
		return src.split(regular, limit);
	}
	
	public final static boolean isNumberic(String src) {
		if (src == null || (src = src.trim()).equals("")) return false;
		char[] chars = src.toCharArray();
		boolean result = true;
		for (char c : chars)
		{
			if (c < '0' || c > '9') return false;
		}
		return result;
	}
	
	final static String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	
	final static char   pad   = '=';
	
	public final static byte[] base64Decoder(char[] src, int start) throws IOException {
		if (src == null || src.length == 0) return null;
		char[] four = new char[4];
		int i = 0, l, aux;
		char c;
		boolean padded;
		ByteArrayOutputStream dst = new ByteArrayOutputStream(src.length >> 1);
		while (start < src.length)
		{
			i = 0;
			do
			{
				if (start >= src.length)
				{
					if (i > 0) throw new IOException("bad BASE 64 In->");
					else return dst.toByteArray();
				}
				c = src[start++];
				if (chars.indexOf(c) != -1 || c == pad) four[i++] = c;
				else if (c != '\r' && c != '\n') throw new IOException("bad BASE 64 In->");
			}
			while (i < 4);
			padded = false;
			for (i = 0; i < 4; i++)
			{
				if (four[i] != pad && padded) throw new IOException("bad BASE 64 In->");
				else if (!padded && four[i] == pad) padded = true;
			}
			if (four[3] == pad)
			{
				if (start < src.length) throw new IOException("bad BASE 64 In->");
				l = four[2] == pad ? 1 : 2;
			}
			else l = 3;
			for (i = 0, aux = 0; i < 4; i++)
				if (four[i] != pad) aux |= chars.indexOf(four[i]) << (6 * (3 - i));
			
			for (i = 0; i < l; i++)
				dst.write((aux >>> (8 * (2 - i))) & 0xFF);
		}
		dst.flush();
		byte[] result = dst.toByteArray();
		dst.close();
		dst = null;
		return result;
	}
	
	public final static String base64Encoder(byte[] src, int start, int wrapAt) {
		if (src == null || src.length == 0) return null;
		StringBuffer encodeDst = new StringBuffer();
		int lineCounter = 0;
		while (start < src.length)
		{
			int buffer = 0, byteCounter;
			for (byteCounter = 0; byteCounter < 3 && start < src.length; byteCounter++, start++)
				buffer |= (src[start] & 0xFF) << (16 - (byteCounter << 3));
			if (wrapAt > 0 && lineCounter == wrapAt)
			{
				encodeDst.append("\r\n");
				lineCounter = 0;
			}
			char b1 = chars.charAt((buffer << 8) >>> 26);
			char b2 = chars.charAt((buffer << 14) >>> 26);
			char b3 = (byteCounter < 2) ? pad : chars.charAt((buffer << 20) >>> 26);
			char b4 = (byteCounter < 3) ? pad : chars.charAt((buffer << 26) >>> 26);
			encodeDst.append(b1).append(b2).append(b3).append(b4);
			lineCounter += 4;
		}
		return encodeDst.toString();
	}
	
	public final static String quoted_print_Encoding(String src, String charSet) {
		if (src == null || src.equals("")) return null;
		try
		{
			byte[] encodeData = src.getBytes(charSet);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			for (int i = 0; i < encodeData.length; i++)
			{
				if (encodeData[i] >= 0) buffer.write(encodeData[i]);
				else
				{
					buffer.write('=');
					char[] charArry = Integer.toHexString(encodeData[i] & 0xFF).toUpperCase().toCharArray();
					for (int j = 0; j < charArry.length; j++)
						buffer.write(charArry[j]);
					charArry = null;
				}
			}
			buffer.flush();
			encodeData = null;
			String result = new String(buffer.toByteArray(), charSet);
			buffer.close();
			return result;
		}
		catch (UnsupportedEncodingException e)
		{
			//#debug error
			e.printStackTrace();
		}
		catch (IOException e)
		{
			//#debug error
			e.printStackTrace();
		}
		return src;
	}
	
	public final static String quoted_print_Decoding(String src, String charSet) {
		if (src == null || src.equals("")) return null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int length = src.length();
		try
		{
			for (int i = 0, k; i < length;)
			{
				k = i + 1;
				boolean canIntParse = src.charAt(i) != '=' ? false : true;
				baos.write(canIntParse ? Integer.parseInt(src.substring(k, i += 3), 16) : src.charAt(i++));
			}
			baos.flush();
			return new String(baos.toByteArray(), charSet);
		}
		catch (UnsupportedEncodingException e)
		{
			//#debug error
			e.printStackTrace();
		}
		catch (IOException e)
		{
			//#debug error
			e.printStackTrace();
		}
		catch (Exception e)
		{
			//#debug error
			e.printStackTrace();
		}
		finally
		{
			try
			{
				baos.close();
			}
			catch (IOException e)
			{
				//#debug error
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public final static int PROTOCOL   = 0, HOST = 1, PORT = 2, PATH = 3, CONTENT = 4, ARGUMENTS = 5;
	public final static int READ       = 1;
	public final static int WRITE      = 1 << 1;
	public final static int READ_WRITE = READ | WRITE;
	
	public final static String[] splitURL(String url) {
		StringBuffer u = new StringBuffer(url.toLowerCase());
		String[] result = new String[6];
		for (int i = 0; i < 6; i++)
		{
			result[i] = "";
		}
		// get protocol
		
		int index = url.indexOf(":");
		if (index > 0)
		{
			result[PROTOCOL] = url.substring(0, index);
			u.delete(0, index + 1);
		}
		else if (index == 0) throw new IllegalArgumentException("url format error - protocol");
		// check for host/port
		if (u.length() >= 2 && u.charAt(0) == '/' && u.charAt(1) == '/')
		{
			// found domain part
			u.delete(0, 2);
			int slash = u.toString().indexOf('/');
			if (slash < 0)
			{
				slash = u.length();
			}
			if (slash != 0)
			{
				int colon = u.toString().indexOf(':');
				int endIndex = slash;
				if (colon >= 0)
				{
					if (colon > slash) throw new IllegalArgumentException("url format error - port");
					endIndex = colon;
					result[PORT] = u.toString().substring(colon + 1, slash);
				}
				result[HOST] = u.toString().substring(0, endIndex);
				u.delete(0, slash);
			}
		}
		if (u.length() > 0)
		{
			url = u.toString();
			int slash = url.lastIndexOf('/');
			if (slash > 0) result[PATH] = url.substring(0, slash);
			else if (slash == 0)
			{
				if (url.indexOf('?') > 0) throw new IllegalArgumentException("url format error - path");
				result[PATH] = url;
				return result;
			}
			if (slash < url.length() - 1)
			{
				String fn = url.substring(slash + 1, url.length());
				int anchorIndex = fn.indexOf('?');
				if (anchorIndex >= 0)
				{
					result[CONTENT] = fn.substring(0, anchorIndex);
					result[ARGUMENTS] = fn.substring(anchorIndex + 1);
				}
				else
				{
					result[CONTENT] = fn;
				}
			}
		}
		else result[PATH] = "/";
		return result;
	}
	
	public final static String mergeURL(String[] splits) {
		StringBuffer buffer = new StringBuffer();
		if (!splits[PROTOCOL].equals("")) buffer.append(splits[PROTOCOL]).append("://");
		if (!splits[HOST].equals("")) buffer.append(splits[HOST]);
		if (!splits[PORT].equals("")) buffer.append(':').append(splits[PORT]);
		if (!splits[PATH].equals(""))
		{
			buffer.append(splits[PATH]);
			if (!splits[PATH].equals("/")) buffer.append('/');
		}
		if (!splits[CONTENT].equals("")) buffer.append(splits[CONTENT]);
		if (!splits[ARGUMENTS].equals("")) buffer.append('?').append(splits[ARGUMENTS]);
		return buffer.toString();
	}
	
	public final static String mergeProxyTarURL(String[] splits) {
		StringBuffer buffer = new StringBuffer();
		if (!splits[HOST].equals("")) buffer.append(splits[HOST]);
		if (!splits[PORT].equals("")) buffer.append(':').append(splits[PORT]);
		if (!splits[PATH].equals(""))
		{
			buffer.append(splits[PATH]);
			if (!splits[PATH].equals("/")) buffer.append('/');
		}
		if (!splits[CONTENT].equals("")) buffer.append(splits[CONTENT]);
		if (!splits[ARGUMENTS].equals("")) buffer.append('?').append(splits[ARGUMENTS]);
		return buffer.toString();
	}
	
	public final static String mergeX_SerLet(String[] splits) {
		StringBuffer buffer = new StringBuffer();
		if (!splits[PATH].equals(""))
		{
			buffer.append(splits[PATH]);
			if (!splits[PATH].equals("/")) buffer.append('/');
		}
		if (!splits[CONTENT].equals("")) buffer.append(splits[CONTENT]);
		if (!splits[ARGUMENTS].equals("")) buffer.append('?').append(splits[ARGUMENTS]);
		return buffer.toString();
	}
	
	public final static String mergeX_Host(String[] splits) {
		StringBuffer buffer = new StringBuffer();
		if (!splits[HOST].equals("")) buffer.append(splits[HOST]);
		if (!splits[PORT].equals("")) buffer.append(':').append(splits[PORT]);
		return buffer.toString();
	}
}
