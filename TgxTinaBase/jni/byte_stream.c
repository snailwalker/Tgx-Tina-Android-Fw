/*
 * byte_stream.c
 *
 *  Created on: 2012-8-3
 *      Author: Zhangzhuo
 */
#include "byte_stream.h"

size_t bufInitSize = 0;

void byteBufferFinal(ByteBuffer* buf) {
	buf->size = 0;
	buf->capacity = 0;
	buf->next = NULL;
	buf->index = 0;
	free(buf->data);
}

void byteBufferInit(ByteBuffer* buf) {
	assert(bufInitSize > 0);
	buf->size = 0;
	buf->mark = 0;
	buf->capacity = bufInitSize;
	buf->next = NULL;
	buf->index = 0;
	buf->data = (byte*) malloc(bufInitSize);
	buf->dispose = &byteBufferFinal;
}

void byteStreamFinal(ByteArrayStream * bas) {
	assert(bas != NULL);
	size_t i = bas->writeAt->size + bas->writeAt->index * bufInitSize;
	ByteBuffer* buf = NULL;
	for (buf = bas->first; i > 0; buf = bas->first) {
		bas->first = buf->next;
		i -= buf->size;
		buf->dispose(buf);
		free(buf);
	}
}

void byteStreamExpand(ByteArrayStream* bas) {
	ByteBuffer * buf = NULL;
	buf = bas->writeAt->next == NULL ? (ByteBuffer*) malloc(sizeof(ByteBuffer)) : bas->writeAt->next;
	byteBufferInit(buf);
	buf->index = bas->writeAt->index + 1;
	bas->writeAt->next = buf;
	bas->writeAt = buf;
}

size_t byteStreamWrite(ByteArrayStream* out, byte value) {
	size_t size = 0;
	size_t capacity = 0;
	WRITE: {
		size = out->writeAt->size;
		capacity = out->writeAt->capacity;
		if (size < capacity) {
			out->writeAt->data[size] = value;
			out->writeAt->size++;
			return 1;
		} else {
			byteStreamExpand(out);
			goto WRITE;
		}
	}
	return 0;
}

size_t byteStreamWriteShort(ByteArrayStream* out, word16 value) {
	size_t written = 0;
	byte* v = (byte*) &value;
	int i = 0;
#if BYTE_ORDER == BIG_ENDIAN
	for (i = 0; i < 2; i++)
#else
	for (i = 1; i >= 0; i--)
#endif
		written += byteStreamWrite(out, v[i]);
	return written;
}

size_t byteStreamWriteInt(ByteArrayStream* out, word32 value) {
	size_t written = 0;
	byte* v = (byte*) &value;
	int i = 0;
#if BYTE_ORDER == BIG_ENDIAN
	for (i = 0; i < 4; i++)
#else
	for (i = 3; i >= 0; i--)
#endif
		written += byteStreamWrite(out, v[i]);
	return written;
}

size_t byteStreamWriteLong(ByteArrayStream* out, word64 value) {
	size_t written = 0;
	byte* v = (byte*) &value;
	int i = 0;
#if BYTE_ORDER == BIG_ENDIAN
	for (i = 0; i < 8; i++)
#else
	for (i = 8; i >= 0; i--)
#endif
		written += byteStreamWrite(out, v[i]);
	return written;
}

size_t byteStreamWriteArray(ByteArrayStream* out, byte* values, size_t len) {
	size_t written = 0, size = 0, capacity = 0, toWrite = len;
	if (len == 0) return 0;
	WRITE: {
		size = out->writeAt->size;
		capacity = out->writeAt->capacity;
		if (size + toWrite <= capacity) {
			memcpy(size + out->writeAt->data, values + written, toWrite);
			out->writeAt->size += toWrite;
			written += toWrite;
			if (written < len) goto WRITE;
			return len;
		} else if (size < capacity) {
			toWrite = capacity - size;
			memcpy(size + out->writeAt->data, values + written, toWrite);
			out->writeAt->size += toWrite;
			written += toWrite;
			goto WRITE;
		} else {
			byteStreamExpand(out);
			toWrite = len - written;
			goto WRITE;
		}
	}
	return 0;
}

void byteStreamShrink(ByteArrayStream* bas) {
	ByteBuffer *buf, *node;
	if (bas->readAt == bas->first) return;
	buf = bas->readAt;
	if (bas->first->next == buf) {
		node = bas->first->next;
		while (node->next != NULL ) {
			node->index--;
			node = node->next;
		}
		node->index--;
		node->next = bas->first;
		node->next->index = node->index + 1;
		node->next->mark = 0;
		node->next->size = 0;
		node->next->next = NULL;
		bas->first = buf;
	} else printf("怎么有这种事~~");
}

int byteStreamRead(ByteArrayStream* in) {
	int result = -1;
	pos rMark, size;
	READ: {
		rMark = in->readAt->mark;
		size = in->readAt->size;
		if (rMark < size) {
			in->readAt->mark++;
			return in->readAt->data[rMark];
		} else if (in->readAt->next != NULL && size > 0) {
			in->readAt = in->readAt->next;
			byteStreamShrink(in);
			goto READ;
		}
	}
	return result;
}

size_t byteStreamReadArray(ByteArrayStream* in, byte* dest, size_t len) {
	pos rMark, size;
	size_t toRead = len, hasRead = 0;
	READ: {
		rMark = in->readAt->mark;
		size = in->readAt->size;
		if (rMark + toRead < size) {
			in->readAt->mark += toRead;
			memcpy(dest + hasRead, in->readAt->data + rMark, toRead);
			hasRead += toRead;
			return hasRead;
		} else if (rMark < size) {
			toRead = size - rMark;
			in->readAt->mark += toRead;
			memcpy(dest + hasRead, in->readAt->data + rMark, toRead);
			hasRead += toRead;
			goto READ;
		} else if (in->readAt->next != NULL && size > 0) {
			in->readAt = in->readAt->next;
			byteStreamShrink(in);
			toRead = len - hasRead;
			goto READ;
		}
	}
	return hasRead;
}
byte* byteStreamToArray(ByteArrayStream* bas) {
	size_t size = bas->writeAt->size + bufInitSize * bas->writeAt->index;
	byte* array = (byte*) malloc(size);
	word32 i = 0;
	ByteBuffer *node = bas->first;
	size_t done = 0;
	for (; i < bas->writeAt->index; i++) {
		memcpy(array + done, node->data, node->capacity);
		done += node->capacity;
		node = node->next;
	}
	memcpy(array + done, node->data, node->size);
	done += node->size;
	return array;
}
void byteStreamInit(ByteArrayStream* bas, size_t bufSize) {
	assert(bas != NULL);
	ByteBuffer * buf = (ByteBuffer*) malloc(sizeof(ByteBuffer));
	bufInitSize = bufSize;
	byteBufferInit(buf);
	bas->first = buf;
	bas->writeAt = buf;
	bas->readAt = buf;
	//函数指针初始化
	bas->read = &byteStreamRead;
	bas->readArray = &byteStreamReadArray;
	bas->write = &byteStreamWrite;
	bas->writeShort = &byteStreamWriteShort;
	bas->writeInt = &byteStreamWriteInt;
	bas->writeLong = &byteStreamWriteLong;
	bas->writeArray = &byteStreamWriteArray;
	bas->toArray = &byteStreamToArray;
	bas->dispose = &byteStreamFinal;

}
