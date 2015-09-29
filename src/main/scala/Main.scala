import java.io
import java.nio.file._
import java.nio.charset._

/**
 * Run the solver on a particular graph for exemplary purposes.
 */
object App {
  val ant = new Ant with Ant.Defaults
  val colony = new Colony(ant) with Colony.Defaults

  /**
   * The scale factor is used to scale the location coordinates of points to
   * produce a reasonably-sized image.
   */
  def nodeAttr(
    scaleFactor: Double, lookup: ant.Node => Point
  )(node: ant.Node)
      : Map[String, String] =
  {
    val p = lookup(node)
    val sx = p.x / scaleFactor
    val sy = p.y / scaleFactor
    Map("pos" -> s"""\"$sx,$sy!\"""")
  }

  /**
   * The solver progress is recorded in CSV format with a single header row.
   *
   * Columns are:
   *
   * {{{
   *   iteration index, mean cost per iteration, best solution cost so far
   * }}}
   */
  def prepareLog(fileName: String): io.BufferedWriter = {
    val log = Files.newBufferedWriter(Paths.get(fileName), Charset.forName("UTF-8"))
    log.write("iteration,iteration-mean-cost,best-cost\n")
    log
  }

  /**
   * Log solver progress to stderr and to the log.
   *
   * The format matches that described in [[prepareLog]].
   */
  def loggingListener(log: io.BufferedWriter)(progress: colony.Progress): Unit = {
    def formatCost(c: Double): String =
      "%.1f".format(c)

    import progress._
    val iterationMean = costMeans(iterationCount)

    Console.err.println(
      s"#$iterationCount: ${formatCost(bestSolution.cost)}/${formatCost(iterationMean)}")

    log.write(
      s"$iterationCount,${formatCost(iterationMean)},${formatCost(bestSolution.cost)}\n")
  }

  def main(args: Array[String]): Unit = {
    val log = prepareLog("travelling-ants.log")

    val lookup = Nodes.toMap.apply _

    val graph = ConnectedGraph(Nodes.map(_._1)) { (a, b) =>
      Point.distance(lookup(a), lookup(b))
    }

    val solution = colony.solve(
      graph,
      iterations = 10000,
      Some(loggingListener(log))).run

    val output = DirectedGraph.formatAsDot(
      solution.steps.toSet,
      Some(nodeAttr(scaleFactor = 280.0, lookup) _))

    println(output)
    log.flush()
  }

  /**
   * Sample problem. The solver can be invoked with an arbitrary set of nodes.
   */
  final val Nodes = Seq(
    1-> Point(1150.0, 1760.0),
    2-> Point(630.0, 1660.0),
    3-> Point(40.0, 2090.0),
    4-> Point(750.0, 1100.0),
    5-> Point(750.0, 2030.0),
    6-> Point(1030.0, 2070.0),
    7-> Point(1650.0, 650.0),
    8-> Point(1490.0, 1630.0),
    9-> Point(790.0, 2260.0),
    10-> Point(710.0, 1310.0),
    11-> Point(840.0, 550.0),
    12-> Point(1170.0, 2300.0),
    13-> Point(970.0, 1340.0),
    14-> Point(510.0, 700.0),
    15-> Point(750.0, 900.0),
    16-> Point(1280.0, 1200.0),
    17-> Point(230.0, 590.0),
    18-> Point(460.0, 860.0),
    19-> Point(1040.0, 950.0),
    20-> Point(590.0, 1390.0),
    21-> Point(830.0, 1770.0),
    22-> Point(490.0, 500.0),
    23-> Point(1840.0, 1240.0),
    24-> Point(1260.0, 1500.0),
    25-> Point(1280.0, 790.0),
    26-> Point(490.0, 2130.0),
    27-> Point(1460.0, 1420.0),
    28-> Point(1260.0, 1910.0),
    29-> Point(360.0, 1980.0))
}
