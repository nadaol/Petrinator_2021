import re
from xml.dom import minidom
from xml.etree import ElementTree
from xml.etree.ElementTree import SubElement


class Arc:
    def __init__(self, arcType, source, destination, weight):
        self.arcType = arcType
        self.srcId = source
        self.dstId = destination
        self.weight = weight


class Place:
    def __init__(self, id, tokens):
        self.id = id
        self.tokens = tokens
        self.label = "P" + str(id + 1)


class Transition:
    def __init__(self, id):
        self.id = id
        self.label = "T" + str(id + 1)


class PetriShape:
    def __init__(self, places, transitions, arcs):
        self.places = places
        self.transitions = transitions
        self.arcs = arcs


def obtain_elements(iPlus, iMinus, inhibition, marking):
    nPlaces = iPlus.shape[0]
    nTransitions = iPlus.shape[1]

    placesList = []
    transitionsList = []
    arcsList = []

    for i in range(nPlaces):
        placesList.append(Place(i, marking[i]))

    for i in range(nTransitions):
        transitionsList.append(Transition(i))

    for i in range(nPlaces):
        for j in range(nTransitions):
            if iPlus[i][j] > 0:
                arcsList.append(
                    Arc('regular', transitionsList[j].label, placesList[i].label, iPlus[i][j]))
            if iMinus[i][j] > 0:
                arcsList.append(
                    Arc('regular', placesList[i].label, transitionsList[j].label, iMinus[i][j]))
            if inhibition[i][j]:
                arcsList.append(
                    Arc('inhibitor', placesList[i].label, transitionsList[j].label, inhibition[i][j]))

    return PetriShape(placesList, transitionsList, arcsList)


def modify_net(netName, newPlaces, newTransitions, newArcs):
    tree = ElementTree.parse(netName)

    for place in newPlaces:
        tree = addPlace(tree, place.label, place.tokens)

    for transitions in newTransitions:
        tree = addTransition(tree, transitions.label)

    for arc in newArcs:
        tree = addArc(tree, arc.arcType, arc.srcId, arc.dstId, arc.weight)

    # tree.write('test.pflow')
    formated_xml = prettify(tree.getroot())
    fileName = netName #"out.pflow"
    with open(fileName, 'w') as f:
        f.write(formated_xml)
    print('Red modificada exitosamente en', fileName)


x = 0
y = 0


def addPlace(tree, placeID, marking):
    global x
    global y
    a = tree.find('subnet')
    place = SubElement(a, 'place')
    place_id = SubElement(place, 'id')
    place_id.text = placeID
    place_x = SubElement(place, 'x')
    place_x.text = str(x)
    place_y = SubElement(place, 'y')
    place_y.text = str(y)
    place_label = SubElement(place, 'label')
    place_label.text = placeID
    place_tokens = SubElement(place, 'tokens')
    place_tokens.text = str(marking)
    place_isStatic = SubElement(place, 'isStatic')
    place_isStatic.text = 'false'
    #nuevo para agregarle tipo regular a las plazas
    place_type = SubElement(place, 'type')
    place_type.text = 'regular'
    #
    x += 20
    y += 20
    # mod = prettify(tree.getroot())

    # with open('test.pflow', 'w') as f:
    #    f.write(ElementTree.tostring(tree.getroot(), 'utf-8'))
    return tree


def addTransition(tree, transitionID):
    global x
    global y
    a = tree.find('subnet')
    transition = SubElement(a, 'transition')
    t_id = SubElement(transition, 'id')
    t_id.text = transitionID
    t_x = SubElement(transition, 'x')
    t_x.text = str(x)
    t_y = SubElement(transition, 'y')
    t_y.text = str(y)
    t_label = SubElement(transition, 'label')
    t_label.text = transitionID
    t_timed = SubElement(transition, 'timed')
    t_timed.text = 'false'
    t_rate = SubElement(transition, 'rate')
    t_rate.text = '1.0'
    t_auto = SubElement(transition, 'automatic')
    t_auto.text = 'false'
    t_info = SubElement(transition, 'informed')
    t_info.text = 'true'
    t_en = SubElement(transition, 'enableWhenTrue')
    t_en.text = 'false'
    t_guard = SubElement(transition, 'guard')
    t_guard.text = 'none'
    t_properties = SubElement(transition, 'stochasticProperties')
    t_properties.set('distribution', 'Exponential')
    t_properties.set('labelVar1', 'Rate (λ)')
    t_properties.set('labelVar2', ' ')
    t_properties.set('var1', '1.0')
    t_properties.set('var2', '1.0')
    x += 10
    y += 10
    # mod = prettify(tree.getroot())

    # with open('test.pflow', 'w') as f:
    #    f.write(ElementTree.tostring(tree.getroot(), 'utf-8'))
    return tree


def addArc(tree, arctype, srcID, dstID, weight):
    a = tree.find('subnet')
    arc = SubElement(a, 'arc')
    arcType = SubElement(arc, 'type')
    arcType.text = arctype
    arcSrc = SubElement(arc, 'sourceId')
    arcSrc.text = srcID
    arcDst = SubElement(arc, 'destinationId')
    arcDst.text = dstID
    arcWeight = SubElement(arc, 'multiplicity')
    arcWeight.text = str(int(weight))

    return tree


def prettify(elem):
    rough_string = ElementTree.tostring(elem, 'utf-8')
    decoded = rough_string.decode('utf-8')
    replaced = re.sub('\n *', '', decoded)
    # rough_string = rough_string.decode('utf-8').replace('\n', '')
    # rough_string = rough_string.replace(' ', '')
    reparsed = minidom.parseString(replaced)
    prettyString = reparsed.toprettyxml(indent="  ")
    return prettyString
