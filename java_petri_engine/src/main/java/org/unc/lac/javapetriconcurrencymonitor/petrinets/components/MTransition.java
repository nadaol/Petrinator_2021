/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.unc.lac.javapetriconcurrencymonitor.petrinets.components;

import org.javatuples.Pair;
import org.apache.commons.math3.distribution.*;

public class MTransition extends PetriNode{
	
	private Label label;
	private TimeSpan interval;
	private String guardName;
	private boolean guardEnablingValue;

	/** Information for stochastic distribution **/
	double var1;
	double var2;
	double time = 0;
	String distribution = "";
	
	/** This is true for a timed transition only **/
	private boolean timed;
	
	/**
	 * Constructor for transitions with time and guard
	 * @param _id The transition id
	 * @param _label The transition label object
	 * @param _index The transition index. It must match the petri's column which correspond to this transition 
	 * @param _interval TimeSpan object for the timed transition interval
	 * @param _guard Pair<String,Boolean> name for the variable used as guard for this transition and value that enables the guard.
	 * @param _name A User-defined name. Empty is not allowed
	 * @throws IllegalArgumentException When _index is negative
	 */
	public MTransition(String _id, Label _label, int _index, TimeSpan _interval, Pair<String, Boolean> _guard, String _name) throws IllegalArgumentException{
		super(_id, _index, _name);
		this.label = _label;
		this.interval = _interval;
		if(_guard == null){
			_guard = new Pair<String, Boolean>("", false);
		}
		this.guardName = _guard.getValue0() == null ? "" : _guard.getValue0();
		this.guardEnablingValue = _guard.getValue1() == null ? false : _guard.getValue1();
		
		this.timed = interval != null;
	}

	/**
	 * Constructor for stochastic transitions
	 */
	public MTransition(String _id, Label _label, int _index, String _dist, double _var1, double _var2, Pair<String, Boolean> _guard, String _name) throws IllegalArgumentException{
		super(_id, _index, _name);
		this.label = _label;
		this.var1 = _var1;
		this.var2 = _var2;
		this.distribution = _dist;
		if(_guard == null){
			_guard = new Pair<String, Boolean>("", false);
		}
		this.guardName = _guard.getValue0() == null ? "" : _guard.getValue0();
		this.guardEnablingValue = _guard.getValue1() == null ? false : _guard.getValue1();

		this.timed = interval != null;

		System.out.println("Transition " + _id + " has distribution " + distribution + " with values " + var1 + " and " + var2);
	}
	
	/**
	 * Constructor for transitions with guard
	 * @param _id The transition id
	 * @param _label The transition label object
	 * @param _index The transition index. It must match the petri's column which correspond to this transition
	 * @param _name A User-defined name. Empty is not allowed
	 * @param _guard Pair<String,Boolean> name for the variable used as guard for this transition and value that enables the guard.
	 * @throws IllegalArgumentException When _index is negative
	 */
	public MTransition(String _id, Label _label, int _index, Pair<String, Boolean> _guard, String _name) throws IllegalArgumentException{
		this(_id, _label, _index, null, _guard, _name);
	}
	
	/**
	 * This constructor is intended for timed transitions only. If this transition is not timed use {@link MTransition(String, Label, int)}
	 * @param _id The transition id
	 * @param _label The transition label object
	 * @param _index The transition index. It must match the petri's column which correspond to this transition 
	 * @param _interval TimeSpan object for the timed transition interval
	 * @param _name A User-defined name. Empty is not allowed
	 * @throws IllegalArgumentException When _index is negative
	 */
	public MTransition(String _id, Label _label, int _index, TimeSpan _interval, String _name) throws IllegalArgumentException{
		this(_id, _label, _index, _interval, null, _name);
	}
	
	/**
	 * Simple constructor for Transition without time nor guard
	 * @param _id The transition id
	 * @param _label The transition label object
	 * @param _index The transition index. It must match the petri's column which correspond to this transition
	 * @param _name A User-defined name. Empty is not allowed
	 * @throws IllegalArgumentException When _index is negative
	 */
	public MTransition(String _id, Label _label, int _index , String _name) throws IllegalArgumentException{
		this(_id, _label, _index, (TimeSpan) null, null, _name);
	}
	
	/**
	 * @return the transition's label
	 */
	public Label getLabel() {
		return label;
	}
	
	/**
	 * @return the transition's interval
	 */
	public TimeSpan getTimeSpan() {
		return interval;
	}

	/**
	 * @return the transition's rate
	 */
	public double getRate() {
		return 0;
	}

	
	/**
	 * @return the name for this transition's guard. Empty string if none
	 */
	public String getGuardName() {
		return guardName;
	}
	
	/**
	 * @return true if the guard is set by low level
	 */
	public boolean getGuardEnablingValue() {
		return guardEnablingValue;
	}
	
	/**
	 * @return true if the transition has a guard
	 */
	public boolean hasGuard() {
		return !guardName.isEmpty();
	}
	
	/**
	 * @return True if the transition is timed.
	 */
	public boolean isTimed() {
		return timed;
	}
	
	/**
	 * Tests whether timestamp is inside this transition's span for timed transitions.
	 * For non-timed transitions this method always returns true;
	 * @param timestamp the time in the format given by {@link System#currentTimeMillis()}
	 * @return true is timestamp is inside the span
	 */
	public boolean insideTimeSpan(long timestamp){
		if(isTimed()){
			return interval.inTimeSpan(timestamp);
		}
		
		return true;
	}
	
	/**
	 * Tests whether timestamp is before this transition's span for timed transitions.
	 * For non-timed transitions this method always returns false;
	 * @param timestamp the time in the format given by {@link System#currentTimeMillis()}
	 * @return true is timestamp is before the span
	 */
	public boolean isBeforeTimeSpan(long timestamp){
		if(isTimed()){
			return interval.isBeforeTimeSpan(timestamp);
		}
		
		return false;
	}
	
	/**
	 * @return The enabling timestamp. For non-timed transitions this method returns zero.
	 */
	public long getEnablingTime(){
		if(isTimed()){
			return interval.getEnablingTime() + interval.getTimespanBeginning();
		}
		
		return 0;
	}

	public void setNewTime(double time)
	{
		this.time = time;
	}

	public double getLastTime()
	{
		return time;
	}

	public double getVar1() { return var1; }

	public double getVar2() { return var2; }

	public double generateSample()
	{
		switch (distribution) {

			case "Exponential": {
				ExponentialDistribution transitionDistribution = new ExponentialDistribution(1 / var1);
				return transitionDistribution.sample();
			}
			case "Normal": {
				NormalDistribution transitionDistribution = new NormalDistribution(var1, var2);
				return transitionDistribution.sample();
			}
			case "Cauchy": {
				CauchyDistribution transitionDistribution = new CauchyDistribution(var1, var2);
				return transitionDistribution.sample();
			}
			case "Uniform": {
				UniformRealDistribution transitionDistribution = new UniformRealDistribution(var1, var2);
				return transitionDistribution.sample();
			}
			case "Log-Nomal": {
				LogNormalDistribution transitionDistribution = new LogNormalDistribution(var1, var2);
				return transitionDistribution.sample();
			}
			default:
				break;
		}
		return 0;
	}

}
