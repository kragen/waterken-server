#!/bin/sh
# Copyright 2005-06 Regents of the University of California.  May be used 
# under the terms of the revised BSD license.  See LICENSING for details.
# Author: Adrian Mettler

makeClass () {
  sed -e s/char/$1/g -e s/Character/$2/g -e s/Char/$3/g CharArray.java > ${3}Array.java
  echo Wrote ${3}Array.java
}

# primitive type, boxed type, capitalized primitive type
makeClass boolean Boolean Boolean
# makeClass byte    Byte    Byte # has custom serialization methods; change manually
makeClass short   Short   Short
makeClass int     Integer Int
makeClass long    Long    Long
makeClass float   Float   Float
makeClass double  Double  Double

