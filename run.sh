#!/bin/bash

JAVA_HOME=/opt/jdk1.8.0_331 mvn package

rm /opt/Fiji.app/plugins/worm-cluster-1-0.1.0.jar
cp ./target/worm-cluster-1-0.1.0.jar /opt/Fiji.app/plugins/worm-cluster-1-0.1.0.jar

/opt/Fiji.app/ImageJ-linux64 --run IdentifyService

