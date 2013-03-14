#!/bin/sh

./scripts/cmd $1 initialize
./scripts/cmd $1 useradd mike -group admin
./scripts/cmd $1 ead-import --createrepo -repo wiener-library -user mike -tolerant ~/Dropbox/EHRI-WP19-20/TestData/wiener-library/*xml
./scripts/cmd $1 eac-import -repo wiener-library -user mike -tolerant ~/Dropbox/EHRI-WP19-20/TestData/eac-dump-140313/*xml
./scripts/cmd $1 eag-import -repo wiener-library -user mike -tolerant ~/Dropbox/EHRI-WP19-20/TestData/eag-dump-080313/*xml
