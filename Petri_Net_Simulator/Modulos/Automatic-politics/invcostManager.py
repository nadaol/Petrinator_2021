import re
import statistics

from costManager import costManager


class invcostManager(costManager):

    def __init__(self, tInvariants, rdp):

        print("Simulacion con costo por invariantes")

        self.tInvariants = tInvariants
        self.invCost = []
        self.invHistoric = []
        self.invMean = []
        self.rdp = rdp

        for _ in self.tInvariants:
            self.invCost.append(0)
            self.invHistoric.append([])
            self.invMean.append(0)

        self.invariantStr = self.createRegexStr()

        self.partialInvariants = []

    def transitionFired(self, transition, cost):
        self.invariantAnalysis(transition)
        return

    def getBeta(self, cluster):

        cost = []
        for inv in self.tInvariants:
            if cluster.LA.firedAction in inv:
                cost.append(self.invCost[self.tInvariants.index(inv)])

        cost = [val for val in cost if val != 0]
        mean = [val for val in self.invMean if val != 0]

        if (not cost) or (not mean):
            return -1

        if statistics.fmean(self.invMean) == min(mean):
            selectedMean = -1
        else:
            selectedMean = min(mean)

        if min(cost) <= selectedMean:
            beta = 0
        else:
            beta = 1

        return beta

    def createRegexStr(self):
        tinv = ';\n'.join(';'.join('%d' % x for x in y)
                          for y in self.tInvariants)
        return "\n{0!s};\n".format(tinv)

    def updateCost(self, cost, invNum):
        self.invCost[invNum] = cost
        self.invHistoric[invNum].append(cost)
        self.invMean[invNum] = statistics.mean(self.invHistoric[invNum][-5:])

    def invariantAnalysis(self, fireTransition):

        updateT = True
        for inv in self.tInvariants:
            if fireTransition in inv:
                updateT = False
        if updateT:
            return

        newPartial = True

        for partInv in self.partialInvariants:
            pattern = '\\n(?:{}{};)'.format(partInv, fireTransition)

            match = re.search(pattern, self.invariantStr)
            if match:
                newPartial = False
                pattern = '\\n(?:{}{};\\n)'.format(partInv, fireTransition)
                if re.search(pattern, self.invariantStr):
                    cost, invNum = self.rdp.calcularCosto(
                        '{}{}'.format(partInv, fireTransition))

                    self.updateCost(cost, invNum)

                    self.partialInvariants.remove(partInv)
                    break
                else:
                    self.partialInvariants[self.partialInvariants.index(
                        partInv)] += '{};'.format(fireTransition)
                    break

        if newPartial:
            self.partialInvariants.append('{};'.format(fireTransition))
