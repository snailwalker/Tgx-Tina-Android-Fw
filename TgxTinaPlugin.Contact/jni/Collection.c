/*
 * Collection.c
 *
 *  Created on: Oct 15, 2011
 *      listuthor: Zhangzhuo
 *
 *  记住: 当使用宏定义的类型时需要将此方法放到其使用者的前面定义
 *  funca(){
 *  }
 *  funcb(){
 *  	funca();
 *  }
 */
#include "Collection.h"

boolean arrayResize(ArrayList* list)
{
	int* desData;
	int newCapacity;
	//内存不够
	if (list->pIndexCount + 1 >= list->maxCapacity)
	{
		return false;
	}

	if (list->pIndexes == NULL)
	{
		list->pIndexes = (int**) malloc(list->maxCapacity * sizeof(int*));
	}

	newCapacity = list->capacity + PER_BLOCK_POINT_COUNT;
	desData = (int*) malloc(MALLOC_SIZE);

	*(list->pIndexes + list->pIndexCount) = desData;
	list->pIndexCount++;

	list->pDataEnd = desData;
	list->capacity = newCapacity;

	return true;
}

void arrayAppend(ArrayList* list, const int adrOrValue)
{
	int* ptr_index = 0;
	int pIndex = 0;
	int end = 0;
	if (list->size < list->capacity || arrayResize(list))
	{
		*list->pDataEnd = adrOrValue;
		list->size++;
		if (list->size < list->capacity)
		{
			end = list->size;
			pIndex = end >> BIT_OFFSET;
			ptr_index = list->pIndexes[pIndex];
			end = end & (PER_BLOCK_POINT_COUNT - 1);
			list->pDataEnd = ptr_index + end;
		}
	}
}

void arrayInsert(ArrayList* list, const int adrOrValue, const int pos)
{
	int index = 0;
	int* ptr = 0;
	int* ptr1 = 0;
	int* ptr_index = 0;
	int pIndex = 0;

	if (pos < 0)
	{
		return;
	}

	if (pos < list->size)
	{ //check position
		if (list->size < list->capacity || arrayResize(list))
		{
			ptr1 = list->pDataEnd;
			index = list->size - 1;
			while (index >= pos)
			{
				pIndex = index >> BIT_OFFSET;
				ptr_index = *(list->pIndexes + pIndex);
				ptr = ptr_index + (index & (PER_BLOCK_POINT_COUNT - 1));
				*ptr1 = *ptr;
				ptr1 = ptr;
				index--;
			}
			*ptr = adrOrValue;
			list->size++;

			if (list->size < list->capacity)
			{
				index = list->size;
				pIndex = index >> BIT_OFFSET;
				ptr_index = *(list->pIndexes + pIndex);
				ptr = ptr_index + (index & (PER_BLOCK_POINT_COUNT - 1));
				list->pDataEnd = ptr;
			}
		}
	}
	else
	{
		arrayAppend(list, adrOrValue);
	}
}

boolean arrayRemove(ArrayList* list, const int index, int* value)
{
	int* ptr1 = 0;
	int* ptr = 0;
	int* ptr_index = 0;
	int pIndex = 0;
	int pos = index + 1;
	if (index < 0 || list == NULL || index >= list->size) return false;
	pIndex = index >> BIT_OFFSET;
	ptr_index = *(list->pIndexes + pIndex);
	ptr = ptr_index + (index & (PER_BLOCK_POINT_COUNT - 1));
	*value = *ptr;

	while (pos < list->size)
	{
		pIndex = pos >> BIT_OFFSET;
		ptr_index = *(list->pIndexes + pIndex);
		ptr1 = ptr_index + (pos & (PER_BLOCK_POINT_COUNT - 1));
		*ptr = *ptr1;
		ptr = ptr1;
		pos++;
	}

	list->size--;
	pos = list->size;
	pIndex = pos >> BIT_OFFSET;
	ptr_index = *(list->pIndexes + pIndex);
	ptr = ptr_index + (pos & (PER_BLOCK_POINT_COUNT - 1));
	list->pDataEnd = ptr;
	return true;
}

void arrayDispose(ArrayList* list)
{
	int i = 0;

	if (list->capacity > 0)
	{
		for (i = 0; i < list->pIndexCount; i++)
			free(list->pIndexes[i]);

		free(list->pIndexes);
		list->size = 0;
		list->capacity = 0;
	}
}

void arrayClear(ArrayList* list)
{
	list->size = 0;
	if (list->capacity > 0)
	{
		list->pDataEnd = *list->pIndexes;
	}
}

void arrayTirm(ArrayList* list)
{
	int toTirmSize = list->capacity - list->size;
	if (toTirmSize > 0 && toTirmSize % PER_BLOCK_POINT_COUNT == 0)
	{
		int i = list->pIndexCount - 1;
		int pIndex = list->pIndexCount - toTirmSize - 1;
		for (; i > pIndex; i--)
		{
			free(*(list->pIndexes + i));
		}
	}
}

boolean arrayGet(ArrayList* list, const int index, int* value)
{
	int* ptr = 0;
	int* ptr_index = 0;
	int pIndex = 0;
	if (index < 0 || list == NULL || index >= list->size) return false;
	pIndex = index >> BIT_OFFSET;
	ptr_index = *(list->pIndexes + pIndex);
	ptr = ptr_index + (index & (PER_BLOCK_POINT_COUNT - 1));
	*value = *ptr;
	return true;
}

void arraySet(ArrayList* list, const int value, const int index)
{
	int* ptr = 0;
	int* ptr_index = 0;
	int pIndex = 0;

	if (index >= 0 && index < list->size)
	{
		pIndex = index >> BIT_OFFSET;
		ptr_index = *(list->pIndexes + pIndex);
		ptr = ptr_index + (index & (PER_BLOCK_POINT_COUNT - 1));
		*ptr = value;
	}
}

void initArrayCapacity(struct ArrayList* list, int max)
{
	list->maxCapacity = max;
	list->size = 0;
	list->capacity = 0;

	list->pIndexes = NULL;
	list->pIndexCount = 0;

	list->pDataEnd = NULL;

	list->append = &arrayAppend;
	list->insert = &arrayInsert;
	list->remove = &arrayRemove;
	list->dispose = &arrayDispose;
	list->clear = &arrayClear;
	list->tirm = &arrayTirm;
	list->get = &arrayGet;
	list->set = &arraySet;
}
void initArray(struct ArrayList* list)
{
	initArrayCapacity(list, INDEX_NUM_MAX);
}

boolean newLinkNode(struct LinkedList* list, ListData* posNode)
{
	ListData* desPtr = NULL;
	ListData* t = NULL;
	if (list->capacity < 0x7FFFFFFF)
	{
		desPtr = (ListData*) malloc(SIZEOF_LISTDATA);
		desPtr->next = NULL;
		list->capacity++;
		if (list->first == NULL && list->end == NULL)
		{
			list->first = list->end = desPtr;
			list->pDataEnd = &(list->end->pData);
		}
		else
		{
			if (posNode == NULL)
			{ //insert to head
				desPtr->next = list->first;
				list->first = desPtr;
			}
			else if (posNode->next != NULL)
			{ //insert to position
				t = posNode->next;
				posNode->next = desPtr;
				desPtr->next = t;
			}
			else
			{ //append
				list->end->next = desPtr;
				list->end = desPtr;
				list->pDataEnd = &(list->end->pData);
			}
		}
		return true;
	}
	return false;
}

void linkedAppend(struct LinkedList* list, const int adrOrValue)
{
	if (list->capacity > 0 && list->size < list->capacity)
	{
		*(list->pDataEnd) = adrOrValue;
		list->size++;
		if (list->size < list->capacity)
		{
			list->end = list->end->next;
			list->pDataEnd = &(list->end->pData);
		}
	}
	else if (newLinkNode(list, list->end))
	{
		*(list->pDataEnd) = adrOrValue;
		list->size++;
	}
}

void linkedDeleteLast(struct LinkedList* list)
{
	if (list->size < list->capacity)
	{ //由于size小于capacity 所以list->end->next一定非NULL
		ListData* node = list->end->next;
		list->end->next = node->next;
		free(node);
		list->capacity--;
	}
}

void linkedInsert(struct LinkedList* list, const int adrOrValue, const int index)
{
	if (index < 0) return;
	if (index < list->size)
	{
		int i = 0;
		ListData* node = NULL;
		if (index > 0)
		{
			node = list->first;
			for (i = 1; i < index; i++)
			{
				node = node->next;
			}
			linkedDeleteLast(list);
			if (newLinkNode(list, node))
			{
				node->next->pData = adrOrValue;
				list->size++;
			}
		}
		else if (newLinkNode(list, node))
		{
			list->first->pData = adrOrValue;
			list->size++;
		}
	}
	else linkedAppend(list, adrOrValue);
}

boolean linkedRemoveAt(struct LinkedList* list, const int index, int* value)
{
	int i = 0;
	if (index < 0 || list == NULL || list->size == 0 || index >= list->size) return false;
	ListData** curr = &list->first;
	ListData* entry = NULL;
	boolean result = false;
	for (; *curr; i++)
	{
		entry = *curr;
		if (i == index)
		{
			result = true;
			*value = entry->pData;
			*curr = entry->next;
			free(entry);
			goto RESULT;
		}
		else curr = &entry->next;
	}

	RESULT:
	{
		if (result)
		{
			list->size--;
			list->capacity--;
			if (list->capacity == 0) list->first = list->end = NULL;
		}
	}
	return result;
//
//	ListData* t = NULL;
//	ListData* node = NULL;
//	if (index < list->size)
//	{
//		node = list->first;
//		if (index > 0)
//		{
//			for (i = 1; i < index; i++)
//			{
//				node = node->next;
//			}
//			t = node->next;
//			node->next = t->next;
//			if (list->end == t)
//			{
//				list->end = node;
//				list->pDataEnd = &(list->end->pData);
//			}
//			result = t->pData;
//			free(t);
//		}
//		else
//		{
//			list->first = node->next;
//			result = node->pData;
//			free(node);
//		}
//		list->size--;
//		list->capacity--;
//		if (list->capacity == 0)
//		{
//			list->first = list->end = NULL;
//		}
//	}
//	return result;
}

boolean linkedRemove(struct LinkedList* list, const int value)
{
	ListData** curr = &list->first;
	ListData* entry = NULL;
	boolean result = false;
	int i = 0;
	for (; *curr;)
	{
		if (++i == list->size && (*curr)->pData == value) list->end = entry;
		entry = *curr;
		if (value == entry->pData)
		{
			*curr = entry->next; //最关键而精妙的设计 链接了当前的Next与下一个-Node节点
			free(entry);
			result = true;
			goto RESULT;
		}
		else curr = &entry->next;
	}

	RESULT:
	{
		if (result)
		{
			list->size--;
			list->capacity--;
			if (list->capacity == 0) list->first = list->end = NULL;
		}
	}
//	ListData* t = NULL;
//	ListData* node = NULL;
//	int i = 0;
//	node = list->first;
//	t = node->next;
//	for (i = 0; i < list->size; i++)
//	{
//		if (i > 0)
//		{
//			if (value == t->pData)
//			{
//				node->next = t->next;
//				free(t);
//				result = true;
//				goto RESULT;
//			}
//			else
//			{
//				node = node->next;
//				t = t->next;
//			}
//		}
//		else if (t != NULL && value == t->pData)
//		{ // 如果size只有一个，就没有下一个的情况，t也就为NULL了
//			list->first = node->next;
//			free(node);
//			result = true;
//			goto RESULT;
//		}
//	}

//	RESULT:
//	{
//		if (result)
//		{
//			list->size--;
//			list->capacity--;
//			if (list->capacity == 0)
//			{
//				list->first = list->end = NULL;
//			}
//			else if (i == list->size)
//			{
//				list->end = node;
//			}
//		}
//	}
	return result;
}

void linkedDispose(struct LinkedList* list)
{
	int i = 0;
	ListData* node = NULL;
	if (list->capacity > 0)
	{
		for (i = 0; i < list->capacity; i++)
		{
			node = list->first;
			list->first = node->next;
			node->next = NULL;
			free(node);
		}
		list->first = list->end = NULL;
		list->pDataEnd = NULL;
		list->capacity = 0;
		list->size = 0;
	}
}

void linkedClear(struct LinkedList* list)
{
	list->size = 0;
	list->end = list->first;
	list->pDataEnd = &(list->end->pData);
}

boolean linkedGet(struct LinkedList* list, const int index, int* value)
{
	ListData* node = NULL;
	if (index < 0 || list == NULL || index >= list->size) return false;
	int i;
	node = list->first;
	for (i = 0; i != index; i++)
	{
		node = node->next;
	}
	*value = node->pData;
	return true;
}

void linkedIterator(struct LinkedList* list)
{
	list->itNode = list->first;
}

boolean linkedHasNext(struct LinkedList* list)
{
	return list->itNode != NULL;
}

int linkedNext(struct LinkedList* list)
{
	if (list->itNode == NULL) return NULL;
	int result = list->itNode->pData;
	list->itNode = list->itNode->next;
	return result;
}

int linked2Array(LinkedList* list, int** array, const int length)
{
	int i = 0;
	int len = 0;
	ListData* node = NULL;
	if (array == NULL || list == NULL)
	{
		return 0;
	}
	len = length > list->size ? list->size : length;
	if (*array == NULL)
	{
		*array = (int*) malloc(sizeof(int) * len);
	}
	if (list->size == 0)
	{
		return 0;
	}
	node = list->first;
	for (i = 0; i < len; i++)
	{
		(*array)[i] = node->pData;
		node = node->next;
	}
	return len;
}

void initLinked(struct LinkedList* list)
{
	list->size = 0;
	list->capacity = 0;
	list->first = NULL;
	list->end = NULL;
	list->pDataEnd = NULL;
	list->append = &linkedAppend;
	list->insert = &linkedInsert;
	list->removeAt = &linkedRemoveAt;
	list->remove = &linkedRemove;
	list->dispose = &linkedDispose;
	list->clear = &linkedClear;
	list->get = &linkedGet;
	list->toArray = &linked2Array;
	list->iterator = &linkedIterator;
	list->next = &linkedNext;
	list->hasNext = &linkedHasNext;
}
