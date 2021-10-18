#!/bin/sh

mvn clean package -DskipTests
sudo docker build -t ehri/ehri-rest --build-arg GIT_COMMIT=$(git rev-parse HEAD) .
