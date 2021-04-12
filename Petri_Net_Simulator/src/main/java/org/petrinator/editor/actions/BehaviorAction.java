/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
 * Copyright (C) 2016-2017 Joaquin Rodriguez Felici <joaquinfelici at gmail.com>
 * Copyright (C) 2016-2017 Leandro Asson <leoasson at gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.petrinator.editor.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import org.petrinator.editor.Root;
import org.petrinator.editor.commands.SeeBehaviorCommand;
import org.petrinator.util.GraphicsTools;

public class BehaviorAction extends AbstractAction {
	
	
	private Root root;

    public BehaviorAction(Root root) {
        this.root = root;
        String name = "Behavior";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/Preferences16.gif"));
        putValue(SHORT_DESCRIPTION, name);
        setEnabled(true);
    }

	@Override
	public void actionPerformed(ActionEvent arg0) 
	{
		root.getUndoManager().executeCommand(new SeeBehaviorCommand(root.getDocument().petriNet.getRootSubnet()));
	}

}
