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

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.Root;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class NewFileAction extends AbstractAction {

    private Root root;

    public NewFileAction(Root root) {
        this.root = root;
        String name = "New file";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/New16.gif"));
        putValue(SHORT_DESCRIPTION, name);
        putValue(MNEMONIC_KEY, KeyEvent.VK_N);
    }

    public void actionPerformed(ActionEvent e) {
        if (!root.isModified() || JOptionPane.showOptionDialog(
                root.getParentFrame(),
                "Any unsaved changes will be lost. Continue?",
                "New file",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"New file", "Cancel"},
                "Cancel") == JOptionPane.YES_OPTION) {
            root.getDocument().petriNet.clear();
            root.getDocument().roles.clear();
            root.getDocument().petriNet.getNodeSimpleIdGenerator().fixFutureUniqueIds();
            root.getDocument().petriNet.getNodeSimpleIdGenerator().ensureNumberIds();
            root.getDocument().petriNet.getNodeLabelGenerator().fixFutureUniqueLabels();
            root.setClickedElement(null);
            root.refreshAll();
            root.getUndoManager().eraseAll();
            root.setCurrentFile(null);
            root.setModified(false);
        }
    }
}
