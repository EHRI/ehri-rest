#!/bin/sh

VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)')
mvn clean package -DskipTests
sudo docker build -t ehri/ehri-rest -t ehri/ehri-rest:$VERSION --build-arg GIT_COMMIT=$(git rev-parse HEAD) .
