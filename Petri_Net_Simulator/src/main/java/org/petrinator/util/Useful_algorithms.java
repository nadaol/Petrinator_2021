package org.petrinator.util;

import java.util.ArrayList;

import org.petrinator.editor.Root;
import org.petrinator.petrinet.Node;
import org.petrinator.petrinet.Place;

public class Useful_algorithms {

    public static ArrayList<ArrayList<String>> get_Conflicts(Root root,ArrayList<Integer> Control_conflicts_indexes)
    {
        ArrayList<ArrayList<String>> conflicts = new ArrayList<ArrayList<String>>();

        int[][] IncidenceMatrix = root.getDocument().getPetriNet().getIncidenceMatrix();

        ArrayList<String> transitions_labels = root.getDocument().getPetriNet().getSortedTransitionsNames();
        ArrayList<Node> places =root.getDocument().getPetriNet().getSortedPlaces();

        

        for(int place=0;place< IncidenceMatrix.length;place++)
        {

            ArrayList<String> place_out_transitions = new ArrayList<String>();

            for(int transition=0;transition< IncidenceMatrix[place].length;transition++)
            {
                
                if(IncidenceMatrix[place][transition] == -1)
                {
                    // Add tranition output to a posible conflict from place
                    place_out_transitions.add(transitions_labels.get(transition));
                }

            }

            if(place_out_transitions.size() >1)
            {
                // If the place has more than 1 transition output , add the conflict transitions
                boolean repeated=false;
                for(ArrayList<String> conflict : conflicts)
                {
                    if (conflict.equals(place_out_transitions))
                    {
                        repeated = true;
                    }
                }
                if(!repeated)
                {
                    conflicts.add(new ArrayList<String>(place_out_transitions));
                    Place place_aux = (Place) places.get(place);
                    if(place_aux.getType() == Place.CONTROL)
                    {
                        Control_conflicts_indexes.add(conflicts.size());
                    }
                }
                    
            }

            place_out_transitions.clear();
        }

        return conflicts;
    }
    
}
