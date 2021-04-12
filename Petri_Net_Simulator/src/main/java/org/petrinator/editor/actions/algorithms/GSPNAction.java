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

import org.petrinator.editor.Root;
import org.petrinator.editor.filechooser.*;
import java.text.DecimalFormat;

import org.petrinator.util.GraphicsTools;
import pipe.calculations.StateSpaceGenerator;
import pipe.calculations.SteadyStateSolver;
import pipe.calculations.myTree;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.views.MarkingView;
import pipe.views.PetriNetView;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Date;
import pipe.modules.gspn.GSPNNew;
import pipe.exceptions.*;
import java.io.IOException;
import java.util.LinkedList;

/**
 * @author Joaquin Felici <joaquinfelici at gmail.com>
 */
public class GSPNAction extends AbstractAction
{
    private Root root;
    private static final String MODULE_NAME = "GSPN Analysis";
    private final File output = new File("tmp/GSPN_Analysis.html");
    private PetriNetView pnmlData;
    private ResultsHTMLPane results;

    public GSPNAction(Root root)
    {
        String name = "GSPN Analysis";
        putValue(NAME, name);
        this.root = root;
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/gspn16.png"));
    }

    public void actionPerformed(ActionEvent e)
    {
        /*
         * Create tmp.pnml file
         */
        FileChooserDialog chooser = new FileChooserDialog();

        if (root.getCurrentFile() != null) {
            chooser.setSelectedFile(root.getCurrentFile());
        }

        chooser.addChoosableFileFilter(new PipePnmlFileType());
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setCurrentDirectory(root.getCurrentDirectory());
        chooser.setDialogTitle("Save as...");

        File file = new File("tmp/" + "tmp" + "." + "pnml");
        FileType chosenFileType = (FileType) chooser.getFileFilter();
        try {
            chosenFileType.save(root.getDocument(), file);
        } catch (FileTypeException e1) {
            e1.printStackTrace();
        }

        /*
         * Show initial pane
         */
        this.pnmlData = new PetriNetView("tmp/tmp.pnml");
        EscapableDialog guiDialog =  new EscapableDialog(root.getParentFrame(), MODULE_NAME, true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        results = new ResultsHTMLPane(pnmlData.getPNMLName());
        contentPane.add(results);
        contentPane.add(new ButtonBar("Analyse GSPN", runAnalysis, guiDialog.getRootPane()));
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }

    /**
     * Analyse button click handler
     */
    private final ActionListener runAnalysis = new ActionListener() {

        public void actionPerformed(final ActionEvent arg0)
        {
            if (arg0.getSource() instanceof JButton)
            {
                ((JButton) arg0.getSource()).setEnabled(false);
            }
            results.setText("");

            /*
             * We let another thread run the analysis
             */
            SwingWorker sw = new SwingWorker()
            {
                @Override
                protected Object doInBackground()
                {
                    long start = new Date().getTime();
                    long efinished;
                    long ssdfinished;
                    long allfinished;
                    double explorationtime;
                    double steadystatetime;
                    double totaltime;

                    PetriNetView sourceDataLayer = new PetriNetView("tmp/tmp.pnml");
                    boolean bounded = false;

                    /*
                     * Check if petri net is bounded
                     */
                    LinkedList<MarkingView>[] markings = sourceDataLayer.getCurrentMarkingVector();
                    int[] markup = new int[markings.length];
                    for(int k = 0; k < markings.length; k++)
                    {
                        markup[k] = markings[k].getFirst().getCurrentMarking();
                    }
                    try
                    {
                        myTree tree = new myTree(sourceDataLayer, markup);
                        bounded = !tree.foundAnOmega;
                    } catch(TreeTooBigException e)
                    {
                        e.printStackTrace();
                    }

                    // This will be used to store the reachability graph data
                    File reachabilityGraph = new File("results.rg");

                    // This will be used to store the steady state distribution
                    double[] pi;

                    String s = "<h2>GSPN Steady State Analysis Results</h2>";

                    results.setVisibleProgressBar(true);
                    results.setIndeterminateProgressBar(false);

                    for(int i = 0; i < 1; i++)
                    {
                        if(!sourceDataLayer.hasTimedTransitions())
                        {
                            s += "This Petri net has no timed transitions, so GSPN analysis cannot be performed.";
                            results.setText(s);
                        }
                        else if(!bounded)
                        {
                            s += "This Petri net does not seem to be bounded, so GSPN analysis cannot be performed.";
                            results.setText(s);
                        }
                        else
                        {
                            try
                            {
                                results.setStringProgressBar("State Space exploration...");
                                results.setIndeterminateProgressBar(true);

                                /*
                                 * Let's create the reachability graph
                                 */
                                StateSpaceGenerator.generate(sourceDataLayer, reachabilityGraph, results);
                                efinished = new Date().getTime();
                                System.gc();

                                results.setIndeterminateProgressBar(false);
                                results.setStringProgressBar("Solving the steady state ...");
                                results.setIndeterminateProgressBar(true);

                                /*
                                 * Analyse data from reachability graph
                                 */
                                pi = SteadyStateSolver.solve(reachabilityGraph);

                                ssdfinished = new Date().getTime();
                                System.gc();

                                results.setIndeterminateProgressBar(false);
                                results.setStringProgressBar("Computing and formating resutls ...");
                                results.setIndeterminateProgressBar(true);

                                /*
                                 * Now format and display the results nicely
                                 */
                                GSPNNew gspn = new GSPNNew();
                                s += gspn.displayResults(sourceDataLayer, reachabilityGraph, pi, output);

                                allfinished = new Date().getTime();
                                explorationtime = (efinished - start) / 1000.0;
                                steadystatetime = (ssdfinished - efinished) / 1000.0;
                                totaltime = (allfinished - start) / 1000.0;
                                DecimalFormat f = new DecimalFormat();
                                f.setMaximumFractionDigits(5);

                                s += "<br>State space exploration took "
                                        + f.format(explorationtime) + "s";
                                s += "<br>Solving the steady state distribution took "
                                        + f.format(steadystatetime) + "s";
                                s += "<br>Total time was "
                                        + f.format(totaltime) + "s";

                                results.setEnabled(true);
                                results.setText(s);//
                                System.gc();
                            } catch (OutOfMemoryError e)
                            {
                                System.gc();
                                results.setText("");
                                s += "Memory error: " + e.getMessage();

                                s += "<br>Not enough memory. Please use a larger heap size."
                                        + "<br>"
                                        + "<br>Note:"
                                        + "<br>The Java heap size can be specified with the -Xmx option."
                                        + "<br>E.g., to use 512MB as heap size, the command line looks like this:"
                                        + "<br>java -Xmx512m -classpath ...\n";
                                results.setText(s);
                                return null;
                            } catch (TimelessTrapException e) // If we got caught in a timeless trap, let's report it.
                            {
                                s += "<br>" + e.getMessage();
                                results.setText(s);
                                return null;
                            } catch (IOException e) // If there was a problem reading or writing files
                            {
                                s += "<br>" + e.getMessage();
                                results.setText(s);
                                return null;
                            } catch (MarkingNotIntegerException e)
                            {
                                JOptionPane.showMessageDialog(null,
                                        "Functional arc weight cannot have non-integer value. Please check again.");
                                sourceDataLayer.restorePlaceViewsMarking();
                                return null;

                            } catch(Exception e)
                            {
                                e.printStackTrace();
                            }

                            pi = null;
                        }
                    }

                    return null;
                }

                @Override
                protected void done()
                {
                    super.done();
                    results.setVisibleProgressBar(false);

                    if(arg0.getSource() instanceof JButton)
                    {
                        ((JButton) arg0.getSource()).setEnabled(true);
                    }

                }
            };
            sw.execute();
        }
    };
}