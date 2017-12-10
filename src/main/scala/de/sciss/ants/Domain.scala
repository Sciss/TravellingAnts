package de.sciss.ants

import scala.util.Random

/**
  * Two dimension point.
  */
final case class Point(x: Double, y: Double)

object Point {
  /**
    * Euclidean distance between two points.
    */
  def distance(t: Point, u: Point): Double = {
    val dx = t.x - u.x
    val dy = t.y - u.y
    math.sqrt((dx * dx) + (dy * dy))
  }
}

final case class Solution[Node](steps: List[(Node, Node)], graph: ConnectedGraph[Node, Double]) {

  /**
    * The cost of a solution is the sum of the distances between each step.
    */
  lazy val cost: Double =
    steps.iterator.map { case (source, dest) => graph.weight(source, dest) }.sum
}

object Solution {
  def random[Node](graph: ConnectedGraph[Node, Double]): Solution[Node] = {
    val shuffled = Random.shuffle(graph.nodeSeq)

    Solution(
      shuffled.sliding(2).map { case Seq(x, y) =>
        (x, y)
      }.toList ++ List(shuffled.last -> shuffled.head),
      graph)
  }
}
