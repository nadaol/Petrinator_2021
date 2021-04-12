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
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.petrinator.editor.actions;

import org.petrinator.editor.Root;
import org.petrinator.editor.commands.SetArcReaderCommand;
import org.petrinator.petrinet.Arc;
import org.petrinator.util.GraphicsTools;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author jan.tancibok
 */
public class SetArcReaderAction extends AbstractAction {

    private Root root;

    public SetArcReaderAction(Root root) {
        this.root = root;
        String name = "Set/unset reader arc type";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/reader.png"));
        putValue(SHORT_DESCRIPTION, name);
        //putValue(MNEMONIC_KEY, KeyEvent.VK_I);
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        if (root.getClickedElement() != null) {
            if (root.getClickedElement() instanceof Arc) {
                Arc arc = (Arc) root.getClickedElement();
                boolean isReader = arc.getType().equals(Arc.READ);
                root.getUndoManager().executeCommand(new SetArcReaderCommand(arc, !isReader));
            }
        }
    }
}
