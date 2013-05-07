#include "SearchCore.h"
#include "SearchTools.h"
#include "SearchSpellCode.h"
#include <stdio.h>

#define CapsOff ('a'-'A')
//kmp比较缓存
int iKmpBuf[32];

//搜索初始化
void searchTreeInit(SearchTree* tree, boolean isInitFIndex, char userMode)
{
	int i = 0;
	tree->matchFunc = NULL;
	tree->keySearchData = NULL;
	tree->foreignTreeIndexes = NULL;
	if (isInitFIndex)
	{
		tree->foreignTreeIndexes = (ArrayList*) malloc(SIZEOF_ARRAYLIST);
	}
	initArrayCapacity(&tree->searchDatas, 512); //初始化到16K 空间量

	for (i = 0; i < CachedHitNum; i++)
	{
		initArrayCapacity(&(tree->firstSpellIndex[i]), 256);
	}
	initLinked(&(tree->searchPosQueue));
	initLinked(&(tree->searchPhones));
	initLinked(&(tree->lastSearched));
	initArrayCapacity(&(tree->desNameResult), 512);
	initArrayCapacity(&(tree->desPhoneResult), 512);
	tree->userMode = userMode;
}

int ucLength(const uchar* text)
{
	int length = 0;
	int i = 0;
	if (text)
	{
		while (text[i++] != '\0')
		{
			length++;
		}
	}
	return length;
}

int cLength(const char* text)
{
	int length = 0;
	int i = 0;
	if (text)
	{
		while (text[i++] != '\0')
		{
			length++;
		}
	}
	return length;
}

int findIndexInMultiPYin(uchar key)
{
	int low = 0;
	int high = MultiPyCodeCount - 1;
	int mid = 0;
	uchar midVal = 0;
	while (low <= high)
	{
		mid = (low + high) >> 1;
		midVal = MultiPinyinIndex[mid][0];
		if (midVal < key) low = mid + 1;
		else if (midVal > key) high = mid - 1;
		else return mid; // key found
	}
	return -(low + 1); // key not found.
}

int word2Code(uchar srcWord, WordCode* code, int charIndex)
{
	ubyte t1 = (srcWord >> 8) & 0xFF;
	ubyte t2 = srcWord & 0xFF;
	uchar word = 0;
	uchar pyIndex = 0;
	int tabIndex = -1;
	int i = 0;
	int sortKey = 0;
	int pyCodeNum = 0;
	code->srcUnicode = srcWord;
	code->pyCodeNum = charIndex << 3;
	code->pyCodeIndex = NULL;
	code->wordUnicode = 0;

	if (t1 == 0 && t2 < 128)
	{
		//Ascii
		word = t2;

		// 转换到小写
		if (word >= 'A' && word <= 'Z')
		{
			word = word + CapsOff;
		}
		code->wordUnicode = word;
		sortKey = word < 'a' || word > 'z' ? PyCodeNum + 1 : CharCodeIndex[word - 'a'];
	}
	else if (t1 == 255)
	{
		// 全角ASCII
		//转换到半角区
		t2 = t2 + 0x20;
		word = t2;
		code->srcUnicode = word;
		if (word >= 'A' && word <= 'Z')
		{
			word = word + CapsOff;
		}
		code->wordUnicode = word;
		sortKey = word < 'a' || word > 'z' ? PyCodeNum + 1 : CharCodeIndex[word - 'a'];
	}
	else if (t1 >= 78 && t1 <= 159)
	{
		code->wordUnicode = srcWord;
		// 一般汉字处理
		pyIndex = PyCodeIndex[t1 - 78][t2];

		if (pyIndex > 0)
		{
			//进行多拼音处理

			tabIndex = findIndexInMultiPYin(srcWord);
			if (tabIndex >= 0)
			{
				//多音字
				int multiPyIndex = 0;
				for (i = 1; i < MaxPyCode; i++)
				{
					multiPyIndex = MultiPinyinIndex[tabIndex][i];
					if (multiPyIndex > 0)
					{
						pyCodeNum++;
					}
					else
					{
						break;
					}
				}
				code->pyCodeIndex = (short*) malloc(pyCodeNum << 1);
				for (i = 0; i < pyCodeNum; i++)
				{
					multiPyIndex = MultiPinyinIndex[tabIndex][i + 1];
					code->pyCodeIndex[i] = multiPyIndex;
				}
				code->pyCodeNum = code->pyCodeNum | pyCodeNum;
			}
			else
			{
				pyCodeNum = 1;
				code->pyCodeNum++;
				code->pyCodeIndex = (short*) malloc(1 << 1);
				code->pyCodeIndex[0] = pyIndex;
			}
		}
		if ((pyCodeNum & 0x07) > 0)
		{
			sortKey = code->pyCodeIndex[0];
		}
	}
	else
	{
		//其他Unicode字符
		code->wordUnicode = srcWord;
	}
	if (sortKey == 0)
	{ //全部列入无法整理区
		sortKey = PyCodeNum + 1;
	}
	return sortKey = (sortKey << 16) | code->wordUnicode;
}

void text2SearchData(const uchar* text, const int textLength, SearchData *data)
{
	int i = 0, j = 0;
	if (data == NULL) return;
	data->codesCount = 0;
	data->isNameAllDigit = true;
	for (i = 0; i < textLength; i++)
	{
		if (text[i] == 0x20 || text[i] == 0x3000)
		{
			continue;
		}
		data->codesCount++;
	}
	if (text)
	{
		data->wordCodes = (WordCode*) malloc(SIZEOF_WORDCODE* data->codesCount);
		data->sortKeys = (int*) malloc(sizeof(int) * data->codesCount);
		for (i = 0, j = 0; i < textLength; i++)
		{
			if (text[i] == 0x20 || text[i] == 0x3000)
			{
				continue;
			}
			data->sortKeys[j] = word2Code(text[i], &(data->wordCodes[j]), i);
			data->isNameAllDigit = (data->wordCodes[j].pyCodeNum & 0x07) == 0 && data->isNameAllDigit;
			j++;
		}
	}
}

int* getInofsPrimaryKeys(SearchTree* tree, ulong64 filter)
{
	SearchData* dataToSearch = NULL;
	LinkedList tar;
	int i = 0;
	int* array = NULL;
	initLinked(&tar);
	for (i = 0; i < tree->searchDatas.size; i++)
	{
		tree->searchDatas.get(&tree->searchDatas, i, &dataToSearch);
		if (filter != 0 && (dataToSearch->filter & filter) == 0 && dataToSearch->filter != 0) continue;
		tar.append(&tar, dataToSearch->primaryKey);
	}
	if (tar.size == 0)
	{
		tar.dispose(&tar);
		return NULL ;
	}
	int* primaryKeys = malloc((tar.size + 1) * sizeof(int));
	primaryKeys[0] = tar.size;
	array = &primaryKeys[1];
	tar.toArray(&tar, &array, tar.size);
	tar.dispose(&tar);
	return primaryKeys;
}

void getFirstPyPrimaryKeys(SearchTree* tree, int* primaryKeys, ulong64 filter)
{
	SearchData* dataToSearch = NULL;
	int i = 0, j = 0, mask = 0x7FFFFFF, t = 0;
	uchar pyChar = 0;
	for (i = 0; i < tree->searchDatas.size; i++)
	{ //如需提升性能可以取一级索引的首项
		tree->searchDatas.get(&tree->searchDatas, i, &dataToSearch);
		if (filter != 0 && (dataToSearch->filter & filter) == 0) continue;
		//不需要校验dataToSearch.codesCount;<=0时,初始化时会失败

		if ((dataToSearch->wordCodes[0].pyCodeNum & 0x07) > 0)
		{ //存在拼音码的,比较拼音
			pyChar = PySpellCode[dataToSearch->wordCodes[0].pyCodeIndex[0]][0];
		}
		else
		{
			pyChar = dataToSearch->wordCodes[0].wordUnicode;
		}
		j = pyChar - 'a';
		if (j >= 0 && j < 26 && primaryKeys[j] < 0)
		{ //由于searchDatas是有序排列~
			primaryKeys[j] = t;
			mask ^= (1 << j);
		}
		else if (j >= 0 && j < 26 && primaryKeys[j] >= 0)
		{ //已经有数据了
			t++;
			continue;
		}
		else if (primaryKeys[26] < 0)
		{
			primaryKeys[26] = t;
			mask ^= 1 << 26;
		}
		if (mask == 0) break;
		t++;
	}
}

/*
 * 只有在完成了初始化之后才可以使用primaryKey作为搜索主键
 */
int findSearchData(SearchTree* tree, int primaryKey, SearchData** data)
{
	int low = 0;
	int mid = 0;
	ArrayList* ptr = &(tree->searchDatas);
	int high = ptr->size - 1;

	SearchData* temp;

	while (low <= high)
	{
		mid = (low + high) >> 1;
		ptr->get(ptr, mid, &temp);

		if (temp->primaryKey < primaryKey) low = mid + 1;
		else if (temp->primaryKey > primaryKey) high = mid - 1;
		else
		{
			*data = temp;
			return mid;
		}
	}
	return -(low + 1);
}

int findStorageData2Insert(SearchTree* tree, SearchData* data)
{
	int low = 0;
	int mid = 0, pos = 0;
	ArrayList* ptr = &(tree->searchDatas);
	int high = ptr->size - 1;
	SearchData* temp = NULL;
	int c;
	if (data == NULL)
	{
		return -2;
	}
	while (low <= high)
	{
		mid = (low + high) >> 1;
		ptr->get(ptr, mid, &temp);
		c = intsCmp(temp->sortKeys, temp->codesCount, data->sortKeys, data->codesCount);
		c = c == 0 ? (temp->primaryKey < data->primaryKey ? -1 : (temp->primaryKey > data->primaryKey ? 1 : 0)) : c;
		if (c < 0)
		{
			low = mid + 1;
		}
		else if (c > 0)
		{
			high = mid - 1;
		}
		else
		{
			pos = mid;
			goto Result;
		}
	}
	pos = -(low + 1);

	Result:
	{
		if (pos < 0)
		{
			return (-pos) - 1;
		}
		else
		{
			return -1;
		}
	}
}
/**
 * 没有校验destbuf的长度，假设恒比len和srcPhone的长度大
 */
size_t parserPhone(const uchar* srcPhone, char* destBuf, size_t len)
{
	if (destBuf == NULL || srcPhone == NULL || len == 0) return 0;
	size_t i = 0, j = 0;
	for (i = 0, j = 0; i < len; i++)
	{
		if (srcPhone[i] > 0xFF00 && (srcPhone[i] >= 0xFF10 && srcPhone[i] <= 0xFF19))
		{
			destBuf[j++] = '0' + (srcPhone[i] - 0xFF10);
			continue;
		}
		else if (srcPhone[i] >= '0' && srcPhone[i] <= '9')
		{
			destBuf[j++] = (char) srcPhone[i];
			continue;
		}
		switch (srcPhone[i])
		{
			case 0xFF0B:
			case '+':
				destBuf[j++] = '+';
				break;
			case 0xFF0D:
			case '-':
				destBuf[j++] = '-';
				break;
			case 0xFF03:
			case '#':
				destBuf[j++] = '#';
				break;
			case 0xFF0A:
			case '*':
				destBuf[j++] = '*';
				break;
			case 0x20: //space
			case 0x3000:
				break;
			default: //遇到不可解析的字符就当作分隔符了，进行返回
				return i + 1;
		}
	}
	return len;
}
/**
 * 返回值表示当前输入段是否完全符合phone的格式，如果符合将在destBuf中获得规整后的phone串
 */
boolean checkPhone(const uchar* srcPhone, char* destBuf, size_t len)
{
	if (destBuf == NULL || srcPhone == NULL || len == 0) return false;
	size_t i = 0, j = 0;
	boolean success = true;
	for (i = 0, j = 0; i < len; i++)
	{
		if (srcPhone[i] == 0x20 || srcPhone[i] == 0x3000) continue;
		if (srcPhone[i] > 0xFF00 && (srcPhone[i] >= 0xFF10 && srcPhone[i] <= 0xFF19))
		{
			destBuf[j++] = '0' + (srcPhone[i] - 0xFF10);
			continue;
		}
		else if (srcPhone[i] >= '0' && srcPhone[i] <= '9')
		{
			destBuf[j++] = (char) srcPhone[i];
			continue;
		}
		switch (srcPhone[i])
		{
			case 0xFF0B:
			case '+':
				destBuf[j++] = '+';
				break;
			case 0xFF0D:
			case '-':
				destBuf[j++] = '-';
				break;
			case 0xFF03:
			case '#':
				destBuf[j++] = '#';
				break;
			case 0xFF0A:
			case '*':
				destBuf[j++] = '*';
				break;
			case 0x20: //space
			case 0x3000:
				break;
			default:
				success = false;
				break;
		}
	}
	return success;
}

SearchData* newSearchData(const uchar* text, const int textLength, const int primaryKey, const ulong64 filter)
{
	SearchData* data = NULL;
	data = (SearchData*) malloc(SIZEOF_SEARCHDATA);
	data->primaryKey = primaryKey;
	data->filter = filter;
	data->wordCodes = NULL;
	data->sortKeys = NULL;
	data->phoneCount = 0;
	data->phones = NULL;
	data->isMatched = false;
	data->codesCount = 0;
	text2SearchData(text, textLength, data);
	return data;
}

void treeAddData(SearchTree* tree, const int primaryKey, const ulong64 filter, const uchar* text, const int textLength, const uchar* phoneNum, const int phoneLength)
{
	SearchData* data = newSearchData(text, textLength, primaryKey, filter);
	SearchPhone* phoneData = NULL;
	char* buf = NULL;
	int size = 0;
	int pos = 0;
	int i = 0, j = 0;
//插入时按照字的首字母进行排序
	pos = findStorageData2Insert(tree, data);
	if (pos < 0)
	{
		//已存在
		freeSearchData(data);
		free(data);
		return;
	}
	tree->searchDatas.insert(&(tree->searchDatas), (int) data, pos);

	if (phoneNum)
	{ //phoneNum存在
		size = 0;
		if (phoneLength > 0)
		{
			buf = (char*) malloc(phoneLength + 1);
			memset(buf, 0, phoneLength + 1);
			do
			{
				size = parserPhone(&phoneNum[i], buf, phoneLength - i);
				i += size;
				data->phoneCount++;
			}
			while (i < phoneLength);

			i = 0;
			j = 0;
			phoneData = (SearchPhone*) malloc(SIZEOF_SEARCHPHONE* data->phoneCount);
			do
			{
				memset(buf, 0, phoneLength + 1);
				size = parserPhone(&phoneNum[i], buf, phoneLength - i);
				i += size;
				size = cLength(buf);
				if (size > 32) size = 32;
				phoneData[j].phoneNum = (char*) malloc(size + 1);
				memcpy(phoneData[j].phoneNum, buf, size);
				phoneData[j].phoneNum[size] = '\0';
				phoneData[j].index = j;
				phoneData[j].phoneMatch = 0;
				tree->searchPhones.append(&(tree->searchPhones), (int) &phoneData[j]);
				j++;
			}
			while (i < phoneLength);
			data->phones = phoneData;
			free(buf);
		}
	}
}

void treeEndAdd(SearchTree* tree, int* outArray, int outLength)
{
	SearchData* data = NULL;
	int i = 0, j = 0;
	int size = (tree->searchDatas).size;

	for (i = 0; i < size; i++)
	{
		tree->searchDatas.get(&(tree->searchDatas), i, &data);
		if (i < outLength) *(outArray++) = data->primaryKey;
		data->primaryKey = i;
		for (j = 0; j < data->phoneCount; j++)
			data->phones[j].primaryKey = data->primaryKey;
	}
}

void add2Cached(SearchData* data, ArrayList* cache)
{
	int pos = findCached2Insert(cache, data);
	if (pos >= 0)
	{
		cache->insert(cache, data->primaryKey, pos);
	}
}

void treeBuildIndex(SearchTree* tree)
{
	SearchData* data = NULL;
	WordCode* wordCodes = NULL;
	ArrayList* cache = NULL;
	int i = 0, primaryKey = 0, j = 0, k = 0, pos = 0, charIndex = 0;
	int size = (tree->searchDatas).size;
	int pyCodeNum = 0, pyIndex = 0;
	for (i = 0; i < size; i++)
	{
		tree->searchDatas.get(&(tree->searchDatas), i, &data);
		primaryKey = data->primaryKey;
		for (k = 0; k < data->codesCount; k++)
		{
			wordCodes = &(data->wordCodes[k]);
			pyCodeNum = wordCodes->pyCodeNum & 0x07;
			if (pyCodeNum > 0)
			{
				for (j = 0; j < pyCodeNum; j++)
				{
					charIndex = PySpellCode[wordCodes->pyCodeIndex[j]][0] - 'a';
					cache = &(tree->firstSpellIndex[charIndex]);
					add2Cached(data, cache);
				}
			}
			else
			{
				// 不具有拼音码的字符
				if (wordCodes->wordUnicode >= 'a' && wordCodes->wordUnicode <= 'z')
				{
					charIndex = wordCodes->wordUnicode - 'a';
				}
				else if (wordCodes->wordUnicode >= '0' && wordCodes->wordUnicode <= '9')
				{
					charIndex = wordCodes->wordUnicode - '0' + LetterNum;
				}
				else
				{
					charIndex = CachedHitSymbol;
				}
				cache = &(tree->firstSpellIndex[charIndex]);
				add2Cached(data, cache);
			}
		}
	}
}

/*
 *	由于存在T9输入法,所以需要将字母映射到特定的数字上进行匹配
 */
char word2Digit(SearchTree* tree, const char src)
{
	int len = 0;
	int index = 0;
	char word = src;
	if (word >= 'A' && word <= 'Z')
	{
		word = word - CapsOff;
	}

	if (word >= 'a' && word <= 'z' && tree->matchFunc)
	{
		index = word - 'a';
		if (index >= 0)
		{
			word = tree->matchFunc[index];
		}
	}
	return word;
}

boolean compareWord(SearchTree* tree, const char src, const char input, boolean isT9)
{
	char word = src;
	if (input >= '0' && input <= '9' && isT9)
	{
		word = word2Digit(tree, word);
	}

	if (word == input)
	{
		return true;
	}

	return false;
}

int findCached2Insert(ArrayList* cache, SearchData* data)
{
	int pos = 0;
	int low = 0;
	int high = cache->size - 1;
	int primaryKey = 0;
	int mid = 0;
	while (low <= high)
	{
		mid = (low + high) >> 1;
		cache->get(cache, mid, &primaryKey);

		if (data->primaryKey > primaryKey) low = mid + 1;
		else if (data->primaryKey < primaryKey) high = mid - 1;
		else
		{
			pos = mid;
			goto Result;
		}
	}
	pos = -(low + 1);

	Result:
	{
		if (pos < 0) return (-pos) - 1; // 找不到期待插入的位置,pos将等于-1,
		else return -1;
	}
}

/*
 * wordCodes 第 wordIndex 字的第 pyCodeIndex拼音 的 第 charIndex字符 是否和input相符
 */
boolean isMatch(SearchTree* tree, WordCode* wordCodes, const short codesCount, const short wordIndex, const short pyCodeIndex, const short charIndex, uchar input, boolean isT9)
{
	const char *pyCode = NULL;
	int pyCodeNum = 0;
	WordCode* word = NULL;
	if (wordIndex >= codesCount) return false;

	word = &wordCodes[wordIndex];
	pyCodeNum = word->pyCodeNum & 0x07;
// 如果input为非ASCII字符，则直接比对Unicode码是否一致

	if (input & 0xFF00)
	{
		return word->wordUnicode == input ? true : false;
	}
//pyCodeIndex 为0时 且word 为非拼音字符 检查字符本身是否匹配
	if (pyCodeIndex == 0 && charIndex == 0 && pyCodeNum == 0)
	{
		return compareWord(tree, (char) word->wordUnicode, (char) input, isT9);
	}

	if (pyCodeIndex >= pyCodeNum)
	{
		return false;
	}

	pyCode = PySpellCode[word->pyCodeIndex[pyCodeIndex]];

	if (!pyCode[charIndex])
	{
		return false;
	}

// 最后检查对应字母是否匹配
	return compareWord(tree, pyCode[charIndex], (char) input, isT9);
}

int findResult2Insert(SearchTree* tree, ArrayList* list, SearchResult* result)
{
	int low = 0;
	int high = list->size - 1;
	int mid = 0;
	int bc = 0;
	int i = 0;
	int pos = 0;
	int wPyCodeNumKey = 0;
	int wPyCodeNumMid = 0;
	SearchPos* tmpKey = NULL;
	SearchPos* tmpMid = NULL;
	SearchPos* ftmpKey = NULL;
	SearchPos* ftmpMid = NULL;
	SearchData* dtmpKey = NULL;
	SearchData* dtmpMid = NULL;
	WordCode* wtmpKey = NULL;
	WordCode* wtmpMid = NULL;
	SearchResult* midVal = 0;
	while (low <= high)
	{
		mid = (low + high) >> 1;
		list->get(list, mid, &midVal);
		ftmpKey = result->headMatchPos;
		ftmpMid = midVal->headMatchPos;

		bc = (ftmpMid->pos & 0xFFC0) - (ftmpKey->pos & 0xFFC0); //比较第一匹配位置
		tmpKey = ftmpKey->child;
		tmpMid = ftmpMid->child;
		//由于Key串决定了SearchPos的总长 所以无需校验 tmpKey|tmpMid 中有任一为NULL另一方不为NULL的情形
		while (bc == 0)
		{
			if (tmpKey != NULL && tmpMid != NULL)
			{
				bc = (tmpKey->pos & 0xFFC0) - (tmpMid->pos & 0xFFC0); //逐一比较匹配位置 ,匹配的深度优先排前
				tmpKey = tmpKey->child;
				tmpMid = tmpMid->child;
			}
			else
			{
				break;
			}
		}
		if (bc == 0)
		{
			//被匹配到的wordCode位置一致
			tree->searchDatas.get(&(tree->searchDatas), result->primaryKey, &dtmpKey);
			tree->searchDatas.get(&(tree->searchDatas), midVal->primaryKey, &dtmpMid);
			wtmpKey = &(dtmpKey->wordCodes[ftmpKey->pos >> 6]);
			wtmpMid = &(dtmpMid->wordCodes[ftmpMid->pos >> 6]);
			wPyCodeNumKey = wtmpKey->pyCodeNum;
			wPyCodeNumMid = wtmpMid->pyCodeNum;
			if ((wPyCodeNumKey & 0x07) > 0 && (wPyCodeNumMid & 0x07) > 0)
			{
				bc = PySpellCode[wtmpMid->pyCodeIndex[(ftmpMid->pos >> 3) & 0x07]][ftmpMid->pos & 0x07]
						- PySpellCode[wtmpKey->pyCodeIndex[(ftmpKey->pos >> 3) & 0x07]][ftmpKey->pos & 0x07];
			}
			else if ((wPyCodeNumKey & 0x07) == 0 && (wPyCodeNumMid & 0x07) == 0)
			{
				bc = wtmpMid->wordUnicode - wtmpKey->wordUnicode;
			}
			else if ((wPyCodeNumKey & 0x07) != 0 && (wPyCodeNumMid & 0x07) == 0)
			{
				bc = wtmpMid->wordUnicode - PySpellCode[wtmpKey->pyCodeIndex[(ftmpKey->pos >> 3) & 0x07]][ftmpKey->pos & 0x07];
			}
			else if ((wPyCodeNumKey & 0x07) == 0 && (wPyCodeNumMid & 0x07) != 0)
			{
				bc = PySpellCode[wtmpMid->pyCodeIndex[(ftmpMid->pos >> 3) & 0x07]][ftmpMid->pos & 0x07] - wtmpKey->wordUnicode;
			}

			if (bc == 0)
			{
				tmpKey = ftmpKey->child;
				tmpMid = ftmpMid->child;
				while (bc == 0)
				{
					if (tmpKey != NULL && tmpMid != NULL)
					{
						wtmpKey = &(dtmpKey->wordCodes[tmpKey->pos >> 6]);
						wtmpMid = &(dtmpMid->wordCodes[tmpMid->pos >> 6]);
						wPyCodeNumKey = wtmpKey->pyCodeNum;
						wPyCodeNumMid = wtmpMid->pyCodeNum;
						if ((wPyCodeNumKey & 0x07) > 0 && (wPyCodeNumMid & 0x07) > 0)
						{
							bc = -wtmpKey->pyCodeIndex[(tmpKey->pos >> 3) & 0x07] + wtmpMid->pyCodeIndex[(tmpMid->pos >> 3) & 0x07];
						}
						else if ((wPyCodeNumKey & 0x07) == 0 && (wPyCodeNumMid & 0x07) == 0)
						{
							bc = -wtmpKey->wordUnicode + wtmpMid->wordUnicode;
						}
						else if ((wPyCodeNumKey & 0x07) != 0 && (wPyCodeNumMid & 0x07) == 0)
						{
							bc = -PySpellCode[wtmpKey->pyCodeIndex[(tmpKey->pos >> 3) & 0x07]][tmpKey->pos & 0x07] + wtmpMid->wordUnicode;
						}
						else if ((wPyCodeNumKey & 0x07) == 0 && (wPyCodeNumMid & 0x07) != 0)
						{
							bc = -wtmpKey->wordUnicode + PySpellCode[wtmpMid->pyCodeIndex[(tmpMid->pos >> 3) & 0x07]][tmpMid->pos & 0x07];
						}
						tmpKey = tmpKey->child;
						tmpMid = tmpMid->child;
					}
					else
					{
						break;
					}
				}
			}
		}
		if (bc == 0)
		{
			bc = midVal->primaryKey - result->primaryKey;
		}
		if (bc < 0) //mid < key
		low = mid + 1;
		else if (bc > 0) //mid > key
		high = mid - 1;
		else
		{
			pos = mid; // key found
			goto Result;
		}

	}
	pos = -(low + 1); // key not found.

	Result:
	{
		if (pos < 0)
		{
			return (-pos) - 1; // 找不到期待插入的位置,pos将等于-1,
		}
		else
		{
			return -1;
		}
	}
}

/*
 * @param SearchData* data 当前遍历到的SearchData
 * @param WordCode* searchWord 输入的搜索key串
 * @param const short searchCount 输入的搜索key串的长度
 * @return 匹配到的首个SearchPos* 的值
 */
int isHit(SearchTree* tree, SearchData* data, WordCode* searchWord, const short searchCount, boolean isT9)
{
	LinkedList* posQueue = &(tree->searchPosQueue);
	WordCode* srcCodes = data->wordCodes;
	short srcLen = data->codesCount;
	short searchLen = srcLen > searchCount ? searchCount : srcLen;
	WordCode code = searchWord[0];
	int wordIndex = 0;
	int pyIndex = 0;
	int charIndex = 0;
	int pWordIndex = 0;
	int pPyIndex = 0;
	int pCharIndex = 0;
	int pyCodeNum = 0;
	WordCode codeT;
	ubyte wordPyCount = 0;

	SearchPos* pos = NULL;
	SearchPos* nextPos = NULL;
	SearchPos* matchPos = NULL;

	/*----------------------------------------------------------------------------*/
	if (searchCount == 0)
	{
		return false;
	}

	/*----------------------------------------------------------------------------*/
	/*
	 * 判断搜索串的第一个输入
	 * 在本data数据中各项匹配位置
	 * 并加入到posQueue中
	 * FIFO
	 */
	charIndex = 0; //首字母位置
	for (wordIndex = 0; wordIndex < srcLen; wordIndex++)
	{
		codeT = srcCodes[wordIndex];
		pyCodeNum = codeT.pyCodeNum & 0x07;
		wordPyCount = pyCodeNum > 0 ? pyCodeNum : 1;
		for (pyIndex = 0; pyIndex < wordPyCount; pyIndex++)
		{
			if (isMatch(tree, srcCodes, srcLen, wordIndex, pyIndex, charIndex, code.wordUnicode, isT9))
			{
				pos = (SearchPos*) addPosCache((wordIndex << 6) | (pyIndex << 3) | charIndex, NULL, 1);
				posQueue->append(posQueue, (int) pos);
			}
		}
	}

	/*----------------------------------------------------------------------------*/
// 对于后续的字符，从当前搜索位置开始继续查找可能的匹配项
	while (true)
	{
		if (posQueue->size == 0)
		{
			//没有找到任何匹配项
			break;
		}
		posQueue->removeAt(posQueue, 0, &pos);
		if (pos->step == searchCount)
		{
			//搜索步长已经抵达 Key串的总长
			matchPos = pos;
			break;
		}
		code = searchWord[pos->step]; //Key串中的下一个Key
		pWordIndex = pos->pos >> 6;
		pPyIndex = (pos->pos & 0x3F) >> 3;
		pCharIndex = pos->pos & 0x07;

		//下一个字
		wordIndex = pWordIndex + 1;
		charIndex = 0; //首字母位置
		if (wordIndex < srcLen)
		{ //本data中需要判断的字的位置 未抵达末尾
			codeT = srcCodes[wordIndex];
			pyCodeNum = codeT.pyCodeNum & 0x07;
			wordPyCount = pyCodeNum > 0 ? pyCodeNum : 1;
			for (pyIndex = 0; pyIndex < wordPyCount; pyIndex++)
			{
				if (isMatch(tree, srcCodes, srcLen, wordIndex, pyIndex, charIndex, code.wordUnicode, isT9))
				{
					nextPos = (SearchPos*) addPosCache((wordIndex << 6) | (pyIndex << 3) | charIndex, pos, pos->step + 1);
					posQueue->append(posQueue, (int) nextPos);
				}
			}
		}
		//当前字的拼音字符的下一个
		if ((code.pyCodeNum & 0x07) == 0)
		{
			wordIndex = pWordIndex;
			pyIndex = pPyIndex;
			charIndex = pCharIndex + 1;
			if (isMatch(tree, srcCodes, srcLen, wordIndex, pyIndex, charIndex, code.wordUnicode, isT9))
			{
				nextPos = (SearchPos*) addPosCache((wordIndex << 6) | (pyIndex << 3) | charIndex, pos, pos->step + 1);
				posQueue->append(posQueue, (int) nextPos);
			}
		}
	}
	posQueue->clear(posQueue);
	return (int) matchPos;
}

void add2Result(SearchTree* tree, SearchResult *result, ArrayList* cache)
{
	int pos = findResult2Insert(tree, cache, result);
	if (pos >= 0)
	{
		cache->insert(cache, (int) result, pos);
	}
}
void add2SearchPhone(SearchTree* tree, SearchPhone* toSearch, LinkedList* cache)
{
	cache->append(cache, (int) toSearch);
}

void add2PhoneResult(SearchTree* tree, SearchPhone* matchedPhone, ArrayList* cache)
{
	int low = 0;
	int high = cache->size - 1;
	int mid = 0;
	int pos = 0;
	int primaryKey = 0;
	SearchPhone* midSearchPhone = NULL;
	int midValue = 0;
	int bc = 0;
	while (low <= high)
	{
		mid = (low + high) >> 1;
		cache->get(cache, mid, &midSearchPhone);
		midValue = midSearchPhone->phoneMatch;
		primaryKey = matchedPhone->primaryKey;
		//由于匹配是连续型字段,并且不会出现多个匹配不符合的情况
		bc = midValue - matchedPhone->phoneMatch;
		if (bc == 0)
		{
			bc = midSearchPhone->primaryKey - primaryKey;
		}
		if (bc < 0)
		{
			low = mid + 1;
		}
		else if (bc > 0)
		{
			high = mid - 1;
		}
		else
		{
			pos = mid;
			goto Result;
		}
	}
	pos = -(low + 1);
	Result:
	{
		if (pos < 0)
		{
			pos = (-pos) - 1;
			if (pos >= 0)
			{
				cache->insert(cache, (int) matchedPhone, pos);
			}
		}
	}
}

/**
 * 寻找结果集是否包含关键字
 * 和C当中的strstr/strchr方法相当
 */
boolean isMatchByKmp(const char* srcText, const char* keyText, int* phoneMatchPos)
{
	int i, j;
	int len1 = 0;
	int len2 = 0;
	int p1 = 0;
	int p2 = 0;
	int k = 0;
	iKmpBuf[0] = 0;
	memset(&iKmpBuf, 0, 32);
	j = 0;
	len1 = cLength(keyText);
	for (i = 1; i < len1; i++)
	{
		while (keyText[j] != keyText[i] && j > 0)
			j = iKmpBuf[j - 1];
		if (keyText[j] == keyText[i]) j++;
		iKmpBuf[i] = j;
	}

	j = 0;
	len2 = cLength(srcText);
	for (i = 0; i < len2; i++)
	{
		while (keyText[j] != srcText[i] && j > 0)
		{
			j = iKmpBuf[j - 1];
		}

		if (keyText[j] == srcText[i])
		{
			j++;
		}
		if (j >= len1)
		{
			if (phoneMatchPos)
			{
				p1 = i - j + 1;
				p2 = p1 + j;
				*phoneMatchPos = 0;
				for (k = p1; k < p2; k++)
				{
					*phoneMatchPos = *phoneMatchPos | (1 << k);
				}
			}
			return true; //i - j +1; 匹配定位
		}
	}

	return false;
}

void searchCachedHit(SearchTree* tree, WordCode *word, ArrayList **desCacheList, boolean* needFreeCache)
{
	int i = 0;
	int j = 0;
	int len = 0;
	int value = 0;
	int charIndex;
	ArrayList* cache = NULL;
	int primaryKey = 0;
	int pyCodeNum = word->pyCodeNum & 0x07;
	int pos = 0;
	SearchData* data = NULL;

	if ((word->wordUnicode & 0xFF00) > 0)
	{ //编码集为Unicode部分
		if (pyCodeNum > 0)
		{
			// 有拼音码的汉字,只取首拼音项
			charIndex = PySpellCode[word->pyCodeIndex[0]][0] - 'a'; // 获取字符拼音码的首字母
			*desCacheList = &tree->firstSpellIndex[charIndex];
		}
		else
		{
			//无拼音编码的汉字在treeBuildIndex方法中已被归入特殊字符
			//输入为特殊字符
			*desCacheList = &tree->firstSpellIndex[CachedHitSymbol];
		}
	}
	else
	{
// 无拼音码
		charIndex = 0;

		if (word->wordUnicode >= 'a' && word->wordUnicode <= 'z')
		{
			// ASCII字母
			charIndex = word->wordUnicode - 'a'; // 获取字符拼音码的首字母
		}
		else if (word->wordUnicode >= '0' && word->wordUnicode <= '9')
		{
			charIndex = word->wordUnicode - '0' + LetterNum;
		}
		else
		{
			//Ascii 中的非字母、数字
			charIndex = CachedHitSymbol; // 获取字符拼音码的首字母
		}

		cache = &tree->firstSpellIndex[charIndex];

//数字键盘 与字母对应
		if (word->wordUnicode >= '0' && word->wordUnicode <= '9' && tree->matchFunc)
		{
			len = cLength(tree->matchFunc);
			if (len <= 0)
			{
				*desCacheList = cache;
				return;
			}
			*desCacheList = (ArrayList*) malloc(SIZEOF_ARRAYLIST);
			*needFreeCache = true;
			initArrayCapacity(*desCacheList, cache->maxCapacity << 1);
			for (i = 0; i < cache->size; i++)
			{
				cache->get(cache, i, &value);
				(*desCacheList)->append(*desCacheList, value);
			}

			for (i = 0; i < len; i++)
			{
				if (tree->matchFunc[i] != word->wordUnicode)
				{
					continue;
				}

				cache = &tree->firstSpellIndex[i];
				for (j = 0; j < cache->size; j++)
				{
					cache->get(cache, j, &primaryKey);
					tree->searchDatas.get(&tree->searchDatas, primaryKey, &data);
					pos = findCached2Insert(*desCacheList, data);
					if (pos >= 0)
					{
						//不存在
						(*desCacheList)->insert(*desCacheList, primaryKey, pos);
					}
				}
			}
		}
		else
		{
			*desCacheList = cache;
		}
	}
}

int intsCmp(int* src, int srcLen, int* target, int tarLen)
{
	int length = srcLen > tarLen ? tarLen : srcLen;
	int i = 0, v = 0;
	for (i = 0; i < length; i++)
	{
		v = src[i] < target[i] ? -1 : src[i] > target[i] ? 1 : 0;
		if (v != 0)
		{
			return v;
		}
	}
	return srcLen < tarLen ? -1 : srcLen > tarLen ? 1 : 0;
}

void treeSearch(SearchTree* tree, const uchar* keyText, const int keyLength, LinkedList* lastSearched, ArrayList* desNameMatchHits, ArrayList* desPhoneMatchHits, int resultLimit,
		ulong64 filter, boolean isT9)
{
	SearchData* keySearchData = tree->keySearchData;
	SearchData *dataToMatch = NULL;
	SearchPhone* phoneToMatch = NULL;
	SearchData* phoneData = NULL;
	ArrayList** cachedHits = NULL; //为了避免跨进程访问时带来的问题 此处不将其生成全局变量使用
	ArrayList* cache = NULL;
	LinkedList* pCache = &(tree->searchPhones);
	boolean needFreeCache = false;
	boolean allDigit = true;
	SearchPos* matchPos = NULL;
	SearchPos* mFatherPos = NULL;
	SearchResult* sResult = NULL;
	char* phoneNum;
	int i = 0, j = 0;
	int primaryKey = 0;
	int keyLen = 0;
	int value = 0;
	/*----------------------------------------------------------------------------*/
	/**
	 * 构建搜索串的SearchData
	 */
	if (keySearchData != NULL) freeSearchData(keySearchData);
	else
	{
		keySearchData = (SearchData*) malloc(SIZEOF_SEARCHDATA);
		keySearchData->wordCodes = NULL;
		keySearchData->sortKeys = NULL;
		keySearchData->phones = NULL;
		tree->keySearchData = keySearchData;
	}

	if (keyText)
	{
		text2SearchData(keyText, keyLength, keySearchData);

		keyLen = keySearchData->codesCount;
		if (keyLen > 32) keyLen = 32;
		phoneNum = (char*) malloc(keyLen + 1);
		memset(phoneNum, 0, keyLen + 1);
		allDigit = checkPhone(keyText, phoneNum, keyLen);
	}

	/*----------------------------------------------------------------------------*/
	//获取缓存搜索集
	if (lastSearched == NULL)
	{
		cachedHits = (ArrayList**) malloc(sizeof(ArrayList*));
		searchCachedHit(tree, &keySearchData->wordCodes[0], cachedHits, &needFreeCache);
		cache = *cachedHits;
		free(cachedHits);
	}
	else
	{
		cache = (ArrayList*) malloc(SIZEOF_ARRAYLIST);
		needFreeCache = true;
		initArrayCapacity(cache, 256);
		for (i = 0; i < lastSearched->size; i++)
		{
			lastSearched->get(lastSearched, i, &value);
			cache->append(cache, value);
		}
		cachedHits = &cache;
	}

	/*----------------------------------------------------------------------------*/
	if (keySearchData->codesCount >= 1)
	{
		for (i = 0; i < cache->size; i++)
		{
			cache->get(cache, i, &primaryKey);
			tree->searchDatas.get(&tree->searchDatas, primaryKey, &dataToMatch);
			if (filter != 0 && (dataToMatch->filter & filter) == 0 && dataToMatch->filter != 0) continue;
			matchPos = (SearchPos*) isHit(tree, dataToMatch, keySearchData->wordCodes, keySearchData->codesCount, isT9);
			if (matchPos != NULL)
			{
				do
				{
					mFatherPos = matchPos->father;
					if (mFatherPos != NULL)
					{
						mFatherPos->child = matchPos;
						matchPos = mFatherPos;
					}
				}
				while (mFatherPos != NULL );
				sResult = (SearchResult*) addResultCache(matchPos, primaryKey);
				add2Result(tree, sResult, desNameMatchHits);
				dataToMatch->isMatched = true;
			}
		}

		for (i = 0; i < desNameMatchHits->size; i++)
		{
			desNameMatchHits->get(desNameMatchHits, i, &sResult);
			primaryKey = sResult->primaryKey;
			desNameMatchHits->set(desNameMatchHits, primaryKey, i);
		}

		clearPosCache();
		clearResultCache();
	}
	else
	{ //取缓冲里的值作为匹配项进行结果输出.
		for (i = 0; i < cache->size; i++)
		{
			cache->get(cache, i, &value);
			desNameMatchHits->append(desNameMatchHits, value);
		}
	}

	/*----------------------------------------------------------------------------*/

	if (needFreeCache)
	{
		cache->dispose(cache);
		free(cache);
	}
	cache = NULL;
	cachedHits = NULL;
	/*----------------------------------------------------------------------------*/
	if (desPhoneMatchHits && allDigit && keyLength > 0)
	{
		//搜索号码匹配

		if (pCache->size > 0)
		{
			for (pCache->iterator(pCache); pCache->hasNext(pCache);)
			{
				phoneToMatch = (SearchPhone*) pCache->next(pCache);
				tree->searchDatas.get(&(tree->searchDatas), phoneToMatch->primaryKey, &phoneData);
				if (cLength(phoneToMatch->phoneNum) < keyLen || phoneData->isMatched || (filter != 0 && (phoneData->filter & filter) == 0 && phoneData->filter != 0)) continue;
				//号码匹配
				if (isMatchByKmp(phoneToMatch->phoneNum, phoneNum, &phoneToMatch->phoneMatch))
				{
					add2PhoneResult(tree, phoneToMatch, desPhoneMatchHits);
				}
			}
		}
	}
	for (i = 0; i < desNameMatchHits->size; i++)
	{
		desNameMatchHits->get(desNameMatchHits, i, &primaryKey);
		tree->searchDatas.get(&tree->searchDatas, primaryKey, &dataToMatch);
		dataToMatch->isMatched = false;
	}
	if (phoneNum) free(phoneNum);
}

/*
 * @param keyText 搜索Key应少于18个字符.
 * @return boolean 是否有搜索到新的结果集.否将不进行结果集的传递与更新
 */
void getPhoneHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen,
		uchar ** result)
{
	SearchData* dataToSearch = NULL;
	SearchPhone* phoneToSearch = NULL;
	char* phoneNum = NULL;
	int length = 0;
	int i = 0, j = 0, k = 0, m = 0, n = 0, t = 0;
	int primaryKey = 0;
	WordCode* wordCode = NULL;
	ListData* node = NULL;
	LinkedList charsCache;

	if (keyText == NULL) return;
	/*----------------------------------------------------------------------------*/
	phoneNum = (char*) malloc(keyLength + 1);
	memset(phoneNum, 0, keyLength + 1);
	if (!checkPhone(keyText, phoneNum, keyLength))
	{
		free(phoneNum);
		return;
	}
	initLinked(&charsCache);

	for (i = 0; i < count; i++)
	{
		primaryKey = lastSearched[i] >> 8;
		tree->searchDatas.get(&tree->searchDatas, primaryKey, &dataToSearch);
		t = lastSearched[i] & 0xFF;
		if (t == 0xFF) continue;
		phoneToSearch = &(dataToSearch->phones[t]);
		if (isMatchByKmp(phoneToSearch->phoneNum, phoneNum, &phoneToSearch->phoneMatch))
		{
			for (k = 0, m = -1, n = -1, t = cLength(phoneToSearch->phoneNum); k < 32; k++)
			{
				if ((phoneToSearch->phoneMatch & (1 << k)) > 0 && m < 0)
				{ //  首次匹配到关键字 加入着色段 标记m
					m = k;
					for (j = 0; j < dyeStrLen; j++)
					{
						charsCache.append(&charsCache, dyeStr[j]);
					}
				}
				else if ((phoneToSearch->phoneMatch & (1 << k)) == 0 && n < 0 && m >= 0)
				{ // 在匹配到关键字之后第一次发现非匹配字,插入</font>结束符
					charsCache.append(&charsCache, '<');
					charsCache.append(&charsCache, '/');
					charsCache.append(&charsCache, 'f');
					charsCache.append(&charsCache, 'o');
					charsCache.append(&charsCache, 'n');
					charsCache.append(&charsCache, 't');
					charsCache.append(&charsCache, '>');
					n = k;
				}
				if (k < t) charsCache.append(&charsCache, phoneToSearch->phoneNum[k]);
			}
		}

		result[i] = (uchar*) malloc((charsCache.size + 1) * sizeof(char));
		node = charsCache.first;
		for (j = 0; j < charsCache.size; j++)
		{
			result[i][j] = (char) node->pData;
			node = node->next;
		}
		result[i][charsCache.size] = '\0';
		charsCache.clear(&charsCache);
	}
	clearPosCache();
	charsCache.dispose(&charsCache);
	if (phoneNum) free(phoneNum);
}

void concatStr(LinkedList* charsCache, char* str)
{
	size_t len = 0;
	size_t i = 0;
	while (str[len] != '\0')
		len++;
	for (i = 0; i < len; i++)
		charsCache->append(charsCache, str[i]);
}
/*
 *	@param dyeStr <font color=#D64206>
 */
void getHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen, uchar ** result,
		boolean isT9)
{
	SearchData* keySearchData = tree->keySearchData;
	SearchData* dataToSearch = NULL;
	SearchPhone* phoneToSearch = NULL;
	int i = 0, j = 0, k = 0, m = 0, n = 0, p = 0, t = 0;
	int length = 0;
	int primaryKey = 0;
	SearchPos* matchPos = NULL;
	SearchPos* mFatherPos = NULL;
	WordCode* wordCode = NULL;
	ListData* node = NULL;
	ListData* node2 = NULL;
	char* phoneNum = NULL;
	int wordPos = 0;
	int wordIndex = 0;
	int pyIndex = 0;
	int charIndex = 0;
	char pyChar = 0;
	char nextPyChar = 0;
	boolean allDigit = true;
	boolean dyeOpen = false;
	boolean dyeOpenPy = false;
	LinkedList charsCache;
	LinkedList pyCache;
	LinkedList phoneCache;
	/*----------------------------------------------------------------------------*/
	if (keyText == NULL) return;
	if (keySearchData != NULL) freeSearchData(keySearchData);
	else
	{
		keySearchData = (SearchData*) malloc(SIZEOF_SEARCHDATA);
		keySearchData->wordCodes = NULL;
		keySearchData->sortKeys = NULL;
		keySearchData->phones = NULL;
		tree->keySearchData = keySearchData;
	}
	initLinked(&charsCache);
	initLinked(&pyCache);
	initLinked(&phoneCache);
	text2SearchData(keyText, keyLength, keySearchData);

	length = keySearchData->codesCount;
	if (length > 32) length = 32;
	phoneNum = (char*) malloc(length + 1);
	memset(phoneNum, 0, length + 1);
	allDigit = checkPhone(keyText, phoneNum, length);

	for (i = 0; i < count; i++)
	{
		primaryKey = lastSearched[i] >> 8;
		tree->searchDatas.get(&tree->searchDatas, primaryKey, &dataToSearch);
		matchPos = (SearchPos*) isHit(tree, dataToSearch, keySearchData->wordCodes, keySearchData->codesCount, isT9);
		if (matchPos != NULL)
		{
			do
			{ //获取到匹配项构建路径序列
				mFatherPos = matchPos->father;
				if (mFatherPos != NULL)
				{
					mFatherPos->child = matchPos;
					matchPos = mFatherPos;
				}
			}
			while (mFatherPos != NULL ); //matchPos已指向最开始匹配的SearchPos
		}
		else
		{ //此项匹配应为号码匹配项
			t = lastSearched[i] & 0xFF;
			if (t == 0xFF) continue; // 编号-1 名字也没匹配，此项数据已错误，跳过
			phoneToSearch = &dataToSearch->phones[t];
			if (isMatchByKmp(phoneToSearch->phoneNum, phoneNum, &phoneToSearch->phoneMatch))
			{ //寻找
				for (k = 0, m = -1, n = -1, t = cLength(phoneToSearch->phoneNum); k < 32; k++)
				{
					if ((phoneToSearch->phoneMatch & (1 << k)) > 0 && m < 0)
					{ //  首次匹配到关键字 加入着色段 标记m
						m = k;
						for (j = 0; j < dyeStrLen; j++)
						{
							phoneCache.append(&phoneCache, dyeStr[j]);
						}
					}
					else if ((phoneToSearch->phoneMatch & (1 << k)) == 0 && n < 0 && m >= 0)
					{ // 在匹配到关键字之后第一次发现非匹配字,插入</font>结束符
						phoneCache.append(&phoneCache, '<');
						phoneCache.append(&phoneCache, '/');
						phoneCache.append(&phoneCache, 'f');
						phoneCache.append(&phoneCache, 'o');
						phoneCache.append(&phoneCache, 'n');
						phoneCache.append(&phoneCache, 't');
						phoneCache.append(&phoneCache, '>');
						n = k;
					}
					if (k < t) phoneCache.append(&phoneCache, phoneToSearch->phoneNum[k]);
				}
				goto DyePhone;
			}
		}
		for (j = 0, k = 0, pyIndex = 0; j < dataToSearch->codesCount; j++, k++)
		{
			wordCode = &(dataToSearch->wordCodes[j]);
			wordIndex = wordCode->pyCodeNum >> 3;
			while (k < wordIndex)
			{ //当前字位置如果超越K的位置需要向charCache中填充空格 ->0x20
				charsCache.append(&charsCache, 0x20);
				pyCache.append(&pyCache, 0x20);
				k++;
			}

			if (matchPos != NULL)
			{
				wordPos = matchPos->pos >> 6; // matchPos匹配到的字位置,是SearchData 中的第wordPos字
				pyIndex = (matchPos->pos >> 3) & 0x07;
				charIndex = matchPos->pos & 0x07;

				if (wordPos == j)
				{ //本字被匹配
					if (!dyeOpen)
					{
						dyeOpen = true;
						for (n = 0; n < dyeStrLen; n++)
						{
							charsCache.append(&charsCache, dyeStr[n]);
						}
					}
					charsCache.append(&charsCache, wordCode->srcUnicode);

					if (!dyeOpenPy && !dataToSearch->isNameAllDigit)
					{
						dyeOpenPy = true;
						for (n = 0; n < dyeStrLen; n++)
						{
							pyCache.append(&pyCache, dyeStr[n]);
						}
					}
					if ((wordCode->pyCodeNum & 0x07) > 0)
					{ //存在拼音

						for (m = 0;; m++)
						{
							pyChar = PySpellCode[wordCode->pyCodeIndex[pyIndex]][m];
							nextPyChar = PySpellCode[wordCode->pyCodeIndex[pyIndex]][m + 1];
							if (m == 0) pyChar = pyChar - CapsOff; //对首字母进行大写
							if (m == charIndex)
							{
								matchPos = matchPos->child; //此匹配过程已被染色过程消耗掉,消耗拼音过程
								if (matchPos != NULL && (matchPos->pos >> 6) == wordPos) charIndex = matchPos->pos & 0x07; //由于同一次匹配过程中不可能出现相同字的不同多音字不被匹配的情形,所以此处不再继续讨论pyIndex是否相等
							}
							else if (dyeOpenPy)
							{
								pyCache.append(&pyCache, '<');
								pyCache.append(&pyCache, '/');
								pyCache.append(&pyCache, 'f');
								pyCache.append(&pyCache, 'o');
								pyCache.append(&pyCache, 'n');
								pyCache.append(&pyCache, 't');
								pyCache.append(&pyCache, '>');
								dyeOpenPy = false;
							}
							pyCache.append(&pyCache, pyChar);
							if (nextPyChar == '\0') break;
						}

					}
					else
					{
						//所有字符都是"数字" 将不计入pyCache
						if (!dataToSearch->isNameAllDigit) pyCache.append(&pyCache, wordCode->srcUnicode); //非汉字直接将原文加入pyCache
						matchPos = matchPos->child;
					}
					continue;
				}
				else
				//本字非匹配
				goto NoMatch;
			}

			NoMatch:
			{
				if (dyeOpen)
				{ //当匹配到一个非匹配字时完成<font></font>标签关闭
					charsCache.append(&charsCache, '<');
					charsCache.append(&charsCache, '/');
					charsCache.append(&charsCache, 'f');
					charsCache.append(&charsCache, 'o');
					charsCache.append(&charsCache, 'n');
					charsCache.append(&charsCache, 't');
					charsCache.append(&charsCache, '>');
					dyeOpen = false;
				}
				charsCache.append(&charsCache, wordCode->srcUnicode);
				if (dyeOpenPy)
				{
					pyCache.append(&pyCache, '<');
					pyCache.append(&pyCache, '/');
					pyCache.append(&pyCache, 'f');
					pyCache.append(&pyCache, 'o');
					pyCache.append(&pyCache, 'n');
					pyCache.append(&pyCache, 't');
					pyCache.append(&pyCache, '>');
					dyeOpenPy = false;
				}
				if ((wordCode->pyCodeNum & 0x07) > 0)
				{ //存在拼音
					for (m = 0;; m++)
					{
						pyChar = PySpellCode[wordCode->pyCodeIndex[0]][m];
						nextPyChar = PySpellCode[wordCode->pyCodeIndex[0]][m + 1];
						if (m == 0) pyChar = pyChar - CapsOff; //对首字母进行大写
						pyCache.append(&pyCache, pyChar);
						if (nextPyChar == '\0') break;
					}

				}
				else if (!dataToSearch->isNameAllDigit) //所有字符都是"数字" 将不计入pyCache
				{
					pyCache.append(&pyCache, wordCode->srcUnicode); //非汉字直接将原文加入pyCache
				}
			} //end NoMatch
		}
		if (dyeOpen)
		{
			charsCache.append(&charsCache, '<');
			charsCache.append(&charsCache, '/');
			charsCache.append(&charsCache, 'f');
			charsCache.append(&charsCache, 'o');
			charsCache.append(&charsCache, 'n');
			charsCache.append(&charsCache, 't');
			charsCache.append(&charsCache, '>');
			dyeOpen = false;
		}
		if (dyeOpenPy)
		{
			pyCache.append(&pyCache, '<');
			pyCache.append(&pyCache, '/');
			pyCache.append(&pyCache, 'f');
			pyCache.append(&pyCache, 'o');
			pyCache.append(&pyCache, 'n');
			pyCache.append(&pyCache, 't');
			pyCache.append(&pyCache, '>');
			dyeOpenPy = false;
		}
		//染色结束
		DyeName:
		{
			if (pyCache.size > 0)
			{
				pyCache.insert(&pyCache, '>', 0);
				pyCache.insert(&pyCache, 'l', 0);
				pyCache.insert(&pyCache, 'l', 0);
				pyCache.insert(&pyCache, 'a', 0);
				pyCache.insert(&pyCache, 'm', 0);
				pyCache.insert(&pyCache, 's', 0);
				pyCache.insert(&pyCache, '<', 0);
				pyCache.append(&pyCache, '<');
				pyCache.append(&pyCache, '/');
				pyCache.append(&pyCache, 's');
				pyCache.append(&pyCache, 'm');
				pyCache.append(&pyCache, 'a');
				pyCache.append(&pyCache, 'l');
				pyCache.append(&pyCache, 'l');
				pyCache.append(&pyCache, '>');

				// +1 +1 空格 和 结尾
				result[i] = (uchar*) malloc((charsCache.size + 1 + pyCache.size + 1) * sizeof(uchar));

				node = charsCache.first;
				for (j = 0; j < charsCache.size; j++)
				{
					result[i][j] = (uchar) node->pData;
					node = node->next;
				}

				result[i][j++] = (uchar) 0x20;

				node = pyCache.first;
				for (p = 0; p < pyCache.size; p++, j++)
				{
					result[i][j] = (uchar) node->pData;
					node = node->next;
				}

				// + 1为空格
				result[i][charsCache.size + pyCache.size + 1] = '\0';
			}
			else
			{
				// +1 结尾
				result[i] = (uchar*) malloc((charsCache.size + 1) * sizeof(uchar));
				node = charsCache.first;
				for (j = 0; j < charsCache.size; j++)
				{
					result[i][j] = (uchar) node->pData;
					node = node->next;
				}
				result[i][charsCache.size] = '\0';
			}
			charsCache.clear(&charsCache);
			pyCache.clear(&pyCache);
			continue;
		} //end DyName
		DyePhone:
		{
			result[i] = (uchar*) malloc((phoneCache.size + 1) * sizeof(uchar));
			node = phoneCache.first;
			for (j = 0; j < phoneCache.size; j++)
			{
				result[i][j] = (uchar) node->pData;
				node = node->next;
			}
			result[i][phoneCache.size] = '\0';
			phoneCache.clear(&phoneCache);
		} //end DyPhone
	}
	clearPosCache();
	charsCache.dispose(&charsCache);
	pyCache.dispose(&pyCache);
	phoneCache.dispose(&phoneCache);
	if (phoneNum) free(phoneNum);
}
/*
 *	@param dyeStr <font color=#D64206>
 */
void getNameHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen,
		uchar ** result, boolean isT9)
{
	SearchData* keySearchData = tree->keySearchData;
	SearchData* dataToSearch = NULL;
	int i = 0, j = 0, k = 0, m = 0, n = 0;
	int primaryKey = 0;
	SearchPos* matchPos = NULL;
	SearchPos* mFatherPos = NULL;
	WordCode* wordCode = NULL;
	ListData* node = NULL;
	int wordPos = 0;
	int wordIndex = 0;
	int charIndex = 0;
	boolean allDigit = true;
	boolean dyeOpen = false;
	LinkedList charsCache;
	/*----------------------------------------------------------------------------*/
	if (keyText == NULL)
	{
		return;
	}

	if (keySearchData != NULL)
	{
		freeSearchData(keySearchData);
	}
	else
	{
		keySearchData = (SearchData*) malloc(SIZEOF_SEARCHDATA);
		keySearchData->wordCodes = NULL;
		keySearchData->sortKeys = NULL;
		tree->keySearchData = keySearchData;
	}
	initLinked(&charsCache);

	text2SearchData(keyText, keyLength, keySearchData);

	for (i = 0; i < count; i++)
	{
		primaryKey = lastSearched[i] >> 8;
		tree->searchDatas.get(&tree->searchDatas, primaryKey, &dataToSearch);
		matchPos = (SearchPos*) isHit(tree, dataToSearch, keySearchData->wordCodes, keySearchData->codesCount, isT9);

		if (matchPos == NULL) break;
		do
		{ //获取到匹配项,构建路径序列
			mFatherPos = matchPos->father;
			if (mFatherPos != NULL)
			{
				mFatherPos->child = matchPos;
				matchPos = mFatherPos;
			}
		}
		while (mFatherPos != NULL );
		//matchPos已指向最开始匹配的SearchPos 搜索路径构建完毕
		for (j = 0, k = 0; j < dataToSearch->codesCount; j++, k++)
		{
			wordCode = &(dataToSearch->wordCodes[j]);
			wordIndex = wordCode->pyCodeNum >> 3;
			while (k < wordIndex)
			{ //当前字位置如果超越K的位置需要向charCache中填充空格 ->0x20
				charsCache.append(&charsCache, 0x20);
				k++;
			}
			if (matchPos != NULL && (wordPos = matchPos->pos >> 6) == j)
			{ // matchPos匹配到的字位置,是SearchData 中的第wordPos字,与toSearch中的第J个位置进行了匹配
				if (!dyeOpen)
				{
					dyeOpen = true;
					for (n = 0; n < dyeStrLen; n++)
					{
						charsCache.append(&charsCache, dyeStr[n]);
					}
				}
				charsCache.append(&charsCache, wordCode->srcUnicode);
				do
				{
					matchPos = matchPos->child; //消耗掉当前的匹配过程  -> 检查下一个匹配路径是否依然是当前字. 直到消耗完毕为止
				}
				while (matchPos != NULL && (matchPos->pos >> 6) == j);
			}
			else
			{ //由于匹配过程完全都是连续的,不存在跳字,所以一旦发现不在匹配即可封闭<font></font>标签
				if (dyeOpen)
				{
					charsCache.append(&charsCache, '<');
					charsCache.append(&charsCache, '/');
					charsCache.append(&charsCache, 'f');
					charsCache.append(&charsCache, 'o');
					charsCache.append(&charsCache, 'n');
					charsCache.append(&charsCache, 't');
					charsCache.append(&charsCache, '>');
					dyeOpen = false;
				}
				charsCache.append(&charsCache, wordCode->srcUnicode);
			}
		}

		if (dyeOpen)
		{ //当匹配到最后一个字时完成<font></font>标签关闭
			charsCache.append(&charsCache, '<');
			charsCache.append(&charsCache, '/');
			charsCache.append(&charsCache, 'f');
			charsCache.append(&charsCache, 'o');
			charsCache.append(&charsCache, 'n');
			charsCache.append(&charsCache, 't');
			charsCache.append(&charsCache, '>');
			dyeOpen = false;
		}

		result[i] = (uchar*) malloc((charsCache.size + 1) * sizeof(uchar));
		node = charsCache.first;
		for (j = 0; j < charsCache.size; j++)
		{
			result[i][j] = (uchar) node->pData;
			node = node->next;
		}
		result[i][charsCache.size] = '\0';
		charsCache.clear(&charsCache);
	}
	clearPosCache();
	charsCache.dispose(&charsCache);
}

/*
 *	@param dyeStr <font color=#D64206>
 */
void getPyHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen, uchar** result,
		boolean isT9)
{
	SearchData* keySearchData = tree->keySearchData;
	SearchData* dataToSearch = NULL;
	int i = 0, j = 0, k = 0, m = 0, n = 0;
	int primaryKey = 0;
	SearchPos* matchPos = NULL;
	SearchPos* mFatherPos = NULL;
	WordCode* wordCode = NULL;
	ListData* node = NULL;
	int wordPos = 0;
	int wordIndex = 0;
	int pyIndex = 0;
	int charIndex = 0;
	uchar pyChar = 0;
	uchar nextPyChar = 0;
	boolean allDigit = true;
	boolean dyeOpenPy = false;
	LinkedList pyCache;
	/*----------------------------------------------------------------------------*/
	if (keyText == NULL)
	{
		return;
	}
	if (keySearchData != NULL)
	{
		freeSearchData(keySearchData);
	}
	else
	{
		keySearchData = (SearchData*) malloc(SIZEOF_SEARCHDATA);
		keySearchData->wordCodes = NULL;
		keySearchData->sortKeys = NULL;
		tree->keySearchData = keySearchData;
	}
	initLinked(&pyCache);
	text2SearchData(keyText, keyLength, keySearchData);

	for (i = 0; i < count; i++)
	{
		primaryKey = lastSearched[i] >> 8;
		tree->searchDatas.get(&tree->searchDatas, primaryKey, &dataToSearch);
		matchPos = (SearchPos*) isHit(tree, dataToSearch, keySearchData->wordCodes, keySearchData->codesCount, isT9);
		if (matchPos == NULL) break;
		//获取到匹配项构建路径序列
		do
		{
			mFatherPos = matchPos->father;
			if (mFatherPos != NULL)
			{
				mFatherPos->child = matchPos;
				matchPos = mFatherPos;
			}
		}
		while (mFatherPos != NULL );
		//matchPos已指向最开始匹配的SearchPos

		for (j = 0, k = 0; j < dataToSearch->codesCount; j++, k++)
		{
			wordCode = &(dataToSearch->wordCodes[j]);
			wordIndex = wordCode->pyCodeNum >> 3;
			while (k < wordIndex)
			{ //当前字位置如果超越K的位置需要向charCache中填充空格 ->0x20
				pyCache.append(&pyCache, 0x20);
				k++;
			}

			if (matchPos != NULL)
			{
				wordPos = matchPos->pos >> 6; // matchPos匹配到的字位置,是SearchData 中的第wordPos字
				pyIndex = (matchPos->pos >> 3) & 0x07;
				charIndex = matchPos->pos & 0x07;

				if (wordPos == j)
				{ //本字被匹配
					if (!dyeOpenPy)
					{
						dyeOpenPy = true;
						for (n = 0; n < dyeStrLen; n++)
						{
							pyCache.append(&pyCache, dyeStr[n]);
						}
					}
					if ((wordCode->pyCodeNum & 0x07) > 0)
					{ //存在拼音
						for (m = 0;; m++)
						{
							pyChar = PySpellCode[wordCode->pyCodeIndex[pyIndex]][m];
							nextPyChar = PySpellCode[wordCode->pyCodeIndex[pyIndex]][m + 1];
							if (m == 0) pyChar = pyChar - CapsOff; //对首字母进行大写
							if (m == charIndex)
							{
								matchPos = matchPos->child; //此匹配过程已被染色过程消耗掉,消耗拼音过程
								if (matchPos != NULL && (matchPos->pos >> 6) == wordPos) charIndex = matchPos->pos & 0x07; //由于同一次匹配过程中不可能出现相同字的不同多音字不被匹配的情形,所以此处不再继续讨论pyIndex是否相等
							}
							else if (dyeOpenPy)
							{
								pyCache.append(&pyCache, '<');
								pyCache.append(&pyCache, '/');
								pyCache.append(&pyCache, 'f');
								pyCache.append(&pyCache, 'o');
								pyCache.append(&pyCache, 'n');
								pyCache.append(&pyCache, 't');
								pyCache.append(&pyCache, '>');
								dyeOpenPy = false;
							}
							pyCache.append(&pyCache, pyChar);
							if (nextPyChar == '\0') break;
						}

					}
					else
					{
						pyCache.append(&pyCache, wordCode->srcUnicode); //非汉字直接将原文加入pyCache
						matchPos = matchPos->child;
					}
					continue;
				}
				else
				//本字非匹配
				goto NoMatch;
			}

			NoMatch:
			{

				if (dyeOpenPy)
				{ //当匹配到一个非匹配字时完成<font></font>标签关闭
					pyCache.append(&pyCache, '<');
					pyCache.append(&pyCache, '/');
					pyCache.append(&pyCache, 'f');
					pyCache.append(&pyCache, 'o');
					pyCache.append(&pyCache, 'n');
					pyCache.append(&pyCache, 't');
					pyCache.append(&pyCache, '>');
					dyeOpenPy = false;
				}
				if ((wordCode->pyCodeNum & 0x07) > 0)
				{ //存在拼音
					for (m = 0;; m++)
					{
						pyChar = PySpellCode[wordCode->pyCodeIndex[0]][m];
						nextPyChar = PySpellCode[wordCode->pyCodeIndex[0]][m + 1];
						if (m == 0) pyChar = pyChar - CapsOff; //对首字母进行大写
						pyCache.append(&pyCache, pyChar);
						if (nextPyChar == '\0') break;
					}

				}
				else pyCache.append(&pyCache, wordCode->srcUnicode); //非汉字直接将原文加入pyCache
			}
		}

		result[i] = (uchar*) malloc(sizeof(uchar) * (pyCache.size + 1));
		node = pyCache.first;
		for (j = 0; j < pyCache.size; j++)
		{
			result[i][j] = (uchar) node->pData;
			node = node->next;
		}
		result[i][pyCache.size] = '\0';
		pyCache.clear(&pyCache);
	}
	clearPosCache();
	pyCache.dispose(&pyCache);
}

void freeWordCode(WordCode* pWordCode)
{
	if (pWordCode == NULL)
	{
		return;
	}
	if ((pWordCode->pyCodeNum & 0x07) > 0)
	{
		free(pWordCode->pyCodeIndex);
	}
	pWordCode->pyCodeIndex = NULL;
}

void freeSearchPhone(SearchPhone* sPhone)
{
	free(sPhone->phoneNum);
}

void freeSearchData(SearchData* data)
{
	int count = data->codesCount;
	int i = 0;
	if (data->sortKeys != NULL) free(data->sortKeys);
	data->sortKeys = NULL;
	if (data->wordCodes != NULL)
	{
		for (i = 0; i < count; i++)
		{
			freeWordCode(&(data->wordCodes[i]));
		}
		free(data->wordCodes);
	}
	data->wordCodes = NULL;
	if (data->phones != NULL)
	{
		for (i = 0; i < data->phoneCount; i++)
			freeSearchPhone(&(data->phones[i]));
		free(data->phones);
	}
	data->phones = NULL;
}

void freeSearchTree(SearchTree * tree)
{
	int i = 0;
	ArrayList * cache = NULL;
	SearchData* data = NULL;
	if (tree->foreignTreeIndexes != NULL)
	{
		tree->foreignTreeIndexes->dispose(tree->foreignTreeIndexes);
		free(tree->foreignTreeIndexes);
	}

	if (tree->matchFunc != NULL)
	{
		free(tree->matchFunc);
	}
	tree->searchPhones.dispose(&(tree->searchPhones));
	tree->searchPosQueue.dispose(&(tree->searchPosQueue));
	tree->lastSearched.dispose(&(tree->lastSearched));
	cache = &tree->searchDatas;
	for (i = 0; i < cache->size; i++)
	{
		cache->get(cache, i, &data);
		freeSearchData(data);
		free(data);
	}
	cache->dispose(cache);
	for (i = 0; i < CachedHitNum; i++)
	{
		cache = &(tree->firstSpellIndex[i]);
		cache->dispose(cache);
	}

	cache = &(tree->desNameResult);
	cache->dispose(cache);
	cache = &(tree->desPhoneResult);
	cache->dispose(cache);

	if (tree->keySearchData != NULL)
	{
		freeSearchData(tree->keySearchData);
		free(tree->keySearchData);
	}
}
