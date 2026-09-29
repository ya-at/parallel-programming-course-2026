#!/usr/bin/env bash

java -Xms2g -Xmx2g -XX:+UseG1GC -cp out dev.github.ablearthy.SpeedTester "$@"
