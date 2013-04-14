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

import java.util.Collection;
import java.util.LinkedList;

import base.tina.core.task.AbstractResult;

public abstract class ProfilePack<T extends Profile>
				extends
				AbstractResult
{
	public final static int		SerialNum			= Profile.SerialDomain + 100;
	protected final static int	TContactPackSN		= SerialNum + 1;
	protected final static int	CallPackSN			= TContactPackSN + 1;
	protected final static int	CategoryPackSN		= CallPackSN + 1;
	protected final static int	PhonePackSN			= CategoryPackSN + 1;
	protected final static int	RawContactPackSN	= PhonePackSN + 1;

	public ProfilePack()
	{
		profiles = new LinkedList<T>();
	}

	public final int getCount() {
		return profiles.size();
	}

	@Override
	public void dispose() {
		if (profiles != null)
		{
			for (Profile profile : profiles)
				profile.dispose();
			profiles.clear();
		}
		profiles = null;
	}

	public final void addProfile(T profile) {
		profiles.add(profile);
	}

	public Collection<T>	profiles;

	@SuppressWarnings("unchecked")
	public ProfilePack<T> clone() {
		ProfilePack<T> pack = subInstance();
		for (Profile p : profiles)
			pack.profiles.add((T) p.clone());
		return pack;
	}

	protected abstract ProfilePack<T> subInstance();
}
