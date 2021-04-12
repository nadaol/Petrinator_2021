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

import org.apache.commons.math3.distribution.*;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Leandro Asson leoasson at gmail.com
 */
public abstract class TransitionNode extends Node implements Cloneable
{
    //initial state
    private String behavior = "<F,I,(!none)>";
    private boolean automatic = false;
    private boolean informed = true;
    private boolean enablewhentrue = false;
    private boolean timed = false;
    private String guard = "none";
    private double rate = 1.0;
    private String distribution = "Exponential";
    private double var1 = 1.0;
    private double var2= 1.0;
    private String label_var1 = "Rate (λ)";
    private String label_var2 = " ";
    private int time = 0;

    public Set<PlaceNode> getConnectedPlaceNodes()
    {
        Set<PlaceNode> connectedPlaceNodes = new HashSet<PlaceNode>();
        for (ArcEdge arc : getConnectedArcEdges()) {
            connectedPlaceNodes.add(arc.getPlaceNode());
        }
        return connectedPlaceNodes;
    }

    /**
     * Returns the behavior.
     *
     * @return the behavior.
     */
    public String getBehavior()
    {
        return behavior;
    }

    /**
     * Sets a new behavior.
     *
     * @param behavior - behavior to set.
     */
    public void setBehavior( String behavior)
    {
        this.behavior = behavior;
    }

    /**
     * If the transition is automatic return true, else return false.
     * @return automatic.
     */
    public boolean isAutomatic()
    {
        return automatic;
    }

    /**
     * If the transition is informed return true, else return false.
     * @return informed.
     */
    public boolean isInformed()
    {
        return informed;
    }

    /**
     * If the transition is enable when true return true, else return false.
     * @return Enablewhentrue.
     */
    public boolean isEnablewhentrue()
    {
        return enablewhentrue;
    }

    /**
     * If the transition is timed return true, else return false.
     * @return timed.
     */
    public boolean isTimed()
    {
        return timed;
    }

    /**
     * Return the name of the guard
     * @return guard.
     */
    public String getGuard()
    {
        return guard;
    }

    /**
     * Return the rate
     *
     * @return rate
     */
    public double getRate()
    {
        return rate;
    }

    /**
     * Return the var1
     *
     * @return var1
     */
    public double getVar1()
    {
        return var1;
    }

    /**
     * Return the var2
     *
     * @return var2
     */
    public double getVar2()
    {
        return var2;
    }

    /**
     * Return the label of var1
     *
     * @return label_var1
     */
    public String getLabelVar1()
    {
        return label_var1;
    }

    /**
     * Return the label of var2
     *
     * @return label_Var2
     */
    public String getLabelVar2()
    {
        return label_var2;
    }

    /**
     * Return the index of distribution
     *
     * @return indexofdistribution
     */
    public int getIndexDistribution()
    {
        if (distribution.equals("Exponential"))
            return 0;
        else if (distribution.equals("Normal"))
            return 1;
        else if (distribution.equals("Cauchy"))
            return 2;
        else
            return 3;
    }

    /**
     * Return the name of distribution
     *
     * @return distribution
     */
    public String getDistribution() { return distribution; }

    /**
     * Sets a new state.
     *
     * @param automatic - state to set.
     */
    public void setAutomatic(boolean automatic)
    {
        this.automatic = automatic;
    }

    /**
     * Sets a new state.
     *
     * @param informed - state to set.
     */
    public void setInformed(boolean informed)
    {
        this.informed = informed;
    }

    /**
     * Sets a new name of guard.
     *
     * @param guard - state to set.
     */
    public void setGuard(String guard)
    {
        this.guard = guard;
    }

    /**
     * Sets a new state.
     *
     * @param enablewhentrue - state to set.
     */
    public void setEnableWhenTrue(boolean enablewhentrue)
    {
        this.enablewhentrue = enablewhentrue;
    }

    /**
     * Sets a new rate.
     *
     * @param rate - state to set.
     */
    public void setRate(double rate)
    {
        this.rate = rate;
    }

    /**
     * Set time.
     *
     * @param timed - time to set.
     */
    public void setTime(boolean timed)
    {
        this.timed = timed;
    }

    /**
     * Set distribution.
     *
     * @param distribution - distribution to set.
     */
    public void setDistribution(String distribution)
    {
        this.distribution = distribution;
    }

    /**
     * Set var1.
     *
     * @param var1 - var1 to set.
     */
    public void setVar1(double var1)
    {
        this.var1 = var1;
    }

    /**
     * Set var2.
     *
     * @param var2 - var2 to set.
     */
    public void setVar2(double var2)
    {
        this.var2 = var2;
    }

    /**
     * Set label of var1.
     *
     * @param label_var1 - label of var1 to set.
     */
    public void setLabelvar1(String label_var1)
    {
        this.label_var1 = label_var1;
    }

    /**
     * Set label of var2.
     *
     * @param label_var2 - label of var2 to set.
     */
    public void setLabelVar2(String label_var2)
    {
        this.label_var2 = label_var2;
    }

    /**
     * Generates behavior based on the selected configuration.
     * @param automatic value that determines if the transition is automatic.
     * @param informed value that determines if the transition is informed.
     * @param guardValue Name of the guard.
     * @param enablewhentrue Initial State of the guard.
     * The syntax of the behavior is the following:
     * &lt;automatic,informed,(~guard_name)&gt;
     * where:
     * automatic can be A for the automatic transition or F for fired transition.
     * informed can be I for the informed transition or N for non-informed transition.
     * guard is the name of the guard associated on this transition.
     * Guards can be shared by any amount of transitions and can be negated using ! or ~ token before the guard name.
     * The default values are:
     * automatic: F
     * informed: I
     * guard: none
     * initialState: false
     */
    public void generateBehavior(boolean automatic, boolean informed, String guardValue, boolean enablewhentrue) {
        String behavior;
        String statusAutomatic;
        String statusInformed;
        String statusEnablewhentrue;
        if (automatic) {
            statusAutomatic = "A";
        } else {
            statusAutomatic = "F";
        }

        if (informed) {
            statusInformed = "I";
        } else {
            statusInformed = "N";
        }

        if (enablewhentrue) {
            statusEnablewhentrue = "";
        } else {
            statusEnablewhentrue = "!";
        }
        behavior = "<" + statusAutomatic + "," + statusInformed + "," + "(" + statusEnablewhentrue + guardValue + ")" + ">";
        setBehavior(behavior);
    }

    public void setTime(int time)
    {
        this.time = time;
    }

    public int getTime()
    {
        return time;
    }
}
