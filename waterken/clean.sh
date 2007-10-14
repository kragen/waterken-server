#!/bin/sh

rm -rf javadoc/
(cd joe-e/; ./clean.sh $@)
(cd ref_send/; ./clean.sh $@)
(cd web_send/; ./clean.sh $@)
(cd example/; ./clean.sh $@)
(cd network/; ./clean.sh $@)
(cd persistence/; ./clean.sh $@)
(cd remote/; ./clean.sh $@)
(cd dns/; ./clean.sh $@)
(cd server/; ./clean.sh $@)
