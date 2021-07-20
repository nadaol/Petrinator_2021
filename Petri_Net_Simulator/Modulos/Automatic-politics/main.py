from Rdp import Rdp
from ApnLa import ApnLa
from exceptions import NetException
import pflowEditor as editor
import argparse
import numpy as np
import matplotlib.pyplot as plt


def main():
    args = getArgs()

    loadModified = args.load_mod
    verbose = args.verbose
    pflowFile = args.net_name
    apn = None

    f = open("results.txt", "w")
    for j in range(int(args.repeat)):
        block = False
        cantDisparos = []
        probabilidades = []

        print("<br><h3>","Inicializando iteracion ", j,"</h3>")
        apn = ApnLa(args.jsonFile[0], loadModified, args.type)

        if verbose:
            apn.printClusters()

        for i in range(1, int(args.fireNumber) + 1):
            try:
                apn.fireNext()
            except NetException:
                print("<br>Red bloqueada - se anula la ejecucion<br>")
                block = True
                break

            if verbose and i % 2000 == 0:
                print(i)
                printClustersProbabilities(apn)

            if i == 1:
                probs = apn.getClusterProbs()
                for elem in probs:
                    probabilidades.append([])
                    for _ in elem:
                        probabilidades[-1].append([])

            if i % 50 == 0 or i == 1:
                cantDisparos.append(i)
                probs = apn.getClusterProbs()
                for l in range(len(probs)):
                    for k in range(len(probs[l])):
                        probabilidades[l][k].append(probs[l][k])



        showPlots(cantDisparos, probabilidades, apn.getClusterTransitions())

        if not block:
            writeResults(f, apn)

        printClustersProbabilities(apn)
    f.close()

    if verbose and apn is not None:
        printInvariantCosts(apn)

    if pflowFile != 'null' and apn is not None:
        editPflow(apn, pflowFile)

    print(f'\nUpdate T : {apn.rdp.updateT}')
    print(f'Conflictos {apn.rdp.conflictList[1:-1]}')


def getArgs():
    # Program arguments
    parser = argparse.ArgumentParser(description='learning automata')
    parser.add_argument('jsonFile', type=str, nargs=1)
    parser.add_argument('-n', '--num', dest='fireNumber',
                        default=0, help='number of transistions to fire')
    parser.add_argument('-l', '--load_mod', action='store_true',
                        help='load net wih control places')
    parser.add_argument('-m', '--mod', dest='net_name', default='null',
                        help='net to modify')
    parser.add_argument('-r', '--rep', dest='repeat',
                        default=1, help='repeat complete execution')
    parser.add_argument('-v', '--verbose', action='store_true',
                        help='load net wih control places')
    parser.add_argument('-t', '--type', dest='type', default='simp', help='simp/inv')

    return parser.parse_args()


def printClustersProbabilities(apn):
    print("<br>CLUSTER PROBABILITY:<br>")
    for cluster in apn.clusterManager.clusters:
        if cluster.LA is not None:
            printCluster(cluster)

    for cluster in apn.clusterManager.controlClusters:
        printCluster(cluster)


def printCluster(cluster):
    str_vector = ''
    for i in cluster.transitionList:
        str_vector += '{:>6}, '.format(i+1)#str_vector += '{:>6}, '.format(i)
    str_vector = str_vector[:-2]
    print('&emsp[{}]'.format(str_vector))

    str_vector = ''
    for i in cluster.LA.probabilityVector.tolist():
        str_vector += '{:1.4f}, '.format(i)
    str_vector = str_vector[:-2]
    print('&emsp[{}]<br>'.format(str_vector))


def printInvariantCosts(apn):
    print('<br>INVARIANT COST:<br>')
    for inv in apn.clusterManager.tInvariants:
        cost = 0
        for t in inv:
            cost += apn.rdp.costVector[t]
        print('&emsp* Invariant: ', inv)
        print('&emsp- Cost: ', cost)


def writeResults(f, apn):
    for cluster in apn.clusterManager.clusters:
        if cluster.LA is not None:
            f.write(np.array2string(cluster.LA.probabilityVector))
    for cluster in apn.clusterManager.controlClusters:
        if cluster.LA is not None:
            f.write(np.array2string(cluster.LA.probabilityVector))
    f.write('\n')


def editPflow(apn, pflowFile):
    petriShape = editor.obtain_elements(
        apn.rdp.iPlusMatrix, apn.rdp.iMinusMatrix, apn.rdp.inhibitionMatrix, apn.rdp.initialMarking)

    newTransitions = petriShape.transitions[-(len(apn.rdp.updateT) - 1):]
    newPlaces = petriShape.places[-len(newTransitions) * 2:]
    newArcs = []

    for arc in petriShape.arcs:
        for place in newPlaces:
            if arc.srcId == place.label or arc.dstId == place.label:
                newArcs.append(arc)

    editor.modify_net(pflowFile, newPlaces, newTransitions, newArcs)


def showPlots(cantDisparos, probabilidades, labels):
    types = ['-', '--', '-.', ':', '.', ',']
    fig, axs = plt.subplots(round((len(probabilidades) + 1) / 2), 2, figsize=(12, 12))
    for l in range(len(probabilidades)):
        for k in range(len(probabilidades[l])):
            label = "T" + str(labels[l][k] + 1)
            axs.flat[l].plot(cantDisparos, probabilidades[l][k], types[k], label=label, alpha=0.7)
        axs.flat[l].legend()
    if len(probabilidades) % 2 != 0:
        axs.flat[-1].set_visible(False)
    fig.savefig('plot.png', dpi=60)#300


if __name__ == "__main__":
    main()
