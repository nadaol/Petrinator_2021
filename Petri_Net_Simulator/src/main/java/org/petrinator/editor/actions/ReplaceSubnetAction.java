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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.petrinator.editor.commands.ReplaceSubnetsCommand;
import org.petrinator.editor.commands.ReplaceSubnetsLooseMethodCommand;
import org.petrinator.editor.filechooser.FileChooserDialog;
import org.petrinator.editor.filechooser.FileTypeException;
import org.petrinator.editor.filechooser.PflowxFileType;
import org.petrinator.petrinet.Document;
import org.petrinator.petrinet.Element;
import org.petrinator.petrinet.PetriNet;
import org.petrinator.petrinet.Subnet;
import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.Root;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class ReplaceSubnetAction extends AbstractAction {

    private Root root;

    public ReplaceSubnetAction(Root root) {
        this.root = root;
        String name = "Replace subnet(s)...";
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/replacesubnet.gif"));
        setEnabled(false);
    }

    private JCheckBox looseMethodCheckBox = new JCheckBox("Use loose method");

    public void actionPerformed(ActionEvent e) {
        if (root.getClickedElement() instanceof Subnet || !root.getSelection().getSubnets().isEmpty()) {
            FileChooserDialog chooser = new FileChooserDialog();
            chooser.addChoosableFileFilter(new PflowxFileType());
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setCurrentDirectory(root.getCurrentDirectory());
            chooser.setDialogTitle("Choose subnet");

            chooser.getSidebar().add(looseMethodCheckBox, BorderLayout.SOUTH);

            if (chooser.showDialog(root.getParentFrame(), "Choose") == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    Subnet storedSubnet = importSubnet(file);

                    Set<Subnet> selectedSubnets = new HashSet<Subnet>();
                    for (Element element : root.getSelectedElementsWithClickedElement()) {
                        if (element instanceof Subnet) {
                            selectedSubnets.add((Subnet) element);
                        }
                    }

                    Set<Subnet> previousSubnets = root.getDocument().petriNet.getCurrentSubnet().getSubnets();

                    PetriNet petriNet = root.getDocument().petriNet;

                    if (looseMethodCheckBox.isSelected()) {
                        root.getUndoManager().executeCommand(new ReplaceSubnetsLooseMethodCommand(selectedSubnets, storedSubnet, petriNet));
                    } else {
                        root.getUndoManager().executeCommand(new ReplaceSubnetsCommand(selectedSubnets, storedSubnet, petriNet));
                    }

                    Set<Subnet> createdElements = root.getDocument().petriNet.getCurrentSubnet().getSubnets();
                    createdElements.removeAll(previousSubnets);

                    root.getSelection().clear();
                    root.getSelection().getElements().addAll(createdElements);
                    root.getSelection().selectionChanged();

//                  root.refreshAll();
//                  root.getUndoManager().executeCommand(new ReplaceSubnetCommand(clickedSubnet, storedSubnet));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(root.getParentFrame(), ex.getMessage());
                }
            }
            root.setCurrentDirectory(chooser.getCurrentDirectory());
        }
    }

    private Subnet importSubnet(File file) throws FileTypeException {
        Subnet subnet = null;
        Document document = new PflowxFileType().load(file);
        subnet = document.petriNet.getRootSubnet();
        return subnet;
    }
}
