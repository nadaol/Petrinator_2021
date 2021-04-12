package org.unc.lac.javapetriconcurrencymonitor.monitor;

import org.unc.lac.javapetriconcurrencymonitor.errors.IllegalTransitionFiringError;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.NotInitializedPetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.exceptions.PetriNetException;
import org.unc.lac.javapetriconcurrencymonitor.monitor.policies.FirstInLinePolicy;
import org.unc.lac.javapetriconcurrencymonitor.monitor.policies.TransitionsPolicy;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.*;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.factory.PetriNetFactory;
import org.unc.lac.javapetriconcurrencymonitor.petrinets.factory.PetriNetFactory.petriNetType;

public class main 
{

	public static void main(String[] args) 
	{
		setUp();
		//System.out.println("Holis");
	}
	
	public static void setUp() 
	{
		  PetriNetFactory factory = new PetriNetFactory("/home/jna/Desktop/tmp.pnml");
		  RootPetriNet petri =  (RootPetriNet) factory.makePetriNet(petriNetType.PLACE_TRANSITION);
		  TransitionsPolicy policy = new FirstInLinePolicy();
		  PetriMonitor monitor = new PetriMonitor(petri, policy);
		  monitor.simulationRunning = true;

		  // generate my worker threads here
		  
		  Thread worker1 = new Thread( new Runnable() {
			  @Override
			  public void run() {
			    try {
			      // non-exlusive tasks
			      monitor.fireTransition("t0");
			      // do some other task
			      // maybe fire another transition if needed
			    } catch (IllegalArgumentException | NotInitializedPetriNetException e) {
			      // handle the exceptions
			    } catch (IllegalTransitionFiringError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (PetriNetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			  }
			});
			

			 Thread worker2 = new Thread( new Runnable() {
				  @Override
				  public void run() {
				    try {
				      // non-exlusive tasks
						Thread.sleep(50);
				      monitor.fireTransition("t3");
				      // do some other task
				      // maybe fire another transition if needed
				    } catch (IllegalArgumentException | NotInitializedPetriNetException e) {
				      // handle the exceptions
				    } catch (IllegalTransitionFiringError e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (PetriNetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				  }
				});

		  petri.initializePetriNet(); // never forget to initialize the petri net before using it

		// launch my worker threads here

		  // do something in the main thread or lock waiting for worker threads to finish
		  // for example, print the petri net current marking every 5 seconds
		worker1.start();
		worker2.start();

		 /* while(true)
		  {
		    try
		    {
		      Integer [] marking = petri.getCurrentMarking();
		      for(int i = 0; i < marking.length; i++)
		    	  System.out.print(marking[i] + "  ");
		      System.out.println("");
		      Thread.sleep(50);
		    } catch (InterruptedException e){}
		  }
*/
	}
}
