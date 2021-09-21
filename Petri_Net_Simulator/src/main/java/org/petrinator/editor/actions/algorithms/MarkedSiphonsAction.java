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
        Integer total_places [] = new Integer[root.getDocument().getPetriNet().getSortedPlaces().size()];
        for (int i=0;i<total_places.length;i++)
        {
            total_places[i]=i;
        }
        ArrayList<Integer> marked_places_index = new ArrayList();
        ArrayList<Integer[]> conjunto_marked_places_index = new ArrayList();
        ArrayList<ArrayList<Integer>> OutputArcs = new ArrayList<>();
        ArrayList<ArrayList<Integer>> InputArcs = new ArrayList<>();
        ArrayList<Integer> siphon_tramps = new ArrayList();

        for(int i=0; i<inicial_marking.length;i++)
        {
            if(inicial_marking[i]>0)
            {
                marked_places_index.add(i);
            }
        }
        System.out.println("----- Running Marked Siphons Analysis -----\n");
        Print.print_arraylist_int(marked_places_index,"index plazas marcadas");
        Integer[] array_marked_places_index = marked_places_index.toArray(new Integer[0]);

        //get_groups(conjunto_marked_places_index,marked_places_index,total_places);//total plazas
        get_groups(conjunto_marked_places_index,marked_places_index,array_marked_places_index);//total plazas
        Print.print_arraylist_int_array(conjunto_marked_places_index,"conjunto de plazas marcadas");

        get_inputs_and_outpus(conjunto_marked_places_index,OutputArcs,InputArcs);
        Print.print_arraylist_arraylist_int(InputArcs,"-------Groups Inputs-------");
        Print.print_arraylist_arraylist_int(OutputArcs,"-------Groups Outputs-------");
        get_siphons_and_traps(OutputArcs,InputArcs,siphon_tramps);
        Print.print_arraylist_int(siphon_tramps,"Sifones o trampas");
        return "Todo OK!";
    }
    /* arr[]  ---> Input Array
    data[] ---> Temporary array to store current combination
    start & end ---> Staring and Ending indexes in arr[]
    index  ---> Current index in data[]
    r ---> Size of a combination to be printed */
    static void combinationUtil(Integer arr[], int data[], int start,int end, int index, int r,ArrayList<Integer[]> conjunto_marked_places_index,ArrayList<Integer> marked_places_index)
    {
        // Current combination is ready to be printed, print it
        if (index == r)
        {
            Integer[] conjunto = new Integer[r];
            for (int j=0; j<r; j++)
            {
                conjunto[j]=data[j];
            }
            /*
            for (int k =0 ;k<conjunto.length;k++)
            {
                if(marked_places_index.contains(conjunto[k]))
                {
                    conjunto_marked_places_index.add(conjunto);
                    break;
                }
            }*/
            conjunto_marked_places_index.add(conjunto);//solo plazas marcadas
            return;
        }

        // replace index with all possible elements. The condition
        // "end-i+1 >= r-index" makes sure that including one element
        // at index will make a combination with remaining elements
        // at remaining positions
        for (int i=start; i<=end && end-i+1 >= r-index; i++)
        {
            data[index] = arr[i];
            combinationUtil(arr, data, i+1, end, index+1, r,conjunto_marked_places_index,marked_places_index);
        }
    }

    /*Driver function to check for above function*/
    public void get_groups (ArrayList<Integer[]> conjunto_marked_places_index,
                            ArrayList<Integer> marked_places_index,Integer total_places[])
    {
        int n = total_places.length;
        for(int r=2; r<=n;r++)
        {
            int data[] = new int[r];
            // Print all combination using temporary array 'data[]'
            combinationUtil(total_places, data, 0, n-1, 0, r, conjunto_marked_places_index,marked_places_index);
        }
    }
    public void get_inputs_and_outpus(ArrayList<Integer[]> groups,ArrayList<ArrayList<Integer>> OutputArcs,ArrayList<ArrayList<Integer>> InputArcs)
    {
        int[][] FordwardMatrix = root.getDocument().getPetriNet().getForwardIMatrix();
        int[][] BackwardsMatrix = root.getDocument().getPetriNet().getBackwardsIMatrix();
        int t = 0;

        for(Integer[] group : groups)
        {
            ArrayList<Integer> auxI = new  ArrayList<>();
            //inicializo en cero el vector de grupo
            for (int i : FordwardMatrix[0])
            {
                auxI.add(0);
            }
            //recorro las plazas del grupo
            for (int j=0; j<group.length; j++)
            {
                //recorro las transiciones de las plazas
                t=0;//iterador para saber la transicion
                for (int i : FordwardMatrix[group[j]])
                {
                    if(i==1)
                    {
                        auxI.set(t,i);
                    }
                    t++;
                }
            }
            InputArcs.add(auxI);

            ArrayList<Integer> auxO = new  ArrayList<>();
            //inicializo en cero el vector de grupo
            for (int i : BackwardsMatrix[0])
            {
                auxO.add(0);
            }
            for (int j=0; j<group.length; j++)
            {
                t=0;//iterador para saber la transicion
                for (int i : BackwardsMatrix[group[j]])
                {
                    if(i==1)
                    {
                        auxO.set(t,-i);//se guardan como -1 para sumar XOR
                    }
                    t++;
                }
            }
            OutputArcs.add(auxO);
        }
    }
    /*
    siphon_tramps = -1 :siphon
                     1 :tramps
                     0 :nothing
     */
    public void get_siphons_and_traps(ArrayList<ArrayList<Integer>> OutputArcs,ArrayList<ArrayList<Integer>> InputArcs,ArrayList<Integer> siphon_tramps)
    {
        for (int i=0;i<OutputArcs.size();i++)
        {
            //InputArcs.get(i).addAll(OutputArcs.get(i));
            ArrayList<Integer> auxS = new ArrayList();
            for (int j=0;j<InputArcs.get(i).size();j++)
            {
                auxS.add(InputArcs.get(i).get(j)+OutputArcs.get(i).get(j));
            }
            Print.print_arraylist_int(auxS,"Suma grupo "+i);
            if ((auxS.contains(-1)) && (!auxS.contains(1)))
            {
                siphon_tramps.add(-1);
            }
            else if ((auxS.contains(1)) && (!auxS.contains(-1)))
            {
                siphon_tramps.add(1);
            }
            else
                siphon_tramps.add(0);
        }
    }

}