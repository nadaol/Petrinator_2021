package org.petrinator.editor.commands;

import java.util.Set;

import org.petrinator.petrinet.Node;
import org.petrinator.petrinet.Subnet;
import org.petrinator.petrinet.Transition;
import org.petrinator.util.Command;

public class SeeBehaviorCommand implements Command  {
	
	Subnet subnet;
	static boolean showBehavior = false;
	
    public SeeBehaviorCommand(Subnet subnet) {
        this.subnet = subnet;
    }
	@Override
	public void execute() 
	{
		Set<Transition> transitions = subnet.getTransitions();
		
		for(Transition t : transitions)
		{
			t.setShowBehavior(!showBehavior);
			
		}
		showBehavior = !showBehavior;
		
	}

	@Override
	public void undo() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void redo() {
		// TODO Auto-generated method stub
		
	}

}
