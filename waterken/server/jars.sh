#!/bin/sh
jar cmf SERVE.MF ../serve.jar X.class
jar cmf SHARE.MF ../share.jar X.class
jar cmf GENKEYRSA.MF ../genKeyRSA.jar X.class
