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

import org.petrinator.petrinet.ReferenceArc;
import org.petrinator.petrinet.ReferencePlace;
import org.petrinator.util.Command;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class DeleteReferenceArcCommand implements Command {

    private ReferenceArc ReferenceArc;
    private Command deleteReferencePlace;

    public DeleteReferenceArcCommand(ReferenceArc referenceArc) {
        this.ReferenceArc = referenceArc;
        ReferencePlace referencePlace = referenceArc.getReferencePlace();
        deleteReferencePlace = new DeletePlaceNodeCommand(referencePlace);
    }

    public void execute() {
        ReferenceArc.getParentSubnet().removeElement(ReferenceArc);
        deleteReferencePlace.execute();
    }

    public void undo() {
        ReferenceArc.getParentSubnet().addElement(ReferenceArc);
        deleteReferencePlace.undo();
    }

    public void redo() {
        ReferenceArc.getParentSubnet().removeElement(ReferenceArc);
        deleteReferencePlace.redo();
    }

    @Override
    public String toString() {
        return "Delete reference arc";
    }

}
