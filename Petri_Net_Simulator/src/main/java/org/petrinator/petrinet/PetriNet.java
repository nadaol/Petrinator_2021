/*
 * Copyright (C) 2008-2010 Martin Riesz <riesz.martin at gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.petrinator.petrinet;
import org.petrinator.auxiliar.MergeSort;
import org.petrinator.editor.commands.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * PetriNet class stores reference to the root subnet and manages a view of
 * currently opened subnet in form of a stack. Default view is only the root
 * subnet opened. Opening and closing subnets does not influence anything other
 * and serves only for informational purposes.
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class PetriNet {

    private Stack<Subnet> openedSubnets = new Stack<Subnet>();
    private Subnet rootSubnet;
    private Marking initialMarking = new Marking(this);
    private NodeSimpleIdGenerator nodeSimpleIdGenerator = new NodeSimpleIdGenerator(this);
    private NodeLabelGenerator nodeLabelGenerator = new NodeLabelGenerator(this);

    /**
     * Constructor. Creates a new Petri net with empty root subnet.
     */
    public PetriNet() {
        clear();
    //Generar matrix de S*S
        /*int S = 400;
        int[][] ip = new int[S][S];
        int[][] im = new int[S][S];
        int[][] rd = new int[S][S];
        int[][] in = new int[S][S];
        int[][] rs = new int[S][S];
        for(int i = 0; i< S; i++){
            for(int j = 0; j<S; j++){
                ip[i][j] = 0;
                im[i][j] = 0;
                rd[i][j] = 0;
                in[i][j] = 0;
                rs[i][j] = 0;
                if(i==j){
                    im[i][j]= 1;
                    if(i>0)
                        ip[i-1][j] = 1;
                }
            }
        }
        ip[S-1][0] = 1;
        reconstructFromMatrix(ip, im, in, rd, rs);*/
    }

    /**
     * Returns the root subnet of the Petri net. It is the only commonly used
     * method.
     *
     * @return the root subnet of the Petri net
     */
    public Subnet getRootSubnet() {
        return rootSubnet;
    }

    /**
     * Replaces the root subnet with a different one and thus destroys old
     * reference. Currently useful only for DocumentImporter.
     *
     * @param rootSubnet a subnet to replace with
     */
    public void setRootSubnet(Subnet rootSubnet) {
        this.rootSubnet = rootSubnet;
    }

    /**
     * Determines whether the are no opened subnets except the root subnet
     *
     * @return true if only root subnet is opened otherwise false
     */
    public boolean isCurrentSubnetRoot() {
        return getCurrentSubnet() == getRootSubnet();
    }

    /**
     * Returns currenly opened subnet.
     *
     * @return currenly opened subnet
     */
    public Subnet getCurrentSubnet() {
        return openedSubnets.peek();
    }

    /**
     * Replaces the root subnet with empty one and resets view so that opened
     * subnet is the new root subnet.
     */
    public void clear() {
        rootSubnet = new Subnet();
        resetView();
    }

    /**
     * Resets view, so that currently opened subnet is root subnet.
     */
    public void resetView() {
        openedSubnets.clear();
        openedSubnets.add(rootSubnet);
    }

    /**
     * Opens a subnet. Changes view, so that specified subnet is currently
     * opened. The specified subnet must be directly nested in currenly opened
     * subnet.
     *
     * @param subnet subnet to be opened
     */
    public void openSubnet(Subnet subnet) {
        openedSubnets.push(subnet);
    }

    /**
     * Closes currenly opened subnet, so that parent subnet becomes next opened.
     */
    public void closeSubnet() {
        if (!isCurrentSubnetRoot()) {
            openedSubnets.pop();
        }
    }

    /**
     * Returns a ordered collection of currently opened subnets, i.e. a path to
     * the currently opened subnet.
     *
     * @return collection of opened subnets
     */
    public Collection<Subnet> getOpenedSubnets() {
        return Collections.unmodifiableCollection(openedSubnets);
    }

    public Marking getInitialMarking() {
        return initialMarking;
    }

    @Deprecated
    public void setInitialMarking(Marking initialMarking) {
        this.initialMarking = initialMarking;
    }

    public NodeSimpleIdGenerator getNodeSimpleIdGenerator() {
        return nodeSimpleIdGenerator;
    }

    public NodeLabelGenerator getNodeLabelGenerator() {
        return nodeLabelGenerator;
    }

    public boolean hasStaticPlace() {
        for (Place place : getRootSubnet().getPlacesRecursively()) {
            if (place.isStatic()) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getSortedPlacesNames(){

        ArrayList<String> sortednames = new ArrayList<>();

        ArrayList<Node> places = getSortedPlaces(); //TODO

        for(int i=0; i<places.size(); i++){
            sortednames.add(places.get(i).getLabel());
        }

        return sortednames;
    }

    public ArrayList<String> getSortedTransitionsNames(){

        ArrayList<String> sortednames = new ArrayList<>();

        ArrayList<Node> transitions = getSortedTransitions(); //TODO

        for(int i=0; i<transitions.size(); i++){
            sortednames.add(transitions.get(i).getLabel());
        }

        return sortednames;
    }
    
    /*
     * Agregado. Calcular matriz de incidencia a partir de la subnet.
     */
    public int[][] getIncidenceMatrix()
    {

    	ArrayList<Node> sortedPlaces = getSortedPlaces();
    	ArrayList<Node> sortedTransitions = getSortedTransitions();

        int iMinus [][] = getBackwardsIMatrix().clone();
        int iPlus [][] = getForwardIMatrix().clone();

        int I [][] = new int [sortedPlaces.size()][sortedTransitions.size()];
        for(int i=0; i<getRootSubnet().getPlaces().size(); i++)
        {
       	 for(int j=0; j<getRootSubnet().getTransitions().size(); j++)
       		 I[i][j] = iPlus[i][j] - iMinus[i][j];
        }

        return I;
    }

    public int[][] getForwardIMatrix(){

        ArrayList<Node> sortedPlaces = getSortedPlaces();
        ArrayList<Node> sortedTransitions = getSortedTransitions();

        /*
         * Calculo I+
         */
        int iPlus [][]  = new int [sortedPlaces.size()][sortedTransitions.size()];

        for (int i=0; i<sortedPlaces.size(); i++) {

            HashSet<Arc> arcstoNode = (HashSet<Arc>) sortedPlaces.get(i).getConnectedArcsToNode();
            for(Arc a : arcstoNode)
            {
                if(a.getType().equals("regular"))
                    iPlus[i][sortedTransitions.indexOf(a.getSource())] = a.getMultiplicity();
            }

        }

        return iPlus;

    }

    public int[][] getBackwardsIMatrix(){

        ArrayList<Node> sortedPlaces = getSortedPlaces();
        ArrayList<Node> sortedTransitions = getSortedTransitions();

        /*
         * Calculo I-
         */
        int iMinus [][]  = new int [sortedPlaces.size()][sortedTransitions.size()];
        for (int i=0; i<sortedPlaces.size(); i++) {

            HashSet<Arc> arcsFromNode = (HashSet<Arc>) sortedPlaces.get(i).getConnectedArcsFromNode();
            for(Arc a : arcsFromNode)
            {
                if(a.getType().equals("regular"))
                    iMinus[i][sortedTransitions.indexOf(a.getDestination())] = a.getMultiplicity();
            }
        }

        return iMinus;

    }
    
    public int[][] getInhibitionMatrix()
    {

    	ArrayList<Node> sortedPlaces = getSortedPlaces();
    	ArrayList<Node> sortedTransitions = getSortedTransitions();
   	 
    	/*
         * Calculo H
         */
        int H [][]  = new int [sortedPlaces.size()][sortedTransitions.size()];

        for (int i=0; i<sortedPlaces.size(); i++) {

        		HashSet<Arc> arcsFromNode = (HashSet<Arc>) sortedPlaces.get(i).getConnectedArcsFromNode();
        		for(Arc a : arcsFromNode)
        		{
        			if(a.getType().equals("inhibitor"))
        			H[i][sortedTransitions.indexOf(a.getDestination())] = a.getMultiplicity();
        		}
        } 
        
       return H;
    }
    
    public int[][] getResetMatrix()
    {

    	ArrayList<Node> sortedPlaces = getSortedPlaces();
    	ArrayList<Node> sortedTransitions = getSortedTransitions();
   	 
    	/*
         * Calculo R
         */
        int R [][]  = new int [sortedPlaces.size()][sortedTransitions.size()];
        for (int i=0; i<sortedPlaces.size(); i++) {
        		HashSet<Arc> arcsFromNode = (HashSet<Arc>) sortedPlaces.get(i).getConnectedArcsFromNode();
        		for(Arc a : arcsFromNode)
        		{
        			if(a.getType().equals("reset"))
        			R[i][sortedTransitions.indexOf(a.getDestination())] = a.getMultiplicity();
        		}
        } 
        
        return R;
    }

    public int[][] getReaderMatrix()
    {



        ArrayList<Node> sortedPlaces = getSortedPlaces();
        ArrayList<Node> sortedTransitions = getSortedTransitions();

        /*
         * Calculo R
         */
        int R [][]  = new int [sortedPlaces.size()][sortedTransitions.size()];
        for (int i=0; i<sortedPlaces.size(); i++) {
            HashSet<Arc> arcsFromNode = (HashSet<Arc>) sortedPlaces.get(i).getConnectedArcsFromNode();
            for(Arc a : arcsFromNode)
            {
                if(a.getType().equals("read"))
                    R[i][sortedTransitions.indexOf(a.getDestination())] = a.getMultiplicity();
            }
        }

        return R;
    }

    public ArrayList<Node> getSortedPlaces(){

        Set<Place> allPlaces = getRootSubnet().getPlaces();
        ArrayList<Node> places = new ArrayList<Node>();

        for(Place p : allPlaces)
        {
            places.add(p);
        }

        MergeSort merge = new MergeSort();
        return merge.mergeSort(places);
    }

    public ArrayList<Node> getSortedTransitions(){

        Set<Transition>  allTransitions = getRootSubnet().getTransitions();
        ArrayList<Node> transitions = new ArrayList<Node>();

        for(Transition t : allTransitions)
        {
            transitions.add(t);
        }

        MergeSort merge = new MergeSort();
        return merge.mergeSort(transitions);
    }
    /*
     * Reconstruye el grafo con elementos Plaza y Transiciones a partir de las matrices I+ e I-.
     * Falta agregar inhibici�n y reset.
     */
    public void reconstructFromMatrix(int [][] matrixIPlus, int [][] matrixIMinus, int [][] matrixInhibition, int [][] matrixReader, int [][] matrixReset) {

        //TODO: chequear que las matrices son coherentes entre sí

    	/*
    	 * Create nodes
    	 */
    	for(int i = 0; i < matrixIPlus.length; i++)
    	{
    		AddPlaceCommand p1 = new AddPlaceCommand(rootSubnet,0,50*i, this);
        	p1.execute();	
    	}
    	
    	for(int j = 0; j < matrixIPlus[0].length; j++)
		{
			AddTransitionCommand t1 = new AddTransitionCommand(rootSubnet,100,50*j,this);
	    	t1.execute();
		}	
    	
    	for(int i = 0; i < matrixIPlus.length; i++)
    	{
    		for(int j = 0; j < matrixIPlus[0].length; j++)
    		{
    			if(matrixIPlus[i][j] != 0)
    			{
    				AddArcCommand a1 = new AddArcCommand((Place) rootSubnet.getNodeById("P"+(i+1)), (Transition) rootSubnet.getNodeById("T"+(j+1)),false);
    				a1.execute();
    				a1.getCreatedArc().setMultiplicity(matrixIPlus[i][j]);	
    			}
    			if(matrixIMinus[i][j] != 0)
    			{
    				AddArcCommand a1 = new AddArcCommand((Place) rootSubnet.getNodeById("P"+(i+1)), (Transition) rootSubnet.getNodeById("T"+(j+1)),true);
    				a1.execute();
    				a1.getCreatedArc().setMultiplicity(matrixIMinus[i][j]);
    			}
    			if(matrixInhibition[i][j] !=0){
                    AddArcCommand a1 = new AddArcCommand((Place) rootSubnet.getNodeById("P"+(i+1)), (Transition) rootSubnet.getNodeById("T"+(j+1)),true);
                    a1.execute();
                    a1.getCreatedArc().setType(Arc.INHIBITOR);
                    a1.getCreatedArc().setMultiplicity(matrixInhibition[i][j]);
                }
    			if(matrixReader[i][j] != 0){
                    AddArcCommand a1 = new AddArcCommand((Place) rootSubnet.getNodeById("P"+(i+1)), (Transition) rootSubnet.getNodeById("T"+(j+1)),true);
                    a1.execute();
                    a1.getCreatedArc().setType(Arc.READ);
                    a1.getCreatedArc().setMultiplicity(matrixReader[i][j]);
                }
                if(matrixReset[i][j] != 0){
                    AddArcCommand a1 = new AddArcCommand((Place) rootSubnet.getNodeById("P"+(i+1)), (Transition) rootSubnet.getNodeById("T"+(j+1)),true);
                    a1.execute();
                    a1.getCreatedArc().setType(Arc.RESET);
                    a1.getCreatedArc().setMultiplicity(matrixReset[i][j]);
                }
    		}
    	}

    }



}

