#!/usr/bin/env bash

set -eu

mkdir -p out

javac -d out $(find src -name "*.java")

#java -Xms2g -Xmx2g -XX:+UseG1GC -cp out dev.github.ablearthy.SpeedTester "$@"
java -Xms2g -Xmx2g -XX:+UseG1GC -cp out dev.github.ablearthy.InconsistencyTester "$@"
