## TravellingAnts.py
##
## Copyright Jesse Haber-Kucharsky 2013
## See LICENSE for details.

import bisect
import copy
import itertools
import math
import multiprocessing
import random

import pygraphviz as pgv

NUM_ANTS = 8
IS_COOPERATIVE = True
USE_LOCAL_SEARCH = True

if IS_COOPERATIVE:
    NUM_COLONIES = 2

# A sample problem.
CITIES = {
    1 : (1150.0, 1760.0),
    2 : (630.0, 1660.0),
    3 : (40.0, 2090.0),
    4 : (750.0, 1100.0),
    5 : (750.0, 2030.0),
    6 : (1030.0, 2070.0),
    7 : (1650.0, 650.0),
    8 : (1490.0, 1630.0),
    9 : (790.0, 2260.0),
    10 : (710.0, 1310.0),
    11 : (840.0, 550.0),
    12 : (1170.0, 2300.0),
    13 : (970.0, 1340.0),
    14 : (510.0, 700.0),
    15 : (750.0, 900.0),
    16 : (1280.0, 1200.0),
    17 : (230.0, 590.0),
    18 : (460.0, 860.0),
    19 : (1040.0, 950.0),
    20 : (590.0, 1390.0),
    21 : (830.0, 1770.0),
    22 : (490.0, 500.0),
    23 : (1840.0, 1240.0),
    24 : (1260.0, 1500.0),
    25 : (1280.0, 790.0),
    26 : (490.0, 2130.0),
    27 : (1460.0, 1420.0),
    28 : (1260.0, 1910.0),
    29 : (360.0, 1980.0)
}

NUM_CITIES = len(CITIES.keys())

def euclideanDistance(cityA, cityB):
    xa, ya = CITIES[cityA]
    xb, yb = CITIES[cityB]

    return math.sqrt( math.pow( xb - xa, 2 ) + math.pow( yb - ya, 2 ) )

def weightedRandomChoice(choices):
    (values, weights) = zip(*choices)

    total = 0
    cumulativeWeights = []

    for w in weights:
        total += w
        cumulativeWeights.append(total)

    x = random.random() * total
    i = bisect.bisect(cumulativeWeights, x)
    return values[i]

class TspSolution(object):
    def __init__(self, tour):
        """
        The tour is expressed as a list of 2-tuples of integers. Each
        2-tuple represents a directed edge between two cities.
        """
        self.tour = tour

    @staticmethod
    def RandomSolution():
        """
        Generate a random solution to the TSP.  This is effectively a
        random permutation of the integers 1 through 29.
        """
        x = range(1, NUM_CITIES + 1)
        random.shuffle(x)
        
        return TspSolution([(a, b) for (a, b) in zip(x, x[1:])])

    def cost(self):
        """
        Returns the cost of this solution.
        """
        return sum(euclideanDistance(a, b) for (a, b) in self.tour)

    def improveWithLocalSearch(self):
        """
        Attempt to decrease the cost to a given solution by making
        random variations and checking to see if it decreases the
        solution cost.
        """
        def swap(index):
            """
            Swap two consecutive edges, starting at edge 'index' where
            the first edge corresponds to index 0.
            """
            secondIndex = (index + 1) % len(self.tour)
            thirdIndex = (index + 2) % len(self.tour)

            edge1 = self.tour[index]
            edge2 = self.tour[secondIndex]
            edge3 = self.tour[thirdIndex]

            step1 = edge1[0]
            step2 = edge2[1]
            step3 = edge1[1]

            newTour = copy.copy(self.tour)
            newTour[index] = (step1, step2)
            newTour[secondIndex] = (step2, step3)
            newTour[thirdIndex] = (step3, edge3[1])

            return TspSolution(newTour)

        # On every iteration, populate a dictionary with the index of
        # swapped edges and the corresponding difference between the
        # resulting score and the current best score. Make the swap
        # with the best score, and iterate until there are no
        # improvements.

        bestCost = self.cost()

        while True:
            scoreDeltas = {}

            for i in xrange(len(self.tour)):
                newTour = swap(i)
                scoreDeltas[i] = newTour.cost() - bestCost

            # Find the index corresponding to the most beneficial change.
            bestDelta = min(scoreDeltas.values())
            
            # We've found a better tour.
            if bestDelta < 0:
                # print "Improved solution by %.2f" % bestDelta

                index = scoreDeltas.values().index(bestDelta)
                self.tour = swap(index).tour
                bestCost = self.cost()
            else:
                # We haven't found a better tour. Stop.
                break
                
    def draw(self, fileName="graph.ps"):
        graph = pgv.AGraph(strict=True, directed=True)

        # Add all the cities, and their position.
        for city in CITIES.keys():
            (x, y) = CITIES[city]

            # The exclamation point forces the node position. Otherwise,
            # the layout algorithm moves the node to a different location.
            #
            # We're scaling the position of the nodes so that they all fit
            # in a single reasonably-sized image.
            graph.add_node(str(city), pos="%f,%f!" % (x / 280.0, y / 280.0))

        # Add the edges.
        for (a, b) in self.tour:
            graph.add_edge(str(a), str(b))
    
        graph.node_attr['shape'] = 'circle'
        graph.node_attr['style'] = 'filled'
        graph.node_attr['fillcolor'] = 'blue'
        graph.node_attr['fontcolor'] = 'white'
        graph.node_attr['fontsize'] = 14
        graph.node_attr['resolution'] = 128
        
        # Mark the first city green.
        startingCity = graph.get_node(str(self.tour[0][0]))
        startingCity.attr['fillcolor'] = 'darkgreen'

        graph.layout()
        graph.draw(fileName)

class Ant(object):
    # Local and global search abilities of the ant, respectively. I
    # have no idea what good values are.
    ALPHA = 0.5
    BETA = 0.8

    def __init__(self, initialCity):
        self.initialCity = initialCity
        self.currentCity = initialCity
        self.tour = []
        self.visitedCities = set([self.currentCity,])
        self.remainingCities = set(range(1, NUM_CITIES + 1)).difference(self.visitedCities)

    def tourCities(self, pheromones):
        while self.remainingCities:
            edges = zip([self.currentCity] * len(self.remainingCities), self.remainingCities)

            # Compute all the distances.
            distances = dict((e, euclideanDistance(*e)) for e in edges)
        
            # The sum of the existing pheromone level divided by the
            # distance for every possible edge connectd to the ant's
            # current city.

            denom = sum(math.pow(pheromones[e], self.ALPHA) / math.pow(distances[e], self.BETA) \
                            for e in edges)

            probabilities = {}
            for e in edges:
                probabilities[e] = \
                    (math.pow(pheromones[e], self.ALPHA) / math.pow(distances[e], self.BETA)) / denom

            chosenEdge = weightedRandomChoice((e, p) for (e, p) in probabilities.iteritems())
            chosenCity = chosenEdge[1]
            self.tour.append(chosenEdge)
            self.currentCity = chosenCity
            
            self.visitedCities.add(chosenCity)
            self.remainingCities.remove(chosenCity)

        # Add the final edge which takes us back to our original city.
        self.tour.append((self.currentCity, self.initialCity))

        return TspSolution(self.tour)

class AntColony(object):
    NUM_ITERATIONS = 1000
    EVAPORATION_RATE = 0.3
    FIXED_PHEROMONE_ADDITION = 10
    ONLINE_PHEROMONE_CONSTANT = 100000000

    def __init__(self, numAnts):
        self.numAnts = numAnts

        # Initialize all pheromone values to 1.
        self.pheromones = dict((e, 1) for e in itertools.permutations(xrange(1, NUM_CITIES + 1), 2))

    def updatePheromones(self, solutions):
        """
        Given a list of tours ('TspSolution' objects) for all ants, update
        the pheromones on all the edges between the cities.
        """
        
        # First, we perform evaporation on all edges.
        for (e, ph) in self.pheromones.iteritems():
            self.pheromones[e] = (1 - self.EVAPORATION_RATE) * ph

        # Now we update the pheromones for each of the solutions.
        for s in solutions:
            for e in s.tour:
                # Offline pheromone updates. That is,
                # pheromones additions that defined prior to the execution of
                # the program itself. For our purposes, we'll add a fixed
                # amount of pheromone on each edge visited by the ant.
                #self.pheromones[e] += self.FIXED_PHEROMONE_ADDITION

                # Now online pheromone addition. This is defined as
                # changes to the pheromone levels that are determined
                # during execution. We'll calculate the solution cost
                # 'c' and add an amount of pheromone equal to
                # 'ONLINE_PHEROMONE_CONSTANT' / ('d' * 'c'), where 'd'
                # is the cost for each edge.
                d = euclideanDistance(*e)
                c = s.cost()
                newPh = self.ONLINE_PHEROMONE_CONSTANT / (c * d)

                self.pheromones[e] += newPh

    def execute(self):
        bestSolution = None
        bestCost = None

        for i in xrange(self.NUM_ITERATIONS):
            print "Iteration #%d --> %.2f" % (i + 1, float("inf") if not bestCost else bestCost)
            
            # Initialize the ants on random cities.
            self.ants = [Ant(random.randint(1, NUM_CITIES)) for x in xrange(self.numAnts)]

            solutions = []

            # Construct the solution candidates (tours)
            for ant in self.ants:
                solutions.append(ant.tourCities(self.pheromones))
            
            self.updatePheromones(solutions)

            for s in solutions:
                if USE_LOCAL_SEARCH:
                    # Perform local search on the solution.
                    s.improveWithLocalSearch()
                
                c = s.cost()

                if bestSolution is None or c < bestCost:
                    bestSolution = s
                    bestCost = c

        return bestSolution

class CooperativeAntColony(AntColony):
    NUM_SHARED_EDGES = 10

    def __init__(self, numAnts, solnQ, txQ, rxQs):
        super(CooperativeAntColony, self).__init__(numAnts)

        self.txQ = txQ
        self.rxQs = rxQs
        self.solnQ = solnQ

    def cooperate(self):
        # Push NUM_SHARED_EDGES edges to all the other colonies.
        for edge in random.sample(self.pheromones.keys(), self.NUM_SHARED_EDGES):
            self.txQ.put((edge, self.pheromones[edge]))

        # Read NUM_SHARED_EDGES edges randomly from all the other
        # colonies, or until all the queues are empty.
        updates = []
        isEmpty = dict((q, False) for q in self.rxQs)
        
        while len(updates) < self.NUM_SHARED_EDGES:
            # Stop if all the queues are empty, even if we don't have
            # NUM_SHARED_EDGES edges yet.
            if all(isEmpty.values()):
                break

            q = random.choice(self.rxQs)

            if not q.empty():
                updates.append(q.get())
            else:
                isEmpty[q] = True
                
        for (edge, ph) in updates:
            ourPh = self.pheromones[edge]
            
            # Calculate the mean.
            m = (ourPh + ph) / 2

            self.pheromones[edge] = m

    def execute(self):
        """
        The same as AntColony.execute but with additional cooperation
        and a different method of returning the final solution.
        """
        bestSolution = None
        bestCost = None

        for i in xrange(self.NUM_ITERATIONS):
            print "Iteration #%d --> %.2f" % (i + 1, float("inf") if not bestCost else bestCost)

            # Initialize the ants on random cities.
            self.ants = [Ant(random.randint(1, NUM_CITIES)) for x in xrange(self.numAnts)]

            solutions = []

            # Construct the solution candidates (tours)
            for ant in self.ants:
                solutions.append(ant.tourCities(self.pheromones))
            
            self.updatePheromones(solutions)

            for s in solutions:
                if USE_LOCAL_SEARCH:
                    # Perform local search on the solution.
                    s.improveWithLocalSearch()

                c = s.cost()

                if bestSolution is None or c < bestCost:
                    bestSolution = s
                    bestCost = c

            self.cooperate()

        self.solnQ.put(bestSolution)

if __name__ == "__main__":
    
    if IS_COOPERATIVE:
        solnQ = multiprocessing.Queue()

        colonies = []
        queues = []

        # Create all our queues.
        for i in xrange(NUM_COLONIES):
            queues.append(multiprocessing.Queue())

        for i in xrange(NUM_COLONIES):
            colonies.append(CooperativeAntColony(NUM_ANTS,
                                                 solnQ,
                                                 queues[i],
                                                 # All queues but the i'th one
                                                 queues[:i] + queues[(i + 1):]))

        processes = [multiprocessing.Process(target=c.execute) for c in colonies]
        
        for p in processes:
            p.start()

        for p in processes:
            p.join()

        solutions = {}
        while not solnQ.empty():
            s = solnQ.get()
            solutions[s] = s.cost()

        # Choose the best solution.
        bestCost = None
        bestSolution = None

        for (s, c) in solutions.iteritems():
            print "Solution has cost %.2f" % c

            if bestCost is None or c < bestCost:
                bestSolution = s
                bestCost = c

        index = 0
        for s in solutions.keys():
            index += 1
            title = "Solution%d" % index
            s.draw(title + ".ps")

        print "\nBest solution has cost %.2f" % bestCost
    else:
        colony = AntColony(NUM_ANTS)
        solution = colony.execute()
        solution.draw()

        print "Best solution has cost %.2f" % solution.cost()

