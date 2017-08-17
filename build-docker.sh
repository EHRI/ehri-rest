#!/bin/sh

mvn clean package -DskipTests
sudo docker build -t ehri/ehri-rest .
