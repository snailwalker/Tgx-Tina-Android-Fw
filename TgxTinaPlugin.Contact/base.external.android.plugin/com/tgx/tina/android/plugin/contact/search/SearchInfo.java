package com.tgx.tina.android.plugin.contact.search;

public class SearchInfo<T>
{
	private static int factoryIndex;
	
	/**
	 * @param phone
	 *            存在多个号码请将号码以','，分隔组成一个串
	 * @param name
	 * @return
	 */
	public static <E> SearchInfo<E> infoFactory(String phone, String name) {
		SearchInfo<E> info = new SearchInfo<E>();
		if (factoryIndex > 0xFFFF) throw new IllegalArgumentException("Array index is out of bound");
		if ("".equals(phone) || "".equals(name) || phone == null || name == null) throw new IllegalArgumentException("Info's argument is invalid!");
		info.index = factoryIndex++;
		info.phoneNum = phone;
		info.name = name;
		return info;
	}
	
	public static void resetFactory() {
		factoryIndex = 0;
	}
	
	//下面四个字段java端不能修改
	public int    index;
	public int    cOrder;
	public long   filter;
	public String phoneNum;
	public String name;
	
	//以下为java侧使用，可以随意
	MATCH_TYPE    matchType;
	public String dyePhone;
	public String dyeName;
	public int    matchPhoneID = -1;
	public int    indexInGroup;
	public String indexTitle;
	public T      attach;
	
	public SearchInfo<T> clone() {
		SearchInfo<T> info = new SearchInfo<T>();
		info.index = index;
		info.filter = filter;
		info.phoneNum = phoneNum;
		info.name = name;
		info.dyePhone = dyePhone;
		info.dyeName = dyeName;
		return info;
	}
	
	public void dispose() {
		phoneNum = null;
		name = null;
		dyePhone = null;
		dyeName = null;
		indexTitle = null;
	}
	
	enum MATCH_TYPE {
		NameMatch, PhoneMatch
	}
}
