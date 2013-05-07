#ifndef SEARCHCORE_H
#define SEARCHCORE_H
#include "Collection.h"
#include "SearchSpell.h"

#define CachedHitSymbol (LetterNum+DignitalNum) // 符号 搜索缓存集
#define CachedHitNum (LetterNum+DignitalNum+SymbolNum+1) // 搜索缓存集个数(26个英文字母 10个数字 1组符号)
/*
 * exmaple:
 * WordCode* word{
 * 长:
 * wordUnicode:957F
 * pyCodeNum:2
 * pyCodeIndex:short[2]
 * [0] = x
 * PyMusicCode[x]->chang
 * [1] = y
 * PyMusicCode[y]->zhang
 * }
 */

typedef struct WordCode
{
	uchar srcUnicode;
	uchar wordUnicode; //原始Unicode码
	uchar pyCodeNum; // 拼音码个数 处于低3位上 0-7,因为多音字最多有5个.高三位到低三位中间的10位表示本字在全字串中的Index,共1024个 ,高3位无用
	short* pyCodeIndex; // 拼音码值（支持多拼音）
} WordCode;

#define SIZEOF_WORDCODE  (sizeof(WordCode))

typedef struct SearchPhone
{
	char* phoneNum;
	int phoneMatch;
	int primaryKey;
	int index;
} SearchPhone;
#define SIZEOF_SEARCHPHONE  (sizeof(SearchPhone))
/*
 *	用于存储搜索项的数据
 */
typedef struct SearchData
{
	int primaryKey;
	ulong64 filter;
	int* sortKeys; //在SearchTree中排序应用的排序Key  拼音码 pyCodeIndex 在高16位.Unicode码在低16位
	struct WordCode* wordCodes;
	struct SearchPhone* phones;
	int phoneCount;
	short codesCount;
	boolean isMatched;
	boolean isNameAllDigit;
} SearchData;

#define SIZEOF_SEARCHDATA  (sizeof(SearchData))

typedef struct SearchPos
{
	int pos; //第几个字：10bit + 第几个拼音码：3bit + 第几个字母：3bit
	int step; //步长
	struct SearchPos* father;
	struct SearchPos* child;
} SearchPos;

#define SIZEOF_SEARCHPOS (sizeof(SearchPos))

typedef struct SearchResult
{
	int primaryKey;
	SearchPos* headMatchPos;
} SearchResult;

#define SIZEOF_SEARCHRESULT (sizeof(SearchResult))

typedef struct SearchTree
{
	//键盘字母与数字的对应关系
	char* matchFunc;
	//是否使用T9输入类型的Match过程
	boolean matchType;
	char userMode;
	//当前输入转换的搜索串
	struct SearchData* keySearchData;
	//外部系统
	struct ArrayList* foreignTreeIndexes;
	struct ArrayList searchDatas;
	struct LinkedList searchPhones;
	//内容存储为primaryKey由于其递增唯一的特性,所以同时作为Index内部排序规则使用.
	struct ArrayList firstSpellIndex[CachedHitNum];
	struct ArrayList desNameResult;
	struct ArrayList desPhoneResult;
	struct LinkedList searchPosQueue;
	struct LinkedList lastSearched;
} SearchTree;

#define SIZEOF_SEARCHTREE (sizeof(SearchTree))

enum
{
	ID_Ascii = 0, ID_PySpellCode = 1, ID_uchar_tIndex = 2, ID_uchar_tIndex2 = 3, ID_CannotSpell = 4
};

//==============================================  BEGIN  ================================================
//供外部调用的函数

//搜索初始化
void searchTreeInit(SearchTree* tree, boolean isInitFIndex, char userMode);
/*
 * 添加数据源信息
 * primaryKey: 数据索引primaryKey.初始化时此参数对Jni有意义
 * text: 名字
 * phoneNum：号码
 */
void treeAddData(SearchTree* tree, const int primaryKey, const ulong64 filter, const uchar* text, const int textLength, const uchar* phoneNum, const int phoneLength);

void treeEndAdd(SearchTree* tree, int* outArray, int outLength);

void treeBuildIndex(SearchTree* tree);

/*
 * 搜索数据
 * text: 搜索串
 * aSearchedArray 搜索域，输入NULL时搜索tree中的联系人
 * aNameMatchHits: 名字搜索结果
 * aPhoneMatchHits：号码搜索结果
 * sortByMatchValue：按搜索位置排序
 */
void treeSearch(SearchTree* tree, const uchar* keyText, const int keyLength, LinkedList* lastSearched, ArrayList* desNameMatchHits, ArrayList* desPhoneMatchHits, int resultLimit,
		ulong64 filter, boolean isT9);

void getHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen, uchar ** result,
		boolean isT9);

void getPyHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen, uchar** result,
		boolean isT9);
void getNameHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen,
		uchar ** result, boolean isT9);

void getPhoneHighLights(SearchTree* tree, const int* lastSearched, const int count, const uchar* keyText, const int keyLength, const char* dyeStr, const int dyeStrLen,
		uchar ** result);

int* getInofsPrimaryKeys(SearchTree* tree, ulong64 filter);
void getFirstPyPrimaryKeys(SearchTree* tree, int* primaryKeys, ulong64 filter);
/*
 * 所有的free_操作都必须由调用者对传入的 指针进行free(point)的操作 来完成最终的释放过程
 */
void freeWordCode(WordCode* pWordCode);
void freeSearchData(SearchData* data);
void freeSearchTree(SearchTree * tree);

//================================================  END  ==============================================

void text2SearchData(const uchar* text, const int textLength, SearchData *data);

int ucLength(const uchar* text);
int cLength(const char* text);

#endif /*SEARCHCORE_H*/

