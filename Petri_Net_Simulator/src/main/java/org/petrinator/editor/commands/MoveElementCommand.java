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

import java.awt.Point;

import org.petrinator.petrinet.Element;
import org.petrinator.util.Command;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class MoveElementCommand implements Command {

    private Element element;
    private Point deltaPosition;

    public MoveElementCommand(Element element, Point deltaPosition) {
        this.element = element;
        this.deltaPosition = deltaPosition;
    }

    public void execute() {
        element.moveBy(deltaPosition.x, deltaPosition.y);
    }

    public void undo() {
        element.moveBy(-deltaPosition.x, -deltaPosition.y);
    }

    public void redo() {
        execute();
    }

    @Override
    public String toString() {
        return "Move element";
    }

}
