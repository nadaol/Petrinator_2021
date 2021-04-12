package org.unc.lac.javapetriconcurrencymonitor.petrinets;

import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MArc;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MPlace;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MTransition;

import java.util.Arrays;

public class PlaceTransitionPetriNet extends RootPetriNet{

	/**
	 * extends the abstract class PetriNet
	 * @see RootPetriNet#PetriNet(MPlace[], MTransition[], MArc[], Integer[], Integer[][], Integer[][], Integer[][], Boolean[][], Boolean[][], Integer[][])
	 */
	public PlaceTransitionPetriNet(MPlace[] _places, MTransition[] _transitions, MArc[] _arcs, Integer[] _initialMarking,
			Integer[][] _preI, Integer[][] _posI, Integer[][] _I, Boolean[][] _inhibition, Boolean[][] _resetMatrix, Integer[][] _readerMatrix) {
		super(_places, _transitions, _arcs, _initialMarking, _preI, _posI, _I, _inhibition, _resetMatrix, _readerMatrix);
	}
	
	/**
	 * Computes all enabled transitions
	 * @return An array containing true for an enabled transition and false for a disabled one.
	 * @see RootPetriNet#computeEnabledTransitions()
	 */
	protected final boolean[] computeEnabledTransitions(){

		/*boolean[] _enabledTransitions = new boolean[transitions.length];
		for(MTransition t : transitions){
			_enabledTransitions[t.getIndex()] = isEnabled(t);
		}

		_enabledTransitions = areEnabled();
		if(!Arrays.equals(enabledTransitions, _enabledTransitions))
			System.out.println("Error!!!!!!!!");*/
		return areEnabled();
	}

}
