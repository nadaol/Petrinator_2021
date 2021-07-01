from Rdp import Rdp
from ClusterManager import ClusterManager
from invcostManager import invcostManager
from simpcostManager import simpcostManager

# Interface between petri net and clusters containing learning automatas
class ApnLa:

    def __init__(self, jsonFile, loadModified, costType):

        self.rdp = Rdp(jsonFile, loadModified)

        clusterList = self.rdp.clusterlist
        updateT = self.rdp.updateT
        controlConflicts = self.rdp.controlConflicts
        tInvariants = self.rdp.tInvariants
        costManager = self.costCreator(costType)

        self.clusterManager = ClusterManager(
            clusterList, updateT, controlConflicts, tInvariants, costManager)

    # Transition firing sequence. Uses clusterManager to get transition to fire, updates marking vector in rdp
    # and then updates all necesary information in cluster manager.
    def fireNext(self):
        enabledT = self.rdp.calcularSensibilizadas()
        fireTransition = self.clusterManager.getFireTransition(enabledT)
        cost = self.rdp.fire(fireTransition)
        self.clusterManager.updateCost(fireTransition, cost)
        self.clusterManager.setClusterFiredTransition(fireTransition)
        self.clusterManager.setControlClusterFiredTransition(fireTransition)  # TODO
        self.clusterManager.updateIfNecessary(fireTransition)
        return

    def switcharoo(self):
        # self.rdp.costVector[0] = 50
        self.rdp.costVector[6] = 50
        self.rdp.costVector[4] = 25
        print(self.rdp.costVector)

    def printClusters(self):
        print("<br>CLUSTERS:<br>")
        print("&emspRegular clusters")
        for cluster in self.clusterManager.clusters:
            print('&emsp*', cluster.transitionList)
        if len(self.clusterManager.controlClusters) > 0:
            print("&emspControl clusters")
            for cluster in self.clusterManager.controlClusters:
                print('&emsp&emsp*', cluster.transitionList)

    def getClusterProbs(self):
        probs = []
        for cluster in self.clusterManager.clusters:
            if cluster.LA is not None:
                probs.append(cluster.LA.probabilityVector.tolist())
        for cluster in self.clusterManager.controlClusters:
            probs.append(cluster.LA.probabilityVector.tolist())
        return probs

    def getClusterTransitions(self):
        labels = []
        for cluster in self.clusterManager.clusters:
            if cluster.LA is not None:
                labels.append(cluster.transitionList)
        for cluster in self.clusterManager.controlClusters:
            labels.append(cluster.transitionList)
        return labels

    def costCreator(self, costType):
        if costType == "inv":
            costManager = invcostManager(self.rdp.tInvariants, self.rdp)
        elif costType == "simp":
            costManager = simpcostManager(self.rdp)
        else:
            costManager = simpcostManager(self.rdp)
        return costManager
