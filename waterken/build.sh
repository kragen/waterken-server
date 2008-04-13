#!/bin/sh

mkdir -p vat
touch vat/.persistent-object-store
(cd joe-e/; ./build.sh $@)
(cd ref_send/; ./build.sh $@)
(cd web_send/; ./build.sh $@)
(cd shared/; ./build.sh $@)
(cd log/; ./build.sh $@)
(cd example/; ./build.sh $@)
(cd network/; ./build.sh $@)
(cd persistence/; ./build.sh $@)
(cd remote/; ./build.sh $@)
(cd dns/; ./build.sh $@)
(cd server/; ./build.sh $@)
(cd test/; ./build.sh $@)
