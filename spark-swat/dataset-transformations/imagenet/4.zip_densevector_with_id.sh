#!/bin/bash

set -e

SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
source $SCRIPT_DIR/../../functional-tests/common.sh

${HADOOP_HOME}/bin/hdfs dfs -rm -f -r /input
${HADOOP_HOME}/bin/hdfs dfs -rm -f -r /converted

${HADOOP_HOME}/bin/hdfs dfs -mkdir /input
${HADOOP_HOME}/bin/hdfs dfs -put $SPARK_DATASETS/imagenet/3.converted/* /input/

spark-submit --class ImagenetZipper --jars ${SWAT_JARS} \
        --master spark://localhost:7077 \
        --conf "spark.driver.maxResultSize=4g" \
        --conf "spark.storage.memoryFraction=0.3" \
        ${SWAT_HOME}/dataset-transformations/imagenet/target/imagenet-0.0.0.jar \
        hdfs://$(hostname):54310/input hdfs://$(hostname):54310/converted

rm -rf $SPARK_DATASETS/imagenet/4.zipped
${HADOOP_HOME}/bin/hdfs dfs -get /converted $SPARK_DATASETS/imagenet/4.zipped
