#!/bin/sh

(cd example/; ./test.sh $@)
(cd network/; ./test.sh $@)
(cd remote/; ./test.sh $@)
