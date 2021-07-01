import statistics

from costManager import costManager


class simpcostManager(costManager):

    def __init__(self, rdp):

        print("Simulacion con costo simple")

        self.rdp = rdp

        self.transitionHistoric = []
        self.transitionMean = []

        for i in range(rdp.nTransitions):
            self.transitionHistoric.append([])
            self.transitionMean.append(0)

    def transitionFired(self, transition, cost):

        self.transitionHistoric[transition].append(cost)
        self.transitionMean[transition] = statistics.mean(self.transitionHistoric[transition][-5:])

        pass

    def getBeta(self, cluster):

        clusterMeans = []

        for t in cluster.transitionList:
            clusterMeans.append(self.transitionMean[t])

        meanValues = [val for val in clusterMeans if val != 0]
        if not meanValues:
            return -1

        if statistics.fmean(meanValues) == min(meanValues):
            selectedMean = -1
        else:
            selectedMean = min(meanValues)

        if self.transitionHistoric[cluster.LA.firedAction][-1] <= selectedMean:
            beta = 0
        else:
            beta = 1

        return beta
