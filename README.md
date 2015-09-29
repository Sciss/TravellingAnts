TravellingAnts
==============

TravellingAnts is an implementation of the Ant Colony metaheuristic optimization algorithm (specifically, Ant System) for solving the travelling salesmen problem.

The repository includes a library for solving arbitrary graphs and also includes a specific example.

The code is intended to be clear and modular. Ants are launched concurrently to improve execution speed. The solver provides call-backs for listening to the running computation rather than hard-coding loggers.

## Sample run

![Sample solution](https://raw.github.com/hakuch/TravellingAnts/master/sample/sample.png)

![Sample iterations](https://raw.github.com/hakuch/TravellingAnts/master/sample/sample-iterations.png)

## Use

TravellingAnts is written in Scala. It requires that sbt is installed.

To execute the solver on the example solution and save the solution in the DOT format, execute:

```bash
$ sbt --warn run > output.dot
```

Intermediate progress is printed to the standard error device (`stderr`) and also to a log file (`travelling-ants.log`) in comma-separated value (CSV) format suitable for analysis.

To produce a visual image of the resulting solution, you can use `neato` from the grapviz suite of programs:

```bash
$ neato -Tpng output.dot > output.png
```
