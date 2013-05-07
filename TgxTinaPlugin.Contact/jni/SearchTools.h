/*
 * SearchTools.h
 *
 *  Created on: 2011-11-3
 *      Author: Zhangzhuo
 */

#ifndef SEARCHTOOLS_H_
#define SEARCHTOOLS_H_
#include "SearchCore.h"
void dispose(SearchTree* tree);
void disposeAll();
SearchTree* createTree(char userMode);
int addPosCache(int position, SearchPos* father, int step);
void clearPosCache();
int addResultCache(SearchPos* headMatchPos, int primaryKey);
void clearResultCache();

#endif /* SEARCHTOOLS_H_ */
