#!/usr/bin/env bash

./run-speed-tester.sh sync 1

./run-speed-tester.sh mutex 1
./run-speed-tester.sh mutex 2
./run-speed-tester.sh mutex 4
./run-speed-tester.sh mutex 6
./run-speed-tester.sh mutex 8
./run-speed-tester.sh mutex 10
./run-speed-tester.sh mutex 12

./run-speed-tester.sh sharded 1
./run-speed-tester.sh sharded 2
./run-speed-tester.sh sharded 4
./run-speed-tester.sh sharded 6
./run-speed-tester.sh sharded 8
./run-speed-tester.sh sharded 10
./run-speed-tester.sh sharded 12

./run-speed-tester.sh thread-local 1
./run-speed-tester.sh thread-local 2
./run-speed-tester.sh thread-local 4
./run-speed-tester.sh thread-local 6
./run-speed-tester.sh thread-local 8
./run-speed-tester.sh thread-local 10
./run-speed-tester.sh thread-local 12

./run-speed-tester.sh double-buffering 1
./run-speed-tester.sh double-buffering 2
./run-speed-tester.sh double-buffering 4
./run-speed-tester.sh double-buffering 6
./run-speed-tester.sh double-buffering 8
./run-speed-tester.sh double-buffering 10
./run-speed-tester.sh double-buffering 12
