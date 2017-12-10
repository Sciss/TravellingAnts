package de.sciss.ants

import scala.util.Random
import scala.collection.breakOut
import scala.collection.immutable.{IndexedSeq => Vec}

/** Undirected graph where every node is connected to every other node.
  *
  * Graphs are immutable and created via [[ConnectedGraph#apply]] with an
  * edge-generating function.
  */
final class ConnectedGraph[A, W] private (val edges: Map[(A, A), W]) {
  lazy val nodeSet: Set[A] = {
    // Warning: this relies on a `flatMap` to a _set_, i.e. distinct output.
    // So don't use `keysIterator.flatMap` or `flatMap (breakOut)`!
    val keys = edges.keySet
    keys.flatMap { case (s, d) => List(s, d) }
  }

  lazy val nodeSeq: Vec[A] = nodeSet.toVector

  def weight(source: A, dest: A): W =
    edges((source, dest))

  def mapEdge(source: A, dest: A)(f: W => W): ConnectedGraph[A, W] = {
    val v = f(edges((source, dest)))
    new ConnectedGraph(edges.updated((source, dest), v))
  }

  def map[B](f: A => B): ConnectedGraph[B, W] = {
    val mappedNodes: Map[A, B] = nodeSeq.map(a => a -> f(a))(breakOut)

    val newEdges = edges.map { case ((source, dest), w) =>
      ((mappedNodes(source), mappedNodes(dest)), w)
    }

    new ConnectedGraph(newEdges)
  }

  def transform[V](f: W => V): ConnectedGraph[A, V] = {
    val newEdges = edges.map { case (k, v) => k -> f(v) }
    new ConnectedGraph(newEdges)
  }

  def randomNode(): A =
    nodeSeq(Random.nextInt(nodeSeq.size))
}

object ConnectedGraph {
  def empty[A, W]: ConnectedGraph[A, W] =
    new ConnectedGraph(Map.empty[(A, A), W])

  /** Creates a new graph.
    *
    * Edges between nodes are specified with the edge-generating function `f`.
    *
    * Graphs must consist of at least two nodes, or else they are empty.
    */
  def apply[A, W](vertices: Seq[A])(f: (A, A) => W): ConnectedGraph[A, W] = {
    val edges = vertices.combinations(2).flatMap { case Seq(a, b) =>
      List(
        ((a, b), f(a, b)),
        ((b, a), f(b, a))
      )
    } .toMap

    new ConnectedGraph(edges)
  }
}

object DirectedGraph {

  /** Formats a simple representation of a directed graph in the DOT format.
    *
    * The DOT format is described at
    * [[https://en.wikipedia.org/wiki/DOT_(graph_description_language)]].
    *
    * Each node in the graph can have arbitrary node attributes included in the
    * exported graph. These attributes are set via an optional
    * attribute-generating function.
    */
  def formatAsDot[A](graph: Set[(A, A)], attr: Option[A => Map[String, String]] = None): String = {
    val output = new StringBuilder()

    output.append(s"digraph exported {\n")

    val nodes = graph.flatMap { case (a, b) => Set(a, b) }

    nodes.foreach { node =>
      val a = attr match {
        case None => ""
        case Some(f) => f(node).map { case (k, v) => s"$k=$v" }.mkString(" ")
      }

      output.append(s"""\"$node\" [$a]\n""")
    }

    graph.foreach { case (source, dest) =>
      output.append(
        s"""\"$source\" -> \"$dest\"\n""")
    }

    output.append("}\n")
    output.toString()
  }
}
