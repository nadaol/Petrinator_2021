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

package org.petrinator.auxiliar;

import javax.swing.*;
import java.awt.*;
import org.petrinator.editor.Root;

/**
 * @author Joaquin Felici <joaquinfelici at gmail.com>
 */
public class ProgressBarDialog
{
    private JDialog dialog;

    public ProgressBarDialog(Root root, String message)
    {
        dialog = new JDialog(root.getParentFrame(), false); // We set it modal so it doesn't interrupt the thread
        dialog.setUndecorated(false);
        dialog.setLocationRelativeTo(root.getParentFrame());
        dialog.setTitle("Petrinator");

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setStringPainted(true);
        bar.setPreferredSize(new Dimension(200, 25));
        bar.setBorderPainted(true);
        bar.setString(message);

        dialog.add(bar);
        dialog.pack();
    }

    public void show(boolean b)
    {
        dialog.setVisible(b);
        if(!b)
            dialog.dispose();
    }
}
