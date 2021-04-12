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

import org.petrinator.auxiliar.GraphPanel;
import org.petrinator.editor.Root;
import org.petrinator.petrinet.Element;
import org.petrinator.petrinet.Place;
import org.petrinator.petrinet.TransitionNode;
import org.petrinator.util.GraphicsTools;

import javax.swing.*;
import java.awt.*;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;

public class GraphPlaceAction extends AbstractAction
{
    private Root root;

    public GraphPlaceAction(Root root)
    {
        this.root = root;
        String name = "Generate graph";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/generateGraph16.png"));
        putValue(SHORT_DESCRIPTION, name);
        putValue(MNEMONIC_KEY, KeyEvent.VK_D);
        putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke("Graph"));
        setEnabled(true);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        Place clickedPlace = (Place) root.getClickedElement();

        if(clickedPlace.getValues().size() < 2 || SimulateAction.instants.size() < 2) // Only one firing cannot be displayed
        {
            JOptionPane.showMessageDialog(null, "Simulation must be run before graph can be displayed.");
            return;
        }

        java.util.List<java.util.List<Double>> vectors = new ArrayList<java.util.List<Double>>();
        vectors.add(SimulateAction.instants);
        vectors.add(clickedPlace.getValues());

        ArrayList<String> labels = new ArrayList<String>();
        labels.add("");
        labels.add(clickedPlace.getLabel());

        GraphPanel mainPanel = new GraphPanel(root, vectors, labels);
    }
}
