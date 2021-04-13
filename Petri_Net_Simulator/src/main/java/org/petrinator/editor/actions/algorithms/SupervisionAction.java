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
import org.petrinator.editor.actions.algorithms.reachability.CRTree;
import org.petrinator.petrinet.*;
import org.petrinator.util.GraphicsTools;
import pipe.gui.widgets.ButtonBar;
import pipe.gui.widgets.FileBrowser;
import pipe.gui.widgets.ResultsHTMLPane;
import pipe.utilities.math.Matrix;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;


public class SupervisionAction extends AbstractAction
{
    private static final String MODULE_NAME = "Deadlock Supervisor";
    private ResultsHTMLPane results;
    private Root root;
    private JDialog guiDialog;
    private ButtonBar analizeButton;
    private ButtonBar superviseButton;
    InvariantAction accion;

    public SupervisionAction(Root root)
    {

        this.root = root;
        putValue(NAME, MODULE_NAME);
        putValue(SHORT_DESCRIPTION, MODULE_NAME);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/deadlock.png"));

        guiDialog =  new JDialog(root.getParentFrame(), MODULE_NAME, true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        results = new ResultsHTMLPane("");
        contentPane.add(results);

        analizeButton = new ButtonBar("Analyse", new ClassifyListener(), guiDialog.getRootPane());
        superviseButton = new ButtonBar("Add Supervisor/s", new ClassifyListener(), guiDialog.getRootPane());
        contentPane.add(analizeButton);
        contentPane.add(superviseButton);
        //creo un objeto de invariantes
        accion = new InvariantAction(this.root);

    }

    public void actionPerformed(ActionEvent e)
    {

        results.setText("");

        // Disables the copy and save buttons
        results.setEnabled(false);

        // Enables classify button
        analizeButton.setButtonsEnabled(true);
        superviseButton.setButtonsEnabled(false);

        // Shows initial pane
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
    }

    /*
        ANALISYS & EXPORT

     */
    public void Runanalysis()
    {
        //JOptionPane.showMessageDialog(null, "llego al run alanisis", "Error", JOptionPane.ERROR_MESSAGE, null);
        invariantAnalysis();

    }
    /*
        INVARIANT ANALYSIS
     */
    public void invariantAnalysis()
    {
        //PetriNetView sourceDataLayer = new PetriNetView("tmp/tmp.pnml");
        accion._incidenceMatrix = new Matrix(root.getDocument().getPetriNet().getIncidenceMatrix());;
        accion._incidenceMatrix.print(0,0);
        String s = "<h2>Petri Net Invariant Analysis</h2>";

        if(!root.getDocument().getPetriNet().getRootSubnet().hasPlaces() || !root.getDocument().getPetriNet().getRootSubnet().hasTransitions())
        {
            s += "Invalid net!";
        }
        else
        {
            try
            {

                //PNMLWriter.saveTemporaryFile(sourceDataLayer,this.getClass().getName());
                s += accion.analyse();
                results.setEnabled(false);
            }
            catch(OutOfMemoryError oome)
            {
                System.gc();
                results.setText("");
                s = "Memory error: " + oome.getMessage();

                s += "<br>Not enough memory. Please use a larger heap size." + "<br>" + "<br>Note:" + "<br>The Java heap size can be specified with the -Xmx option." + "<br>E.g., to use 512MB as heap size, the command line looks like this:" + "<br>java -Xmx512m -classpath ...\n";
                results.setText(s);
                return;
            }
            catch(Exception e)
            {
                e.printStackTrace();
                s = "<br>Error" + e.getMessage();
                results.setText(s);
                return;
            }
        }
        results.setText(s);
        SaveHTML("inv");
    }
    /*
        SAVE AS HTML
     */
    public void SaveHTML(String name)
    {
        try
        {
            /*
            File defaultPath = new File(".");
            FileBrowser fileBrowser = new FileBrowser("HTML file", "html", defaultPath.getPath());
            String destFN = fileBrowser.saveFile();
            if(!destFN.toLowerCase().endsWith(".html"))
            {
                destFN += ".html";
            }*/
            String path= new File (".").getCanonicalPath()+
                    "/Modulos/Deadlock-supervisor/tmp/"+ name +".html";

            FileWriter writer = new FileWriter(new File(path));
            String output = "<html><head><style type=\"text/css\">" +
                    "body{font-family:Arial,Helvetica,sans-serif;" +
                    "text-align:center;background:#ffffff}" +
                    "td.colhead{font-weight:bold;text-align:center;" +
                    "background:#ffffff}" +
                    "td.rowhead{font-weight:bold;background:#ffffff}" +
                    "td.cell{text-align:center;padding:5px,0}" +
                    "tr.even{background:#a0a0d0}" +
                    "tr.odd{background:#c0c0f0}" +
                    "td.empty{background:#ffffff}" +
                    "</style>" + results.getText();
            writer.write(output);
            writer.close();
        }
        catch(Exception e)
        {
            System.out.println("Error saving HTML to file");
        }
    }
    /**
     * Classify button click handler
     */
    private class ClassifyListener implements ActionListener {

        public void actionPerformed(ActionEvent actionEvent)
        {

            // Checks if the net is valid
            if(!root.getDocument().getPetriNet().getRootSubnet().isValid()) {
                JOptionPane.showMessageDialog(null, "Invalid Net!", "Error", JOptionPane.ERROR_MESSAGE, null);
                return;
            }

            analizeButton.setButtonsEnabled(false);

            String s = "<h2>Deadlock and S3PR analysis</h2>";

            try {
                /*
                 * Information for boundedness, safeness and deadlock
                 */
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);

                boolean S3PR = statesTree.hasDeadlock();
                boolean Deadlock = statesTree.hasDeadlock();

                if(!(Deadlock && S3PR))
                {
                    s+="The net is not compatible with a deadlock supervision ,the net has to be S3PR and have a deadlock";
                    String[] treeInfo = new String[]{
                            "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                            "S3PR", "" + Deadlock,        // ----------------------  ADD S3PR CLASSIFICATION
                            "Deadlock", "" + S3PR
                    };
                    s += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);
                    results.setEnabled(true);
                    results.setText(s);
                    return;
                }
                superviseButton.setButtonsEnabled(true);
                String[] treeInfo = new String[]{
                        "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                        "S3PR", "" + Deadlock,        // ----------------------  ADD S3PR CLASSIFICATION
                        "Deadlock", "" + S3PR
                };

                s += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);



                if(statesTree.hasDeadlock())
                {
                    s += "<h3 style=\"margin-top:10px\">Shortest Path to Deadlock</h3>";
                    s += "<div style=\"margin-top:10px; margin-bottom:10px;\">"+statesTree.getShortestPathToDeadlock()+"</div>";
                }
                results.setEnabled(true);
                Runanalysis();
            }
            catch(OutOfMemoryError e)
            {
                System.gc();
                results.setText("");
                s = "Memory error: " + e.getMessage();

                s += "<br>Not enough memory. Please use a larger heap size." +
                        "<br>" + "<br>Note:" +
                        "<br>The Java heap size can be specified with the -Xmx option." +
                        "<br>E.g., to use 512MB as heap size, the command line looks like this:" +
                        "<br>java -Xmx512m -classpath ...\n";
                results.setText(s);
            }
            catch (StackOverflowError e){
                results.setText("An error has occurred, the net might have too many states...");
            }
            catch(Exception e)
            {
                e.printStackTrace();
                s = "<br>Error" + e.getMessage();
                results.setText(s);
            }

            results.setText(s);

        }
    };


    /**
     * State machine detection
     *
     * @return true if and only if all transitions have at most one input or output
     */
    private boolean stateMachine(PetriNet petriNet)
    {
        ArrayList<Node> sortedTransitions = petriNet.getSortedTransitions();

        for (Node transition : sortedTransitions) {

            if (transition.getConnectedArcsToNode().size() > 1 || transition.getConnectedArcsFromNode().size() > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Marked graph detection
     *
     * @return true if and only if all places have at most one input or output
     */
    private boolean markedGraph(PetriNet petriNet)
    {

        ArrayList<Node> sortedPlaces = petriNet.getSortedPlaces();

        for (Node place : sortedPlaces) {

            if(place.getConnectedArcsToNode().size() > 1 || place.getConnectedArcsFromNode().size() > 1){
                return false;
            }
        }

        return true;
    }

    /**
     * Free choice net detection
     *
     * @return true iff no places' outputs go to the same transition, unless those places both have only one output
     */
    private boolean freeChoiceNet(PetriNet petriNet)
    {
        ArrayList<Node> sortedTransitions = petriNet.getSortedTransitions();

        for (Node transition: sortedTransitions) {

            Set<Node> inputPlaces = transition.getInputNodes();

            if(inputPlaces.size() > 1){

                for(Node place: inputPlaces){
                    if(place.getConnectedArcsFromNode().size() > 1){
                        return false;
                    }
                }
            }


        }

        return true;
    }

    /**
     * Extended free choice net detection
     *
     * @return true iff no places' outputs go to the same transition, unless both places outputs are identical
     */
    private boolean extendedFreeChoiceNet(PetriNet petriNet)
    {

        ArrayList<Node> sortedTransitions = petriNet.getSortedTransitions();

        for (Node transition: sortedTransitions) {

            Set<Node> inputPlaces = transition.getInputNodes();
            Set<Node> previousOutputs = null;

            if(inputPlaces.size() > 1){

                for(Node place: inputPlaces){

                    if(previousOutputs != null){
                        if(!previousOutputs.containsAll(place.getOutputNodes())){
                            return false;
                        }
                    }
                    else {
                        previousOutputs = place.getOutputNodes();
                    }

                }
            }
        }

        return true;
    }

    /**
     * Simple net (SPL-net) detection
     *
     * @return true iff no places' outputs go to the same transition, unless one of the places only has one output
     */
    private boolean simpleNet(PetriNet petriNet)
    {

        ArrayList<Node> transitions = petriNet.getSortedTransitions();

        for(Node trans : transitions){
            boolean t_simple = false;
            Set<Node> inputs = trans.getInputNodes();
            if(inputs.size() == 1) //en el caso que la transicion tenga una sola entrada, es una simpleNet
            {
                t_simple = true;
            }
            for(Node n : inputs){
                int outputs = n.getOutputNodes().size();
                if(outputs == 1)
                    t_simple = true;
            }
            if(!t_simple){
                return false;
            }
        }
        return true;
    }

    /**
     * Extended simple net (ESPL-net) detection
     *
     * @return true iff no places' outputs go to the same transition, unless one of the places' outputs is a subset of or equal to the other's
     */
    private boolean extendedSimpleNet(PetriNet petriNet)
    {

        ArrayList<Node> sortedTransitions = petriNet.getSortedTransitions();

        for (Node transition: sortedTransitions) {

            Set<Node> inputPlaces = transition.getInputNodes();
            ArrayList<Set<Node>> placesOutputs = new ArrayList<>();

            if(inputPlaces.size() > 1){

                for(Node place: inputPlaces){
                    placesOutputs.add(place.getOutputNodes());
                }


                int largest = 0;
                int index = 0;

                for(int i=0; i<placesOutputs.size(); i++){
                    if(placesOutputs.get(i).size() > largest){
                        largest = placesOutputs.get(i).size();
                        index = i;
                    }
                }

                for (Set<Node> placesOutput : placesOutputs) {
                    if (!placesOutputs.get(index).containsAll(placesOutput)) {
                        return false;
                    }
                }

            }
        }

        return true;
    }
    /*
    FUNCIONES DUPLICADAS DE InvariantAction
     */
}
