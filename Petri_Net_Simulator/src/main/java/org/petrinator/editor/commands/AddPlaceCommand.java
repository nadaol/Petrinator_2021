/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.petrinator.editor.commands;

import java.util.LinkedList;

import org.petrinator.petrinet.Element;
import org.petrinator.petrinet.Node;
import org.petrinator.petrinet.PetriNet;
import org.petrinator.petrinet.Place;
import org.petrinator.petrinet.Subnet;
import org.petrinator.util.Command;


/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class AddPlaceCommand implements Command {

    private Subnet subnet;
    private int x, y;
    private Place createdPlace;
    private PetriNet petriNet;

    public AddPlaceCommand(Subnet subnet, int x, int y, PetriNet petriNet) {
        this.subnet = subnet;
        this.x = x;
        this.y = y;
        this.petriNet = petriNet;
    }

    public void execute() {
        createdPlace = new Place();
        createdPlace.setCenter(x, y);
        petriNet.getNodeSimpleIdGenerator().setUniqueId(createdPlace);
        petriNet.getNodeLabelGenerator().setLabelToNewlyCreatedNode(createdPlace);
        petriNet.getInitialMarking().setTokens(createdPlace,0);
        subnet.addElement(createdPlace);
    }

    public void undo() {
        new DeleteElementCommand(createdPlace).execute();
    }

    public void redo() {
        subnet.addElement(createdPlace);
    }

    @Override
    public String toString() {
        return "Add place";
    }

    public Place getCreatedPlace() {
        return createdPlace;
    }
}
