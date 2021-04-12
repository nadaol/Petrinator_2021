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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.util.Scanner;
import javax.swing.*;
import org.petrinator.util.GraphicsTools;
import org.petrinator.editor.Root;
import pipe.gui.widgets.EscapableDialog;
import org.petrinator.auxiliar.ResultsHTMLPane;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class AboutAction extends AbstractAction {

    private Root root;

    public AboutAction(Root root) {
        this.root = root;
        String name = "About...";
        putValue(NAME, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/About16.gif"));
        putValue(SHORT_DESCRIPTION, name);
    }

    public void actionPerformed(ActionEvent e)
    {
        /*
         * Create the dialog
         */
        EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "About", false);
        Container contentPane = guiDialog.getContentPane();
        ResultsHTMLPane results = new ResultsHTMLPane("");
        contentPane.add(results);
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);

        /*
         * Read the about.html file
         */
        InputStream aboutFile = getClass().getResourceAsStream("/about.html");
        Scanner scanner = null;
        scanner = new Scanner(aboutFile, "UTF-8");
        String s = scanner.useDelimiter("\\Z").next();
        scanner.close();

        /*
         * Show the text on dialog
         */
        results.setText(s);
    }
}
