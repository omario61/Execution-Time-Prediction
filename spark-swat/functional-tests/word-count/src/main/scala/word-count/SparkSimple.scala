import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.hadoop.fs.Path
import java.nio.file.Path
import scala.util.Try
import java.io.File
import java.io.File
import org.apache.spark._
import org.apache.spark.SparkContext._



object SparkSimple {
  def deleteOutput(): Unit = {
    val file = new File("output")
    if(file.exists()){
      file.listFiles().foreach { x => x.delete() }
      file.delete()
    }
  }
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
                              .setAppName("Spark Test")
                              .setMaster("local[*]")
    val sc = new SparkContext(conf)
   
    deleteOutput();
    val inputPath = args(1)
    val lines = sc.textFile(inputPath)
    val words = lines.flatMap (line => line.split(" "))
    val units = words.map( word => (word, 1))
    val counts = units.reduceByKey((x,y) => x+y)
    counts.saveAsTextFile(args(2))
  }
}
