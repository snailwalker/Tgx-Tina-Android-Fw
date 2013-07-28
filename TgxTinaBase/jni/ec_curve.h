#ifndef __EC_CURVE_H
#define __EC_CURVE_H

#include <stddef.h>

#include "environment.h"
#include "ec_field.h"
#include "ec_vlong.h"

#ifndef USUAL_TYPES
#define USUAL_TYPES
typedef unsigned char byte; /*  8 bit */
typedef unsigned short word16; /* 16 bit */
#ifdef _BSD_UNIX_X64
typedef unsigned int word32; /* 32 bit */
typedef unsigned long word64; /* 64 bit */
#else
typedef unsigned long word32; /* 32 bit */
typedef unsigned long long word64; /* 64 bit */
#endif
#endif /* ?USUAL_TYPES */

typedef struct {
	gfPoint x, y;
} ecPoint;


extern const vlPoint prime_order;
extern const ecPoint curve_point;

#ifdef SELF_TESTING

int ecCheck (const ecPoint *p);
	/* confirm that y^2 + x*y = x^3 + EC_B for point p */

void ecPrint (FILE *out, const char *tag, const ecPoint *p);
	/* printf prefix tag and the contents of p to file out */

int ecEqual (const ecPoint *p, const ecPoint *q);
	/* evaluates to 1 if p == q, otherwise 0 (or an error code) */
#endif /* SELF_TESTING */
void ecCopy (ecPoint *p, const ecPoint *q);
	/* sets p := q */

int ecCalcY (ecPoint *p, int ybit);
	/* given the x coordinate of p, evaluate y such that y^2 + x*y = x^3 + EC_B */
#ifdef SELF_TESTING
void ecRandom (ecPoint *p);
	/* sets p to a random point of the elliptic curve defined by y^2 + x*y = x^3 + EC_B */

void ecClear (ecPoint *p);
	/* sets p to the point at infinity O, clearing entirely the content of p */
#endif /* SELF_TESTING */
void ecAdd (ecPoint *p, const ecPoint *r);
	/* sets p := p + r */

void ecSub (ecPoint *p, const ecPoint *r);
	/* sets p := p - r */
#ifdef SELF_TESTING
void ecNegate (ecPoint *p);
	/* sets p := -p */
#endif
void ecDouble (ecPoint *p);
	/* sets p := 2*p */

void ecMultiply (ecPoint *p, const vlPoint k);
	/* sets p := k*p */

int ecYbit (const ecPoint *p);
	/* evaluates to 0 if p->x == 0, otherwise to gfYbit (p->y / p->x) */

void ecPack (const ecPoint *p, vlPoint k);
	/* packs a curve point into a vlPoint */

void ecUnpack (ecPoint *p, const vlPoint k);
	/* unpacks a vlPoint into a curve point */
#ifdef SELF_TESTING
int ecSelfTest (int test_count);
	/* perform test_count self tests */
#endif /* SELF_TESTING */
#endif /* __EC_CURVE_H */
