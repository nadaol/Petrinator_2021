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
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.petrinator.editor.filechooser.FileChooserDialog;
import org.petrinator.editor.filechooser.FileType;
import org.petrinator.editor.filechooser.FileTypeException;
import org.petrinator.petrinet.Document;
import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.Root;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class OpenFileAction extends AbstractAction {

    private Root root;
    private List<FileType> fileTypes;

    public OpenFileAction(Root root, List<FileType> fileTypes) {
        this.root = root;
        this.fileTypes = fileTypes;
        String name = "Open...";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/Open16.gif"));
        putValue(SHORT_DESCRIPTION, name);
        putValue(MNEMONIC_KEY, KeyEvent.VK_O);
    }

    public void actionPerformed(ActionEvent e) {
        if (!root.isModified() || JOptionPane.showOptionDialog(
                root.getParentFrame(),
                "Any unsaved changes will be lost. Continue?",
                "Open file...",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new String[]{"Open...", "Cancel"},
                "Cancel") == JOptionPane.YES_OPTION) {
            FileChooserDialog chooser = new FileChooserDialog();

            for (FileType fileType : fileTypes) {
                chooser.addChoosableFileFilter(fileType);
            }
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setCurrentDirectory(root.getCurrentDirectory());

            if (chooser.showOpenDialog(root.getParentFrame()) == JFileChooser.APPROVE_OPTION) {

                File file = chooser.getSelectedFile();
                FileType chosenFileType = (FileType) chooser.getFileFilter();

                try {
                    root.getEventList().resetEvents();
                    Document document = chosenFileType.load(file);
                    root.setDocument(document);
                    root.setCurrentFile(file);
                    root.setModified(false);
                } catch (FileTypeException ex) {
                    JOptionPane.showMessageDialog(root.getParentFrame(), ex.getMessage());
                }

            }
            root.setCurrentDirectory(chooser.getCurrentDirectory());
        }
    }
}
