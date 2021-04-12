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
import pipe.gui.widgets.ResultsHTMLPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Set;


public class ClassificationAction extends AbstractAction
{
    private static final String MODULE_NAME = "Net classification";
    private ResultsHTMLPane results;
    private Root root;
    private JDialog guiDialog;
    private ButtonBar classifyButton;

    public ClassificationAction(Root root)
    {

        this.root = root;
        putValue(NAME, MODULE_NAME);
        putValue(SHORT_DESCRIPTION, MODULE_NAME);
        putValue(SMALL_ICON, GraphicsTools.getIcon("pneditor/classification16.png"));

        guiDialog =  new JDialog(root.getParentFrame(), MODULE_NAME, true);
        Container contentPane = guiDialog.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));

        results = new ResultsHTMLPane("");
        contentPane.add(results);

        classifyButton = new ButtonBar("Classify", new ClassifyListener(), guiDialog.getRootPane());
        contentPane.add(classifyButton);
    }

    public void actionPerformed(ActionEvent e)
    {

        results.setText("");

        // Disables the copy and save buttons
        results.setEnabled(false);

        // Enables classify button
        classifyButton.setButtonsEnabled(true);

        // Shows initial pane
        guiDialog.pack();
        guiDialog.setLocationRelativeTo(root.getParentFrame());
        guiDialog.setVisible(true);
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

            classifyButton.setButtonsEnabled(false);

            String s = "<h2>Petri Net Classification</h2>";

            try {


                /*
                 * Information for boundedness, safeness and deadlock
                 */
                CRTree statesTree = new CRTree(root, root.getCurrentMarking().getMarkingAsArray()[Marking.CURRENT]);

                s += "<h3>Mathematical Properties</h3>";

                String[] treeInfo = new String[]{
                        "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                        "Bounded", "" + statesTree.isBounded(),
                        "Safe", "" + statesTree.isSafe(),
                        "Deadlock", "" + statesTree.hasDeadlock()
                };

                s += ResultsHTMLPane.makeTable(treeInfo, 2, false, true, false, true);


                if(statesTree.hasDeadlock())
                {
                    s += "<h3 style=\"margin-top:10px\">Shortest Path to Deadlock</h3>";
                    s += "<div style=\"margin-top:10px; margin-bottom:10px;\">"+statesTree.getShortestPathToDeadlock()+"</div>";
                }

                s += "<h3 style=\"margin-top:20px\">Petri Net Types</h3>";

                String[] petriInfo = new String[]{
                        "&nbsp&emsp &emsp&nbsp", "&emsp&emsp&emsp",
                        "State Machine", "" + stateMachine(root.getDocument().getPetriNet()),
                        "Marked Graph", "" + markedGraph(root.getDocument().getPetriNet()),
                        "Free Choice Net", "" + freeChoiceNet(root.getDocument().getPetriNet()),
                        "Extended FCN", "" + extendedFreeChoiceNet(root.getDocument().getPetriNet()),
                        "Simple Net", "" + simpleNet(root.getDocument().getPetriNet()),
                        "Extended SN", "" + extendedSimpleNet(root.getDocument().getPetriNet()),
                        "Bounded", "" + statesTree.isBounded(),
                        "Safe", "" + statesTree.isSafe(),
                        "Deadlock", "" + statesTree.hasDeadlock()
                };

                s += ResultsHTMLPane.makeTable(petriInfo, 2, false, true, false, true);

                results.setEnabled(true);

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

}
