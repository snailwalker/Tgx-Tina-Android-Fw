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
 * Collection.h
 *
 *  Created on: Oct 15, 2011
 *      Author: Zhangzhuo
 */

#ifndef COLLECTION_H
#define COLLECTION_H
#include "AndroidEnv.h"
#include <stdlib.h>

#define BIT_OFFSET  	5   //2进制  个数为1<<MALLOC_NUM
#define PER_BLOCK_POINT_COUNT 	(1 << BIT_OFFSET)  //points' count
#define INDEX_NUM_MAX   2048 //默认使用极大初始化参数,index 需要4*2048 = 8K 空间,最大内容(地址)空间为32*2048 = 65536
#define MALLOC_INDEX_SIZE (INDEX_NUM_MAX*sizeof(int))	 //bit 50 block
#define MALLOC_SIZE (PER_BLOCK_POINT_COUNT*sizeof(int))	 //
#define true 1
#define false 0
typedef unsigned char boolean;
typedef unsigned short uchar;
typedef unsigned char ubyte;
typedef unsigned long long ulong64;

#define SIZEOF_UCHAR 2
typedef struct ListData
{
	int pData;
	struct ListData* next;
} ListData;

#define SIZEOF_LISTDATA (sizeof(ListData))

typedef struct ArrayList
{
	int capacity; //all memory space
	int maxCapacity;
	int size; //used memory space

	int** pIndexes; //索引空间首地址
	int pIndexCount; //索引空间个数

	int* pDataEnd;

	void (*append)(struct ArrayList* list, const int value);
	void (*insert)(struct ArrayList* list, const int value, const int pos);
	boolean (*remove)(struct ArrayList* list, const int index, int* value);
	void (*dispose)(struct ArrayList* list);
	void (*clear)(struct ArrayList* list);
	void (*tirm)(struct ArrayList* list);
	boolean (*get)(struct ArrayList* list, const int index, int* value);
	void (*set)(struct ArrayList* list, const int value, const int index);

} ArrayList;
#define SIZEOF_ARRAYLIST (sizeof(ArrayList))

void initArrayCapacity(struct ArrayList* list, int max);
void initArray(struct ArrayList* list);

typedef struct LinkedList
{
	int size;
	int capacity;
	struct ListData* first;
	struct ListData* end;
	struct ListData* itNode;
	int* pDataEnd;
	void (*append)(struct LinkedList* list, const int value);
	void (*insert)(struct LinkedList* list, const int value, const int index);
	boolean (*removeAt)(struct LinkedList* list, const int index, int* value);
	boolean (*remove)(struct LinkedList* list, int value);
	void (*dispose)(struct LinkedList* list);
	void (*clear)(struct LinkedList* list);
	boolean (*get)(struct LinkedList* list, const int index, int* value);
	void (*iterator)(struct LinkedList* list);
	int (*next)(struct LinkedList* list);
	boolean (*hasNext)(struct LinkedList* list);
	int (*toArray)(struct LinkedList* list, int** array, const int length);
} LinkedList;
#define SIZEOF_LINKEDLIST (sizeof(LinkedList))

void initLinked(struct LinkedList* list);

#endif /* COLLECTION_H_ */
