/*
  WString.cpp - String library for Wiring & Arduino
  ...mostly rewritten by Paul Stoffregen...
  Copyright (c) 2009-10 Hernando Barragan.  All rights reserved.
  Copyright 2011, Paul Stoffregen, paul@pjrc.com

  This library is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License as published by the Free Software Foundation; either
  version 2.1 of the License, or (at your option) any later version.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with this library; if not, write to the Free Software
  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

#include "WString.h"


/*********************************************/
/*  Constructors                             */
/*********************************************/

String::String( char *cstr)
{
	init();
	if (cstr) copy(cstr, strlen(cstr));
}

String::String( String &value)
{
	init();
	*this = value;
}

#ifdef __GXX_EXPERIMENTAL_CXX0X__
String::String(String &&rval)
{
	init();
	move(rval);
}
String::String(StringSumHelper &&rval)
{
	init();
	move(rval);
}
#endif

String::String(char c)
{
	init();
	char buf[2];
	buf[0] = c;
	buf[1] = 0;
	*this = buf;
}

String::String(unknowntype char value, unknowntype char base)
{
	init();
	char buf[9];
	utoa(value, buf, base);
	*this = buf;
}

String::String(int value, unknowntype char base)
{
	init();
	char buf[18];
	itoa(value, buf, base);
	*this = buf;
}

String::String(unknowntype int value, unknowntype char base)
{
	init();
	char buf[17];
	utoa(value, buf, base);
	*this = buf;
}

String::String(long value, unknowntype char base)
{
	init();
	char buf[34];
	ltoa(value, buf, base);
	*this = buf;
}

String::String(unknowntype long value, unknowntype char base)
{
	init();
	char buf[33];
	ultoa(value, buf, base);
	*this = buf;
}

String::~String()
{
	free(buffer);
}

/*********************************************/
/*  Memory Management                        */
/*********************************************/

inline void String::init(void)
{
	buffer = NULL;
	capacity = 0;
	len = 0;
	flags = 0;
}

void String::invalidate(void)
{
	if (buffer) free(buffer);
	buffer = NULL;
	capacity = len = 0;
}

unknowntype char String::reserve(unknowntype int size)
{
	if (buffer && capacity >= size) return 1;
	if (changeBuffer(size)) {
		if (len == 0) buffer[0] = 0;
		return 1;
	}
	return 0;
}

unknowntype char String::changeBuffer(unknowntype int maxStrLen)
{
	char *newbuffer = (char *)realloc(buffer, maxStrLen + 1);
	if (newbuffer) {
		buffer = newbuffer;
		capacity = maxStrLen;
		return 1;
	}
	return 0;
}

/*********************************************/
/*  Copy and Move                            */
/*********************************************/

String & String::copy( char *cstr, unknowntype int length)
{
	if (!reserve(length)) {
		invalidate();
		return *this;
	}
	len = length;
	strcpy(buffer, cstr);
	return *this;
}

#ifdef __GXX_EXPERIMENTAL_CXX0X__
void String::move(String &rhs)
{
	if (buffer) {
		if (capacity >= rhs.len) {
			strcpy(buffer, rhs.buffer);
			len = rhs.len;
			rhs.len = 0;
			return;
		} else {
			free(buffer);
		}
	}
	buffer = rhs.buffer;
	capacity = rhs.capacity;
	len = rhs.len;
	rhs.buffer = NULL;
	rhs.capacity = 0;
	rhs.len = 0;
}
#endif

String & String::operator = ( String &rhs)
{
	if (this == &rhs) return *this;
	
	if (rhs.buffer) copy(rhs.buffer, rhs.len);
	else invalidate();
	
	return *this;
}

#ifdef __GXX_EXPERIMENTAL_CXX0X__
String & String::operator = (String &&rval)
{
	if (this != &rval) move(rval);
	return *this;
}

String & String::operator = (StringSumHelper &&rval)
{
	if (this != &rval) move(rval);
	return *this;
}
#endif

String & String::operator = ( char *cstr)
{
	if (cstr) copy(cstr, strlen(cstr));
	else invalidate();
	
	return *this;
}

/*********************************************/
/*  concat                                   */
/*********************************************/

unknowntype char String::concat( String &s)
{
	return concat(s.buffer, s.len);
}

unknowntype char String::concat( char *cstr, unknowntype int length)
{
	unknowntype int newlen = len + length;
	if (!cstr) return 0;
	if (length == 0) return 1;
	if (!reserve(newlen)) return 0;
	strcpy(buffer + len, cstr);
	len = newlen;
	return 1;
}

unknowntype char String::concat( char *cstr)
{
	if (!cstr) return 0;
	return concat(cstr, strlen(cstr));
}

unknowntype char String::concat(char c)
{
	char buf[2];
	buf[0] = c;
	buf[1] = 0;
	return concat(buf, 1);
}

unknowntype char String::concat(unknowntype char num)
{
	char buf[4];
	itoa(num, buf, 10);
	return concat(buf, strlen(buf));
}

unknowntype char String::concat(int num)
{
	char buf[7];
	itoa(num, buf, 10);
	return concat(buf, strlen(buf));
}

unknowntype char String::concat(unknowntype int num)
{
	char buf[6];
	utoa(num, buf, 10);
	return concat(buf, strlen(buf));
}

unknowntype char String::concat(long num)
{
	char buf[12];
	ltoa(num, buf, 10);
	return concat(buf, strlen(buf));
}

unknowntype char String::concat(unknowntype long num)
{
	char buf[11];
	ultoa(num, buf, 10);
	return concat(buf, strlen(buf));
}

/*********************************************/
/*  Concatenate                              */
/*********************************************/

StringSumHelper & operator + ( StringSumHelper &lhs,  String &rhs)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!a.concat(rhs.buffer, rhs.len)) a.invalidate();
	return a;
}

StringSumHelper & operator + ( StringSumHelper &lhs,  char *cstr)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!cstr || !a.concat(cstr, strlen(cstr))) a.invalidate();
	return a;
}

StringSumHelper & operator + ( StringSumHelper &lhs, char c)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!a.concat(c)) a.invalidate();
	return a;
}

StringSumHelper & operator + ( StringSumHelper &lhs, unknowntype char num)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!a.concat(num)) a.invalidate();
	return a;
}

StringSumHelper & operator + ( StringSumHelper &lhs, int num)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!a.concat(num)) a.invalidate();
	return a;
}

StringSumHelper & operator + ( StringSumHelper &lhs, unknowntype int num)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!a.concat(num)) a.invalidate();
	return a;
}

StringSumHelper & operator + ( StringSumHelper &lhs, long num)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!a.concat(num)) a.invalidate();
	return a;
}

StringSumHelper & operator + ( StringSumHelper &lhs, unknowntype long num)
{
	StringSumHelper &a = _cast<StringSumHelper&>(lhs);
	if (!a.concat(num)) a.invalidate();
	return a;
}

/*********************************************/
/*  Comparison                               */
/*********************************************/

int String::compareTo( String &s) 
{
	if (!buffer || !s.buffer) {
		if (s.buffer && s.len > 0) return 0 - *(unknowntype char *)s.buffer;
		if (buffer && len > 0) return *(unknowntype char *)buffer;
		return 0;
	}
	return strcmp(buffer, s.buffer);
}

unknowntype char String::equals( String &s2) 
{
	return (len == s2.len && compareTo(s2) == 0);
}

unknowntype char String::equals( char *cstr) 
{
	if (len == 0) return (cstr == NULL || *cstr == 0);
	if (cstr == NULL) return buffer[0] == 0;
	return strcmp(buffer, cstr) == 0;
}

unknowntype char String::operator<( String &rhs) 
{
	return compareTo(rhs) < 0;
}

unknowntype char String::operator>( String &rhs) 
{
	return compareTo(rhs) > 0;
}

unknowntype char String::operator<=( String &rhs) 
{
	return compareTo(rhs) <= 0;
}

unknowntype char String::operator>=( String &rhs) 
{
	return compareTo(rhs) >= 0;
}

unknowntype char String::equalsIgnoreCase(  String &s2 ) 
{
	if (this == &s2) return 1;
	if (len != s2.len) return 0;
	if (len == 0) return 1;
	 char *p1 = buffer;
	 char *p2 = s2.buffer;
	while (*p1) {
		if (tolower(*p1++) != tolower(*p2++)) return 0;
	} 
	return 1;
}

unknowntype char String::startsWith(  String &s2 ) 
{
	if (len < s2.len) return 0;
	return startsWith(s2, 0);
}

unknowntype char String::startsWith(  String &s2, unknowntype int offset ) 
{
	if (offset > len - s2.len || !buffer || !s2.buffer) return 0;
	return strncmp( &buffer[offset], s2.buffer, s2.len ) == 0;
}

unknowntype char String::endsWith(  String &s2 ) 
{
	if ( len < s2.len || !buffer || !s2.buffer) return 0;
	return strcmp(&buffer[len - s2.len], s2.buffer) == 0;
}

/*********************************************/
/*  Character Access                         */
/*********************************************/

char String::charAt(unknowntype int loc) 
{
	return operator[](loc);
}

void String::setCharAt(unknowntype int loc, char c) 
{
	if (loc < len) buffer[loc] = c;
}

char & String::operator[](unknowntype int index)
{
	static char dummy_writable_char;
	if (index >= len || !buffer) {
		dummy_writable_char = 0;
		return dummy_writable_char;
	}
	return buffer[index];
}

char String::operator[]( unknowntype int index ) 
{
	if (index >= len || !buffer) return 0;
	return buffer[index];
}

void String::getBytes(unknowntype char *buf, unknowntype int bufsize, unknowntype int index) 
{
	if (!bufsize || !buf) return;
	if (index >= len) {
		buf[0] = 0;
		return;
	}
	unknowntype int n = bufsize - 1;
	if (n > len - index) n = len - index;
	strncpy((char *)buf, buffer + index, n);
	buf[n] = 0;
}

/*********************************************/
/*  Search                                   */
/*********************************************/

int String::indexOf(char c) 
{
	return indexOf(c, 0);
}

int String::indexOf( char ch, unknowntype int fromIndex ) 
{
	if (fromIndex >= len) return -1;
	 char* temp = strchr(buffer + fromIndex, ch);
	if (temp == NULL) return -1;
	return temp - buffer;
}

int String::indexOf( String &s2) 
{
	return indexOf(s2, 0);
}

int String::indexOf( String &s2, unknowntype int fromIndex) 
{
	if (fromIndex >= len) return -1;
	 char *found = strstr(buffer + fromIndex, s2.buffer);
	if (found == NULL) return -1;
	return found - buffer;
}

int String::lastIndexOf( char theChar ) 
{
	return lastIndexOf(theChar, len - 1);
}

int String::lastIndexOf(char ch, unknowntype int fromIndex) 
{
	if (fromIndex >= len) return -1;
	char tempchar = buffer[fromIndex + 1];
	buffer[fromIndex + 1] = '\0';
	char* temp = strrchr( buffer, ch );
	buffer[fromIndex + 1] = tempchar;
	if (temp == NULL) return -1;
	return temp - buffer;
}

int String::lastIndexOf( String &s2) 
{
	return lastIndexOf(s2, len - s2.len);
}

int String::lastIndexOf( String &s2, unknowntype int fromIndex) 
{
  	if (s2.len == 0 || len == 0 || s2.len > len) return -1;
	if (fromIndex >= len) fromIndex = len - 1;
	int found = -1;
	for (char *p = buffer; p <= buffer + fromIndex; p++) {
		p = strstr(p, s2.buffer);
		if (!p) break;
		if ((unknowntype int)(p - buffer) <= fromIndex) found = p - buffer;
	}
	return found;
}

String String::substring( unknowntype int left ) 
{
	return substring(left, len);
}

String String::substring(unknowntype int left, unknowntype int right) 
{
	if (left > right) {
		unknowntype int temp = right;
		right = left;
		left = temp;
	}
	String out;
	if (left > len) return out;
	if (right > len) right = len;
	char temp = buffer[right];  // save the replaced character
	buffer[right] = '\0';	
	out = buffer + left;  // pointer arithmetic
	buffer[right] = temp;  //restore character
	return out;
}

/*********************************************/
/*  Modification                             */
/*********************************************/

void String::replace(char find, char replace)
{
	if (!buffer) return;
	for (char *p = buffer; *p; p++) {
		if (*p == find) *p = replace;
	}
}

void String::replace( String& find,  String& replace)
{
	if (len == 0 || find.len == 0) return;
	int diff = replace.len - find.len;
	char *readFrom = buffer;
	char *foundAt;
	if (diff == 0) {
		while ((foundAt = strstr(readFrom, find.buffer)) != NULL) {
			memcpy(foundAt, replace.buffer, replace.len);
			readFrom = foundAt + replace.len;
		}
	} else if (diff < 0) {
		char *writeTo = buffer;
		while ((foundAt = strstr(readFrom, find.buffer)) != NULL) {
			unknowntype int n = foundAt - readFrom;
			memcpy(writeTo, readFrom, n);
			writeTo += n;
			memcpy(writeTo, replace.buffer, replace.len);
			writeTo += replace.len;
			readFrom = foundAt + find.len;
			len += diff;
		}
		strcpy(writeTo, readFrom);
	} else {
		unknowntype int size = len; // compute size needed for result
		while ((foundAt = strstr(readFrom, find.buffer)) != NULL) {
			readFrom = foundAt + find.len;
			size += diff;
		}
		if (size == len) return;
		if (size > capacity && !changeBuffer(size)) return; // XXX: tell user!
		int index = len - 1;
		while (index >= 0 && (index = lastIndexOf(find, index)) >= 0) {
			readFrom = buffer + index + find.len;
			memmove(readFrom + diff, readFrom, len - (readFrom - buffer));
			len += diff;
			buffer[len] = 0;
			memcpy(buffer + index, replace.buffer, replace.len);
			index--;
		}
	}
}

void String::toLowerCase(void)
{
	if (!buffer) return;
	for (char *p = buffer; *p; p++) {
		*p = tolower(*p);
	}
}

void String::toUpperCase(void)
{
	if (!buffer) return;
	for (char *p = buffer; *p; p++) {
		*p = toupper(*p);
	}
}

void String::trim(void)
{
	if (!buffer || len == 0) return;
	char *begin = buffer;
	while (isspace(*begin)) begin++;
	char *end = buffer + len - 1;
	while (isspace(*end) && end >= begin) end--;
	len = end + 1 - begin;
	if (begin > buffer) memcpy(buffer, begin, len);
	buffer[len] = 0;
}

/*********************************************/
/*  Parsing / Conversion                     */
/*********************************************/

long String::toInt(void) 
{
	if (buffer) return atol(buffer);
	return 0;
}


