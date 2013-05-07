/*
 * toKeep_jni_SearchPy.c
 *
 *  Created on: Oct 18, 2011
 *      Author: Zhangzhuo
 */
#include "com_tgx_tina_android_plugin_contacts_search_SearchPy.h"
#include "SearchTools.h"
extern const char* PySpellCode[];

jclass searchInfo = NULL;
jfieldID fid_index = NULL;
jfieldID fid_phoneNum = NULL;
jfieldID fid_name = NULL;
jfieldID fid_foreign = NULL;
jfieldID fid_filter = NULL;

JNIEXPORT jintArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_initInfos(JNIEnv * env, jobject thiz, jobjectArray array, jint mode, jint oldAdr)
{
	// 此处映射java的SearchInfo类
	if (searchInfo == NULL)
	{
		searchInfo = (*env)->FindClass(env, "base/tina/external/contacts/serach/SearchInfo");
		fid_index = (*env)->GetFieldID(env, searchInfo, "index", "I");
		fid_filter = (*env)->GetFieldID(env, searchInfo, "filter", "J");
		fid_phoneNum = (*env)->GetFieldID(env, searchInfo, "phoneNum", "Ljava/lang/String;");
		fid_name = (*env)->GetFieldID(env, searchInfo, "name", "Ljava/lang/String;");
	}

	jstring str_name = NULL, str_phone = NULL;
	jsize arrayLength = 0;

	SearchTree *tree = NULL;
	jintArray result = NULL;
	jint* buf = NULL;
	jobject srcInfo = NULL;
	jchar* tar = NULL;
	int i = 0, j = 0;
	int jid = 0;
	ulong64 jfilter = 0;
	int toMalloc = 0;
	int nameLength = 0;
	int phoneLength = 0;
	uchar* chars_name;
	uchar* chars_phone;
	/*-------------------------------------------------------------------------------------*/

	if (array == NULL) return NULL;

	arrayLength = (*env)->GetArrayLength(env, array);
	dispose((SearchTree*) oldAdr);
	tree = createTree(mode);

	toMalloc = sizeof(jint) * (arrayLength + 1);
	buf = (jint*) malloc(toMalloc);
	memset(buf, 0, toMalloc);
	buf[0] = (int) tree;
	result = (*env)->NewIntArray(env, arrayLength + 1);
	for (i = 0; i < arrayLength; i++)
	{
		srcInfo = (*env)->GetObjectArrayElement(env, array, i);
		jid = (*env)->GetIntField(env, srcInfo, fid_index);
		jfilter = (*env)->GetLongField(env, srcInfo, fid_filter);
		str_name = (*env)->GetObjectField(env, srcInfo, fid_name);
		str_phone = (*env)->GetObjectField(env, srcInfo, fid_phoneNum);
		nameLength = 0;
		phoneLength = 0;
		if (str_name)
		{
			nameLength = (*env)->GetStringLength(env, str_name);
			(*env)->ReleaseStringChars(env, str_name, (*env)->GetStringChars(env, str_name, (jboolean*) 0));
			(*env)->DeleteLocalRef(env, str_name);
		}

		if (str_phone)
		{
			phoneLength = (*env)->GetStringLength(env, str_phone);
			(*env)->ReleaseStringChars(env, str_phone, (*env)->GetStringChars(env, str_phone, (jboolean*) 0));
			(*env)->DeleteLocalRef(env, str_phone);
		}
		treeAddData(tree, jid, jfilter, chars_name, nameLength, chars_phone, phoneLength);

		(*env)->DeleteLocalRef(env, srcInfo);
	}
	treeEndAdd(tree, &buf[1], arrayLength);
	treeBuildIndex(tree);
	/*-------------------------------------------------------------------------------------*/

	(*env)->SetIntArrayRegion(env, result, 0, arrayLength + 1, buf);
	free(buf);
	return result;
}

JNIEXPORT jintArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_getFirstPyPrimaryKeys(JNIEnv * env, jobject thiz, jint adr, jlong filter)
{
	jintArray result = NULL;
	int* srcPrimaryKeys = NULL;
	int srcPrimaryKeysLength = 0;
	SearchTree* tree = NULL;
	tree = (SearchTree*) adr;
	int primaryKeys[27];
	memset(primaryKeys, -1, sizeof(int) * 27);
	getFirstPyPrimaryKeys(tree, primaryKeys, filter);
	result = (*env)->NewIntArray(env, 27);
	(*env)->SetIntArrayRegion(env, result, 0, 27, primaryKeys);
	return result;
}
JNIEXPORT jintArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_getInfosPrimaryKeys(JNIEnv * env, jobject thiz, jint adr, jlong filter)
{
	jintArray result = NULL;
	int* primaryKeys = NULL;
	SearchTree* tree = NULL;
	tree = (SearchTree*) adr;
	primaryKeys = getInofsPrimaryKeys(tree, filter);
	if (primaryKeys)
	{
		result = (*env)->NewIntArray(env, primaryKeys[0]);
		(*env)->SetIntArrayRegion(env, result, 0, primaryKeys[0], &primaryKeys[1]);
		free(primaryKeys);
	}
	return result;
}
JNIEXPORT jintArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_search(JNIEnv * env, jobject thiz, jint adr, jstring key, jintArray lastSearched, jint start,
		jint limit, jlong filters, jboolean isT9)
{
	jintArray result = NULL;
	SearchTree* tree = NULL;
	int keyLength = 0;
	int lastSearchLength = 0;
	int i = 0, p = 0;
	int toMalloc = 0;
	int* lastSearchedPtr = NULL;
	int* resultBuf = NULL;
	SearchPhone* phoneMatched;
	/*-------------------------------------------------------------------------------------*/
	if (key == NULL || adr == 0)
	{
		return NULL;
	}
	tree = (SearchTree*) adr;
	keyLength = (*env)->GetStringLength(env, key);
	if (keyLength == 0) return NULL;
	const jchar* keyChars = (*env)->GetStringChars(env, key, false);
	if (lastSearched != NULL)
	{
		lastSearchLength = (*env)->GetArrayLength(env, lastSearched);
		if (lastSearchLength == 0 || start < 0)
		{ //对数据边界进行校验.
			goto ToSearch;
		}
		toMalloc = lastSearchLength < limit ? lastSearchLength : limit;
		if (start + toMalloc > lastSearchLength)
		{
			toMalloc = lastSearchLength - start;
		}
		if (toMalloc <= 0)
		{
			goto ToSearch;
		}
		lastSearchedPtr = (int*) malloc(sizeof(int) * toMalloc);
		(*env)->GetIntArrayRegion(env, lastSearched, start, toMalloc, lastSearchedPtr);
		tree->lastSearched.clear(&(tree->lastSearched));
		for (i = 0; i < toMalloc; i++)
		{
			tree->lastSearched.append(&(tree->lastSearched), lastSearchedPtr[i]);
		}
	}
	ToSearch:
	{
		tree->desNameResult.clear(&tree->desNameResult);
		tree->desPhoneResult.clear(&(tree->desPhoneResult));
		treeSearch(tree, keyChars, keyLength, toMalloc > 0 ? &(tree->lastSearched) : NULL, &(tree->desNameResult), &(tree->desPhoneResult), limit, filters, isT9);

		if (tree->desNameResult.size == 0 && tree->desPhoneResult.size == 0)
		{
			goto Result;
		}

		result = (*env)->NewIntArray(env, tree->desNameResult.size + tree->desPhoneResult.size + 1);
		resultBuf = (int*) malloc(sizeof(int) * (tree->desNameResult.size + tree->desPhoneResult.size + 1));
		i = 0;
		for (i = 0; i < tree->desNameResult.size; i++)
		{
			tree->desNameResult.get(&(tree->desNameResult), i, &resultBuf[i]);
		}
		resultBuf[i++] = -1;
		// 加上号码集合
		for (p = 0; p < tree->desPhoneResult.size; p++)
		{
			tree->desPhoneResult.get(&(tree->desPhoneResult), p, &phoneMatched);
			resultBuf[i++] = phoneMatched->primaryKey << 8 | phoneMatched->index;
		}

		(*env)->SetIntArrayRegion(env, result, 0, i, resultBuf);
	}

	/*-------------------------------------------------------------------------------------*/
	Result:
	{
		(*env)->ReleaseStringChars(env, key, keyChars);
		if (lastSearchedPtr != NULL)
		{
			free(lastSearchedPtr);
		}
		if (resultBuf != NULL)
		{
			free(resultBuf);
		}
		return result;
	}
}

JNIEXPORT jobjectArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_getPhoneHightLights(JNIEnv * env, jobject thiz, jint adr, jintArray searched,
		jstring keyText, jstring dyeStr)
{
	jobjectArray result = NULL;
	int count = 0;
	int i = 0;
	jstring str = NULL;
	jclass jString = (*env)->FindClass(env, "java/lang/String");
	count = (*env)->GetArrayLength(env, searched);
	result = (*env)->NewObjectArray(env, count, jString, NULL);
	SearchTree* tree = NULL;
	int primaryKey = 0;
	int* primaryKeys = NULL;
	uchar** highLightStrs = NULL;
	uchar* key = NULL;
	char* dyeChars = NULL;
	int dyeLen = 0;
	int keyLen = 0;
	/*-------------------------------------------------------------------------------------*/
	if (searched == NULL || keyText == NULL || dyeStr == NULL)
	{
		return NULL;
	}
	tree = (SearchTree*) adr;

	key = (uchar *) ((*env)->GetStringChars(env, keyText, 0));
	dyeChars = (char *) ((*env)->GetStringUTFChars(env, dyeStr, 0));
	primaryKeys = (int*) malloc(sizeof(int) * count);
	highLightStrs = (uchar**) malloc(sizeof(uchar*) * count);
	memset(highLightStrs, 0, sizeof(uchar*) * count);
	(*env)->GetIntArrayRegion(env, searched, 0, count, primaryKeys);

	keyLen = (*env)->GetStringLength(env, keyText);
	dyeLen = (*env)->GetStringLength(env, dyeStr);

	getPhoneHighLights(tree, primaryKeys, count, key, keyLen, dyeChars, dyeLen, highLightStrs);

	for (i = 0; i < count; i++)
	{
		str = (*env)->NewString(env, highLightStrs[i], ucLength(highLightStrs[i]));
		(*env)->SetObjectArrayElement(env, result, i, str);
		(*env)->DeleteLocalRef(env, str);
	}

	/*-------------------------------------------------------------------------------------*/
	Result:
	{
		(*env)->ReleaseStringChars(env, keyText, key);
		(*env)->ReleaseStringUTFChars(env, dyeStr, dyeChars);

		if (primaryKeys != NULL)
		{
			free(primaryKeys);
		}

		if (highLightStrs != NULL)
		{
			for (i = 0; i < count; i++)
			{
				free(highLightStrs[i]);
			}
			free(highLightStrs);
		}

		return result;
	}
}

JNIEXPORT jobjectArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_getHightLights(JNIEnv * env, jobject thiz, jint adr, jintArray searched, jstring keyText,
		jstring dyeStr, jboolean isT9)
{
	jobjectArray result = NULL;
	int count = 0;
	int i = 0;
	jstring str = NULL;
	jclass jString = (*env)->FindClass(env, "java/lang/String");
	count = (*env)->GetArrayLength(env, searched);
	result = (*env)->NewObjectArray(env, count, jString, NULL);
	SearchTree* tree = NULL;
	int primaryKey = 0;
	int* primaryKeys = NULL;
	uchar** highLightStrs = NULL;
	uchar* key = NULL;
	char* dyeChars = NULL;
	int keyLen;
	int dyeLen;
	/*-------------------------------------------------------------------------------------*/
	if (searched == NULL || keyText == NULL || dyeStr == NULL)
	{
		return NULL;
	}
	tree = (SearchTree*) adr;

	key = (uchar *) ((*env)->GetStringChars(env, keyText, 0));
	dyeChars = (char *) ((*env)->GetStringUTFChars(env, dyeStr, 0));
	primaryKeys = (int*) malloc(sizeof(int) * count);
	highLightStrs = (uchar**) malloc(sizeof(uchar*) * count);
	memset(highLightStrs, 0, sizeof(uchar*) * count);
	(*env)->GetIntArrayRegion(env, searched, 0, count, primaryKeys);

	keyLen = (*env)->GetStringLength(env, keyText);
	dyeLen = (*env)->GetStringLength(env, dyeStr);

	getHighLights(tree, primaryKeys, count, key, keyLen, dyeChars, dyeLen, highLightStrs, isT9);

	for (i = 0; i < count; i++)
	{
		str = (*env)->NewString(env, highLightStrs[i], ucLength(highLightStrs[i]));
		(*env)->SetObjectArrayElement(env, result, i, str);
		(*env)->DeleteLocalRef(env, str);
	}

	/*-------------------------------------------------------------------------------------*/
	Result:
	{
		(*env)->ReleaseStringChars(env, keyText, key);
		(*env)->ReleaseStringUTFChars(env, dyeStr, dyeChars);

		if (primaryKeys != NULL)
		{
			free(primaryKeys);
		}

		if (highLightStrs != NULL)
		{
			for (i = 0; i < count; i++)
			{
				free(highLightStrs[i]);
			}
			free(highLightStrs);
		}

		return result;
	}
}

JNIEXPORT jobjectArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_getNameHightLights(JNIEnv * env, jobject thiz, jint adr, jintArray searched,
		jstring keyText, jstring dyeStr, jboolean isT9)
{
	jobjectArray result = NULL;
	int count = 0;
	int i = 0;
	jstring str = NULL;
	jclass jString = (*env)->FindClass(env, "java/lang/String");
	count = (*env)->GetArrayLength(env, searched);
	result = (*env)->NewObjectArray(env, count, jString, NULL);
	SearchTree* tree = NULL;
	int primaryKey = 0;
	int* primaryKeys = NULL;
	uchar** highLightStrs = NULL;
	uchar* key = NULL;
	char* dyeChars = NULL;
	int keyLen;
	int dyeLen;
	/*-------------------------------------------------------------------------------------*/
	if (searched == NULL || keyText == NULL || dyeStr == NULL) return NULL;

	tree = (SearchTree*) adr;

	key = (uchar *) ((*env)->GetStringChars(env, keyText, 0));
	dyeChars = (char *) ((*env)->GetStringUTFChars(env, dyeStr, 0));
	primaryKeys = (int*) malloc(sizeof(int) * count);
	highLightStrs = (uchar**) malloc(sizeof(uchar*) * count);
	memset(highLightStrs, 0, sizeof(uchar*) * count);
	(*env)->GetIntArrayRegion(env, searched, 0, count, primaryKeys);

	keyLen = (*env)->GetStringLength(env, keyText);
	dyeLen = (*env)->GetStringLength(env, dyeStr);

	getNameHighLights(tree, primaryKeys, count, key, keyLen, dyeChars, dyeLen, highLightStrs, isT9);

	for (i = 0; i < count; i++)
	{
		str = (*env)->NewString(env, highLightStrs[i], ucLength(highLightStrs[i]));
		(*env)->SetObjectArrayElement(env, result, i, str);
		(*env)->DeleteLocalRef(env, str);
	}

	/*-------------------------------------------------------------------------------------*/
	Result:
	{
		(*env)->ReleaseStringChars(env, keyText, key);
		(*env)->ReleaseStringUTFChars(env, dyeStr, dyeChars);

		if (primaryKeys != NULL)
		{
			free(primaryKeys);
		}

		if (highLightStrs != NULL)
		{
			for (i = 0; i < count; i++)
			{
				free(highLightStrs[i]);
			}
			free(highLightStrs);
		}

		return result;
	}
}

char* Text2Py(const jchar* mChars, int mLength)
{
	char* result = NULL;
	SearchData keySearchData;
	text2SearchData(mChars, mLength, &keySearchData);

	if (keySearchData.codesCount < 1 || (keySearchData.wordCodes[0].pyCodeNum & 0x07) < 1)
	{
		return NULL;
	}

	LinkedList charsCache;
	initLinked(&charsCache);
	WordCode word;
	int i;
	for (i = 0; i < keySearchData.codesCount; i++)
	{
		word = keySearchData.wordCodes[i];
		if ((word.pyCodeNum & 0x07) > 0)
		{
			const char* dyeStr = PySpellCode[word.pyCodeIndex[0]];
			char nextPyChar = dyeStr[0];
			int n = 1;
			do
			{
				charsCache.append(&charsCache, nextPyChar);
				nextPyChar = dyeStr[n++];
			}
			while (nextPyChar != '\0');
		}
		else
		{
			charsCache.append(&charsCache, word.wordUnicode);
		}
	}
	result = (char*) malloc(charsCache.size + 1);
	ListData* node = charsCache.first;
	int j;
	for (j = 0; j < charsCache.size; j++)
	{
		result[j] = (char) node->pData;
		node = node->next;
	}
	result[charsCache.size] = '\0';
	charsCache.clear(&charsCache);
	charsCache.dispose(&charsCache);
	return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_getPyHightLights(JNIEnv * env, jobject thiz, jint adr, jintArray searched, jstring keyText,
		jstring dyeStr, jboolean isT9)
{
	jobjectArray result = NULL;
	int count = 0;
	int i = 0;
	jstring str = NULL;
	jclass jString = (*env)->FindClass(env, "java/lang/String");
	count = (*env)->GetArrayLength(env, searched);
	result = (*env)->NewObjectArray(env, count, jString, NULL);
	SearchPos* matchPos = NULL;
	SearchPos* fatherPos = NULL;
	SearchTree* tree = NULL;
	int primaryKey = 0;
	int* primaryKeys = NULL;
	uchar** highLightStrs = NULL;
	uchar* key = NULL;
	char* dyeChars = NULL;
	int keyLen = 0;
	int dyeLen = 0;
	/*-------------------------------------------------------------------------------------*/
	if (searched == NULL || keyText == NULL || dyeStr == NULL)
	{
		return NULL;
	}
	tree = (SearchTree*) adr;

	// 将所有可转成拼音的字符全转成拼音--------------------------------------------
	char* pyKey = Text2Py((*env)->GetStringChars(env, keyText, 0), (*env)->GetStringLength(env, keyText));
	if (pyKey != NULL)
	{
		keyText = (*env)->NewStringUTF(env, pyKey);
		free(pyKey);
	}

	// ------------------------------------------------------------------------

	key = (*env)->GetStringChars(env, keyText, false);
	dyeChars = (char*) ((*env)->GetStringUTFChars(env, dyeStr, 0));

	primaryKeys = (int*) malloc(sizeof(int) * count);
	highLightStrs = (uchar**) malloc(sizeof(uchar*) * count);
	memset(highLightStrs, 0, sizeof(uchar*) * count);
	(*env)->GetIntArrayRegion(env, searched, 0, count, primaryKeys);

	dyeLen = (*env)->GetStringLength(env, dyeStr);
	keyLen = (*env)->GetStringLength(env, keyText);

	getPyHighLights(tree, primaryKeys, count, key, keyLen, dyeChars, dyeLen, highLightStrs, isT9);
	//此处如果出现问题,需要检验KeyText是否输入正确
	for (i = 0; i < count; i++)
	{
		str = (*env)->NewString(env, highLightStrs[i], ucLength(highLightStrs[i]));
		(*env)->SetObjectArrayElement(env, result, i, str);
		(*env)->DeleteLocalRef(env, str);
	}
	/*-------------------------------------------------------------------------------------*/
	Result:
	{
		(*env)->ReleaseStringChars(env, keyText, key);
		(*env)->ReleaseStringUTFChars(env, dyeStr, dyeChars);

		if (primaryKeys != NULL)
		{
			free(primaryKeys);
		}
		if (highLightStrs != NULL)
		{
			for (i = 0; i < count; i++)
			{
				free(highLightStrs[i]);
			}
			free(highLightStrs);
		}
		return result;
	}
}

JNIEXPORT void JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_modify(JNIEnv * env, jobject thiz, jint adr, jobject src)
{

}

JNIEXPORT void JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_insert(JNIEnv * env, jobject thiz, jint adr, jobject src)
{

}

JNIEXPORT void JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_delete(JNIEnv * env, jobject thiz, jint adr, jobject src)
{

}

JNIEXPORT void JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_dispose(JNIEnv * env, jobject thiz, jint adr)
{
	SearchTree* tree = (SearchTree*) adr;
	dispose(tree);
}

JNIEXPORT void JNICALL Java_com_tgx_tina_android_plugin_contacts_search_SearchPy_disposeAll(JNIEnv * env, jobject thiz)
{
	disposeAll();
}
