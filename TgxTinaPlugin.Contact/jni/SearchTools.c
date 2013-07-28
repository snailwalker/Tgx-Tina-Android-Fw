/*******************************************************************************
 * Copyright 2013 Zhang Zhuo(william@TinyGameX.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
/*
 * SearchTools.c
 *
 *  Created on: 2011-11-3
 *      Author: Zhangzhuo
 */

#include "SearchTools.h"
#include "Collection.h"
/*------------------------global-define-----------------------*/
LinkedList* trees = NULL;
ArrayList* posCached = NULL;
ArrayList* resultCached = NULL;
int posCachedCount = 0;
int posCachedSize = 0;
int resultCachedCount = 0;
int resultCachedSize = 0;
boolean initialized = false;
const char* T9 = "22233344455566677778889999";
/*-------------------------function---------------------------*/
void dirty(SearchTree* tree)
{
	if (tree) tree->dirty = true;
}

void dispose(SearchTree* tree)
{
	boolean result = false;
	if (tree) {
		tree->dirty = true;
		result = trees->remove(trees, (int) tree);
		if (result) {
			freeSearchTree(tree);
			free(tree);
		}
	}
}

void cleanTree()
{
	ListData* node = NULL;
	SearchTree* tree = NULL;
	int i;
	if (initialized) {
		node = trees->first;
		for (i = 0; i < trees->size; i++) {
			tree = (SearchTree*) (node->pData);
			node = node->next;
			if (tree != NULL && tree->dirty) {
				freeSearchTree(tree);
				free(tree);
			}
		}
	}
}

void disposeAll()
{
	ListData* node = NULL;
	SearchTree* tree = NULL;
	SearchPos* pos = NULL;
	SearchResult* result = NULL;
	int i;
	if (!initialized) {
		return;
	}
	node = trees->first;
	for (i = 0; i < trees->size; i++) {
		tree = (SearchTree*) (node->pData);
		node = node->next;
		if (tree != NULL) {
			freeSearchTree(tree);
			free(tree);
		}
	}
	trees->dispose(trees);

	if (posCached != NULL) {
		for (i = 0; i < posCached->size; i++) {
			posCached->get(posCached, i, &pos);
			free(pos);
		}
		posCached->dispose(posCached);
		posCachedCount = 0;
		posCachedSize = 0;
		free(posCached);
	}

	if (resultCached != NULL) {
		for (i = 0; i < resultCached->size; i++) {
			resultCached->get(resultCached, i, &result);
			free(result);
		}
		resultCached->dispose(resultCached);
		resultCachedCount = 0;
		resultCachedSize = 0;
		free(resultCached);
	}

	free(trees);
	trees = NULL;
	initialized = false;
}

SearchTree* createTree(char userMode)
{
	SearchTree* tree = NULL;
	int matchFuncLen = 0;
	if (!initialized) {
		trees = (LinkedList*) malloc(SIZEOF_LINKEDLIST);
		initLinked(trees);
		posCached = (ArrayList*) malloc(SIZEOF_ARRAYLIST);
		initArrayCapacity(posCached, 1024);
		resultCached = (ArrayList*) malloc(SIZEOF_ARRAYLIST);
		initArrayCapacity(resultCached, 512);
		posCachedCount = 0;
		posCachedSize = 0;
		resultCachedCount = 0;
		resultCachedSize = 0;
		initialized = true;
	}
	tree = (SearchTree*) malloc(SIZEOF_SEARCHTREE);
	searchTreeInit(tree, false, userMode);
	if (userMode == 0) { //0为T9模式
		matchFuncLen = cLength(T9);
		tree->matchFunc = (char*) malloc(matchFuncLen + 1);
		memcpy(tree->matchFunc, T9, matchFuncLen + 1);
	}
	trees->append(trees, (int) tree);
	return tree;
}

int addPosCache(int position, SearchPos* father, int step)
{
	SearchPos* pos = NULL;
	if (posCachedCount < posCachedSize) {
		posCached->get(posCached, posCachedCount, &pos);
	}
	else {
		pos = (SearchPos*) malloc(SIZEOF_SEARCHPOS);
		posCached->append(posCached, (int) pos);
		posCachedSize++;
	}
	pos->pos = position;
	pos->father = father;
	pos->step = step;
	pos->child = NULL;
	posCachedCount++;
	return (int) pos;
}

void clearPosCache()
{
	posCachedCount = 0;
}

int addResultCache(SearchPos* headMatchPos, int primaryKey)
{
	SearchResult* result = NULL;
	if (resultCachedCount < resultCachedSize) {
		resultCached->get(resultCached, resultCachedCount, &result);
	}
	else {
		result = (SearchResult*) malloc(SIZEOF_SEARCHRESULT);
		resultCachedSize++;
		resultCached->append(resultCached, (int) result);
	}
	result->headMatchPos = headMatchPos;
	result->primaryKey = primaryKey;
	resultCachedCount++;
	return (int) result;
}

void clearResultCache()
{
	resultCachedCount = 0;
}

