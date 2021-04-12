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

import java.util.HashSet;
import java.util.Set;

import org.petrinator.petrinet.Arc;
import org.petrinator.petrinet.ArcEdge;
import org.petrinator.petrinet.ReferenceArc;
import org.petrinator.petrinet.TransitionNode;
import org.petrinator.util.Command;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class DeleteTransitionNodeCommand implements Command {

    private TransitionNode transitionNode;
    private Set<Command> deleteAllArcEdges = new HashSet<Command>();

    public DeleteTransitionNodeCommand(TransitionNode transitionNode) {
        this.transitionNode = transitionNode;
        Set<ArcEdge> connectedArcs = new HashSet<ArcEdge>(transitionNode.getConnectedArcEdges());
        for (ArcEdge arcEdge : connectedArcs) { //TODO: create new class DeleteArcEdgeCommand (use also DeletePlaceNodeCommand)
            if (arcEdge instanceof Arc) {
                deleteAllArcEdges.add(new DeleteArcCommand((Arc) arcEdge));
            } else if (arcEdge instanceof ReferenceArc) {
                deleteAllArcEdges.add(new DeleteReferenceArcCommand((ReferenceArc) arcEdge));
            } else {
                throw new RuntimeException("arcEdge not instanceof Arc neither ReferenceArc");
            }
        }
    }

    public void execute() {
        for (Command deleteArc : deleteAllArcEdges) {
            deleteArc.execute();
        }
        transitionNode.getParentSubnet().removeElement(transitionNode);
    }

    public void undo() {
        for (Command deleteArc : deleteAllArcEdges) {
            deleteArc.undo();
        }
        transitionNode.getParentSubnet().addElement(transitionNode);
    }

    public void redo() {
        for (Command deleteArc : deleteAllArcEdges) {
            deleteArc.redo();
        }
        transitionNode.getParentSubnet().removeElement(transitionNode);
    }

    @Override
    public String toString() {
        return "Delete transition node";
    }
}
