package de.sciss.ants

import de.sciss.ants.Ant.Weight

import scala.annotation.tailrec
import scala.util.Random

object Ant {
  /** Edge weight between locations */
  final case class Weight(distance: Double, pheromone: Double)
}
/**
  * "Ants" traverse a graph of locations to produce random tours.
  *
  * The choice of which location to visit next ("state transition") is
  * probabilistic, and weighted by both the distance and amount of pheromone
  * deposited between locations.
  *
  * @param  alpha Higher values relative to `beta` will weigh the amount of pheromone on
  *               an edge more significantly than the distance.
  * @param  beta  Higher values relative to `alpha` will weigh the distance between two
  *               locations on an edge more significantly than the pheromone that is present.
  */
final class Ant[V](val alpha: Double = 0.5, val beta: Double = 1.2) {
  /*
   * An ant's current location and where it has yet to visit.
   */
  private case class State(current: V, remaining: Set[V]) {
    def visit(n: V): State =
      if (remaining.contains(n)) State(n, remaining - n)
      else this
  }

  /**
    * Tour the locations in the graph and return to the starting point, producing
    * a new solution.
    */
  def tour(startingPoint: V, graph: ConnectedGraph[V, Weight]): Solution[V] = {
    @tailrec
    def loop(state: State, res: List[(V, V)]): List[(V, V)] =
      if (state.remaining.isEmpty) {
        val lastNode = state.current
        (lastNode -> startingPoint) :: res
      } else {
        val step    = nextStep(state, graph)
        val newRes  = (state.current -> step) :: res
        loop(state.visit(step), newRes)
      }

    val state0  = State(startingPoint, graph.nodeSet - startingPoint)
    val steps   = loop(state0, Nil).reverse

    Solution(steps, graph.transform(_.distance))
  }

  /*
   * Chooses a new location to visit based on the [[travelProbabilities]].
   */
  private def nextStep(state: State, graph: ConnectedGraph[V, Weight]): V =
    weightedRandomChoice(travelProbabilities(state, graph))

  /*
   * Probabilities for determining which location to visit next.
   */
  private def travelProbabilities(state: State, graph: ConnectedGraph[V, Weight]): List[(V, Double)] = {
    // I think this was a bug in the original code - `remaining` is a Set, so
    // it was mapping to a possibly smaller Set of costs; we insert an iterator
    val norm0 = state.remaining.iterator.map { node =>
      val weight = graph.weight(state.current, node)
      val a = math.pow(weight.pheromone, alpha)
      val b = math.pow(weight.distance , beta )
      if (a == 0.0 || b == 0.0) 0.0 else a / b
    }.sum

    val norm = if (norm0 == 0.0) 1.0 else norm0   // avoid division by zero

    state.remaining.toList.map { node =>
      val weight  = graph.weight(state.current, node)
      val a       = math.pow(weight.pheromone, alpha)
      val b       = math.pow(weight.distance , beta )
      val div     = if (a == 0.0 || b == 0.0) 0.0 else (a / b) / norm
      (node, div)
    }
  }

  /*
   * Selects an element of type `A` randomly based on selection weights.
   *
   * The weights need not sum to one (like probabilities); only the relative
   * value of the weights matter for selection.
   */
  private def weightedRandomChoice[A](choices: Seq[(A, Double)]): A = {
    val (items, weights)  = choices.unzip
    val weightsCum        = weights.scanLeft(0.0)(_ + _).tail
    val itemsCum          = items.zip(weightsCum)
    val weightsSum        = weightsCum.last
    val x                 = Random.nextDouble() * weightsSum
    val opt               = itemsCum.find { case (_, bound) => bound >= x }
    opt.getOrElse(itemsCum.last)._1
  }
}