#!/bin/bash

#set -e

TESTNAME="word-count"
CONTINUE_AFTER=0
ENABLED=1

echo "TESTNAME=${TESTNAME} CONTINUE_AFTER=${CONTINUE_AFTER}"
echo

t=${TESTNAME}
    echo $t
    cd $t
    mvn clean &> ../clean.log
    mvn package &> ../package.log
    ./convert.sh &> ../convert.log
    ./run.sh check &> ../check.log
    cd ..

    #set +e 
    cat check.log | grep PASSED
    ERR=$?
    #set -e

    if [[ $ERR != 0 ]]; then
        echo FAILED
        exit 1
    fi
    #echo PASSED
    if [[ ! -z $TESTNAME && $ENABLED == 1 && $CONTINUE_AFTER == 0 ]]; then
        ENABLED=0
    fi

