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

import base.tina.core.task.AbstractResult;

public abstract class Profile
				extends
				AbstractResult
{
	final protected static int	SerialDomain		= -ContactTask.SerialDomain;
	protected final static int	TContactProfileSN	= SerialDomain + 1;
	protected final static int	CallLogProfileSN	= TContactProfileSN + 1;
	protected final static int	CategoryProfileSN	= CallLogProfileSN + 1;
	protected final static int	PhoneProfileSN		= CategoryProfileSN + 1;
	protected final static int	RawContactProfileSN	= PhoneProfileSN + 1;

	protected int				primaryKey;										//_ID
	protected int				foreignKey;
	protected int				externalKey;

	public abstract int getContactID();

	public abstract int getRawContactID();

	public int getPrimaryKey() {
		return primaryKey;
	}

	public abstract Profile clone();
}
