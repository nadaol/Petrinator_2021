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
package org.petrinator.editor.canvas;

import java.awt.Graphics;
import java.awt.event.MouseEvent;

/**
 *
 * @author Martin Riesz <riesz.martin at gmail.com>
 */
public interface Feature {

    public void drawForeground(Graphics g);

    public void drawMainLayer(Graphics g);

    public void drawBackground(Graphics g);

    public void mousePressed(MouseEvent event);

    public void mouseDragged(int x, int y);

    public void mouseReleased(int x, int y);

    public void mouseMoved(int x, int y);

    public void setHoverEffects(int x, int y);

    public void setCursor(int x, int y);
}
