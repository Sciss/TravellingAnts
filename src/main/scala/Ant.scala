import scala.util.Random
import scala.annotation.tailrec

/**
 * "Ants" traverse a graph of locations to produce random tours.
 *
 * The choice of which location to visit next ("state transition") is
 * probabalistic, and weighted by both the distance and amount of pheromone
 * deposited between locations.
 */
abstract class Ant {

  /**
   * Label for locations.
   */
  type Node = Int

  /**
   * Edge weight between locations
   */
  case class Weight(distance: Double, pheromone: Double)

  /**
   * An ant's current location and where it has yet to visit.
   */
  case class State(current: Node, remaining: Set[Node]) {
    def visit(n: Node): State =
      if (remaining.contains(n)) State(n, remaining - n)
      else this
  }

  /**
   * Tour the locations in the graph and return to the starting point, producing
   * a new solution.
   */
  def tour(startingPoint: Node, graph: ConnectedGraph[Node, Weight]): Solution = {
    val state = State(startingPoint, graph.nodes.toSet - startingPoint)

    // Populated later. A cheap trick.
    var lastNode: Option[Node] = None

    val steps = unfold(state) { case state =>
      if (state.remaining.size == 1) {
        lastNode = Some(state.current)
        None
      } else {
        val step = nextStep(state, graph)
        Some(((state.current, step), state.visit(step)))
      }
    }.reverse

    Solution(steps ++ List(lastNode.get -> startingPoint), graph.transform(_.distance))
  }

  /**
   * Choose a new location to visit based on the [[travelProbabilities]].
   */
  def nextStep(state: State, graph: ConnectedGraph[Node, Weight]): Node =
    weightedRandomChoice(travelProbabilities(state, graph))

  /**
   * Probabilites for determining which location to visit next.
   */
  def travelProbabilities(state: State, graph: ConnectedGraph[Node, Weight])
      : List[(Node, Double)] =
  {
    val normalizationFactor = state.remaining.map { node =>
      val weight = graph.weight(state.current, node)
      Math.pow(weight.pheromone, alpha) / Math.pow(weight.distance, beta)
    }.sum

    state.remaining.toList.map { node =>
      val weight = graph.weight(state.current, node)
      val a = Math.pow(weight.pheromone, alpha)
      val b = 1.0 / Math.pow(weight.distance, beta)
      (node, (a * b) / normalizationFactor)
    }
  }

  /**
   * Select an element of type `A` randomly based on selection weights.
   *
   * The weights need not sum to one (like probabilities); only the relative
   * value of the weights matter for selection.
   */
  def weightedRandomChoice[A](choices: Seq[(A, Double)]): A = {
    val (items, weights) = choices.unzip
    val cumulativeWeights = items.zip(weights.scanLeft(0.0)(_ + _).tail)
    val x = Random.nextDouble() * weights.sum

    cumulativeWeights.find { case (_, bound) => bound > x }.get._1
  }

  /**
   * Beginning with state `z`, iteratively populate a sequence of values and
   * update the state.
   *
   * Termination is indicated when `f` evaluates to [[None]].
   */
  def unfold[A, S](z: S)(f: S => Option[(A, S)]): List[A] = {
    var result = List.empty[A]

    @tailrec
    def loop(state: S): Unit =
      f(state) match {
        case None =>

        case Some((e, newS)) =>
          result ::= e
          loop(newS)
      }

    loop(z)
    result
  }

  /**
   * Higher values relative to [[beta]] will weigh the amount of pheromone on
   * an edge more significantly than the distance.
   */
  def alpha: Double

  /**
   * Higher values relative to [[alpha]] will weigh the distance between two
   * locations on an edge more significantly than the pheromone that is present.
   */
  def beta: Double
}

object Ant {
  trait Defaults {
    val alpha = 0.5
    val beta = 1.2
  }
}
