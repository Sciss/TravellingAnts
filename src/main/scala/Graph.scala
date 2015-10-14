import scala.util.Random

/**
 * Undirected graph where every node is connected to every other node.
 *
 * Graphs are immutable and created via [[ConnectedGraph.apply]] with an
 * edge-generating function.
 */
abstract class ConnectedGraph[A, W] private (val edges: Map[(A, A), W]) {
  lazy val nodes: List[A] =
    edges.keys.flatMap { case (s, d) => List(s, d) }.toList

  def weight(source: A, dest: A): W =
    edges((source, dest))

  def mapEdge(source: A, dest: A)(f: W => W): ConnectedGraph[A, W] = {
    val v = f(edges((source, dest)))
    new ConnectedGraph(edges.updated((source, dest), v)) {}
  }

  def map[B](f: A => B): ConnectedGraph[B, W] = {
    val mappedNodes = nodes.map { case a => a -> f(a) }.toMap

    val newEdges = edges.map { case ((source, dest), w) =>
      ((mappedNodes(source), mappedNodes(dest)), w)
    }

    new ConnectedGraph(newEdges) {}
  }

  def transform[V](f: W => V): ConnectedGraph[A, V] = {
    // Forcing the view is required.
    // See [[https://issues.scala-lang.org/browse/SI-4776]]
    val newEdges = edges.mapValues(f).view.force
    new ConnectedGraph(newEdges) {}
  }

  def randomNode(): A =
    nodes(Random.nextInt(nodes.size))
}

object ConnectedGraph {
  def empty[A, W]: ConnectedGraph[A, W] =
    new ConnectedGraph(Map.empty[(A, A), W]) {}

  /**
   * Create a new graph.
   *
   * Edges between nodes are specified with the edge-generating function `f`.
   *
   * Graphs must consist of at least two nodes, or else they are empty.
   */
  def apply[A, W](es: Seq[A])(f: (A, A) => W): ConnectedGraph[A, W] = {
    es match {
      case Seq() | Seq(_) => empty

      case Seq(a, b, _*) =>
        val initial = Map((a, b) -> f(a, b))

        val edges = es.foldRight(initial) { case (node, allEdges) =>
          val newEdges = allEdges.flatMap { case ((otherNode, _), _) =>
            Map(
              ((node, otherNode), f(node, otherNode)),
              ((otherNode, node), f(otherNode, node)))
          }

          allEdges ++ newEdges
        }

        new ConnectedGraph(edges) {}
    }
  }
}

object DirectedGraph {

  /**
   * Format a simple representation of a directed graph in the DOT format.
   *
   * The DOT format is described at
   * [[https://en.wikipedia.org/wiki/DOT_(graph_description_language)]].
   *
   * Each node in the graph can have arbitrary node attributes included in the
   * exported graph. These attributes are set via an optional
   * attribute-generating function.
   */
  def formatAsDot[A](graph: Set[(A, A)], attr: Option[A => Map[String, String]] = None)
      : String =
  {
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
