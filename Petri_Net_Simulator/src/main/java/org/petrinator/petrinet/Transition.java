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

import java.awt.Color;
import java.awt.Graphics;

import org.petrinator.util.GraphicsTools;
import org.petrinator.util.GraphicsTools.HorizontalAlignment;
import org.petrinator.util.GraphicsTools.VerticalAlignment;

/**
 * Represents a transition in Petri net
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public class Transition extends TransitionNode implements Cloneable {

	static boolean showBehavior = false;
	private boolean waiting = false;
	Graphics g = null;
	
    @Override
    public void draw(Graphics g, DrawingOptions drawingOptions)
    {
        g.setColor(Color.white);
        g.fillRect(getStart().x, getStart().y, getWidth(), getHeight());
        g.setColor(color);
        g.drawRect(getStart().x, getStart().y, getWidth() - 1, getHeight() - 1);
        if(super.isTimed())
        {
            int rectanglesGap = 5;
            g.drawRect(getStart().x + rectanglesGap, getStart().y + rectanglesGap, getWidth() - 1 - 2 * rectanglesGap, getHeight() - 1 - 2 * rectanglesGap);
        }
        drawLabel(g);
    }

    @Override
    protected void drawLabel(Graphics g) {
        this.g = g;
        if (getLabel() != null && !getLabel().equals("")) {
//          GraphicsTools.drawString(g, getLabel(), getCenter().x, getCenter().y, HorizontalAlignment.center, VerticalAlignment.center, new Font("Times", Font.BOLD, 24));
            GraphicsTools.drawString(g, getLabel(), getCenter().x, getEnd().y, HorizontalAlignment.center, VerticalAlignment.top);
        }
        if(showBehavior)
        {
            GraphicsTools.drawString(g, getBehavior(), getCenter().x, getEnd().y+10, HorizontalAlignment.center, VerticalAlignment.top);
        }
        if(this.isTimed() && waiting)
        {
            g.setColor(new Color(200, 0, 0));
            GraphicsTools.drawString(g, Integer.toString(this.getTime()) + " ms", getCenter().x, getEnd().y-48, HorizontalAlignment.center, VerticalAlignment.top);
        }

    }
    
    public void setShowBehavior(boolean s)
    {
    	showBehavior = s;
    }

    public void setWaiting(boolean b)
    {
        waiting = b;
    }

    public boolean isWaiting()
    {
        return waiting;
    }
    	
}
