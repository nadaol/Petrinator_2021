package org.unc.lac.javapetriconcurrencymonitor.petrinets.components;

public class Label {
	
	private boolean automatic;
	private boolean informed;
	private boolean stochastic = false;
	
	public Label(boolean au, boolean inf){
		automatic = au;
		informed = inf;
	}

	public boolean isAutomatic() {
		return automatic;
	}

	public boolean isInformed() {
		return informed;
	}
	
	@Override
	public String toString(){
		String isInformed = informed ? "I" : "N";
		String isAutomatic = automatic ? "A" : "D";
		
		return "<" + isAutomatic + "," + isInformed + ">";
	}

	public void setStochastic(boolean sto)
	{
		stochastic = sto;
	}

	public boolean isStochastic()
	{
		return stochastic;
	}

}
