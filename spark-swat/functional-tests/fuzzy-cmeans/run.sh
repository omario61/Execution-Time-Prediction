#!/bin/bash

if [[ $# != 2 ]]; then
    echo usage: run.sh niters use-swat?
    exit 1
fi

spark-submit --class SparkFuzzyCMeans \
        --jars ${SWAT_HOME}/swat/target/swat-1.0-SNAPSHOT.jar,${APARAPI_SWAT}/com.amd.aparapi/dist/aparapi.jar,${ASM_HOME}/lib/asm-5.0.3.jar,${ASM_HOME}/lib/asm-util-5.0.3.jar \
        --master spark://localhost:7077 \
        ${SWAT_HOME}/functional-tests/fuzzy-cmeans/target/sparkfuzzycmeans-0.0.0.jar \
        run 3 $1 hdfs://$(hostname):54310/converted $2