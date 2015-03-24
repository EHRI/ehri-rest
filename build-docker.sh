#!/bin/sh

mvn package -DskipTests
sudo docker build -t ehri/ehri-rest .
