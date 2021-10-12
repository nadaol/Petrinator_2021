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

import org.hamcrest.Matcher;
import org.petrinator.editor.Root;
import org.petrinator.editor.actions.algorithms.reachability.CRTree;
import org.petrinator.editor.filechooser.*;
import org.petrinator.petrinet.Marking;
import org.petrinator.util.GraphicsTools;
import org.petrinator.util.Print;
import org.petrinator.util.Save;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.EscapableDialog;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.modules.minimalSiphons.MinimalSiphons;
import pipe.views.PetriNetView;
import java.util.regex.Pattern;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.net.URLDecoder;

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
            String s = "<h2>Inicial and Deadlock Marked Siphons</h2>";

            if (sourceDataLayer == null) {
                return;
            }
            if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
            {
                s += "Invalid net!";
            } else {
                try {
                    s += get_incial_deadlock_marked_siphons();
                    //s += getMarkedSiphons();
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

    // Get the group of siphons that causes the closest deadlock
    public String get_incial_deadlock_marked_siphons()
    {
        int inicial_marking[] = root.getDocument().getPetriNet().getInitialMarking().getMarkingAsArray()[1];
        CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);
        //sifones iniciales
        PetriNetView sourceDataLayer = new PetriNetView(Save.get_Current_JarPath(SupervisionAction.class, root, results) + "/tmp/tmp.pnml");
        MinimalSiphons siphonsAlgorithm = new MinimalSiphons();
        //System.out.println(siphonsAlgorithm.analyse(sourceDataLayer));
        
        // Get all siphons
        Vector<boolean[]> siphons = get_all_siphons(siphonsAlgorithm,sourceDataLayer,inicial_marking.length);

        // Filter siphons to get the initially marked ones (those that generates deadlocks)
        String output = "<h3>Minimal inicial marked siphons</h3>";
        Vector<boolean[]> InicialMarkedSiphons = new Vector<boolean[]>();
        Date start_time = new Date();
        output += get_marked_siphons(inicial_marking,InicialMarkedSiphons,siphons,true);
        Date stop_time = new Date();
        double etime = (double)(stop_time.getTime() - start_time.getTime()) / 1000.0D;
        output +=  "<br>Analysis time: " + etime + "s";

        //sifones marcados finales
        if(statesTree.hasDeadlock())
        {
            start_time = new Date();
            output+= "<h3>Problematic siphons for each deadlock State</h3>";
            
            ArrayList<int[]> Deadlock_markings = get_deadlock_states();
            for (int[] marking : Deadlock_markings) 
            {
                Vector<boolean[]> MarkedFinalSiphons = new Vector<boolean[]>();
                Vector<boolean[]> UnMarkedFinalSiphons = new Vector<boolean[]>(InicialMarkedSiphons);

                output+= "<h4>Deadlock : " + Arrays.toString(marking) + " </h4>";
                output += get_marked_siphons(marking,MarkedFinalSiphons,UnMarkedFinalSiphons,false);      
            }
            stop_time = new Date();
            etime = (double)(stop_time.getTime() - start_time.getTime()) / 1000.0D;
            output +=  "<br>Analysis time: " + etime + "s";

        }

        

        return output;
    }

    // Get all siphons
    private Vector<boolean[]> get_all_siphons(MinimalSiphons siphonsAlgorithm,PetriNetView sourceDataLayer,int Nplaces)
    {
        String log = siphonsAlgorithm.analyse(sourceDataLayer);
        Vector<boolean[]> siphons_bool = new Vector<boolean[]>();

        // Deadlock on S30 [1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1]</h3>
        String regular_expression = ".*Minimal traps" ;
        Pattern pattern = Pattern.compile(regular_expression);
        java.util.regex.Matcher matcher = pattern.matcher(log);
        if(matcher.find())
        {
            log = matcher.group();
        }
            
        
        regular_expression = "(\\{([^}]+)\\})" ;
        pattern = Pattern.compile(regular_expression);
        matcher = pattern.matcher(log);

        // Find all matches
        while (matcher.find()) 
        {
            // Get the matching string
            String match = matcher.group(1);
            String [] markingS =match.split(", ");

            for(int i =0; i<markingS.length;i++)
            {
                if(i==0) markingS[i] = markingS[i].substring(2);
                else markingS[i] = markingS[i].substring(1);

                if(i==markingS.length-1) markingS[i]= markingS[i].split(" ")[0];
            }
           
            //Print.print_string_array(markingS,"String siphons");
            int[] marking = Arrays.stream(markingS).mapToInt(Integer::parseInt).toArray();
            //Print.print_int_array(marking, "marking");
            
            boolean[] siphon = new boolean[Nplaces];

            for(int i =0; i<marking.length;i++)
            {
                siphon[marking[i]-1] = true;
            }
            siphons_bool.add(siphon);

            
        }

        //Print.print_boolean_vector(siphons_bool,"Boolean array");

        return siphons_bool;
    }

    // Obtains all deadlock states markings
    private ArrayList<int[]> get_deadlock_states()
    {
        CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);
        String log = statesTree.getTreeLog();
        // Deadlock on S30 [1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1]</h3>
        String regular_expression = "(Deadlock on S[0-9]* )(\\[)([0-9, ]*)" ;
        Pattern pattern = Pattern.compile(regular_expression);
        java.util.regex.Matcher matcher = pattern.matcher(log);
        ArrayList<int[]> Deadlock_markings = new ArrayList<>();

        // Find all matches
        while (matcher.find()) 
        {
            // Get the matching string
            String match = matcher.group(3);
            String [] markingS =match.split(", ");
            int[] marking = Arrays.stream(markingS).mapToInt(Integer::parseInt).toArray();
            Deadlock_markings.add(marking);
        }
        return Deadlock_markings;
    }

    // Filter from siphons vector only the ones that are marked
    public String get_marked_siphons(int inicial_marking[],Vector<boolean[]> MarkedSiphons,Vector<boolean[]> siphons,boolean marked)
    {
        PetriNetView sourceDataLayer = new PetriNetView(Save.get_Current_JarPath(SupervisionAction.class, root, results) + "/tmp/tmp.pnml");
        ArrayList<Integer> marked_places_index = new ArrayList<Integer>();
        for (int i = 0; i < inicial_marking.length; i++) {
            if (inicial_marking[i] > 0) {
                marked_places_index.add(i);
            }
        }
        
        Print.print_boolean_vector(siphons,"All siphons");
        Print.print_arraylist_int(marked_places_index,"marked places indexes");
        
        for (int i = 0; i < marked_places_index.size(); i++)
        {
            //System.out.println("Marcado :"+marked_places_index.get(i));
            Iterator iterator = siphons.iterator();
            while (iterator.hasNext()) {
                boolean[] array = (boolean[]) iterator.next();
                if (array[marked_places_index.get(i)]) {

                    if (!MarkedSiphons.contains(array)) {
                        MarkedSiphons.add(array);
                        //ya contiene una plaza marcada y no es necesario volver a analizar
                        iterator.remove();
                    }

                }
            }
        }
        String output;
        if(marked)
            output = Print.toString(MarkedSiphons, sourceDataLayer);
        else
            output = Print.toString(siphons, sourceDataLayer);

        return output ;
    }

    // Get the transitions path to the closest deadlock state
    public int[] get_deadlock_path_vector(int inicial_marking [],CRTree statesTree)
    {
        int deadlock_marking [] = new int[inicial_marking.length];

        String[] transitions = statesTree.getShortestPathToDeadlock().split(" => ");
        int[][] incidenceMatrix = root.getDocument().getPetriNet().getIncidenceMatrix();
        int[] path_vector = new int[incidenceMatrix[0].length];

        for (int j=0; j<transitions.length -1 ; j++)
        {
            path_vector[Integer.valueOf(transitions[j].substring(1))-1]+=1;
        }
        //Print.print_int_array(path_vector,"vector de disparos final");

        int[][] matrix_mult_vector = get_matrix_mult_vector(incidenceMatrix,path_vector);
        //Print.print_matrix(matrix_mult_vector, "Incidencia x vetor de disparos");

        for(int i = 0 ; i < inicial_marking.length; i++)
        {
            //System.out.println(inicial_marking[i]+" + " +matrix_mult_vector[i][0]);
            deadlock_marking[i] = inicial_marking[i] + matrix_mult_vector[i][0];
        }
        //Mj+1 = Mj + I ∗ σ.
        //Print.print_int_array(deadlock_marking,"Marcado deadlock");

        return deadlock_marking;
    }

    // Multiplication of integer Matrix and vector arrays
    public int[][] get_matrix_mult_vector(int[][] matrix,int[] v)
    {
        int fil_m1 = matrix.length;
        int col_m1 = matrix[0].length;
        int fil_m2 = v.length;
        int col_m2 = 1;
        int[][] multiplicacion = new int[fil_m1][col_m2];

        if (col_m1 != fil_m2)
            throw new RuntimeException("No se pueden multiplicar las matrices");

        for (int x=0; x < multiplicacion.length; x++)
        {
            for (int y=0; y < multiplicacion[x].length; y++)
            {
                // El nuevo bucle suma la multiplicación de la fila por la columna
                for (int z=0; z<col_m1; z++) {
                    multiplicacion [x][y] += matrix[x][z]*v[z];
                }
            }
        }
        return multiplicacion;
    }
    
    // Brute force algotithm to find marked siphons (Finds siphons from groups filtering all places in the Net with at least one mark)
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

    // Generates All posible distinct combinations from array 'arr' of size r
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

    // Finds all possible distinct combinatios of total_places
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

    // Classify siphons and traps from inputs and outputs arc groups
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