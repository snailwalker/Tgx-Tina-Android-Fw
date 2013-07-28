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
