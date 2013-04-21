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
package com.tgx.tina.android.plugin.contacts.base;

public class TContactProfile
				extends
				Profile
{
	public final static int	SerialNum	= TContactProfileSN;

	@Override
	public int getSerialNum() {
		return SerialNum;
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

	public String	displayName;		//contact display-name
	public String	phones[][];		//[count][4] | [i][0]:phoneNum 		| [i][1]:phone mime type 			| [i][2]: custom type label    | [i][3]:phone callerloc
	public int		photo_id;
	public int		local_raw_version;

	@Override
	public Profile clone() {
		TContactProfile tContactProfile = new TContactProfile();
		tContactProfile.displayName = displayName;
		if (phones != null)
		{
			tContactProfile.phones = new String[phones.length][];
			int i = 0;
			for (String[] x : phones)
			{
				tContactProfile.phones[i] = new String[x.length];
				System.arraycopy(x, 0, tContactProfile.phones[i], 0, x.length);
				i++;
			}
		}

		return tContactProfile;
	}

	public void addPhone(String phone, int type, String label) {
		if (phones == null) phones = new String[1][4];
		if (!addTel(phone, type, label))
		{
			String[][] tmp = phones;
			phones = new String[tmp.length + 1][4];
			System.arraycopy(tmp, 0, phones, 0, tmp.length);
			addTel(phone, type, label);
		}
	}

	final boolean addTel(String phone, int type, String label) {
		for (String[] tmp : phones)
		{
			if (tmp[CONTENT_INDEX] == null)
			{
				tmp[CONTENT_INDEX] = phone == null ? null : phone;
				if (null != label && !"".equals(label)) tmp[SUB_CONTENT_INDEX] = label;
				tmp[MIMETYPE_INDEX] = String.valueOf(type);
				return true;//恒返回true，防止出现phone==null时死循环
			}
		}
		return false;
	}

	@Override
	public void dispose() {
		displayName = null;
		if (phones != null)
		{
			for (int i = 0, j = 0; i < phones.length; i++)
			{
				if (phones[i] != null) for (j = 0; j < phones[i].length; j++)
					phones[i][j] = null;
				phones[i] = null;
			}
		}
		phones = null;
		super.dispose();
	}

	public final static int	CONTENT_INDEX		= 0;
	public final static int	MIMETYPE_INDEX		= CONTENT_INDEX + 1;
	public final static int	TYPE_LABEL			= MIMETYPE_INDEX + 1;
	public final static int	SUB_CONTENT_INDEX	= MIMETYPE_INDEX + 1;

}
