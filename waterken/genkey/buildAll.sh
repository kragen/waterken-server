#!/bin/sh

(cd ../joe-e/; ./build.sh $@)
(cd ../ref_send/; ./build.sh $@)
(cd ../network/; ./build.sh $@)
(cd ../log/; ./build.sh $@)
(cd ../persistence/; ./build.sh $@)
(cd ../syntax/; ./build.sh $@)
(cd ../remote/; ./build.sh $@)
(cd ../shared/; ./build.sh $@)
(cd ../example/; ./build.sh $@)
(cd ../dns/; ./build.sh $@)
(cd ../genkey/; ./build.sh $@)
