package org.unc.lac.javapetriconcurrencymonitor.petrinets.factory;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import org.javatuples.Quartet;
import org.javatuples.Sextet;
import org.javatuples.Triplet;
import org.unc.lac.javapetriconcurrencymonitor.errors.CannotCreatePetriNetError;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.BadPnmlFormatException;
import org.unc.lac.javapetriconcurrencymonitor.parser.PnmlParser;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.CudaPetriNet;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.RootPetriNet;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.PlaceTransitionPetriNet;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.TimedPetriNet;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MArc;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.PetriNode;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MPlace;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MTransition;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MArc.ArcType;

	/**
	 * PetriNet factory. Gets info from PNML file. Calls makePetriNet to get a PetriNet components
	 *
	 */
	public class PetriNetFactory {
		
		protected PnmlParser reader;
		
		/**
		 * Types accepted by {@link PetriNetFactory#makePetriNet(petriNetType)}
		 */
		public static enum petriNetType {
			PLACE_TRANSITION,
			TIMED,
			CUDA
		};
		
		public PetriNetFactory(String pathToPNML) throws NullPointerException{
			try {
				this.reader = new PnmlParser(pathToPNML);
			} catch (FileNotFoundException | SecurityException e) {
				e.printStackTrace();
			}
		}
		
		public PetriNetFactory(PnmlParser _reader) throws NullPointerException{
			if(_reader == null){
				throw new NullPointerException("Invalid reader argument");
			}
			this.reader = _reader;
		}
		
		/**
		 * makes and returns the petri described in the PNML file passed to the factory
		 * @return PetriNet object containing info described in PNML file
		 * @param type petri net type from enum type {@link petriNetType}
		 * @throws CannotCreatePetriNetError If any a non supported arc type is given,
		 * or if a transition that has a reset arc as input has another arc as input
		 */
		public RootPetriNet makePetriNet(petriNetType type) throws CannotCreatePetriNetError{
			
			Quartet<MPlace[], MTransition[], MArc[], Integer[]> petriComponents = pnmlInfoToPetriNetComponents();
			Sextet<Integer[][], Integer[][], Integer[][], Boolean[][], Boolean[][], Integer[][]> petriMatrices =
					petriNetComponentsToMatrices(petriComponents.getValue0(), petriComponents.getValue1(), petriComponents.getValue2());
			
			switch(type){
			case PLACE_TRANSITION:
				return new PlaceTransitionPetriNet(petriComponents.getValue0(), petriComponents.getValue1(), petriComponents.getValue2(), petriComponents.getValue3(),
						petriMatrices.getValue0(), petriMatrices.getValue1(), petriMatrices.getValue2(), petriMatrices.getValue3(), petriMatrices.getValue4(), petriMatrices.getValue5());
			case TIMED:
				return new TimedPetriNet(petriComponents.getValue0(), petriComponents.getValue1(), petriComponents.getValue2(), petriComponents.getValue3(),
						petriMatrices.getValue0(), petriMatrices.getValue1(), petriMatrices.getValue2(), petriMatrices.getValue3(), petriMatrices.getValue4(), petriMatrices.getValue5());
			case CUDA:
				return new CudaPetriNet(petriComponents.getValue0(), petriComponents.getValue1(), petriComponents.getValue2(), petriComponents.getValue3(),
						petriMatrices.getValue0(), petriMatrices.getValue1(), petriMatrices.getValue2(), petriMatrices.getValue3(), petriMatrices.getValue4(), petriMatrices.getValue5());
			default:
				throw new CannotCreatePetriNetError("Cannot create petri net from unknown type " + type);
			}
		}
		/**
		 * extracts petri net info from PNML file given when constructed and returns a 4-tuple containing
		 * places, transitions, arcs and initial marking
		 * @return a 4-tuple containig (places, transitions, arcs, initial marking)
		 * @throws CannotCreatePetriNetError If an error occurs during parsing
		 */
		protected Quartet<MPlace[], MTransition[], MArc[], Integer[]> pnmlInfoToPetriNetComponents() throws CannotCreatePetriNetError{
			try{
				Triplet<MPlace[], MTransition[], MArc[]> ret = reader.parseFileAndGetPetriComponents();
			
				return ret.add(getMarkingFromPlaces(ret.getValue0()));
			} catch (BadPnmlFormatException e){
				throw new CannotCreatePetriNetError("Error creating petriNet due to PNML error " + e.getClass().getSimpleName()
						+ ". Message: " + e.getMessage());
			}
		}
		
		/**
		 * makes and returns petri net matrices from its components
		 * @param places petri net's places
		 * @param transitions petri net's transitions
		 * @param arcs petri net's arcs
		 * @return a 5-tuple containing (Pre matrix, Post matrix, Incidence matrix, Inhibition matrix, Reset matrix)
		 * @throws CannotCreatePetriNetError If any a non supported arc type is given,
		 * or if a transition that has a reset arc as input has another arc as input
		 * @see org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MArc.ArcType
		 */
		protected Sextet<Integer[][], Integer[][], Integer[][], Boolean[][], Boolean[][], Integer[][]> petriNetComponentsToMatrices(
				MPlace[] places, MTransition[] transitions, MArc[] arcs) throws CannotCreatePetriNetError{
			final int placesAmount = places.length;
			final int transitionsAmount = transitions.length;
			Integer[][] pre = new Integer[placesAmount][transitionsAmount];
			Integer[][] pos = new Integer[placesAmount][transitionsAmount];
			Integer[][] inc = new Integer[placesAmount][transitionsAmount];
			Boolean[][] inhibition = new Boolean[placesAmount][transitionsAmount];
			Boolean[][] resetMatrix = new Boolean[placesAmount][transitionsAmount];
			Integer[][] readerMatrix = new Integer[placesAmount][transitionsAmount];
			for(int i = 0; i < placesAmount; i++){
				for(int j = 0; j < transitionsAmount; j++){
					// Since Integer default value is null
					// I need to fill the matrixes with zeros
					pre[i][j] = 0;
					pos[i][j] = 0;
					inc[i][j] = 0;
					inhibition[i][j] = false;
					resetMatrix[i][j] = false;
					readerMatrix[i][j] = 0;
				}
			}
			
			for(MArc arc : arcs){
				PetriNode source = arc.getSource();
				PetriNode target = arc.getTarget();
				ArcType type = arc.getType();
				int sourceIndex = source.getIndex();
				int targetIndex = target.getIndex();
				int arcWeight = arc.getWeight();
				switch(type){
				case NORMAL:
					if(source.getClass().equals(MPlace.class)){
						// arc goes from place to transition, let's fill the pre-incidence matrix
						pre[sourceIndex][targetIndex] = arcWeight;
						// since inc = pos - pre, here substract the arcWeight from the incidence matrix
						inc[sourceIndex][targetIndex] -= arcWeight;
					}
					else {
						// arc goes from transition to place, let's fill the post-incidence matrix
						pos[targetIndex][sourceIndex] = arcWeight;
						// since inc = pos - pre, here add the arcWeight from the incidence matrix
						inc[targetIndex][sourceIndex] += arcWeight;
					}
					break;
				case INHIBITOR:
					// source has to be a place and target a transition
					inhibition[sourceIndex][targetIndex] = true;
					break;
				case RESET:
					resetMatrix[sourceIndex][targetIndex] = true;
					break;
				case READ:
					readerMatrix[sourceIndex][targetIndex] = arcWeight;
					break;
				default:
					throw new CannotCreatePetriNetError("Arc " + type + " not supported");
				}
			}
			
			//TODO:
			// Now let's check if any transition that has a reset arc as input also has any other input arc
			// That is an illegal condition
			/*MArc[] resetArcs = Arrays.stream(arcs)
					.filter((MArc a) -> a.getType() == ArcType.RESET)
					.toArray((int size) -> new MArc[size]);
			for(MArc resetArc : resetArcs){
				int placeIndex = resetArc.getSource().getIndex();
				int transitionIndex = resetArc.getTarget().getIndex();
				for(int i = 0; i < placesAmount; i++){
					boolean anotherResetArcEntersTransition = i != placeIndex && resetMatrix[i][transitionIndex];
					boolean inhibitionArcEntersTransition = inhibition[i][transitionIndex];
					boolean normalArcEntersTransition = pre[i][transitionIndex] > 0;
					if(normalArcEntersTransition || inhibitionArcEntersTransition || anotherResetArcEntersTransition){
						throw new CannotCreatePetriNetError(
								"Cannot have another input arcs in transition " + resetArc.getTarget().getName() + ", id: " + transitionIndex + ", because there is a reset arc.");
					}
				}
			}*/
			
			return new Sextet<Integer[][], Integer[][], Integer[][], Boolean[][], Boolean[][], Integer[][]>(pre, pos, inc, inhibition, resetMatrix, readerMatrix);
		}
		
		/**
		 * gets initial marking from places array and returns an array containing the markings
		 * @param places the places to check
		 * @return place's initial marking
		 */
		protected Integer[] getMarkingFromPlaces(MPlace[] places){
			ArrayList<Integer> initialMarking = new ArrayList<Integer>(places.length);
			for(MPlace place : places){
				initialMarking.add(place.getMarking());
			}
			Integer[] ret = new Integer[initialMarking.size()];
			return initialMarking.toArray(ret);
		}
		
	}