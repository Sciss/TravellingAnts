# TravellingAnts

[![Build Status](https://travis-ci.org/Sciss/TravellingAnts.svg?branch=master)](https://travis-ci.org/Sciss/TravellingAnts)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/travelling-ants_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/travelling-ants_2.11)

## statement

TravellingAnts implements the ant colony optimisation algorithm for solving the travelling salesman problem.
This is a fork from [https://github.com/hakuch/TravellingAnts], the original author is Jesse Haber-Kucharsky.
All changes to the original code published under the original license  (MIT). The changes include:

- use Scala 2.12, drop sbt plugins
- drop scalaz dependency
- slight code reformatting
- put code into package `de.sciss.ants`
- abstract classes changed for final classes
- demo app is in test sources, thus run `sbt test:run` instead of `sbt run`s
- bug fixes
- publish as library (see section 'linking')

## linking

To link to this library:

    "de.sciss" %% "travelling-ants" % v

The current version `v` is `"0.1.1"`.

## building

This project builds against Scala 2.12, 2.11, using sbt.
To run the demo, use `sbt test:run`.

## Original Read-Me

TravellingAnts is an implementation of the Ant Colony meta-heuristic optimization algorithm (specifically, Ant System) for solving the travelling salesmen problem.

The repository includes a library for solving arbitrary graphs and also includes a specific example.

The code is intended to be clear and modular. Ants are launched concurrently to improve execution speed. The solver provides call-backs for listening to the running computation rather than hard-coding loggers.

### Sample run

A comparison of the mean cost of each ant's solution per iteration to the overall cumulative best solution as the algorithm progress:

![Sample iterations](https://raw.github.com/hakuch/TravellingAnts/master/sample/sample-iterations.png)

The best solution from the run above:

![Sample solution](https://raw.github.com/hakuch/TravellingAnts/master/sample/sample.png)

### Use

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
