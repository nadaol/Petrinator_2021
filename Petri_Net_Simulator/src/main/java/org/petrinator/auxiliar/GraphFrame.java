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
/*
package org.petrinator.auxiliar;

import net.sourceforge.jpowergraph.defaults.DefaultGraph;
import net.sourceforge.jpowergraph.defaults.TextEdge;
import net.sourceforge.jpowergraph.layout.Layouter;
import net.sourceforge.jpowergraph.layout.spring.SpringLayoutStrategy;
import net.sourceforge.jpowergraph.lens.*;
import net.sourceforge.jpowergraph.manipulator.dragging.DraggingManipulator;
import net.sourceforge.jpowergraph.manipulator.popup.PopupManipulator;
import net.sourceforge.jpowergraph.painters.edge.LineEdgePainter;
import net.sourceforge.jpowergraph.painters.edge.LoopEdgePainter;
import net.sourceforge.jpowergraph.swing.SwingJGraphPane;
import net.sourceforge.jpowergraph.swing.SwingJGraphScrollPane;
import org.petrinator.auxiliar.SwingJGraphViewPane;
import net.sourceforge.jpowergraph.swing.manipulator.SwingPopupDisplayer;
import net.sourceforge.jpowergraph.swtswinginteraction.color.JPowerGraphColor;
import org.petrinator.editor.Root;
import pipe.extensions.jpowergraph.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class GraphFrame extends JFrame
{
    private Root root;

    public void constructGraphFrame(DefaultGraph graph, String markingLegend, Root root)
    {
        this.setIconImage(root.getParentFrame().getIconImage());
        this.setLocationRelativeTo(root.getParentFrame());
        setSize(800, 600);
        setLocation(600, 300);

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent wev)
            {
                Window w = wev.getWindow();
                w.setVisible(false);
                w.dispose();
            }
        });

        SwingJGraphPane jGraphPane = new SwingJGraphPane(graph);

        LensSet lensSet = new LensSet();
        lensSet.addLens(new RotateLens());
        lensSet.addLens(new TranslateLens());
        lensSet.addLens(new ZoomLens());
        CursorLens m_draggingLens = new CursorLens();
        lensSet.addLens(m_draggingLens);

        lensSet.addLens(new TooltipLens());
        lensSet.addLens(new LegendLens());
        lensSet.addLens(new NodeSizeLens());

        jGraphPane.setLens(lensSet);

        jGraphPane.addManipulator(new DraggingManipulator(m_draggingLens, -1));
        jGraphPane.addManipulator(new PopupManipulator(jGraphPane, (TooltipLens) lensSet.getFirstLensOfType(TooltipLens.class)));
        jGraphPane.setNodePainter(PIPETangibleState.class, PIPETangibleState.getShapeNodePainter());
        jGraphPane.setNodePainter(PIPEInitialTangibleState.class, PIPEInitialTangibleState.getShapeNodePainter());
        jGraphPane.setNodePainter(PIPEVanishingState.class, PIPEVanishingState.getShapeNodePainter());
        jGraphPane.setNodePainter(PIPEInitialVanishingState.class, PIPEInitialVanishingState.getShapeNodePainter());
        jGraphPane.setNodePainter(PIPEVanishingState.class, PIPEVanishingState.getShapeNodePainter());
        jGraphPane.setNodePainter(PIPEState.class, PIPEState.getShapeNodePainter());
        jGraphPane.setNodePainter(PIPEInitialState.class, PIPEInitialState.getShapeNodePainter());
        jGraphPane.setDefaultNodePainter(PIPENode.getShapeNodePainter());
        jGraphPane.setEdgePainter(TextEdge.class, new PIPELineWithTextEdgePainter(JPowerGraphColor.BLACK, JPowerGraphColor.GRAY, false));
        jGraphPane.setDefaultEdgePainter(new LineEdgePainter(JPowerGraphColor.BLACK, JPowerGraphColor.GRAY, false));
        jGraphPane.setEdgePainter(PIPELoopWithTextEdge.class, new PIPELoopWithTextEdgePainter(JPowerGraphColor.GRAY, JPowerGraphColor.GRAY, LoopEdgePainter.RECTANGULAR));
        jGraphPane.setAntialias(true);
        jGraphPane.setPopupDisplayer(new SwingPopupDisplayer(new PIPESwingToolTipListener(), new PIPESwingContextMenuListener(graph, new LensSet(), new Integer[]{}, new Integer[]{})));

        Layouter m_layouter = new Layouter(new SpringLayoutStrategy(graph));
        m_layouter.start();
        SwingJGraphScrollPane scroll = new SwingJGraphScrollPane(jGraphPane, lensSet);
        SwingJGraphViewPane view = new SwingJGraphViewPane(scroll, lensSet, true);


        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JPanel panel = new JPanel();
        panel.setLayout(gbl);
        getContentPane().add("Center", view);
        getContentPane().add("West", panel);
        if(!markingLegend.equals("") && markingLegend != null)
        {
            markingLegend = "Places order is: " + markingLegend;
        }

        JTextArea legend = new JTextArea(markingLegend + "\nHover mouse over nodes to view state marking");
        legend.setEditable(false);

        getContentPane().add("South", legend);

        setVisible(true);
    }
}


 */