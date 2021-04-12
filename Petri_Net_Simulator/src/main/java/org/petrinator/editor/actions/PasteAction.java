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
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.petrinator.editor.commands.PasteCommand;
import org.petrinator.petrinet.Element;
import org.petrinator.petrinet.PetriNet;
import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.Root;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class PasteAction extends AbstractAction {

    private Root root;

    public PasteAction(Root root) {
        this.root = root;
        String name = "Paste";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/Paste16.gif"));
        putValue(SHORT_DESCRIPTION, name);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl V"));
        setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
        PetriNet petriNet = root.getDocument().petriNet;
        Set<Element> pastedElements = root.getClipboard().getContents(petriNet);
        root.getUndoManager().executeCommand(new PasteCommand(pastedElements, root.getDocument().petriNet.getCurrentSubnet(), petriNet));
        //TODO: getViewTranslation()
        root.setClickedElement(null);
        root.getSelection().clear();
        root.getSelection().addAll(pastedElements);
        root.refreshAll();
    }
}
