package org.ymt.spark.graphx.degree

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.graphx._

import scala.reflect.ClassTag

/**
  * Created by yangmutong on 2017/4/8.
  */
object DegreeCentrality extends Serializable{
  def main(args: Array[String]): Unit = {
    val sc = new SparkContext(new SparkConf().setAppName("Degree Centrality"))
    val inputPath = args(0)
    val outputPath = args(1)
    val numPartitions = args(2).toInt

    // graph loader phase
    val graph = makeGraph(inputPath, sc)
    val g = Graph(graph.vertices.repartition(numPartitions),
      graph.edges.repartition(numPartitions)).partitionBy(PartitionStrategy.RandomVertexCut)


    val in = inDegreeCentrality(g)
    val out = outDegreeCentrality(g)
    val degree = degreeCentrality(g)

    save(in, outputPath + "/in/vertices", outputPath + "/in/edges")
    save(out, outputPath + "/out/vertices", outputPath + "/out/edges")
    save(degree, outputPath + "/all/vertices", outputPath + "/all/edges")
    sc.stop()
  }

  def makeGraph[VD: ClassTag](inputPath: String, sc: SparkContext): Graph[Int, Double] = {
    val graph = GraphLoader.edgeListFile(sc, inputPath, true)
    val edges = sc.textFile(inputPath).map(v => {
      val t = v.split("\t")
      Edge(t(0).toLong, t(1).toLong, t(2).toDouble)
    })
    Graph(graph.vertices, edges)
  }

  def save[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED], vertexPath: String, edegePath: String): Unit = {
    graph.vertices.saveAsTextFile(vertexPath)
    graph.vertices.saveAsTextFile(edegePath)
  }
  def inDegreeCentrality[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Graph[Double, ED] = {
    val length = graph.numVertices
    val s = 1.0 / (length - 1.0)
    val vertices = graph.inDegrees.mapValues( (vid, value) => value * s)
    Graph(vertices, graph.edges)
  }

  def outDegreeCentrality[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Graph[Double, ED] = {
    val length = graph.numVertices
    val s = 1.0 / (length - 1.0)
    val vertices = graph.outDegrees.mapValues( (vid, value) => value * s)
    Graph(vertices, graph.edges)
  }

  def degreeCentrality[VD: ClassTag, ED: ClassTag](graph: Graph[VD, ED]): Graph[Double, ED] = {
    val length = graph.numVertices
    val s = 1.0 / (length - 1.0)
    val vertices = graph.degrees.mapValues( (vid, value) => value * s)
    Graph(vertices, graph.edges)
  }
}
