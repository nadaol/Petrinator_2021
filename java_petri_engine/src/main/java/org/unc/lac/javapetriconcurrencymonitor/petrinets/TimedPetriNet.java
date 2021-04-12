package org.unc.lac.javapetriconcurrencymonitor.petrinets;

import java.util.Arrays;

import org.unc.lac.javapetriconcurrencymonitor.exceptions.NotInitializedPetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.PetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MArc;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MPlace;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.components.MTransition;


public class TimedPetriNet extends RootPetriNet{
	
	/**
	 * Constructs a TimedPetriNet object, which is a {@link RootPetriNet} object with added time semantics
	 * The enabled transitions are not calculated at initialization time, so
	 * before firing the first transition, they must be calculated {@link TimedPetriNet#initializePetriNet()}.
	 * Other way to start times is firing a non timed transition before a timed transition
	 * @see RootPetriNet(MPlace[], MTransition[], MArc[], Integer[], Integer[][], Integer[][], Integer[][], Boolean[][], Boolean[][], Integer[][])
	 */
	public TimedPetriNet(MPlace[] _places, MTransition[] _transitions, MArc[] _arcs, Integer[] _initialMarking,
			Integer[][] _preI, Integer[][] _posI, Integer[][] _I, Boolean[][] _inhibition, Boolean[][] _resetMatrix, Integer[][] _readerMatrix) {
		super(_places, _transitions, _arcs, _initialMarking, _preI, _posI, _I, _inhibition, _resetMatrix, _readerMatrix);
		enabledTransitions = new boolean[_transitions.length];
		Arrays.fill(enabledTransitions, false);
		this.initializedPetriNet = false;
	}

	/**
	 * Fires the transition specified by transitionIndex and updates the enabled transitions with their timestamps.
	 * If the petri net was not initialized before calling this method, {@link NotInitializedPetriNetException} is thrown.
	 * If the fire time is before the timespan, FiringBeforeTimespanException is thrown,
	 * and if it's after the timespan, FiringAfterTimespanException is thrown
	 * @param transitionIndex The index of the transition to be fired
	 * @return True if the fire was successful
	 * @throws IllegalArgumentException If the index is negative or greater than the last transition index.
	 * @throws PetriNetException If an error regarding the petri occurs, for instance if the net hasn't been initialized before calling this method.
	 * @see TimedPetriNet#initializePetriNet()
	 * @see RootPetriNet#fire(int)
	 */
	public PetriNetFireOutcome fire(int transitionIndex) throws IllegalArgumentException, PetriNetException{
		try{
			return fire(transitions[transitionIndex]);
		} catch (IndexOutOfBoundsException e){
			throw new IllegalArgumentException("Given index" + transitionIndex + "doesn't match any transition");
		}
		//Compute new enabled transitions and set new timestamp (done in super.fire)
	}

	/**
	 * Fires the specified transition and updates the enabled transitions with their timestamps.
	 * If the petri net was not initialized before calling this method, {@link NotInitializedPetriNetException} is thrown.
	 * If the fire time is before the timespan, FiringBeforeTimespanException is thrown,
	 * and if it's after the timespan, FiringAfterTimespanException is thrown
	 * @param t The transition to be fired
	 * @return True if the fire was successful
	 * @throws PetriNetException If an error regarding the petri occurs, for instance if the net hasn't been initialized before calling this method.
	 * @throws IllegalArgumentException If the given transition is null or its index doesn't match any existing transition
	 * @see TimedPetriNet#initializePetriNet()
	 * @see RootPetriNet#fire(MTransition)
	 */
	public PetriNetFireOutcome fire(final MTransition t) throws IllegalArgumentException, PetriNetException{
		if(t == null){
			throw new IllegalArgumentException("Tried to fire null transition");
		}
		long fireTime = System.currentTimeMillis();
		if(t.isBeforeTimeSpan(fireTime)){
			return PetriNetFireOutcome.TIMED_BEFORE_TIMESPAN;
		}
		else if (!t.insideTimeSpan(fireTime)){
			return PetriNetFireOutcome.TIMED_AFTER_TIMESPAN;
		}
		return super.fire(t);
	}

	public boolean[] getEnabledTransitions(){
		return this.enabledTransitions;
	}
	
	/**
	 * Computes the enabled transitions, setting the enable times for new enabled transitions only
	 * If a transition was already enabled, its timespan is not updated
	 * @return the boolean array with enabled transitions
	 * @see RootPetriNet#computeEnabledTransitions()
	 */
	protected final boolean[] computeEnabledTransitions(){
		boolean[] _enabledTransitions = new boolean[transitions.length];
		for(MTransition t : transitions){
			int transitionIndex = t.getIndex();
			boolean transitionEnabled = isEnabled(t);
			_enabledTransitions[transitionIndex] = transitionEnabled;
			if(t.getTimeSpan() != null){
				//Check if the enabled transition was enabled by this fire.
				if (transitionEnabled && !enabledTransitions[transitionIndex]){
					t.getTimeSpan().setEnablingTime(System.currentTimeMillis());
				}
			}
		}
		return _enabledTransitions;
	}
	
	public boolean isTimedPetriNetInitialized() {
		return initializedPetriNet;
	}

}
