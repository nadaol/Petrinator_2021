import json
import numpy as np
from exceptions import NetException


class Rdp:

    def __init__(self, jsonFile, loadModified):
        self.updateT = [None]
        self.conflictList = []
        self.iMatrix = []
        self.iPlusMatrix = []
        self.iMinusMatrix = []
        self.inhibitionMatrix = []
        self.costVector = []
        self.marking = []
        self.tInvariants = []
        self.nPlaces = 0
        self.nTransitions = 0
        self.controlPlaces = []
        self.controlConflicts = []

        self.initFromFile(jsonFile, jsonFile.replace(".json", ".cfg.json"), loadModified)

        if not loadModified:
            self.modifyNet()

        self.initialMarking = np.copy(self.marking)
        self.clusterlist = self.defineClusterList(self.conflictList)

    def initFromFile(self, fileName, settingsFileName, loadModified):

        # Load data from both files
        json_file = open(fileName, "r")
        json_matrices = json.load(json_file)
        json_file.close()

        json_file = open(settingsFileName, "r")
        json_settings = json.load(json_file)
        json_file.close()

        # Matrices
        self.marking = np.array(json_matrices["Marcado"])
        self.iPlusMatrix = np.array(json_matrices["I+"])
        self.iMinusMatrix = np.array(json_matrices["I-"])
        self.inhibitionMatrix = np.array(json_matrices["Inhibicion"])

        if loadModified:
            # Conflicts
            conflictList = json_settings["Conflictos"]
            for conflict in conflictList:
                self.conflictList.append(self.parseElementList(conflict, 'T'))
            # Update Transitions
            updateT = json_settings["UpdateT"]
            self.updateT.extend(self.parseElementList(updateT, 'T'))
            # I Matrix
            self.iMatrix = np.array(json_matrices["Incidencia"])
            # Control Conflicts
            controlIndexes = json_settings["ClusterControl"]
            self.controlConflicts = [False] * len(conflictList)
            for index in controlIndexes:
                self.controlConflicts[index - 1] = True

        # Costs
        self.costVector = np.array(json_settings["Costos"])

        # Invariants
        invariantList = json_settings["Invariantes"]
        for invariant in invariantList:
            self.tInvariants.append(self.parseElementList(invariant, 'T'))

        try:
            places = json_settings["ControlPlaces"]
            self.controlPlaces = self.parseElementList(places, 'P')
        except:
            pass

        self.nPlaces = self.iPlusMatrix.shape[0]
        self.nTransitions = self.iPlusMatrix.shape[1]

    # Removes elemType from each member in elist. Substracts 1 to follow matrix notation
    def parseElementList(self, elist, elemType):
        newList = []
        for elem in elist:
            elem = int(elem.replace(elemType, '')) - 1
            newList.append(elem)
        return newList

    # Cluster list composed of conflicts + cluster0
    def defineClusterList(self, conflicts):
        usedTransitions = []
        for conflict in conflicts:
            usedTransitions.extend(conflict)

        # Cluster0 composed of all transitions not in any conflict
        clusterZ = []
        for T in range(self.nTransitions):
            if usedTransitions.count(T) == 0:
                clusterZ.append(T)

        conflictList = conflicts
        conflictList.insert(0, clusterZ)
        self.controlConflicts.insert(0, False)
        return conflictList

    # Adds places and transitions to petri net matrices based on existing conflicts
    def modifyNet(self):
        # define conflicts and add places/transitions
        initConflicts, controlConflicts = self.identifyConflicts()
        self.conflictList, self.controlConflicts = self.joinConflicts(initConflicts, controlConflicts)

        for conflict in self.conflictList:
            self.insertPlacesTransitions(conflict)

    def identifyConflicts(self):

        conflictMatrix = []
        controlConflicts = []
        index = 0
        for row in self.iMinusMatrix:
            conflict = []
            for t in range(len(row)):
                if row[t] > 0:
                    conflict.append(t)

            # Detects Conflict 
            if len(conflict) > 1:
                conflictRow = [0] * self.nTransitions
                for t in conflict:
                    conflictRow[t] = 1
                conflictMatrix.append(conflictRow)

                # Detects Control Conflicts
                if index in self.controlPlaces:
                    controlConflicts.append(True)
                else:
                    controlConflicts.append(False)

            index += 1

        return np.asarray(conflictMatrix), controlConflicts

    def joinConflicts(self, conflictMatrix, controlConflicts):

        joinedConflicts, keepIndex = np.unique(conflictMatrix, return_index=True, axis=0)

        if len(keepIndex) == conflictMatrix.shape[0]:
            joinedConflicts = conflictMatrix

        potentialConflicts = []
        for row in joinedConflicts:
            conflict = []
            for i in range(len(row)):
                if row[i] > 0:
                    conflict.append(i)
            potentialConflicts.append(conflict)

        if len(keepIndex) != conflictMatrix.shape[0]:
            newControlConflict = []

            for i in range(len(joinedConflicts)):
                newControlConflict.append(False)
                for j in range(len(conflictMatrix)):
                    if (joinedConflicts[i] == conflictMatrix[j]).all():
                        newControlConflict[i] |= controlConflicts[j]

            return potentialConflicts, newControlConflict
        return potentialConflicts, controlConflicts

    # Adds update transition and associated places to the net matrices
    def insertPlacesTransitions(self, conflict):

        # Para cada matriz -> 2 filas, 1 col. Marcado 2 col. Costo 1 col
        newIMinus = self.addRowsColumns(self.iMinusMatrix)
        newIPlus = self.addRowsColumns(self.iPlusMatrix)
        newInhibition = self.addRowsColumns(self.inhibitionMatrix)

        # Matriz I-
        # 1er fila, 1 en col de la T agregada
        newIMinus[-2, -1] = 1
        # 2da fila, 1 en col de las T del conflicto
        for T in conflict:
            newIMinus[-1, T] = 1

        # Matriz I+
        # 1er fila: plazas de entradas compartidas de las T del conflicto. Buscar T que entran a esas plazas.
        sharedPlaces = []
        for T in conflict:
            sharedPlaces.append(self.iMinusMatrix[:, T])

        prePlaces = np.ones(self.nPlaces)
        for i in range(len(sharedPlaces)):
            prePlaces = np.logical_and(prePlaces, sharedPlaces[i])

        input_transitions = np.zeros(self.nTransitions)
        for j in range(len(prePlaces)):
            if prePlaces[j]:
                input_transitions = np.logical_or(
                    input_transitions, self.iPlusMatrix[j, :])
        newIPlus[-2, :-1] = input_transitions

        # 2da fila: 1 en col de las T agregada
        newIPlus[-1, -1] = 1

        # I: sumar ambas
        newIMatrix = newIPlus - newIMinus

        # Inhibicion:
        # 1er fila: todos ceros
        # 2da fila: 1 en col de la T agergada
        newInhibition[-1, -1] = 1

        # Marcado: 1 token en 2da col agregada si alguna plaza compartida tiene token
        prePlacesMarking = np.logical_and(prePlaces, self.marking)

        getsToken = np.isin(True, prePlacesMarking)
        newMarking = np.append(self.marking, [0, getsToken])

        # Cost: 0 en la T agregada
        newCost = np.append(self.costVector, 0)

        # Reemplazando
        self.iMinusMatrix = newIMinus
        self.iPlusMatrix = newIPlus
        self.iMatrix = newIMatrix
        self.inhibitionMatrix = newInhibition
        self.marking = newMarking
        self.costVector = newCost

        self.updateT.append(self.nTransitions)

        self.nPlaces += 2
        self.nTransitions += 1

        return

    # Adds 2 rows and 1 column to the matrix.
    def addRowsColumns(self, matrix):
        newMatrix = np.append(matrix, np.zeros(
            (self.nPlaces, 1)), axis=1)

        newMatrix = np.append(newMatrix, np.zeros(
            (2, self.nTransitions + 1)), axis=0)
        return newMatrix

    def calcularSensibilizadas(self):

        # Marking restrictions
        T = self.iMinusMatrix.transpose()
        enabled = np.full(len(T), True)

        for row in range(len(T)):
            for i in range(len(T[row])):
                if self.marking[i] < T[row][i]:
                    enabled[row] = False
                    break

        # Inhibition restrictions
        T = self.inhibitionMatrix.transpose()

        for row in range(len(T)):
            for i in range(len(T[row])):
                if T[row][i] != 0 and T[row][i] <= self.marking[i]:
                    enabled[row] = False
                    break

        # Raise exception when net is blocked to halt progress
        if np.count_nonzero(enabled) == 0:
            print(self.marking)
            raise NetException("Red bloqueada")

        return enabled

    def fire(self, numTransicion):
        self.marking = self.marking + \
                       self.iMatrix[:, numTransicion]
        return self.costVector[numTransicion]

    # Returns current cost for an invariant and its index.
    def calcularCosto(self, invariant):
        costo = 0
        transitions = invariant.split(';')
        for transition in transitions:
            costo += self.costVector[int(transition)]
        temp = [int(val) for val in transitions]
        return costo, self.tInvariants.index(temp)
