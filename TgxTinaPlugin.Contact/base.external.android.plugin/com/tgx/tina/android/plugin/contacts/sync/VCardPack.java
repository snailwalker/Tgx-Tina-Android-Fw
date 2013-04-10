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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import com.tgx.tina.android.plugin.contacts.base.ContactTask;
import com.tgx.tina.android.plugin.contacts.base.ProfilePack;

import android.content.Context;
import base.tina.core.task.AbstractResult;

public class VCardPack
				extends
				AbstractResult
{
	public final static int	SerialNum	= ProfilePack.SerialNum - 1;

	@Override
	public final int getSerialNum() {
		return SerialNum;
	}

	public LinkedList<String>	vCards	= new LinkedList<String>();

	public void addProfileVCard(String vCard) {
		vCards.add(vCard);
		storage += vCard.getBytes().length + 4;
		indexNum++;
	}

	@Override
	public final void dispose() {
		vCards.clear();
		vCards = null;
		skip = 0;
		availale = 0;
		super.dispose();
	}

	public final void dumpTofile(FileOutputStream os) throws IOException {
		if (vCards.isEmpty()) return;
		String str = null;
		DataOutputStream dos = new DataOutputStream(os);
		for (Iterator<String> iterator = vCards.iterator(); iterator.hasNext();)
		{
			str = iterator.next();
			byte[] strX = str.getBytes();
			dos.writeInt(strX.length);
			dos.write(strX);
			storage -= strX.length + 4;
			iterator.remove();
			indexNum--;
		}
	}

	private int	storage, storageLimit = 0x40000;	//256K
	public int	indexNum;

	public final boolean needDump() {
		return storage >= storageLimit;
	}

	public final int availableToWrite() {
		return availale;
	}

	private int	availale;
	private int	skip;

	public final void loadFromFile(Context context) throws IOException {
		DataInputStream dis = new DataInputStream(context.openFileInput(ContactTask.TMP_FILE_FOR_WRITE));
		int vCardLen;
		byte[] vCardX;
		String vCard;
		dis.skip(skip);
		while (storage < storageLimit && skip < availale)
		{
			vCardLen = dis.readInt();
			vCardX = new byte[vCardLen];
			dis.read(vCardX);
			vCard = new String(vCardX);
			addProfileVCard(vCard);
			skip += vCardLen + 4;
		}
		dis.close();
	}

	public final void initFromFile(Context context) throws IOException {
		DataInputStream dis = new DataInputStream(context.openFileInput(ContactTask.TMP_FILE_FOR_WRITE));
		availale = dis.readShort() & 0xFFFF;
		dis.close();
	}

	public static enum Status
	{
		WRITE_COMPLETE, TO_WRITE, READ_COMPLETE, TO_READ
	}

	private Status	name;

	public final Status getStatus() {
		return name;
	}

	public final void setStatus(Status status) {
		name = status;
	}
}
