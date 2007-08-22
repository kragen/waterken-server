#!/bin/sh

(cd joe-e/; ./build.sh $@)
(cd ref_send/; ./build.sh $@)
(cd example/; ./build.sh $@)
(cd network/; ./build.sh $@)
(cd web_send/; ./build.sh $@)
(cd persistence/; ./build.sh $@)
(cd remote/; ./build.sh $@)
(cd server/; ./build.sh $@)
