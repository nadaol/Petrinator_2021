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
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.petrinator.editor.commands.DeleteElementsCommand;
import org.petrinator.petrinet.Element;
import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.Root;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class DeleteAction extends AbstractAction {

    private Root root;

    public DeleteAction(Root root) {
        this.root = root;
        String name = "Delete";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/Delete16.gif"));
        putValue(SHORT_DESCRIPTION, name);
        putValue(MNEMONIC_KEY, KeyEvent.VK_D);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("DELETE"));
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) { //TODO: use getSelectedElementsWithClickedElement()
        Set<Element> elementsToDelete = new HashSet<Element>();

        for (Element selectedElement : root.getSelection()) {
            elementsToDelete.add(selectedElement);
        }
        root.getSelection().clear();
        if (root.getClickedElement() != null) {
            elementsToDelete.add(root.getClickedElement());
            root.setClickedElement(null);
        }

        if (!elementsToDelete.isEmpty()) {
            root.getUndoManager().executeCommand(new DeleteElementsCommand(elementsToDelete));
        }
    }
}
