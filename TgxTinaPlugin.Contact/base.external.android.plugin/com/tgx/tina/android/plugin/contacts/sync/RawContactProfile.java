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
package com.tgx.tina.android.plugin.contacts.sync;

import java.util.Locale;

import android.provider.ContactsContract;
import base.tina.external.io.IoUtil;

import com.tgx.tina.android.plugin.contacts.base.Profile;

/**
 * {@link http://www.ietf.org/rfc/rfc2425.txt }<br>
 * {@link http://www.ietf.org/rfc/rfc2426.txt }<br>
 * {@link http://www.ietf.org/rfc/rfc4770.txt }<br>
 * {@link http://www.ietf.org/rfc/rfc2739.txt }<br>
 * {@link http://www.ietf.org/rfc/rfc6350.txt }<br>
 * 
 * @author Zhangzhuo
 */
public class RawContactProfile
				extends
				Profile
{
	public RawContactProfile()
	{
		this(-1);
	}

	public RawContactProfile(int rawContactID)
	{
		setRawContactID(rawContactID);
	}

	@Override
	public final void dispose() {
		reset();
		vCard = null;
		super.dispose();
	}

	@Override
	public final int getContactID() {
		return foreignKey;
	}

	@Override
	public final int getRawContactID() {
		return primaryKey;
	}

	public void setRawContactID(int rawContactID) {
		primaryKey = rawContactID;
	}

	public void setContactID(int contactID) {
		foreignKey = contactID;
	}

	public final static int	SerialNum	= RawContactProfileSN;

	@Override
	public int getSerialNum() {
		return SerialNum;
	}

	public String		lookUpKey;				// UID
	public String		displayName;			// contact display-name
	public String[]		name;					// name field 7 field
	public String[][]	phones;				// [count][4] | [i][0]:phoneNum
												// | [i][1]:phone mime type |
												// [i][2]: custom type label |
												// [i][3]:phone callerloc
	public String[][]	addresses;				// [count][10]| [i][0]:address |
												// [i][1]:address mime type |
												// [i][2]: custom type label |
												// [i][3-9]:detail
	public String[][]	emails;				// [count][3] | [i][0]:email |
												// [i][1]:email mime type |
												// [i][2]: custom type label
	public String[][]	orgs;					// [count][5] |
												// [i][0]:organization |
												// [i][1]:organization mime type
												// | [i][2]: custom type label |
												// [i][3]:title | [i][4]:role
	public String[]		webUrls;
	public String		birthday;
	public String		photoEncoded;
	public String		nickName;
	public String		note;
	public String		gender;				// TODO 标准 应该为2段字符 [0]:
												// {"M"|male,"F"|fmale,"O"|other,"U"|unknow,""|}
												// //[1]: {";" text}
	public String[]		groupMemberShip;
	public long[]		groupMemberShipID;
	public long			groupMemberShipMask;
	public String		vCard;

	public void clone(RawContactProfile rawContactProfile) {
		rawContactProfile.primaryKey = primaryKey;
		rawContactProfile.foreignKey = foreignKey;
		rawContactProfile.externalKey = externalKey;
		rawContactProfile.vCard = vCard;
		rawContactProfile.groupMemberShipMask = groupMemberShipMask;
		if (groupMemberShipID != null)
		{
			rawContactProfile.groupMemberShipID = new long[groupMemberShipID.length];
			System.arraycopy(groupMemberShipID, 0, rawContactProfile.groupMemberShipID, 0, groupMemberShipID.length);
		}
		if (groupMemberShip != null)
		{
			rawContactProfile.groupMemberShip = new String[groupMemberShip.length];
			System.arraycopy(groupMemberShip, 0, rawContactProfile.groupMemberShip, 0, groupMemberShip.length);
		}
		rawContactProfile.gender = gender;
		rawContactProfile.note = note;
		rawContactProfile.nickName = nickName;
		rawContactProfile.photoEncoded = photoEncoded;
		rawContactProfile.birthday = birthday;
		rawContactProfile.displayName = displayName;
		rawContactProfile.lookUpKey = lookUpKey;
		if (webUrls != null)
		{
			rawContactProfile.webUrls = new String[webUrls.length];
			System.arraycopy(webUrls, 0, rawContactProfile.webUrls, 0, webUrls.length);
		}
		if (name != null)
		{
			rawContactProfile.name = new String[name.length];
			System.arraycopy(name, 0, rawContactProfile.name, 0, name.length);
		}
		if (phones != null)
		{
			rawContactProfile.phones = new String[phones.length][];
			int i = 0;
			for (String[] x : phones)
			{
				rawContactProfile.phones[i] = new String[x.length];
				System.arraycopy(x, 0, rawContactProfile.phones[i], 0, x.length);
				i++;
			}
		}
		if (addresses != null)
		{
			rawContactProfile.addresses = new String[addresses.length][];
			int i = 0;
			for (String[] x : addresses)
			{
				rawContactProfile.addresses[i] = new String[x.length];
				System.arraycopy(x, 0, rawContactProfile.addresses[i], 0, x.length);
				i++;
			}
		}
		if (emails != null)
		{
			rawContactProfile.emails = new String[emails.length][];
			int i = 0;
			for (String[] x : emails)
			{
				rawContactProfile.emails[i] = new String[x.length];
				System.arraycopy(x, 0, rawContactProfile.emails[i], 0, x.length);
				i++;
			}
		}
		if (orgs != null)
		{
			rawContactProfile.orgs = new String[orgs.length][];
			int i = 0;
			for (String[] x : orgs)
			{
				rawContactProfile.orgs[i] = new String[x.length];
				System.arraycopy(x, 0, rawContactProfile.orgs[i], 0, x.length);
				i++;
			}
		}
	}

	@Override
	public Profile clone() {
		RawContactProfile rawContactProfile = new RawContactProfile();
		clone(rawContactProfile);
		return rawContactProfile;
	}

	// 重置所有属性项
	public void reset() {
		lookUpKey = null;
		displayName = null;
		int i, j;
		if (name != null) for (i = 0; i < name.length; i++)
			name[i] = null;
		name = null;
		if (phones != null) for (i = 0; i < phones.length; i++)
		{
			if (phones[i] != null) for (j = 0; j < phones[i].length; j++)
				phones[i][j] = null;
			phones[i] = null;
		}
		phones = null;
		if (addresses != null) for (i = 0; i < addresses.length; i++)
		{
			if (addresses[i] != null) for (j = 0; j < addresses[i].length; j++)
				addresses[i][j] = null;
			addresses[i] = null;
		}
		addresses = null;
		if (emails != null) for (i = 0; i < emails.length; i++)
		{
			if (emails[i] != null) for (j = 0; j < emails[i].length; j++)
				emails[i][j] = null;
			emails[i] = null;
		}
		emails = null;
		if (orgs != null) for (i = 0; i < orgs.length; i++)
		{
			if (orgs[i] != null) for (j = 0; j < orgs[i].length; j++)
				orgs[i][j] = null;
			orgs[i] = null;
		}
		orgs = null;
		if (webUrls != null) for (i = 0; i < webUrls.length; i++)
			webUrls[i] = null;
		webUrls = null;
		if (groupMemberShip != null) for (i = 0; i < groupMemberShip.length; i++)
			groupMemberShip[i] = null;
		groupMemberShip = null;
		groupMemberShipID = null;
		gender = null;
		birthday = null;
		photoEncoded = null;
		nickName = null;
		note = null;
	}

	public String vCardEncode() throws Exception {
		return vCardEncode("2.1", "UTF-8", true);
	}

	public String vCardEncode(boolean encodePhoto) throws Exception {
		return vCardEncode("2.1", "UTF-8", encodePhoto);
	}

	public String vCardEncode(String version, String charSet) {
		return vCardEncode(version, charSet, true);
	}

	protected String[]	common;

	public String vCardEncode(String version, String charSet, boolean encodePhoto) {
		boolean hasData = false;
		common = new String[] {
						"CHARSET=" + charSet ,
						"ENCODING=QUOTED-PRINTABLE"
		};
		StringBuffer buffer = new StringBuffer();
		addField(buffer, "BEGIN", null, "VCARD");
		addField(buffer, "VERSION", null, version);
		addField(buffer, "X-RID", null, String.valueOf(getRawContactID()));
		hasData = addField(buffer, "N", common, formatN(charSet)) || hasData;
		hasData = addField(buffer, "FN", common, IoUtil.quoted_print_Encoding(displayName, charSet)) || hasData;
		if (phones != null)
		{
			for (String[] phone : phones)
			{
				if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_WORK).equals(phone[MIMETYPE_INDEX])) hasData = addField(buffer, "TEL;WORK", null, phone[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_HOME).equals(phone[MIMETYPE_INDEX])) hasData = addField(buffer, "TEL;HOME", null, phone[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME).equals(phone[MIMETYPE_INDEX])) hasData = addField(buffer, "TEL;FAX;HOME", null, phone[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK).equals(phone[MIMETYPE_INDEX])) hasData = addField(buffer, "TEL;FAX;WORK", null, phone[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).equals(phone[MIMETYPE_INDEX])) hasData = addField(buffer, "TEL;CELL", null, phone[CONTENT_INDEX]) || hasData;
				else hasData = addField(buffer, "TEL", null, phone[CONTENT_INDEX]) || hasData;
			}
		}
		if (emails != null)
		{
			for (String[] email : emails)
			{
				if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_WORK).equals(email[MIMETYPE_INDEX])) hasData = addField(buffer, "EMAIL;WORK", null, email[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_HOME).equals(email[MIMETYPE_INDEX])) hasData = addField(buffer, "EMAIL;HOME", null, email[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE).equals(email[MIMETYPE_INDEX])) hasData = addField(buffer, "EMAIL;MOBILE", null, email[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_OTHER).equals(email[MIMETYPE_INDEX])) hasData = addField(buffer, "EMAIL;OTHER", null, email[CONTENT_INDEX]) || hasData;
				else if (String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM).equals(email[MIMETYPE_INDEX])) hasData = addField(buffer, "EMAIL;CUSTOM", null, email[CONTENT_INDEX]) || hasData;
				else hasData = addField(buffer, "EMAIL", null, email[CONTENT_INDEX]) || hasData;
			}
		}
		if (addresses != null)
		{
			for (String[] address : addresses)
			{
				if (address != null && address[CONTENT_INDEX] != null)
				{
					hasData = addField(buffer, "ADR", addressArgs(address, common, charSet), addressField(address, charSet)) || hasData;
				}
			}
		}
		if (orgs != null)
		{
			for (String[] org : orgs)
			{
				if (org != null && org[CONTENT_INDEX] != null)
				{
					hasData = addField(buffer, "ORG", orgArgs(org, common, charSet), orgField(org, charSet)) || hasData;
					hasData = addField(buffer, "TITLE", common, titleField(org, charSet)) || hasData;
				}
			}
		}

		if (groupMemberShip != null)
		{
			for (String group : groupMemberShip)
			{
				if (group != null)
				{
					hasData = addField(buffer, "CATEGORIES", common, IoUtil.quoted_print_Encoding(group, charSet)) || hasData;
				}
			}
		}

		if (webUrls != null)
		{
			for (String webUrl : webUrls)
			{
				if (webUrl != null && !"".equals(webUrl))
				{
					hasData = addField(buffer, "URL", common, IoUtil.quoted_print_Encoding(webUrl, charSet)) || hasData;
				}
			}
		}
		hasData = addField(buffer, "NOTE", common, IoUtil.quoted_print_Encoding(note, charSet)) || hasData;
		if (lookUpKey != null && !lookUpKey.equals("")) hasData = addField(buffer, "UID", common, IoUtil.quoted_print_Encoding(lookUpKey, charSet)) || hasData;
		hasData = addField(buffer, "BDAY", null, birthday) || hasData;
		hasData = addField(buffer, "GENDER", null, gender) || hasData;
		if (encodePhoto)
		{
			hasData = addField(buffer, "PHOTO", new String[] {
							"ENCODING=b"
			}, photoEncoded) || hasData;
		}
		hasData = vCardEncodeEx(buffer, charSet, encodePhoto) || hasData;
		buffer.append("END:VCARD");
		return hasData ? buffer.toString() : null;
	}

	// 留做被继承后做扩展用
	protected boolean vCardEncodeEx(StringBuffer vcard, String charSet, boolean encodePhoto) {
		return false;
	}

	// 此处或许正则更高效,但是使用split可以更好的向Kjava进行兼容.
	public void vCardDecode(String vCard) {
		String[] splits = IoUtil.splitString(vCard, ":", 0);
		if (splits == null || splits.length == 0) throw new IllegalArgumentException("vCard string is invalid");
		if (splits[0].equals("BEGIN") && splits[splits.length - 1].startsWith("VCARD"))
		{
			for (int i = 0; i < splits.length - 1;)// i是key的位置，如果vcard正确的话，key的最后位置是-2(end:vcard)
			{
				offset = 1;
				String[] field = fieldParser(splits, i);
				fieldDecode(field);
				i += offset;
			}
		}
		else throw new IllegalArgumentException("vCard string is invalid");
		this.vCard = vCard;
	}

	final static int	FIELD_KEY	= 0;
	final static int	FIELD_VALUE	= FIELD_KEY + 1;
	final static int	FIELD_COUNT	= FIELD_VALUE + 1;

	int					offset		= 0;

	String[] fieldParser(String[] splits, int start) {
		if (start == splits.length - 2) return new String[] {
						"END" ,
						"VCARD"
		};
		String[] fields = new String[FIELD_COUNT];
		int begin = 0;

		begin = splits[start].lastIndexOf('\n');
		fields[FIELD_KEY] = splits[start].substring(begin + 1); // 先取出key
		StringBuffer text = new StringBuffer();
		while (true)
		{

			begin = splits[start + offset].lastIndexOf('\n'); // value 为
																// key后面值。value是以\n结尾的
			if (begin < 0)
			{
				text.append(splits[start + offset]).append(":");
				offset++;
			}
			else
			{
				text.append(splits[start + offset].substring(0, begin));
				fields[FIELD_VALUE] = text.toString();
				fields[FIELD_VALUE] = fields[FIELD_VALUE].replace("\r", "");
				break;
			}
		}
		return fields;
	}

	void fieldDecode(String[] field) {
		String charSet = "UTF-8"; // default;
		@SuppressWarnings("unused")
		boolean isBase64 = false, isQpEncoded = false;
		String fieldKey = field[FIELD_KEY];
		String fieldValue = field[FIELD_VALUE].trim();
		String[] keySplit = IoUtil.splitString(fieldKey, ";", 0);
		String key, fieldMain = null, fieldArgs;
		StringBuffer fieldArgsBuffer = new StringBuffer();
		for (int i = 0; i < keySplit.length; i++)
		{
			key = keySplit[i];
			if (i > 0)
			{
				if (key.indexOf('=') > 0)
				{
					String[] argSplit = IoUtil.splitString(key, "=", 2);
					if (argSplit[FIELD_KEY].equalsIgnoreCase("CHARSET")) charSet = argSplit[FIELD_VALUE];
					else if (argSplit[FIELD_KEY].equalsIgnoreCase("ENCODING"))
					{
						String encoding = argSplit[FIELD_VALUE];
						if (encoding.equals("b") || encoding.equals("BASE64")) isBase64 = true;
						else if (encoding.equals("QUOTED-PRINTABLE")) isQpEncoded = true;
						// TODO 其他encoding方式
					}
					else
					// 其他包含'='的参数式,TYPE=XXXX;LABEL=XXXX
					{
						if (fieldArgsBuffer.length() > 0) fieldArgsBuffer.append(';');
						fieldArgsBuffer.append(key);
					}
				}
				else
				{
					if (fieldArgsBuffer.length() > 0) fieldArgsBuffer.append(';');
					fieldArgsBuffer.append(key);
				}
			}
			else fieldMain = key;
		}
		fieldArgs = fieldArgsBuffer.toString();
		fieldArgsBuffer.setLength(0);
		if (fieldArgs.indexOf('=') > 0)
		{
			String[] args = IoUtil.splitString(fieldArgs, ";", 0);
			for (String arg : args)
			{
				if (fieldArgsBuffer.length() > 0) fieldArgsBuffer.append(';');
				String[] argSplit = IoUtil.splitString(arg, "=", 2);
				if (argSplit.length > 1)
				{
					fieldArgsBuffer.append(argSplit[FIELD_KEY]).append('=');
					if (isQpEncoded) argSplit[FIELD_VALUE] = IoUtil.quoted_print_Decoding(argSplit[FIELD_VALUE], charSet);
					fieldArgsBuffer.append(argSplit[FIELD_VALUE]);
				}
				else fieldArgsBuffer.append(arg);
			}
			fieldArgs = fieldArgsBuffer.toString();
		}

		if (isQpEncoded) fieldValue = IoUtil.quoted_print_Decoding(fieldValue, charSet);
		if (fieldValue == null) return;
		/*
		 * 此段代码考虑N与FN的出现出现顺序问题,所以需要考虑函数重入的顺序问题,此处不好理解请仔细分析
		 */
		if ("N".equalsIgnoreCase(fieldMain))
		{
			if (name == null) name = new String[7];
			String[] splits = IoUtil.splitString(fieldValue, ";", 0);
			/*
			 * Type special note: The structured type value corresponds, in
			 * sequence, to the Family Name, Given Name, Additional Names,
			 * Honorific Prefixes, and Honorific Suffixes.
			 */
			name[FAMILY_NAME] = splits.length > 0 && splits[0] != null && !splits[0].equals("") ? splits[0].replace(" ", "") : null;
			name[GIVEN_NAME] = splits.length > 1 && splits[1] != null && !splits[1].equals("") ? splits[1].replace(" ", "") : null;
			name[MIDDLE_NAME] = splits.length > 2 && splits[2] != null && !splits[2].equals("") ? splits[2].replace(" ", "") : null;
			name[PREFIX_NAME] = splits.length > 3 && splits[3] != null && !splits[3].equals("") ? splits[3].replace(" ", "") : null;
			name[SUFFIX_NAME] = splits.length > 4 && splits[4] != null && !splits[4].equals("") ? splits[4].replace(" ", "") : null;
			name[OTHER_NAME] = splits.length > 5 && splits[5] != null && !splits[5].equals("") ? splits[5] : null;
			if (name[PREFIX_NAME] != null && name[PREFIX_NAME].equals("") && !name[PREFIX_NAME].endsWith(".")) name[PREFIX_NAME] += ".";
			if (name[SUFFIX_NAME] != null && name[SUFFIX_NAME].equals("") && !name[SUFFIX_NAME].endsWith(".")) name[SUFFIX_NAME] += ".";
			if (displayName == null) name[DISPLAY_NAME] = formatName();
			else swapName();
			if (name[DISPLAY_NAME] != null && !"".equals(name[DISPLAY_NAME]))
			{
				// 有时候用于显示的名字可能未必是名字组合,可能是组织头衔之类的
				displayName = name[DISPLAY_NAME];
			}
		}
		else if ("FN".equalsIgnoreCase(fieldMain))
		{
			displayName = fieldValue;
			// 已经处理过"N"了
			if (name != null) swapName();
		}
		else if ("ADR".equalsIgnoreCase(fieldMain))
		{
			if (addresses == null) addresses = new String[1][10];
			boolean inserted = addAddress(fieldArgs, fieldValue);
			if (!inserted)
			{
				String[][] tmp = addresses;
				addresses = new String[tmp.length + 1][10];
				System.arraycopy(tmp, 0, addresses, 0, tmp.length);
				addAddress(fieldArgs, fieldValue);
			}
		}
		else if ("TEL".equalsIgnoreCase(fieldMain))
		{
			if (phones == null) phones = new String[1][4];
			boolean inserted = addTel(fieldArgs, fieldValue);
			if (!inserted)
			{
				String[][] tmp = phones;
				phones = new String[tmp.length + 1][4];
				System.arraycopy(tmp, 0, phones, 0, tmp.length);
				addTel(fieldArgs, fieldValue);
			}
		}
		else if ("EMAIL".equalsIgnoreCase(fieldMain))
		{
			if (emails == null) emails = new String[1][2];
			boolean inserted = addEmail(fieldArgs, fieldValue);
			if (!inserted)
			{
				String[][] tmp = emails;
				emails = new String[tmp.length + 1][2];
				System.arraycopy(tmp, 0, emails, 0, tmp.length);
				addEmail(fieldArgs, fieldValue);
			}
		}
		else if ("ORG".equalsIgnoreCase(fieldMain))
		{
			if (orgs == null) orgs = new String[1][5];
			boolean inserted = addOrg(fieldArgs, fieldValue);
			if (!inserted)
			{
				String[][] tmp = orgs;
				orgs = new String[tmp.length + 1][5];
				System.arraycopy(tmp, 0, orgs, 0, tmp.length);
				addOrg(fieldArgs, fieldValue);
			}
		}
		else if ("TITLE".equalsIgnoreCase(fieldMain))
		{
			if (orgs != null) addTitle(fieldArgs, fieldValue);
		}
		else if ("CATEGORIES".equalsIgnoreCase(fieldMain))
		{
			if (groupMemberShip == null) groupMemberShip = new String[1];
			else
			{
				String[] tmp = groupMemberShip;
				groupMemberShip = new String[tmp.length + 1];
				System.arraycopy(tmp, 0, groupMemberShip, 0, tmp.length);
			}
			int index = groupMemberShip.length - 1;
			groupMemberShip[index] = fieldValue;
		}
		else if ("URL".equalsIgnoreCase(fieldMain))
		{
			if (webUrls == null) webUrls = new String[1];
			else
			{
				String[] tmp = webUrls;
				webUrls = new String[tmp.length + 1];
				System.arraycopy(tmp, 0, webUrls, 0, tmp.length);
			}
			int index = webUrls.length - 1;
			webUrls[index] = fieldValue;
		}
		else if ("BDAY".equalsIgnoreCase(fieldMain))
		{
			/*
			 * yyyy-mm-dd yyyymmdd --mmdd
			 */
			if (fieldValue.length() == 8)
			{
				if (IoUtil.isNumberic(fieldValue))
				{
					String year = fieldValue.substring(0, 4);
					String month = fieldValue.substring(4, 6);
					String day = fieldValue.substring(6, 8);
					birthday = year + '-' + month + '-' + day;
					return;
				}
			}
			else if (fieldValue.length() == 10)
			{
				String[] splits = IoUtil.splitString(fieldValue, "-", 3);
				if (splits.length == 3 && splits[0].length() == 4 && IoUtil.isNumberic(splits[0]) && splits[1].length() == 2 && IoUtil.isNumberic(splits[1]) && splits[2].length() == 2 && IoUtil.isNumberic(splits[2]))
				{
					birthday = fieldValue;
					return;
				}
			}
			else if (fieldValue.length() == 6)
			{
				String[] splits = IoUtil.splitString(fieldValue, "-", 3);
				if (splits.length == 3 && splits[0].equals("") && splits[1].equals("") && IoUtil.isNumberic(splits[2]))
				{
					String month = splits[2].substring(0, 2);
					String day = splits[2].substring(2, 4);
					birthday = DEFUALT_BDAY_YEAR + '-' + month + '-' + day;
					return;
				}
			}
		}
		else if ("PHOTO".equalsIgnoreCase(fieldMain))
		{
			photoEncoded = fieldValue.equals("") ? null : fieldValue;
		}
		else if ("NOTE".equalsIgnoreCase(fieldMain))
		{
			note = fieldValue.equals("") ? null : fieldValue;
		}
		else if ("UID".equalsIgnoreCase(fieldMain))
		{
			lookUpKey = fieldValue.equals("") ? null : fieldValue;
		}
		else if ("X-RID".equalsIgnoreCase(fieldMain))
		{
			primaryKey = fieldValue.equals("") ? -1 : Integer.parseInt(fieldArgs);
		}
		else if ("GENDER".equalsIgnoreCase(fieldMain))
		{
			gender = fieldValue;
		}
		else vCardDecodeEx(fieldMain, fieldValue);
	}

	// 留做被继承后做扩展用
	protected void vCardDecodeEx(String toParse, String field) {
	}

	final String formatName() {
		StringBuffer nameBuild = new StringBuffer();
		if (name[PREFIX_NAME] != null) nameBuild.append(name[PREFIX_NAME]).append(' ');
		if (name[GIVEN_NAME] != null) nameBuild.append(name[GIVEN_NAME]);
		if (name[MIDDLE_NAME] != null) nameBuild.append(name[MIDDLE_NAME]);
		if (name[FAMILY_NAME] != null) nameBuild.append(name[FAMILY_NAME]);
		if (name[SUFFIX_NAME] != null) nameBuild.append(' ').append(name[SUFFIX_NAME]);
		if (name[OTHER_NAME] != null) nameBuild.append(' ').append(name[OTHER_NAME]);
		return nameBuild.toString();
	}

	final String formatN(String charSet) {
		if (name == null || name[DISPLAY_NAME] == null) return null;
		StringBuffer n = new StringBuffer();
		if (name[FAMILY_NAME] != null) n.append(name[FAMILY_NAME]);
		n.append(';');
		if (name[GIVEN_NAME] != null) n.append(name[GIVEN_NAME]);
		n.append(';');
		if (name[MIDDLE_NAME] != null) n.append(name[MIDDLE_NAME]);
		n.append(';');
		if (name[PREFIX_NAME] != null) n.append(name[PREFIX_NAME]);
		n.append(';');
		if (name[SUFFIX_NAME] != null) n.append(name[SUFFIX_NAME]);
		n.append(';');
		if (name[OTHER_NAME] != null) n.append(name[OTHER_NAME]);
		return IoUtil.quoted_print_Encoding(n.toString(), charSet);
	}

	// final String formatORG(String charSet) {
	// if (orgs == null) return null;
	// StringBuffer n = new StringBuffer();
	// if (orgs[ORG_ROLE] != null) n.append(orgs[FAMILY_NAME]);
	// n.append(';');
	// if (orgs[GIVEN_NAME] != null) n.append(orgs[GIVEN_NAME]);
	// n.append(';');
	// if (orgs[MIDDLE_NAME] != null) n.append(orgs[MIDDLE_NAME]);
	// n.append(';');
	// if (orgs[PREFIX_NAME] != null) n.append(orgs[PREFIX_NAME]);
	// n.append(';');
	// if (orgs[SUFFIX_NAME] != null) n.append(orgs[SUFFIX_NAME]);
	// return IoUtil.quoted_print_Encoding(n.toString(), charSet);
	// }

	final void swapName() {
		String nameBuild = formatName();
		if (!nameBuild.equals("") && !nameBuild.equals(displayName))// nameBuild.equals("")的情况证明name中无有效信息存在直接略过|displayName
																	// 可能为null
		{
			// 先实现最常见的两种可能性,其他的遇到再说
			if (name[GIVEN_NAME] != null && name[FAMILY_NAME] != null)
			{
				if (displayName.equals(name[FAMILY_NAME] + name[GIVEN_NAME]) || (name[MIDDLE_NAME] != null && displayName.equals(name[FAMILY_NAME] + name[MIDDLE_NAME] + name[GIVEN_NAME])))
				{
					swap(name, GIVEN_NAME, FAMILY_NAME);
				}
				// 有可能N字段分析和FN的字段就是不同的,在英文中较常见
				// else
				// {
				// for (int i = 1; i < name.length; i++)
				// name[i] = null;
				// name[GIVEN_NAME] = displayName;
				// }
			}
			// return;
		}
		name[DISPLAY_NAME] = displayName;
		// name[DISPLAY_NAME] = null;
	}

	final String formatAddress(String[] address) {
		StringBuffer addressBuild = new StringBuffer();
		if (address[ADDRESS_POBOX] != null) addressBuild.append(address[ADDRESS_POBOX]).append('\n');
		if (address[ADDRESS_STREET] != null) addressBuild.append(address[ADDRESS_STREET]).append('\n');
		if (address[ADDRESS_CITY] != null) addressBuild.append(address[ADDRESS_CITY]).append('\n');
		if (address[ADDRESS_NEIGHBORHOOD] != null) addressBuild.append(address[ADDRESS_NEIGHBORHOOD]);
		if (address[ADDRESS_REGION] != null)
		{
			if (address[ADDRESS_NEIGHBORHOOD] != null) addressBuild.append(',');
			addressBuild.append(address[ADDRESS_REGION]);
		}
		if (address[ADDRESS_POSTCODE] != null) addressBuild.append(' ').append(address[ADDRESS_POSTCODE]).append('\n');
		if (address[ADDRESS_COUNTRY] != null) addressBuild.append(address[ADDRESS_COUNTRY]);
		return addressBuild.toString();
	}

	final boolean addAddress(String fieldArgs, String fieldValue) {
		String[] splits = IoUtil.splitString(fieldValue, ";", 0);
		boolean inserted = false;
		for (String[] tmp : addresses)
		{
			if (tmp[CONTENT_INDEX] == null)
			{
				String[] args = IoUtil.splitString(fieldArgs, ";", 0);
				for (String arg : args)
				{
					if (arg.equalsIgnoreCase("HOME"))
					{
						tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME);
					}
					else if (arg.equalsIgnoreCase("WORK"))
					{
						tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK);
					}
					else if (arg.startsWith("LABEL"))
					{
						tmp[ADDRESS_LABEL] = IoUtil.splitString(arg, "=", 2)[FIELD_VALUE];
					}
					else
					{
						tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER);
					}
				}
				tmp[ADDRESS_POBOX] = splits.length > 0 && splits[0] != null && !splits[0].equals("") ? splits[0].replace(" ", "") : null;
				tmp[ADDRESS_NEIGHBORHOOD] = splits.length > 1 && splits[1] != null && !splits[1].equals("") ? splits[1].replace(" ", "") : null;
				tmp[ADDRESS_STREET] = splits.length > 2 && splits[2] != null && !splits[2].equals("") ? splits[2].replace(" ", "") : null;
				tmp[ADDRESS_CITY] = splits.length > 3 && splits[3] != null && !splits[3].equals("") ? splits[3].replace(" ", "") : null;
				tmp[ADDRESS_REGION] = splits.length > 4 && splits[4] != null && !splits[4].equals("") ? splits[4].replace(" ", "") : null;
				tmp[ADDRESS_POSTCODE] = splits.length > 5 && splits[5] != null && !splits[5].equals("") ? splits[5].replace(" ", "") : null;
				tmp[ADDRESS_COUNTRY] = splits.length > 6 && splits[6] != null && !splits[6].equals("") ? splits[6].replace(" ", "") : null;
				// tmp[ADDRESS_LABEL] = splits.length > 7 && splits[7] != null
				// && !splits[7].equals("") ? splits[7].replace(" ", "") : null;
				// adr-value = 0*6(text-value ";") text-value
				// ; PO Box, Extended Address, Street, Locality, Region, Postal
				// Code,Country Name
				// The NEIGHBORHOOD field is appended after the CITY field.

				if (tmp[ADDRESS_LABEL] != null) tmp[CONTENT_INDEX] = tmp[ADDRESS_LABEL];
				else
				{
					String formatted = formatAddress(tmp);
					tmp[CONTENT_INDEX] = formatted.equals("") ? null : formatted;
				}
				inserted = true;// 此处恒等true的理由为使addresses 不再增容
				break;
			}
		}
		return inserted;
	}

	final String[] orgArgs(String[] src, String[] oldArg, String charSet) {
		String[] dst = new String[1];
		if (src[MIMETYPE_INDEX] == null) src[MIMETYPE_INDEX] = "2";
		switch (Integer.parseInt(src[MIMETYPE_INDEX])) {
			case ContactsContract.CommonDataKinds.Organization.TYPE_CUSTOM:
				dst[0] = "LABEL=" + IoUtil.quoted_print_Encoding(src[SUB_CONTENT_INDEX], charSet);// LABEL
				break;
			case ContactsContract.CommonDataKinds.Organization.TYPE_WORK:
				dst[0] = "WORK";
				break;
			default:
				dst[0] = "OTHER";
				break;
		}
		String[] result = new String[(oldArg == null ? 0 : oldArg.length) + dst.length];
		System.arraycopy(dst, 0, result, 0, dst.length);
		if (oldArg != null) System.arraycopy(oldArg, 0, result, dst.length, oldArg.length);
		return result;
	}

	final String orgField(String[] org, String charSet) {
		if (org == null || org[CONTENT_INDEX] == null) return null;
		StringBuffer n = new StringBuffer();
		if (org[CONTENT_INDEX] != null) n.append(org[CONTENT_INDEX]);
		return IoUtil.quoted_print_Encoding(n.toString(), charSet);
	}

	final String[] titleArgs(String[] src, String[] oldArg, String charSet) {
		String[] dst = new String[1];
		switch (Integer.parseInt(src[MIMETYPE_INDEX])) {
			case ContactsContract.CommonDataKinds.Organization.TYPE_CUSTOM:
				dst[0] = "TYPE=" + IoUtil.quoted_print_Encoding(src[SUB_CONTENT_INDEX], charSet);// LABEL
				break;
			case ContactsContract.CommonDataKinds.Organization.TYPE_WORK:
				dst[0] = "TYPE=WORK";
				break;
			default:
				dst[0] = "";
				break;
		}
		String[] result = new String[(oldArg == null ? 0 : oldArg.length) + dst.length];
		System.arraycopy(dst, 0, result, 0, dst.length);
		if (oldArg != null) System.arraycopy(oldArg, 0, result, dst.length, oldArg.length);
		return dst;
	}

	final String titleField(String[] org, String charSet) {
		if (org == null || org[ORG_TITEL] == null) return null;
		StringBuffer n = new StringBuffer();
		if (org[ORG_TITEL] != null) n.append(org[ORG_TITEL]);
		return IoUtil.quoted_print_Encoding(n.toString(), charSet);
	}

	final String[] addressArgs(String[] src, String[] oldArg, String charSet) {
		String[] dst = null;
		if (src[ADDRESS_LABEL] != null)
		{
			dst = new String[2];
			dst[1] = "LABEL=" + IoUtil.quoted_print_Encoding(src[ADDRESS_LABEL], charSet);
		}
		if (dst == null) dst = new String[1];
		switch (Integer.parseInt(src[MIMETYPE_INDEX])) {
			case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME:
				dst[0] = "HOME";
				break;
			case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK:
				dst[0] = "WORK";
				break;
			default:
				dst[0] = "OTHER";
				break;
		}
		String[] result = new String[(oldArg == null ? 0 : oldArg.length) + dst.length];
		System.arraycopy(dst, 0, result, 0, dst.length);
		if (oldArg != null) System.arraycopy(oldArg, 0, result, dst.length, oldArg.length);
		return result;
	}

	final String addressField(String[] address, String charSet) {
		if (address == null || address[CONTENT_INDEX] == null) return null;
		StringBuffer n = new StringBuffer();
		if (address[ADDRESS_POBOX] != null) n.append(address[ADDRESS_POBOX]);
		n.append(';');
		if (address[ADDRESS_NEIGHBORHOOD] != null) n.append(address[ADDRESS_NEIGHBORHOOD]);
		n.append(';');
		if (address[ADDRESS_STREET] != null) n.append(address[ADDRESS_STREET]);
		n.append(';');
		if (address[ADDRESS_CITY] != null) n.append(address[ADDRESS_CITY]);
		n.append(';');
		if (address[ADDRESS_REGION] != null) n.append(address[ADDRESS_REGION]);
		n.append(';');
		if (address[ADDRESS_POSTCODE] != null) n.append(address[ADDRESS_POSTCODE]);
		n.append(';');
		if (address[ADDRESS_COUNTRY] != null) n.append(address[ADDRESS_COUNTRY]);
		return IoUtil.quoted_print_Encoding(n.toString(), charSet);
	}

	final boolean addTel(String fieldArgs, String fieldValue) {
		String[] splits = IoUtil.splitString(fieldArgs, ";", 0);
		boolean inserted = false;
		int type = 0;
		for (String[] tmp : phones)
		{
			if (tmp[CONTENT_INDEX] == null)
			{
				for (String arg : splits)
				{
					if (arg != null && !arg.equals(""))
					{
						if (arg.equalsIgnoreCase("HOME"))
						{
							type |= 1 << 0;
						}
						else if (arg.equalsIgnoreCase("WORK"))
						{
							type |= 1 << 1;
						}
						else if (arg.equalsIgnoreCase("FAX"))
						{
							type |= 1 << 2;
						}
						else if (arg.equalsIgnoreCase("PAGER"))
						{
							type |= 1 << 3;
						}
						else if (arg.equalsIgnoreCase("CELL"))
						{
							type |= 1 << 4;
						}
						else if (arg.equalsIgnoreCase("ISDN"))
						{
							type |= 1 << 5;
						}
						else if (arg.equalsIgnoreCase("TLX"))
						{
							type |= 1 << 6;
						}
						else if (arg.equalsIgnoreCase("MSG"))
						{
							type |= 1 << 7;
						}
						else if (arg.equalsIgnoreCase("VOICE"))
						{
							type |= 1 << 8;
						}
						else if (arg.equalsIgnoreCase("CAR"))
						{
							type |= 1 << 9;
						}
						else if (arg.equalsIgnoreCase("PREF"))
						{
							type |= 1 << 10;
						}
						else if (arg.startsWith("LABEL"))
						{
							tmp[SUB_CONTENT_INDEX] = IoUtil.splitString(arg, "=", 2)[FIELD_VALUE];
						}
					}
				}
				Switch:
				{
					switch (type) {
						case 1 << 0:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
							break Switch;
						case 1 << 1:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
							break Switch;
						case 1 << 2:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX);
							break Switch;
						case 1 << 3:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_PAGER);
							break Switch;
						case 1 << 4:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
							break Switch;
						case 1 << 5:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_ISDN);
							break Switch;
						case 1 << 6:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_TELEX);
							break Switch;
						case 1 << 7:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MMS);
							break Switch;
						case 1 << 8:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
							break Switch;
						case 1 << 9:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_CAR);
							break Switch;
						case 1 << 10:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_MAIN);
							break Switch;
						case 1 << 0 | 1 << 2:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME);
							break Switch;
						case 1 << 1 | 1 << 2:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK);
							break Switch;
						case 1 << 1 | 1 << 10:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN);
							break Switch;
						case 1 << 1 | 1 << 4:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE);
							break Switch;
						case 1 << 1 | 1 << 3:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER);
							break Switch;
						default:
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM);
							break Switch;
					}
				}
				tmp[CONTENT_INDEX] = fieldValue.equals("") ? null : fieldValue;
				inserted = true; // 此处恒等true的理由为使phones 不再增容
				break;
			}
		}
		return inserted;
	}

	final boolean addEmail(String fieldArgs, String fieldValue) {
		boolean inserted = false;
		for (String[] tmp : emails)
		{
			if (tmp[CONTENT_INDEX] == null)
			{

				if (fieldArgs.indexOf("HOME") >= 0)
				{
					tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_HOME);
				}
				else if (fieldArgs.indexOf("WORK") >= 0)
				{
					tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_WORK);
				}
				else if (fieldArgs.indexOf("CELL") >= 0)
				{
					tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE);
				}
				else
				{
					tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Email.TYPE_OTHER);
				}

				tmp[CONTENT_INDEX] = fieldValue.equals("") ? null : fieldValue;
				inserted = true;// 此处恒等true的理由为使emails 不再增容
				break;
			}
		}
		return inserted;
	}

	final boolean addOrg(String fieldArgs, String fieldValue) {
		boolean inserted = false;
		String[] splits = IoUtil.splitString(fieldArgs, ";", 0);
		for (String[] tmp : orgs)
		{
			if (tmp[CONTENT_INDEX] == null)
			{
				for (String arg : splits)
				{
					if (arg != null && !arg.equals(""))
					{
						if (arg.equalsIgnoreCase("COMPANY") || arg.equalsIgnoreCase("WORK"))
						{
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Organization.TYPE_WORK);
						}
						else if (arg.equalsIgnoreCase("OTHER"))
						{
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Organization.TYPE_OTHER);
						}
						else if (arg.toUpperCase(Locale.US).startsWith("LABEL＝"))
						{
							tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Organization.TYPE_CUSTOM);
							tmp[SUB_CONTENT_INDEX] = IoUtil.splitString(arg, "=", 2)[FIELD_VALUE];
						}
					}
				}
				if (tmp[MIMETYPE_INDEX] == null) tmp[MIMETYPE_INDEX] = String.valueOf(ContactsContract.CommonDataKinds.Organization.TYPE_OTHER);
				tmp[CONTENT_INDEX] = fieldValue.equals("") ? null : fieldValue;
				if (tmp[CONTENT_INDEX] != null)
				{
					String[] formatted = IoUtil.splitString(fieldValue, ";", 0);
					if (formatted.length > 1)
					{
						tmp[CONTENT_INDEX] = formatted[0];// 即使为""也可以用于显示,且进行null判定时也不影响
						tmp[ORG_TITEL] = formatted[1];
					}
					if (formatted.length > 2)
					{
						tmp[ORG_ROLE] = formatted[2];// Ignored.
					}
				}
				inserted = true;// 此处恒等true的理由为使orgs 不再增容
				break;
			}
		}
		return inserted;
	}

	final void addTitle(String fieldArgs, String fieldValue) {
		for (String[] tmp : orgs)
		{
			if (tmp[ORG_TITEL] == null)
			{
				tmp[ORG_TITEL] = fieldValue.equals("") ? null : fieldValue;
				break;
			}
		}
	}

	final void swap(String[] src, int a, int b) {
		String tmp = src[a];
		src[a] = src[b];
		src[b] = tmp;
	}

	protected boolean addField(StringBuffer buffer, String mimeType, String[] args, String field) {
		if (field == null || field.equals("")) return false;
		buffer.append(mimeType);
		if (args != null) for (String arg : args)
			if (arg != null && !arg.equals("")) buffer.append(';').append(arg);
		buffer.append(':');
		buffer.append(field);
		buffer.append("\r\n");
		return true;
	}

	public final static int		CONTENT_INDEX			= 0;
	public final static int		MIMETYPE_INDEX			= CONTENT_INDEX + 1;
	public final static int		SUB_CONTENT_INDEX		= MIMETYPE_INDEX + 1;
	public final static int		TYPE_LABEL				= MIMETYPE_INDEX + 1;

	public final static int		DISPLAY_NAME			= 0;
	public final static int		PREFIX_NAME				= DISPLAY_NAME + 1;
	public final static int		GIVEN_NAME				= PREFIX_NAME + 1;
	public final static int		MIDDLE_NAME				= GIVEN_NAME + 1;
	public final static int		FAMILY_NAME				= MIDDLE_NAME + 1;
	public final static int		SUFFIX_NAME				= FAMILY_NAME + 1;
	public final static int		OTHER_NAME				= SUFFIX_NAME + 1;

	public final static int		ADDRESS_POBOX			= MIMETYPE_INDEX + 1;
	public final static int		ADDRESS_NEIGHBORHOOD	= ADDRESS_POBOX + 1;		// 县/市
	public final static int		ADDRESS_STREET			= ADDRESS_NEIGHBORHOOD + 1;
	public final static int		ADDRESS_CITY			= ADDRESS_STREET + 1;		// 市?村?
	public final static int		ADDRESS_REGION			= ADDRESS_CITY + 1;		// 省
	public final static int		ADDRESS_POSTCODE		= ADDRESS_REGION + 1;		// 邮证编码
	public final static int		ADDRESS_COUNTRY			= ADDRESS_POSTCODE + 1;	// 国
	public final static int		ADDRESS_LABEL			= ADDRESS_COUNTRY + 1;

	public final static int		ORG_TITEL				= SUB_CONTENT_INDEX + 1;
	public final static int		ORG_ROLE				= ORG_TITEL + 1;
	public final static String	DEFUALT_BDAY_YEAR		= "1995";

	public final static int		PHONE_LOCAL				= SUB_CONTENT_INDEX + 1;

	public byte					toDo;
	public final static byte	toDelete				= 1;
	public final static byte	toInsert				= 1 << 1;
	public final static byte	toReplace				= 1 << 2;

}
