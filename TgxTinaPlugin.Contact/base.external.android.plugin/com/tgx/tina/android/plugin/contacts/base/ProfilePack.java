package com.tgx.tina.android.plugin.contacts.base;

import java.util.Collection;
import java.util.LinkedList;

import base.tina.core.task.AbstractResult;


public abstract class ProfilePack<T extends Profile>
        extends
        AbstractResult
{
	public final static int SerialNum = Profile.SerialDomain + 100;
	
	public ProfilePack() {
		profiles = new LinkedList<T>();
	}
	
	public final int getCount() {
		return profiles.size();
	}
	
	@Override
	public final void dispose() {
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
	
	public Collection<T> profiles;
	
}
