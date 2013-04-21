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
package com.tgx.tina.android.plugin.contacts.category;

import com.tgx.tina.android.plugin.contacts.base.Profile;

public class CategoryProfile
				extends
				Profile
{
	public final static int	SerialNum	= CategoryProfileSN;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	@Override
	public int getContactID() {
		return 0;
	}

	@Override
	public int getRawContactID() {
		return 0;
	}

	@Override
	public Profile clone() {
		CategoryProfile categoryProfile = new CategoryProfile();
		return categoryProfile;
	}

}
