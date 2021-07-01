from Cluster import Cluster
from LearningAutomata import LearningAutomata
import random


# Administers all clusters asociated to a petri net.
class ClusterManager:

    def __init__(self, clusterList, updateT, control, tInvariants, costManager):
        self.updateT = updateT
        self.clusters = []
        self.controlClusters = []
        self.costManager = costManager

        # Creation of all individual clusters and respective learning automata.
        for i in range(len(clusterList)):
            automata = None
            if i != 0:
                automata = LearningAutomata(clusterList[i])
            cluster = Cluster(automata, clusterList[i], updateT[i])

            if not control[i]:
                self.clusters.append(cluster)
            else:
                self.controlClusters.append(cluster)

        self.tInvariants = tInvariants

    def updateCost(self, fireTransition, cost):
        self.costManager.transitionFired(fireTransition, cost)
        return

    def updateIfNecessary(self, numT):
        if self.isUpdate(numT):
            cluster = self.getClusterFromUpdate(self.clusters, numT)
            if cluster is None:
                cluster = self.getClusterFromUpdate(self.controlClusters, numT)

            beta = self.costManager.getBeta(cluster)
            if beta == -1:
                return
            cluster.updateLA(beta)

    def getClusterFromUpdate(self, clusterList, numT):
        for cluster in clusterList:
            if numT == cluster.updateT:
                return cluster
        return None

    def isUpdate(self, transition):
        if transition in self.updateT:
            return True
        else:
            return False

    def localEnabledList(self, transitionList, enabledList):
        localEnList = []

        for i in range(len(enabledList)):
            if (enabledList[i] == True) and (i in transitionList):
                localEnList.append(i)
        return localEnList

    # returns transition to fire based on enabled vector
    def getFireTransition(self, enabledTransitions):

        selectedCluster, localEnabled = self.getFireCluster(enabledTransitions)

        selectedTransition = -1
        if selectedCluster is None:
            selectedTransition = localEnabled[0]
        else:
            if self.clusters.index(selectedCluster) == 0:
                selectedTransition = random.choice(localEnabled)
            else:
                selectedTransition = selectedCluster.executeLA(localEnabled)

        return selectedTransition

    def getFireCluster(self, enabled):

        enabledClusters = self.enabledClusters(enabled)

        for cluster in enabledClusters[1]:
            if cluster.LA is None:
                return cluster, self.getClusterEnabledTransitions(cluster, enabled)

        if len(enabledClusters[0]) > 0:
            controlCluster, clusterEnabledTransitions = self.selectCluster(
                enabled, enabledClusters[0])
            selectedTransition = controlCluster.executeLA(
                clusterEnabledTransitions)
            for cluster in enabledClusters[1]:
                if selectedTransition in cluster.transitionList:
                    return cluster, self.getClusterEnabledTransitions(cluster, enabled)
            return None, [selectedTransition]
        else:
            return self.selectCluster(enabled, enabledClusters[1])

    def enabledClusters(self, enabledTransitions):

        enabledClusters = [[]]

        for cluster in self.controlClusters:
            for t in range(len(enabledTransitions)):
                if enabledTransitions[t]:
                    if t in cluster.transitionList:
                        enabledClusters[0].append(cluster)
                        break

        enabledClusters.append([])

        for cluster in self.clusters:
            for t in range(len(enabledTransitions)):
                if enabledTransitions[t]:
                    if t in cluster.transitionList:
                        enabledClusters[1].append(cluster)
                        break

        return enabledClusters

    def getClusterEnabledTransitions(self, cluster, enabledTransitions):

        localEnabled = []

        for t in cluster.transitionList:
            if enabledTransitions[t]:
                localEnabled.append(t)

        return localEnabled

    def selectCluster(self, enabledTransitions, enabledClusters):
        clusterProb = []
        clusterEnabledTransitions = []

        for cluster in enabledClusters:
            localEnabled = self.getClusterEnabledTransitions(
                cluster, enabledTransitions)
            clusterProb.append(len(localEnabled) / len(enabledTransitions))
            clusterEnabledTransitions.append(localEnabled)
        selectedCluster = random.choices(enabledClusters, clusterProb, k=1)[-1]

        return selectedCluster, clusterEnabledTransitions[enabledClusters.index(selectedCluster)]

    def resolveConflict(self, cluster, enabledList):
        actions = self.localEnabledList(
            self.clusters[cluster].transitionList, enabledList)
        return self.clusters[cluster].executeLA(actions)

    def setClusterFiredTransition(self, transition):
        for cluster in self.clusters:
            if transition in cluster.transitionList:
                cluster.setLastTransition(transition)

    def setControlClusterFiredTransition(self, transition):
        for cluster in self.controlClusters:
            if transition in cluster.transitionList:
                cluster.setLastTransition(transition)
