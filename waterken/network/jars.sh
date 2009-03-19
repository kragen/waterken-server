#!/bin/sh
jar cmf COMPACT.MF ../compact.jar X.class
jar cmf EXTRACT.MF ../extract.jar X.class
jar cmf LIST.MF ../list.jar X.class
