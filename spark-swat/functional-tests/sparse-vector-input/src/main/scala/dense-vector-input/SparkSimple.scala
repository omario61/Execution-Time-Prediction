/*
Copyright (c) 2016, Rice University

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
2.  Redistributions in binary form must reproduce the above
     copyright notice, this list of conditions and the following
     disclaimer in the documentation and/or other materials provided
     with the distribution.
3.  Neither the name of Rice University
     nor the names of its contributors may be used to endorse or
     promote products derived from this software without specific
     prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.cl._
import Array._
import scala.math._
import org.apache.spark.rdd._
import java.net._

import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.SparseVector

object SparkSimple {
    def main(args : Array[String]) {
        if (args.length < 1) {
            println("usage: SparkSimple cmd")
            return;
        }

        val cmd = args(0)

        if (cmd == "convert") {
            convert(args.slice(1, args.length))
        } else if (cmd == "run") {
            run_simple(args.slice(2, args.length), args(1).toBoolean)
        } else if (cmd == "check") {
            val correct : Array[(Int, Double)] = run_simple(args.slice(1, args.length), false)
            val actual : Array[(Int, Double)] = run_simple(args.slice(1, args.length), true)
            assert(correct.length == actual.length)
            for (i <- 0 until correct.length) {
                val a = correct(i)
                val b = actual(i)
                var error : Boolean = false

                if (a._1 != b._1) {
                    System.err.println(i + " expected index " + a._1 + " but got " + b._1)
                    error = true
                }

                if (a._2 != b._2) {
                    System.err.println(i + " expected value " + a._2 + " but got " + b._2)
                    error = true
                }

                if (error) System.exit(1)
            }
            System.err.println("PASSED")
        }
    }

    def get_spark_context(appName : String) : SparkContext = {
        val conf = new SparkConf()
        conf.setAppName(appName)

        val localhost = InetAddress.getLocalHost
        conf.setMaster("spark://" + localhost.getHostName + ":7077") // 7077 is the default port

        return new SparkContext(conf)
    }

    def run_simple(args : Array[String], useSwat : Boolean) : Array[(Int, Double)] = {
        if (args.length != 1) {
            println("usage: SparkSimple run input-path");
            return new Array[(Int, Double)](0);
        }
        val sc = get_spark_context("Spark Simple");

        val inputPath = args(0)
        val inputs_raw : RDD[SparseVector] = sc.objectFile[SparseVector](inputPath).cache
        val inputs = if (useSwat) CLWrapper.cl[SparseVector](inputs_raw) else inputs_raw

        val outputs : RDD[(Int, Double)] = inputs.map(v => {
            var i = 0
            var indexSum = 0
            var valueSum = 0.0
            while (i < v.size) {
                indexSum += v.indices(i)
                valueSum += v.values(i)
                i += 1
            }
            (indexSum, valueSum)
          })
        val outputs2 : Array[(Int, Double)] = outputs.collect
        var i = 0
        while (i < 10) {
            System.err.println(outputs2(i)._1 + " " + outputs2(i)._2)
            i += 1
        }
        System.err.println("...")
        sc.stop
        outputs2
    }

    def convert(args : Array[String]) {
        if (args.length != 2) {
            println("usage: SparkSimple convert input-dir output-dir");
            return
        }
        val sc = get_spark_context("Spark KMeans Converter");

        val inputDir = args(0)
        var outputDir = args(1)
        val input = sc.textFile(inputDir)

        val converted = input.map(line => {
            val tokens : Array[String] = line.split(" ")
            val size : Int = tokens.length / 2
            val indexArr : Array[Int] = new Array[Int](tokens.length / 2)
            val valueArr : Array[Double] = new Array[Double](tokens.length / 2)
            var i = 0
            while (i < tokens.length) {
                indexArr(i / 2) = tokens(i).toInt
                valueArr(i / 2) = tokens(i + 1).toDouble
                i += 2
            }
            Vectors.sparse(size, indexArr, valueArr) })
        converted.saveAsObjectFile(outputDir)
    }
}