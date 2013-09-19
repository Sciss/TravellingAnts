TravellingAnts
==============

TravellingAnts is an experimental implementation of the Ant Colony
Optimization algorithm for solving the travelling salesmen problem.

## Use

TravellingAnts requires:
* Python, version 2
* the Graphviz program
* The [PyGraphviz](http://pygraphviz.github.io/) library 

Once these dependencies are satisfied, just execute the script with
Python, and the computed traversal of the graph will be output as a
postscript file.

The algorithm has a number of adjustable parameters which impact the
computed solution in non-obvious ways. Additionally, the algorithm can
optionally be run cooperatively. In this scheme, multiple ant colonies
tackle the problem in parallel, and share random edges of their
computed solution graph with the other colonies.

## Sample Solution

![Example solution image](https://raw.github.com/hakuch/TravellingAnts/master/SampleSolution.png)
