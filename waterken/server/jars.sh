#!/bin/sh
jar cmf SERVE.MF ../serve.jar X.class
jar cmf SHARE.MF ../share.jar X.class
jar cmf GENKEY.MF ../genkey.jar X.class
