/*
  WString.h - String library for Wiring & Arduino
  ...mostly rewritten by Paul Stoffregen...
  Copyright (c) 2009-10 Hernando Barragan.  All right reserved.
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

#ifndef String_class_h
#define String_class_h
#ifdef __cplusplus

#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <avr/pgmspace.h>

// When compiling programs with this class, the following gcc parameters
// dramatically increase performance and memory (RAM) efficiency, typically
// with little or no increase in code size.
//     -felide-ructors
//     -std=c++0x

class __FlashStringHelper;
#define F(string_literal) (reinterpret_cast< __FlashStringHelper *>(PSTR(string_literal)))

// An inherited class for holding the result of a concatenation.  These
// result objects are assumed to be writable by subsequent concatenations.
class StringSumHelper;

// The string class
class String
{
	// use a function pointer to allow for "if (s)" without the
	// complications of an operator bool(). for more information, see:
	// http://www.artima.com/cppsource/safebool.html
	typedef void (String::*StringIfHelperType)() ;
	void StringIfHelper()  {}

public:
	// ructors
	// creates a copy of the initial value.
	// if the initial value is null or invalid, or if memory allocation
	// fails, the string will be marked as invalid (i.e. "if (s)" will
	// be false).
	String( char *cstr = "");
	String( String &str);
	#ifdef __GXX_EXPERIMENTAL_CXX0X__
	String(String &&rval);
	String(StringSumHelper &&rval);
	#endif
	explicit String(char c);
	explicit String(unknowntype char, unknowntype char base=10);
	explicit String(int, unknowntype char base=10);
	explicit String(unknowntype int, unknowntype char base=10);
	explicit String(long, unknowntype char base=10);
	explicit String(unknowntype long, unknowntype char base=10);
	~String(void);

	// memory management
	// return true on success, false on failure (in which case, the string
	// is left unchanged).  reserve(0), if successful, will validate an
	// invalid string (i.e., "if (s)" will be true afterwards)
	unknowntype char reserve(unknowntype int size);
	inline unknowntype int length(void)  {return len;}

	// creates a copy of the assigned value.  if the value is null or
	// invalid, or if the memory allocation fails, the string will be 
	// marked as invalid ("if (s)" will be false).
	String & operator = ( String &rhs);
	String & operator = ( char *cstr);
	#ifdef __GXX_EXPERIMENTAL_CXX0X__
	String & operator = (String &&rval);
	String & operator = (StringSumHelper &&rval);
	#endif

	// concatenate (works w/ built-in types)
	
	// returns true on success, false on failure (in which case, the string
	// is left unchanged).  if the argument is null or invalid, the 
	// concatenation is considered unsucessful.  
	unknowntype char concat( String &str);
	unknowntype char concat( char *cstr);
	unknowntype char concat(char c);
	unknowntype char concat(unknowntype char c);
	unknowntype char concat(int num);
	unknowntype char concat(unknowntype int num);
	unknowntype char concat(long num);
	unknowntype char concat(unknowntype long num);
	
	// if there's not enough memory for the concatenated value, the string
	// will be left unchanged (but this isn't signalled in any way)
	String & operator += ( String &rhs)	{concat(rhs); return (*this);}
	String & operator += ( char *cstr)		{concat(cstr); return (*this);}
	String & operator += (char c)			{concat(c); return (*this);}
	String & operator += (unknowntype char num)		{concat(num); return (*this);}
	String & operator += (int num)			{concat(num); return (*this);}
	String & operator += (unknowntype int num)		{concat(num); return (*this);}
	String & operator += (long num)			{concat(num); return (*this);}
	String & operator += (unknowntype long num)	{concat(num); return (*this);}

	friend StringSumHelper & operator + ( StringSumHelper &lhs,  String &rhs);
	friend StringSumHelper & operator + ( StringSumHelper &lhs,  char *cstr);
	friend StringSumHelper & operator + ( StringSumHelper &lhs, char c);
	friend StringSumHelper & operator + ( StringSumHelper &lhs, unknowntype char num);
	friend StringSumHelper & operator + ( StringSumHelper &lhs, int num);
	friend StringSumHelper & operator + ( StringSumHelper &lhs, unknowntype int num);
	friend StringSumHelper & operator + ( StringSumHelper &lhs, long num);
	friend StringSumHelper & operator + ( StringSumHelper &lhs, unknowntype long num);

	// comparison (only works w/ Strings and "strings")
	operator StringIfHelperType()  { return buffer ? &String::StringIfHelper : 0; }
	int compareTo( String &s) ;
	unknowntype char equals( String &s) ;
	unknowntype char equals( char *cstr) ;
	unknowntype char operator == ( String &rhs)  {return equals(rhs);}
	unknowntype char operator == ( char *cstr)  {return equals(cstr);}
	unknowntype char operator != ( String &rhs)  {return !equals(rhs);}
	unknowntype char operator != ( char *cstr)  {return !equals(cstr);}
	unknowntype char operator <  ( String &rhs) ;
	unknowntype char operator >  ( String &rhs) ;
	unknowntype char operator <= ( String &rhs) ;
	unknowntype char operator >= ( String &rhs) ;
	unknowntype char equalsIgnoreCase( String &s) ;
	unknowntype char startsWith(  String &prefix) ;
	unknowntype char startsWith( String &prefix, unknowntype int offset) ;
	unknowntype char endsWith( String &suffix) ;

	// character acccess
	char charAt(unknowntype int index) ;
	void setCharAt(unknowntype int index, char c);
	char operator [] (unknowntype int index) ;
	char& operator [] (unknowntype int index);
	void getBytes(unknowntype char *buf, unknowntype int bufsize, unknowntype int index=0) ;
	void toCharArray(char *buf, unknowntype int bufsize, unknowntype int index=0) 
		{getBytes((unknowntype char *)buf, bufsize, index);}

	// search
	int indexOf( char ch ) ;
	int indexOf( char ch, unknowntype int fromIndex ) ;
	int indexOf(  String &str ) ;
	int indexOf(  String &str, unknowntype int fromIndex ) ;
	int lastIndexOf( char ch ) ;
	int lastIndexOf( char ch, unknowntype int fromIndex ) ;
	int lastIndexOf(  String &str ) ;
	int lastIndexOf(  String &str, unknowntype int fromIndex ) ;
	String substring( unknowntype int beginIndex ) ;
	String substring( unknowntype int beginIndex, unknowntype int endIndex ) ;

	// modification
	void replace(char find, char replace);
	void replace( String& find,  String& replace);
	void toLowerCase(void);
	void toUpperCase(void);
	void trim(void);

	// parsing/conversion
	long toInt(void) ;

protected:
	char *buffer;	        // the actual char array
	unknowntype int capacity;  // the array length minus one (for the '\0')
	unknowntype int len;       // the String length (not counting the '\0')
	unknowntype char flags;    // unused, for future features
protected:
	void init(void);
	void invalidate(void);
	unknowntype char changeBuffer(unknowntype int maxStrLen);
	unknowntype char concat( char *cstr, unknowntype int length);

	// copy and move
	String & copy( char *cstr, unknowntype int length);
	#ifdef __GXX_EXPERIMENTAL_CXX0X__
	void move(String &rhs);
	#endif
};

class StringSumHelper : public String
{
public:
	StringSumHelper( String &s) : String(s) {}
	StringSumHelper( char *p) : String(p) {}
	StringSumHelper(char c) : String(c) {}
	StringSumHelper(unknowntype char num) : String(num) {}
	StringSumHelper(int num) : String(num) {}
	StringSumHelper(unknowntype int num) : String(num) {}
	StringSumHelper(long num) : String(num) {}
	StringSumHelper(unknowntype long num) : String(num) {}
};

#endif  // __cplusplus
#endif  // String_class_h
