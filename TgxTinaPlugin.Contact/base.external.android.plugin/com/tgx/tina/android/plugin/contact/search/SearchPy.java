package com.tgx.tina.android.plugin.contact.search;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import com.tgx.tina.android.plugin.contact.search.SearchInfo.MATCH_TYPE;

/**
 * SearchPy 为通讯录号码拼音搜索进行功能提供的类库，使用Apache license Version2 协议进行开源分发。
 * 
 * @author ZhangZhuo
 * @version 0.14
 * @feature ① 首字母匹配：姓名和显示名、昵称按此规则匹配。如输入yw（zhangyonwei） a)
 *          如果姓名字段中包含字母和数字，则字母和数字按首字母处理，如张s123（搜索zs123、zhangs123可搜索到） ② 连续匹配：姓名
 *          , 所有电话号码字段按此规则匹配。如姓名拼音，拼音从左到右的顺序匹配。如输入hu， 则可以搜出所有带有hu的内容
 *          ，如姓名中胡力（huli）、花花（huahua）、黄素琴（huangsuqin）、小花（xiaohua）、虎虎
 *          （huhu）。如输入1234，匹配到黄（15312345555）、张（15312346666）。输入黄，则匹配到黄连、黄素琴、小黄 b)
 *          如果是匹配上两个，如虎虎（huhu）、15355295559，则仅高亮第一个。 ③
 *          全拼+首字母匹配：姓名字段按此规则匹配。如姓名拼音，输入zhangyw
 *          （zhangyonwei）、输入zywei（zhangyonwei），输入zhanyw（zhangyonwei）。 ④
 *          完全匹配：搜索词=字段，包括全拼完全匹配上。如输入huangsuqin（黄素琴），输入15312342344（黄
 *          15312342344）。 a) 搜索结果按以下优先级顺序显示： ①
 *          优先级1：被匹配到的字段是姓名（昵称、备注名）>被匹配到的字段是其他字段 ② 优先级2：完全匹配>首字母匹配>全拼+首字母匹配
 *          >连续模糊匹配。 ③ 优先级3:
 *          被匹配上第1个字符>被匹配上第2个字符>…>被匹配上第N个字符，此字符位置定义为被匹配上的第一个字符的绝对位置。如输入hu ，匹配到的
 *          黄（huang）（匹配上第1个字符“黄”）>吴欢（wuhuan）（匹配上第2个字符’欢”），如输入1234，匹配到的黄（
 *          15312346666）>张（15381234777） ④
 *          优先级4：最近联系人>本机通讯录>个人通讯录>翼友>单位通讯录>Ivpn通讯录>共享通讯录。(java external,no
 *          internal support) ⑤
 *          优先级5：拨号盘中输入2，则按2、a、b、c搜索，则搜索结果按a匹配到的>按b匹配到的>按c匹配到的>按2匹配到的排序。
 *          如混合输入23，则按ad匹配到的>ae >af
 *          >bd>be>bf>cd>ce>cf>23。（联系人列表中搜索忽略此规则）（T9映射式搜索） ⑥
 *          优先级5：匹配到的人的姓名个数越少越排在前面。如果如匹配到的爸（ba）>八哥（bage）>班长陈（banzhangchen） ⑦
 *          优先级6：如果上述5项排序规则依然完全一致，则按照未被匹配到字符做排序，如果是号码类型的字段，则从小到大，如果是非号码类型的字段，
 *          则按照拼音首字母或者英文字母的字母表顺序排序
 *          。如：张三（zhangsan）、张四（zhangsi），如果输入zhangs搜索，则按拼音s后面的an>i排序，则张三>张四。 ⑧
 *          如果上述6项排序规则依然完全一致，则按照搜索到的顺序逐个显示。
 * @other_features ① 支持姓氏多音字。 ② 支持初始化后自动完成SearchInfo的字母序列排序。 ③ 支持字母，数字，汉子的搜索。
 * @unsupport ① 不支持其他东亚语言拆分的拼音 ② 特殊字符搜索结果不完全正常，由于USC-2导致，需要进行USC-4映射，暂不进行支持 ③
 *            最大搜索树集合16K条，超越此范围将获得不可预知的错误
 * @inportmant 每个搜索域都需要建立自己的 SearchPy对象，初始化SearchInfo之前需要对InfoFactory进行reset操作。
 */
public class SearchPy<T>
{
	static
	{
		System.loadLibrary("searchpy");
	}
	public volatile boolean	ReadLock	= false;
	private SearchInfo<T>[]	srcInfos;
	private int				domainAdr;

	/**
	 * @param srcInfos
	 *            用来初始索引的基础数据
	 * @param mode
	 *            0默认模式 支持 T9输入映射的数字->拼音的搜索，1 字母拼音搜索，2 字符匹配搜索模式
	 * @return int[] des[0]为树的内存地址. 后面为infos对应的新的序列拼音位置. 所以总长度为
	 *         srcInfos.length+1
	 */
	native int[] initInfos(SearchInfo<T>[] srcInfos, int mode, int oldAdr);

	/**
	 * 搜索算法
	 */
	native int[] search(int tree, String key, int[] lastSearched, int start, int limit, long filter, boolean isT9);

	native int[] getFirstPyPrimaryKeys(int tree, long filter);

	native int[] getInfosPrimaryKeys(int tree, long filter);

	native String[] getHightLights(int tree, int[] searched, String keyText, String dyeStr, boolean isT9);

	native String[] getPyHightLights(int tree, int[] searched, String keyText, String dyeStr, boolean isT9);

	native String[] getNameHightLights(int tree, int[] searched, String keyText, String dyeStr, boolean isT9);

	native String[] getPhoneHightLights(int tree, int[] searched, String keyText, String dyeStr);

	/**
	 * @param searchedInfos
	 *            已搜出的结果集合
	 * @param start
	 *            着色操作在结果集合中的起始位置
	 * @param limit
	 *            着色操作的数量限定
	 * @param keyText
	 *            搜索时使用的KeyWord
	 * @param dyeStr
	 *            结果使用的着色编码 eg.{<font color=#D64206>}
	 * @author ZhangZhuo
	 * @description 此方法只提供对电话号码进行着色，不对姓名进行处理。
	 */
	public void javaGetPhoneHighLights(SearchInfo<T>[] searchedInfos, int start, int limit, String keyText, String dyeStr) {
		if (searchedInfos.length == 0) throw new IllegalArgumentException("No searched!");
		if (start >= searchedInfos.length || start < 0) throw new ArrayIndexOutOfBoundsException();
		if (start + limit >= searchedInfos.length) limit = searchedInfos.length - start;
		int[] lastSearchedIndex = new int[limit];
		for (int i = 0; i < limit; i++)
			lastSearchedIndex[i] = (searchedInfos[start + i].index << 8) | (searchedInfos[start + i].matchPhoneID & 0xFF);
		String[] resultArray = getPhoneHightLights(domainAdr, lastSearchedIndex, keyText, dyeStr);
		for (int i = 0; i < limit; i++)
			searchedInfos[start + i].dyePhone = resultArray[i];
	}

	/**
	 * @param searchedInfos
	 *            已搜出的结果集合
	 * @param start
	 *            着色操作在结果集合中的起始位置
	 * @param limit
	 *            着色操作的数量限定
	 * @param keyText
	 *            搜索时使用的KeyWord
	 * @param dyeStr
	 *            结果使用的着色编码 eg.{<font color=#D64206>}
	 * @author ZhangZhuo
	 * @description 此方法针对全部数据进行着色，使用SearchInfo内建的MATCHTYPE进行区分
	 */
	public void javaGetHighLights(SearchInfo<T>[] searchedInfos, int start, int limit, String keyText, String dyeStr, boolean isT9) {
		if (searchedInfos.length == 0) throw new IllegalArgumentException("No searched!");
		if (start >= searchedInfos.length || start < 0) throw new ArrayIndexOutOfBoundsException();
		if (start + limit >= searchedInfos.length) limit = searchedInfos.length - start;
		int[] lastSearchedIndex = new int[limit];
		for (int i = 0; i < limit; i++)
			lastSearchedIndex[i] = (searchedInfos[start + i].index << 8) | (searchedInfos[start + i].matchPhoneID & 0xFF);

		String[] resultArray = getHightLights(domainAdr, lastSearchedIndex, keyText, dyeStr, isT9);

		for (int i = 0; i < limit; i++)
		{
			SearchInfo<T> info = searchedInfos[start + i];
			if (info.matchType.equals(MATCH_TYPE.NameMatch))
			{
				info.dyeName = resultArray[i];
				info.dyePhone = info.phoneNum;
			}
			else
			{
				info.dyePhone = resultArray[i];
				info.dyeName = info.name;
			}

		}
	}

	//*----------------------------------------------------------------
	/**
	 * @param searchedInfos
	 *            已搜出的结果集合
	 * @param start
	 *            着色操作在结果集合中的起始位置
	 * @param limit
	 *            着色操作的数量限定
	 * @param keyText
	 *            搜索时使用的KeyWord
	 * @param dyeStr
	 *            结果使用的着色编码 eg.{<font color=#D64206>}
	 * @author ZhangZhuo
	 * @description 此方法只提供姓名与姓名对应的拼音进行着色，不对号码等匹配进行处理。
	 */
	public void javaGetPyHighLights(SearchInfo<T>[] searchedInfos, int start, int limit, String keyText, String dyeStr, boolean isT9) {
		if (searchedInfos.length == 0) throw new IllegalArgumentException("No searched!");
		if (start >= searchedInfos.length || start < 0) throw new ArrayIndexOutOfBoundsException();
		if (start + limit >= searchedInfos.length) limit = searchedInfos.length - start;
		int[] lastSearchedIndex = new int[limit];
		for (int i = 0; i < limit; i++)
			lastSearchedIndex[i] = (searchedInfos[start + i].index << 8) | (searchedInfos[start + i].matchPhoneID & 0xFF);
		String[] resultArray = getPyHightLights(domainAdr, lastSearchedIndex, keyText, dyeStr, isT9);
		for (int i = 0; i < limit; i++)
			searchedInfos[start + i].dyeName = resultArray[i];
	}

	/**
	 * @param searchedInfos
	 *            已搜出的结果集合
	 * @param start
	 *            着色操作在结果集合中的起始位置
	 * @param limit
	 *            着色操作的数量限定
	 * @param keyText
	 *            搜索时使用的KeyWord
	 * @param dyeStr
	 *            结果使用的着色编码 eg.{<font color=#D64206>}
	 * @author ZhangZhuo
	 * @description 此方法只提供对名字进行着色，不对姓名对应的拼音进行处理，号码也不在处理范围之内。
	 */
	public void javaGetNameHighLights(SearchInfo<T>[] searchedInfos, int start, int limit, String keyText, String dyeStr, boolean isT9) {
		if (searchedInfos.length == 0) throw new IllegalArgumentException("No searched!");
		if (start >= searchedInfos.length || start < 0) throw new ArrayIndexOutOfBoundsException();
		if (start + limit >= searchedInfos.length) limit = searchedInfos.length - start;
		int[] lastSearchedIndex = new int[limit];
		for (int i = 0; i < limit; i++)
			lastSearchedIndex[i] = (searchedInfos[start + i].index << 8) | (searchedInfos[start + i].matchPhoneID & 0xFF);
		String[] resultArray = getNameHightLights(domainAdr, lastSearchedIndex, keyText, dyeStr, isT9);
		for (int i = 0; i < limit; i++)
			searchedInfos[start + i].dyeName = resultArray[i];
	}

	/**
	 * @param srcInfos
	 *            需要进行初始化的数据。
	 * @param mode
	 *            0默认模式 支持 T9输入映射的数字->拼音的搜索，1 字母拼音搜索，2 字符匹配搜索模式
	 * @return domainAdr 返回搜索域的数据地址
	 * @description 传递这部分SearchInfo不需要进行其他cache进行访问，SearchPy已经将数据进行内部保存
	 */
	public void javaInitInfos(SearchInfo<T>[] srcInfos, int mode) {
		disposeOld();
		this.srcInfos = srcInfos;
		int[] des = initInfos(srcInfos, mode, domainAdr);
		for (int i = 0; i < srcInfos.length; i++)
			srcInfos[des[i + 1]].cOrder = i;
		Arrays.sort(srcInfos, cOrderInfoComparator);
		for (int i = 0; i < srcInfos.length; i++)
			srcInfos[i].index = i;
		domainAdr = des[0];
	}

	public SearchInfo<T>[] getPrimaryPyIndex(long filter, Map<String, Integer> indexMap) {
		char[] chars = "abcdefghijklmnopqrstuvwxyz#".toUpperCase().toCharArray();
		int[] index = getFirstPyPrimaryKeys(domainAdr, filter);
		int[] primaryKeys = getInfosPrimaryKeys(domainAdr, filter);
		if (primaryKeys == null) return null;
		@SuppressWarnings("unchecked")
		SearchInfo<T>[] infos = new SearchInfo[primaryKeys.length];
		for (int i = 0; i < primaryKeys.length; i++)
		{
			infos[i] = srcInfos[primaryKeys[i]].clone();
			infos[i].indexInGroup = i;
		}
		for (int i = 0, j = -1; i < index.length; i++)
		{
			j = index[i];
			if (j < 0) continue;
			SearchInfo<T> info = infos[j];
			info.indexTitle = "" + chars[i];
			if (indexMap != null) indexMap.put(info.indexTitle, j);
		}
		return infos;
	}

	public void disposeOld() {
		if (srcInfos != null) for (SearchInfo<T> info : srcInfos)
			info.dispose();
		srcInfos = null;
	}

	public final Comparator<SearchInfo<T>>	cOrderInfoComparator	= new Comparator<SearchInfo<T>>()
																	{
																		@Override
																		public int compare(SearchInfo<T> lhs, SearchInfo<T> rhs) {
																			return lhs.cOrder < rhs.cOrder ? -1 : lhs.cOrder > rhs.cOrder ? 1 : 0;
																		}
																	};

	public enum Result
	{
		FOR_NAME, FOR_PHONE
	}

	/**
	 * @param key
	 *            搜索使用的KeyWord
	 * @param lastSearched
	 *            上次已搜索到的结果
	 * @param start
	 *            在搜索结果集中进行搜索时起始的id default:0
	 * @param limit
	 *            搜索集合的结果数量限制 default:0 无限制
	 * @param filter
	 *            过滤器 default:0 当前未使用
	 * @param isT9
	 *            搜索时是否使用T9映射
	 * @return SearchInfo[][] result[0] 姓名搜索集合，result[1] 号码搜索集合
	 */
	@SuppressWarnings("unchecked")
	public SearchInfo<T>[][] javaSearch(String key, int[] lastSearched, int start, int limit, long filter, boolean isT9) {
		if (ReadLock) return null;
		if (lastSearched != null && lastSearched.length > 0 && (start < 0 || start + limit > lastSearched.length)) throw new IllegalArgumentException(
						"<start>/<limit> is invaild");
		int[] resultArray = search(domainAdr, key, lastSearched, start, limit, filter, isT9);

		if (resultArray == null) return null;

		SearchInfo<T>[] resultName, resultPhone;
		int nameArrayLength = 0, phoneArrayLength = 0;
		MATCH_TYPE matchType = MATCH_TYPE.NameMatch;
		for (int j = 0, primaryKey = -1; j < resultArray.length; j++)//primaryKey =-1时用来隔开姓名匹配结果和号码匹配结果
		{
			primaryKey = resultArray[j];
			if (primaryKey < 0)
			{
				matchType = MATCH_TYPE.PhoneMatch;
				continue;
			}
			if (matchType.equals(MATCH_TYPE.PhoneMatch)) phoneArrayLength++;
			else nameArrayLength++;
		}
		resultName = nameArrayLength > 0 ? new SearchInfo[nameArrayLength] : null;
		resultPhone = phoneArrayLength > 0 ? new SearchInfo[phoneArrayLength] : null;

		SearchInfo<T>[][] result = new SearchInfo[2][];
		result[Result.FOR_NAME.ordinal()] = resultName;
		result[Result.FOR_PHONE.ordinal()] = resultPhone;
		matchType = MATCH_TYPE.NameMatch;
		for (int i = 0, j = 0, m = 0, n = 0, primaryKey = -1, phoneIndex = -1; j < resultArray.length; j++)//primaryKey =-1时用来隔开姓名匹配结果和号码匹配结果
		{
			primaryKey = resultArray[j];
			if (primaryKey < 0)
			{
				matchType = MATCH_TYPE.PhoneMatch;
				continue;
			}
			if (matchType.equals(MATCH_TYPE.PhoneMatch))
			{
				phoneIndex = primaryKey & 0xFF;
				primaryKey >>>= 8;
			}
			SearchInfo<T> info = srcInfos[primaryKey].clone();
			info.cOrder = i;
			info.matchType = matchType;
			info.matchPhoneID = phoneIndex;
			i++;
			if (matchType.equals(MATCH_TYPE.PhoneMatch)) resultPhone[n++] = info;
			else resultName[m++] = info;
		}
		return result;
	}

	/**
	 * @param key
	 *            搜索使用的KeyWord
	 * @param lastSearched
	 *            上次已搜索到的结果
	 * @param start
	 *            在搜索结果集中进行搜索时起始的id default:0
	 * @param limit
	 *            搜索集合的结果数量限制 default:0 无限制
	 * @param filter
	 *            过滤器 default:0 当前未使用
	 * @param isT9
	 *            是否对key进行T9映射
	 * @return SearchInfo[] 所有结果都已经返回，以内置的MatchType进行区分。
	 */
	public SearchInfo<T>[] javaSearchAll(String key, int[] lastSearched, int start, int limit, long filter, boolean isT9) {
		if (ReadLock) return null;
		if (lastSearched != null && lastSearched.length > 0 && (start < 0 || start + limit > lastSearched.length)) throw new IllegalArgumentException(
						"<start>/<limit> is invaild");
		int[] resultArray = search(domainAdr, key, lastSearched, start, limit, filter, isT9);

		if (resultArray == null) return null;
		@SuppressWarnings("unchecked")
		SearchInfo<T>[] result = new SearchInfo[resultArray.length - 1];
		MATCH_TYPE matchType = MATCH_TYPE.NameMatch;
		for (int i = 0, j = 0, primaryKey = -1, phoneIndex = -1; j < resultArray.length; j++)//primaryKey =-1时用来隔开姓名匹配结果和号码匹配结果
		{
			primaryKey = resultArray[j];
			if (primaryKey < 0)
			{
				matchType = MATCH_TYPE.PhoneMatch;
				continue;
			}
			if (matchType.equals(MATCH_TYPE.PhoneMatch))
			{
				phoneIndex = primaryKey & 0xFF;
				primaryKey >>>= 8;
			}
			result[i] = srcInfos[primaryKey].clone();
			result[i].cOrder = i;
			result[i].matchType = matchType;
			result[i].matchPhoneID = phoneIndex;
			i++;
		}
		return result;
	}
}
