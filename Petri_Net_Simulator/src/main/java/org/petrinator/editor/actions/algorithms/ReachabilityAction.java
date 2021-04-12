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

package org.petrinator.editor.actions.algorithms;

import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;
import org.petrinator.editor.Root;
import org.petrinator.petrinet.Marking;
import org.petrinator.util.GraphicsTools;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.ResultsHTMLPane;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.*;

import org.petrinator.editor.actions.algorithms.reachability.CRTree;

import org.graphstream.graph.*;


/**
 * Generates the reachability/coverability graph representation for the Petri Net
 */

public class ReachabilityAction extends AbstractAction
{

    private static final String MODULE_NAME = "Reachabilty/Coverability graph";
    
    private Root root;
    private ResultsHTMLPane results;
    private JDialog guiDialog;
    private ButtonBar graphGenerate;
    private ButtonBar calculateButton;

    private ArrayList<Integer>[][] reachMatrix;

    public ReachabilityAction(Root root) {
        this.root = root;
        putValue(NAME, MODULE_NAME);
        putValue(SHORT_DESCRIPTION, MODULE_NAME);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/graph16.png"));

        guiDialog = new JDialog(root.getParentFrame(), "Reachabilty/Coverability graph", false);

        guiDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        /*
            Sets variables on null after closing the dialog window
            to free heap memory
         */
        guiDialog.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                reachMatrix = null;
                results.setText("");
            }
        });

        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        results = new ResultsHTMLPane("");
        contentPane.add(results);


        /* Buttons */
        calculateButton = new ButtonBar("Generate states", new GenerateListener(), guiDialog.getRootPane());
        contentPane.add(calculateButton);

        graphGenerate = new ButtonBar("Generate graph", new GenerateGraphListener(), guiDialog.getRootPane());
        contentPane.add(graphGenerate);

    }

    /**
     * Resets and shows the 'Reachability/Coverability' initial dialog window
     */
    public void actionPerformed(ActionEvent e) {
        results.setText("");

        // Enables button to calculate states
        calculateButton.setButtonsEnabled(true);

        // Disables the copy and save buttons
        results.setEnabled(false);

        // Disables graph button
        graphGenerate.setButtonsEnabled(false);

        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);

    }


    /**
     * Generate Button Listener
     */
    private class GenerateListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {

            // Checks if the net is valid
            if (!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            // Disables the calculate button
            calculateButton.setButtonsEnabled(false);

            String log = "<p></p><h2>Reachability/Coverability Graph Information</h2>";

            log += "<h3> Number of places: "+root.getDocument().getPetriNet().getSortedPlaces().size() +"</h3>";
            log += "<h3> Number of transitions: "+root.getDocument().getPetriNet().getSortedTransitions().size() +"</h3>";

            //TODO check tree size
            try {
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);
                log += statesTree.getTreeLog();
                reachMatrix = statesTree.getReachabilityMatrix();
                // Enables the copy and save buttons
                results.setEnabled(true);
                graphGenerate.setButtonsEnabled(true);
            } catch (StackOverflowError e) {
                log = "An error has occurred, the net might have too many states...";
            }

            results.setText(log);

        }
    }

    ;

    /**
     * Generate Graph Button Listener
     */
    private class GenerateGraphListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent) {

            displayGraph(reachMatrix);

        }

    }

    ;

    /**
     * Displays graph using Graphstream library
     *
     * @param stateMatrix matrix that contains all the states and possible transitions
     */
    private View generateGraph(ArrayList<Integer>[][] stateMatrix) {
        Graph graph = new SingleGraph("Reachability/Coverability");

        //Create a node for each state
        //Each state has a label indicated by S + state number
        for (int i = 0; i < stateMatrix.length; i++) {
            String s = Integer.toString(i);
            Node n = graph.addNode(s);
            n.addAttribute("ui.label", "S" + s);
        }

        //Create arrows that join the previous states based on the values of the stateMatrix
        //Each arrow has a label based on the transition fired that caused the change in state
        for (int i = 0; i < stateMatrix.length; i++) {
            for (int j = 0; j < stateMatrix[0].length; j++) {
                if (stateMatrix[i][j] != null) {

                    String label = "";
                    for (int k = 0; k < stateMatrix[i][j].size(); k++) {
                        label = label.concat("T" + Integer.toString(stateMatrix[i][j].get(k)));
                        if (k != stateMatrix[i][j].size() - 1) {
                            label = label.concat(",");
                        }
                    }

                    String ename = "S" + Integer.toString(i) + "-" + Integer.toString(j);
                    Edge e = graph.addEdge(ename, Integer.toString(i), Integer.toString(j), true);
                    e.addAttribute("ui.label", label);
                }
            }
        }

        //Get view from GraphStream to embedd into a JDialog
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");
        Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        View view = viewer.addDefaultView(false);
        viewer.enableAutoLayout();

        //Atributes for all nodes
        graph.addAttribute("ui.stylesheet", "node {\n" +
                "\tsize: 30px;\n" +
                "\tshape: circle;\n" +
                "\tstroke-mode: plain;\n" +
                "\tstroke-color: black;\n" +
                "\tstroke-width: 1;\n" +
                "\ttext-mode: normal;\n" +
                "\ttext-style: bold;\n" +
                "\tfill-color: rgb(156,230,255);\n" +
                "\tz-index: 1;\n" +
                "}");
        //Atributes for all edges
        graph.addAttribute("ui.stylesheet", "edge {\n" +
                "\ttext-mode: normal;\n" +
                "\ttext-style: bold;\n" +
                "\ttext-alignment: center;\n" +
                "\tz-index: 0;  \n" +
                "}");
        graph.addAttribute("ui.quality");
        graph.addAttribute("ui.antialias");
        //graph.addAttribute("ui.screenshot", "/home/jna/Desktop/test.png");

        return view;
    }

    /**
     * @param stateMatrix used to generate graph
     */
    private void displayGraph(ArrayList<Integer>[][] stateMatrix) {

        //Generate view containing the graph
        View view = generateGraph(stateMatrix);

        //JDialog to contain buttons and graph
        JDialog graphview = new JDialog();
        graphview.setSize(700, 700);
        graphview.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        //Generate buttons and JPanel to contain them
        JPanel panelbuttons = new JPanel();

        JButton zoomplus = new JButton("+");
        zoomplus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double per = view.getCamera().getViewPercent();
                view.getCamera().setViewPercent(per - 0.1);
            }
        });
        JButton zoomminus = new JButton("-");
        zoomminus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                double per = view.getCamera().getViewPercent();
                view.getCamera().setViewPercent(per + 0.1);
            }
        });
        JButton resetview = new JButton("Reset");
        resetview.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                view.getCamera().resetView();
            }
        });

        panelbuttons.add(zoomminus);
        panelbuttons.add(zoomplus);
        panelbuttons.add(resetview);

        JPanel panelgraph = new JPanel(new BorderLayout());
        ((Component) view).setPreferredSize(new Dimension(700, 600));
        panelgraph.add(panelbuttons, BorderLayout.NORTH);
        panelgraph.add((Component) view, BorderLayout.CENTER);

        graphview.getContentPane().add(panelgraph);
        graphview.pack();
        graphview.setVisible(true);
    }
}
