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
import org.petrinator.util.GraphicsTools;
import org.petrinator.util.Print;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.views.PetriNetView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.net.URLDecoder;
import java.util.Arrays;

/**
 * MinimalSiphons computes minimal siphons and minimals traps of a Petri Net.
 * This module implements the algorithm presented in:
 * R. Cordone, L. Ferrarini, L. Piroddi, "Some Results on the Computation of
 * Minimal Siphons in Petri Nets"; Proceedings of the 42nd IEEE Conference on
 * Decision and Control, pp 3754-3759, Maui, Hawaii (USA), December 2003.
 *
 * @author Pere Bonet
 */
public class MarkedSiphonsAction extends AbstractAction
{
    Root root;
    private static final String MODULE_NAME = "Marked Siphons and traps";
    private ResultsHTMLPane results;

    public MarkedSiphonsAction(Root root)
    {
        this.root = root;
        String name = "Marked Siphons and traps";
        putValue(NAME, name);
        putValue(SHORT_DESCRIPTION, name);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/siphons16.png"));
    }

    public void actionPerformed(ActionEvent e) {
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

        File file = new File(get_Current_JarPath() + "/tmp/" + "tmp" + "." + "pnml");
        FileType chosenFileType = (FileType) chooser.getFileFilter();
        try {
            chosenFileType.save(root.getDocument(), file);
        } catch (FileTypeException e1) {
            e1.printStackTrace();
        }

        /*
         * Show initial pane
         */
        EscapableDialog guiDialog = new EscapableDialog(root.getParentFrame(), "Marked siphons and traps", true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
        //sourceFilePanel = new PetriNetChooserPanel("Source net", null);
        results = new ResultsHTMLPane("");
        contentPane.add(results);
        contentPane.add(new ButtonBar("Analyze", analyseButtonClick, guiDialog.getRootPane()));
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }


    public String getOsName() {
        return System.getProperty("os.name");
    }

    //Get actual absolute executed .jar path
    public String get_Current_JarPath()
    {
        String pathNet = SupervisionAction.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        pathNet = pathNet.substring(0, pathNet.lastIndexOf("/"));
        if (getOsName().startsWith("Windows") && pathNet.startsWith("/"))
            pathNet = pathNet.substring(1, pathNet.length());
        String decodedPath = null;
        try {
            decodedPath = URLDecoder.decode(pathNet, "UTF-8");
        } catch (Exception e) {
            results.setText("");
            JOptionPane.showMessageDialog(root.getParentFrame(),e.getMessage(), "Error obtaining absolute jar path", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
        //System.out.println("Jar path : " + decodedPath);
        return decodedPath;
    }

    private final ActionListener analyseButtonClick = new ActionListener() {
        public void actionPerformed(ActionEvent arg0) {
            /*
             * Read tmp file
             */
            PetriNetView sourceDataLayer = new PetriNetView(get_Current_JarPath() +"/tmp/tmp.pnml");
            String s = "<h2>Marked Siphons and Traps</h2>";

            if (sourceDataLayer == null) {
                return;
            }
            if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
            {
                s += "Invalid net!";
            } else {
                try {
                    //MinimalSiphons siphonsAlgorithm = new MinimalSiphons();
                    //s += siphonsAlgorithm.analyse(sourceDataLayer);
                    s += getMarkedSiphons();
                    results.setEnabled(true);
                } catch (OutOfMemoryError oome) {
                    System.gc();
                    results.setText("");
                    s = "Memory error: " + oome.getMessage();

                    s += "<br>Not enough memory. Please use a larger heap size."
                            + "<br>"
                            + "<br>Note:"
                            + "<br>The Java heap size can be specified with the -Xmx option."
                            + "<br>E.g., to use 512MB as heap size, the command line looks like this:"
                            + "<br>java -Xmx512m -classpath ...\n";
                    results.setText(s);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    s = "<br>Error" + e.getMessage();
                    results.setText(s);
                    return;
                }
            }
            results.setText(s);
        }
    };
    public String getMarkedSiphons()
    {
        int inicial_marking [] = root.getDocument().getPetriNet().getInitialMarking().getMarkingAsArray()[1];
        ArrayList<Integer> marked_places_index = new ArrayList();

        for(int i=0; i<inicial_marking.length;i++)
        {
            if(inicial_marking[i]>0)
            {
                marked_places_index.add(i);
            }
        }
        Print.print_arraylist_int(marked_places_index,"index plazas marcadas");
        Integer[] array_marked_places_index = marked_places_index.toArray(new Integer[0]);
        ArrayList<Integer[]> conjunto_marked_places_index = new ArrayList();
        get_groups(conjunto_marked_places_index,array_marked_places_index);
        Print.print_arraylist_int_array(conjunto_marked_places_index,"conjunto de plazas marcadas");
        get_siphons_and_traps(conjunto_marked_places_index);
        return "Todo OK!";
    }
    /* arr[]  ---> Input Array
    data[] ---> Temporary array to store current combination
    start & end ---> Staring and Ending indexes in arr[]
    index  ---> Current index in data[]
    r ---> Size of a combination to be printed */
    static void combinationUtil(Integer arr[], int data[], int start,int end, int index, int r,ArrayList<Integer[]> conjunto_marked_places_index)
    {
        // Current combination is ready to be printed, print it
        if (index == r)
        {
            Integer[] conjunto = new Integer[r];
            for (int j=0; j<r; j++)
            {
                conjunto[j]=data[j];
            }
            conjunto_marked_places_index.add(conjunto);
            return;
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        for (int i=start; i<=end && end-i+1 >= r-index; i++)
        {
            data[index] = arr[i];
            combinationUtil(arr, data, i+1, end, index+1, r,conjunto_marked_places_index);
        }
    }

    /*Driver function to check for above function*/
    public void get_groups (ArrayList<Integer[]> conjunto_marked_places_index,
                                   Integer array_marked_places_index[])
    {
        int n = array_marked_places_index.length;
        for(int r=2; r<=n;r++)
        {
            int data[] = new int[r];
            // Print all combination using temporary array 'data[]'
            combinationUtil(array_marked_places_index, data, 0, n-1, 0, r, conjunto_marked_places_index);
        }
    }
    public void get_siphons_and_traps(ArrayList<Integer[]> groups)
    {
        int[][] FordwardMatrix = root.getDocument().getPetriNet().getForwardIMatrix();
        int[][] BackwardsMatrix = root.getDocument().getPetriNet().getBackwardsIMatrix();
        int cont = 1;
        for(Integer[] group : groups)
        {
            Print.print_int_array(group,"Grupo "+cont);
            ArrayList<ArrayList<Integer>> OutputArcs = new ArrayList<>();
            ArrayList<ArrayList<Integer>> InputArcs = new ArrayList<>();
            for (int j=0; j<group.length; j++)
            {
                ArrayList<Integer> auxO = new  ArrayList<>();
                for (int i : FordwardMatrix[group[j]])
                {
                    auxO.add(i);
                }
                InputArcs.add(auxO);
            }
            Print.print_arraylist_arraylist_int(InputArcs,"Inputs Grupo "+cont);

            for (int j=0; j<group.length; j++)
            {
                ArrayList<Integer> auxI = new  ArrayList<>();
                for (int i : BackwardsMatrix[group[j]])
                {
                    auxI.add(i);
                }
                OutputArcs.add(auxI);
            }
            Print.print_arraylist_arraylist_int(OutputArcs,"Outputs Grupo "+cont);
            cont++;
        }
    }
}