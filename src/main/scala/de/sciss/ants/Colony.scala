package de.sciss.ants

import de.sciss.ants
import de.sciss.ants.Ant.Weight

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.{ExecutionContext, Future}

/**
  * "Colonies" send out many [[Ant]]s to optimize for the best tour of a location
  * graph.
  *
  * Colonies are parametrized by the kind of ant that they are composed of.
  *
  * @param  pheromoneConstant Constant factor of pheromones to deposit for edges in the solution. This
  *                           quantity should be roughly of the same magnitude as the product of the
  *                           distance between edges and the cost of a solution.
  * @param  evaporationRate   Rate of evaporation of pheromones on all edges of the graph. Should be in
  *                           the range `[0, 1]`.
  * @param  antsPerIteration  Number of ants to send out on every iteration to explore the graph.
  */
final class Colony[V](val ant              : Ant[V],
                      val pheromoneConstant: Double  = 1000.0,
                      val evaporationRate  : Double  = 0.5,
                      val antsPerIteration : Int     = 20
                     ) {
  type AntGraph       = ConnectedGraph[V, Weight]
  type LocationGraph  = ConnectedGraph[V, Double]
  type Solution       = ants.Solution[V]

  /**
    * Iterative solution progress.
    *
    * @constructor
    * @param bestSolution The best solution found thus far.
    * @param iterationCount The current iteration of the solver.
    * @param costMeans The mean cost of all solutions found at a particular iteration.
    */
  case class Progress(
    graph         : AntGraph,
    bestSolution  : Solution,
    iterationCount: Int,
    costMeans     : Map[Int, Double])

  private def initializePheromones(graph: LocationGraph): AntGraph =
    graph.transform { distance => Weight(distance, pheromone = 1.0) }

  /*
   * Pheromones are evaporated slightly from all edges in the graph after every
   * iteration.
   */
  private def evaporatePheromones(graph: AntGraph): AntGraph =
    graph.transform { weight =>
      weight.copy(pheromone = (1.0 - evaporationRate) * weight.pheromone)
    }

  /*
   * Pheromones are deposited on the graph for each step in the solution.
   */
  private def depositPheromones(solution: Solution, graph: AntGraph): AntGraph = {
    solution.steps.foldRight(graph) { case (step, g) =>
      g.mapEdge(step._1, step._2) { weight =>
        val addition = pheromoneConstant / (solution.cost * weight.distance)
        weight.copy(pheromone = weight.pheromone + addition)
      }
    }
  }

  /*
   * Constructs a solution based on the current state of a graph.
   */
  private def launchAnt(graph: AntGraph)(implicit exec: ExecutionContext): Future[Solution] = {
    val node = graph.randomNode()
    Future {
      ant.tour(node, graph)
    }
  }

  /*
   * Launches [[antsPerIteration]] ants concurrently to produce many solutions for
   * the same graph.
   */
  private def launchAntGroup(graph: AntGraph)(implicit exec: ExecutionContext): Future[ISeq[Solution]] =
    Future.sequence((1 to antsPerIteration).map(_ => launchAnt(graph)))

  /**
   * Produces an optimized solution of the most efficient tour of all the
   * locations in the graph.
   *
   * The solver will execute for a fixed number of iterations.
   *
   * Whenever a new solution is found, the `listener` will be invoked by the
   * solver with the current solution [[Progress]].
   */
  def solve(
    graph: LocationGraph, iterations: Int, listener: Progress => Unit = _ => ())
           (implicit exec: ExecutionContext): Future[Solution] = {
    val initialGraph = initializePheromones(graph)

    def loop(progress: Progress): Future[Solution] =
      if (progress.iterationCount >= iterations) Future.successful(progress.bestSolution)
      else {
        launchAntGroup(progress.graph).flatMap { candidates =>
          val meanCost  = candidates.map(_.cost).sum / antsPerIteration

          var newBest   = progress.bestSolution
          var newGraph  = evaporatePheromones(progress.graph)

          candidates.foreach { c =>
            newGraph = depositPheromones(c, newGraph)

            if (c.cost < newBest.cost)
              newBest = c
          }

          val updatedProgress = Progress(
            graph           = newGraph,
            bestSolution    = newBest,
            iterationCount  = progress.iterationCount + 1,
            costMeans       = progress.costMeans + (progress.iterationCount + 1 -> meanCost))

          listener(updatedProgress)
          loop(updatedProgress)
        }
      }

    loop(Progress(initialGraph, Solution.random(graph), 0, Map.empty))
  }
}