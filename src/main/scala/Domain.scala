import scala.util.Random

/**
 * Two dimension point.
 */
case class Point(x: Double, y: Double)

object Point {
  /**
   * Euclidean distance between two points.
   */
  def distance(t: Point, u: Point): Double = {
    val dx = t.x - u.x
    val dy = t.y - u.y
    Math.sqrt((dx * dx) + (dy * dy))
  }
}

case class Solution(steps: List[(Int, Int)], graph: ConnectedGraph[Int, Double]) {

  /**
   * The cost of a solution is the sum of the distances between each step.
   */
  lazy val cost: Double =
    steps.map { case (source, dest) => graph.weight(source, dest) }.sum
}

object Solution {
  def random(graph: ConnectedGraph[Int, Double]): Solution = {
    val shuffled = Random.shuffle(graph.nodes)

    Solution(
      shuffled.sliding(2).map { case List(x, y) =>
        (x, y)
      }.toList ++ List(shuffled.last -> shuffled.head),
      graph)
  }
}
