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
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import org.petrinator.editor.PNEditor;
import org.petrinator.editor.canvas.Selection;
import org.petrinator.petrinet.Element;
import org.petrinator.petrinet.PetriNet;
import org.petrinator.util.GraphicsTools;

/**
 *
 * @author matmas
 */
public class SelectAllAction extends AbstractAction {

    public SelectAllAction() {
        String name = "Select All";
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/SelectAll16.png"));
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("ctrl A"));
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PetriNet petriNet = PNEditor.getRoot().getDocument().getPetriNet();

        Selection selection = PNEditor.getRoot().getSelection();
        selection.clear();
        selection.addAll(petriNet.getCurrentSubnet().getElements());

        PNEditor.getRoot().refreshAll();
    }

//  @Override
//  public boolean shouldBeEnabled() {
//      PetriNet petriNet = PNEditor.getRoot().getDocument().getPetriNet();
//      return !petriNet.isEmpty();
//  }
}
